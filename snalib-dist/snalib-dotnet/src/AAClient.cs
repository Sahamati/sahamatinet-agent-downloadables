using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Sahamati.SnaLib
{
    public sealed class AAClient
    {
        private readonly SNAClient _client;

        internal AAClient(SNAClient client)
        {
            _client = client;
        }

        public async Task<DispatchResponse> DispatchAsync(
            AARequest req,
            CancellationToken cancellationToken = default)
        {
            try
            {
                // Accept body as a raw JSON string or any serializable object.
                // If string, parse it first — otherwise it gets embedded as a quoted string.
                object? bodyValue;
                if (req.Body is string bodyStr)
                {
                    if (string.IsNullOrWhiteSpace(bodyStr))
                    {
                        // The peer returned no payload. null, {} and an absent key are all
                        // len == 0 to SNA, and on responseIn that is the transport-failure
                        // signal. Re-encoding "" as null is lossless — JSON cannot carry a
                        // string into an object-typed field.
                        bodyValue = null;
                    }
                    else
                    {
                        try
                        {
                            using var doc = JsonDocument.Parse(bodyStr);
                            bodyValue = doc.RootElement.Clone();
                        }
                        catch (JsonException)
                        {
                            // Not JSON. Send it unchanged and let SNA reject it — the SDK
                            // does not validate, and the parsed value's type is SNA's rule.
                            bodyValue = bodyStr;
                        }
                    }
                }
                else
                {
                    bodyValue = req.Body;
                }

                var payload = new Dictionary<string, object?>
                {
                    ["callType"] = req.CallType,
                    ["route"] = req.Route,
                    ["peerId"] = req.PeerId,
                    ["peerType"] = req.PeerType,
                    ["customerId"] = req.CustomerId,
                    ["txnCorId"] = req.TxnCorId,
                    ["body"] = bodyValue,
                };
                if (req.HttpStatus.HasValue) payload["httpStatus"] = req.HttpStatus.Value;
                if (req.AddlAttr != null) payload["addlAttr"] = req.AddlAttr;

                var (status, body) = await _client
                    .DoPostAsync("/sna/v1/aa", JsonSerializer.Serialize(payload), cancellationToken)
                    .ConfigureAwait(false);

                if (status != 200)
                {
                    _client.ParseError(status, body);
                }
                return DispatchResponse.From(_client.ParseJson(status, body));
            }
            catch (Exception err)
            {
                _client.LogWarn(err.Message);
                return DispatchResponse.Error(err.Message);
            }
        }
    }
}
