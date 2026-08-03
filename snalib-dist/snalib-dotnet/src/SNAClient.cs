using System;
using System.Diagnostics.CodeAnalysis;
using System.Globalization;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Sahamati.SnaLib
{
    public sealed class SNAClient : IDisposable
    {
        private const int DefaultMaxRetries = 3;
        private const int DefaultRetryIntervalMs = 1000;
        private const int DefaultMaxWaitMs = 15000;
        private const int DefaultTimeoutMs = 30000;

        private readonly string _baseUrl;
        private readonly bool _usable;
        private readonly int _timeoutMs;
        private readonly SnaLibLogger _logger;
        private readonly HttpClient _http;
        private int _disposed;

        public EntityClient Entity { get; }

        public AAClient AA { get; }

        // Written to be total: this constructor never throws and tolerates a null config.
        // CreateAsync's never-throw guarantee depends on that.
        private SNAClient(SNAConfig? config, SnaLibLogger logger)
        {
            _logger = logger;

            var raw = config?.BaseUrl;
            _baseUrl = string.IsNullOrWhiteSpace(raw) ? "" : raw!.Trim().TrimEnd('/');
            _usable = _baseUrl.Length > 0;

            var timeout = config?.Timeout ?? DefaultTimeoutMs;
            _timeoutMs = timeout > 0 ? timeout : DefaultTimeoutMs;

            // PooledConnectionLifetime matters because this client is meant to be created once
            // and held for the lifetime of the application (CLAUDE.md section 3.4). The default
            // handler pools connections indefinitely and never re-resolves DNS, so a service
            // that moves is never noticed until the process restarts.
            _http = new HttpClient(new SocketsHttpHandler
            {
                PooledConnectionLifetime = TimeSpan.FromMinutes(5),
            });

            Entity = new EntityClient(this);
            AA = new AAClient(this);
        }

        /// <summary>
        /// Creates a client and pings the SNA service with retry/backoff. Never throws —
        /// if the service is unreachable, a warning is logged and a usable client is still
        /// returned; subsequent API calls surface the failure as error-state responses.
        /// </summary>
        public static async Task<SNAClient> CreateAsync(
            SNAConfig config,
            CancellationToken cancellationToken = default)
        {
            SnaLibLogger logger;
            SNAClient client;

            // Construction is inside the try. A null config or null BaseUrl must produce a
            // warning and a usable client, not an exception out of the factory
            // (CLAUDE.md section 10).
            try
            {
                logger = new SnaLibLogger(config?.LogEnabled ?? false, config?.OnWarning);
                client = new SNAClient(config, logger);
            }
            catch (Exception err)
            {
                var fallback = new SnaLibLogger(false);
                fallback.Warn($"SNAClient.CreateAsync: construction failed: {err.Message}");
                return new SNAClient(null, fallback);
            }

            if (!client._usable)
            {
                logger.Warn("SNAConfig.BaseUrl is empty; every API call will return a connection error");
                return client;
            }

            var maxRetries = config!.MaxRetries ?? DefaultMaxRetries;
            var retryInterval = config.RetryInterval ?? DefaultRetryIntervalMs;
            var maxWait = config.MaxWait ?? DefaultMaxWaitMs;

            try
            {
                await client.ConnectWithRetryAsync(maxRetries, retryInterval, maxWait, cancellationToken)
                    .ConfigureAwait(false);
            }
            catch (Exception err)
            {
                logger.Warn(err.Message);
            }

            return client;
        }

        private async Task ConnectWithRetryAsync(
            int maxRetries,
            int retryInterval,
            int maxWait,
            CancellationToken cancellationToken)
        {
            var started = System.Diagnostics.Stopwatch.GetTimestamp();
            var attempt = 0;
            var interval = retryInterval > 0 ? retryInterval : DefaultRetryIntervalMs;

            while (attempt <= maxRetries)
            {
                try
                {
                    await DoPingAsync(cancellationToken).ConfigureAwait(false);
                    // Matches the Go and Java SDKs, which emit exactly this one info log on a
                    // successful initial connection.
                    _logger.Info("connected to SNA service");
                    return;
                }
                catch
                {
                    // DoGetAsync converts caller cancellation into SNAConnectionError, so the
                    // token has to be checked explicitly — otherwise a cancelled connect would
                    // keep retrying.
                    if (cancellationToken.IsCancellationRequested) break;

                    attempt++;
                    if (attempt > maxRetries) break;

                    var elapsedMs = System.Diagnostics.Stopwatch.GetElapsedTime(started).TotalMilliseconds;
                    if (elapsedMs + interval > maxWait) break;

                    try
                    {
                        await Task.Delay(interval, cancellationToken).ConfigureAwait(false);
                    }
                    catch (OperationCanceledException)
                    {
                        break;
                    }

                    interval *= 2;
                }
            }

            throw new SNAConnectionError($"service unreachable after {attempt} attempt(s): {_baseUrl}");
        }

        private async Task DoPingAsync(CancellationToken cancellationToken)
        {
            var (status, _) = await DoGetAsync("/sna/v1/ping", cancellationToken).ConfigureAwait(false);
            if (status != 200)
            {
                throw new SNAConnectionError($"ping returned HTTP {status}");
            }
        }

        /// <summary>Never throws — returns false and logs a warning if unreachable.</summary>
        public async Task<bool> PingAsync(CancellationToken cancellationToken = default)
        {
            try
            {
                await DoPingAsync(cancellationToken).ConfigureAwait(false);
                return true;
            }
            catch (Exception err)
            {
                _logger.Warn(err.Message);
                return false;
            }
        }

        public async Task<VersionResponse> VersionAsync(CancellationToken cancellationToken = default)
        {
            try
            {
                var (status, body) = await DoGetAsync("/sna/v1/version", cancellationToken).ConfigureAwait(false);
                if (status != 200)
                {
                    ParseError(status, body);
                }
                return VersionResponse.From(ParseJson(status, body));
            }
            catch (Exception err)
            {
                _logger.Warn(err.Message);
                return VersionResponse.Error(err.Message);
            }
        }

        // Internal helpers used by EntityClient and AAClient.

        internal void LogWarn(string msg) => _logger.Warn(msg);

        internal void LogInfo(string msg) => _logger.Info(msg);

        internal Task<(int Status, string Body)> DoGetAsync(string path, CancellationToken cancellationToken) =>
            SendAsync(HttpMethod.Get, path, null, cancellationToken);

        internal Task<(int Status, string Body)> DoPostAsync(string path, string body, CancellationToken cancellationToken) =>
            SendAsync(HttpMethod.Post, path, body, cancellationToken);

        private async Task<(int Status, string Body)> SendAsync(
            HttpMethod method,
            string path,
            string? jsonBody,
            CancellationToken cancellationToken)
        {
            if (!_usable) throw new SNAConnectionError("BaseUrl is not configured");
            if (Volatile.Read(ref _disposed) != 0) throw new SNAConnectionError("client has been disposed");

            // The global timeout and the caller's token are linked, so a per-request
            // CancellationToken can cut a call short without disturbing the global default.
            using var timeoutCts = new CancellationTokenSource(TimeSpan.FromMilliseconds(_timeoutMs));
            using var linked = CancellationTokenSource.CreateLinkedTokenSource(timeoutCts.Token, cancellationToken);

            try
            {
                using var request = new HttpRequestMessage(method, _baseUrl + path);
                if (jsonBody != null)
                {
                    request.Content = new StringContent(jsonBody, Encoding.UTF8, "application/json");
                }

                using var response = await _http
                    .SendAsync(request, HttpCompletionOption.ResponseContentRead, linked.Token)
                    .ConfigureAwait(false);

                var text = await response.Content.ReadAsStringAsync(linked.Token).ConfigureAwait(false);
                return ((int)response.StatusCode, text);
            }
            catch (OperationCanceledException) when (timeoutCts.IsCancellationRequested)
            {
                throw new SNAConnectionError($"request timed out after {_timeoutMs}ms: {path}");
            }
            catch (OperationCanceledException)
            {
                // Cancelled by the caller. Surfaced as a value rather than rethrown — the SDK
                // never throws, even where .NET convention would (CLAUDE.md section 10).
                throw new SNAConnectionError($"request cancelled by caller: {path}");
            }
            catch (Exception err)
            {
                throw new SNAConnectionError(err.Message);
            }
        }

        [DoesNotReturn]
        internal void ParseError(int status, string rawBody)
        {
            var message = rawBody;
            string? errorCode = null;

            try
            {
                using var doc = JsonDocument.Parse(rawBody);
                var root = doc.RootElement;
                message = JsonHelpers.GetOptionalString(root, "message") ?? rawBody;
                errorCode = JsonHelpers.GetOptionalString(root, "errorCode");
            }
            catch (JsonException)
            {
                // best-effort; leave message/errorCode as defaults
            }

            errorCode ??= status.ToString(CultureInfo.InvariantCulture);

            if (status == 400) throw new SNAValidationError(status, message, errorCode);
            if (status == 501) throw new SNANotImplementedError(status, message, errorCode);
            throw new SNAServerError(status, message, errorCode);
        }

        /// <summary>
        /// Parses a 2xx body. A malformed body is a service contract violation, not a network
        /// failure — it maps to SNAUnexpectedResponseError and retains the raw body
        /// (CLAUDE.md section 6).
        /// </summary>
        internal JsonElement ParseJson(int status, string body)
        {
            try
            {
                using var doc = JsonDocument.Parse(body);
                return doc.RootElement.Clone();
            }
            catch (JsonException err)
            {
                throw new SNAUnexpectedResponseError(
                    status,
                    $"failed to parse response body as JSON: {err.Message}",
                    body);
            }
        }

        public void Dispose()
        {
            if (Interlocked.Exchange(ref _disposed, 1) != 0) return;
            _http.Dispose();
        }
    }
}
