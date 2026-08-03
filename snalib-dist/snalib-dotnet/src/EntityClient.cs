using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Sahamati.SnaLib
{
    public sealed class EntityClient
    {
        private readonly SNAClient _client;

        internal EntityClient(SNAClient client)
        {
            _client = client;
        }

        public async Task<RegisterResponse> RegisterAsync(
            RegisterRequest req,
            CancellationToken cancellationToken = default)
        {
            try
            {
                var payload = new Dictionary<string, string?> { ["entity_id"] = req.EntityId };
                // Never log secret or token — protection is here at the callsite.
                if (!string.IsNullOrEmpty(req.Token)) payload["token"] = req.Token!;
                else if (!string.IsNullOrEmpty(req.Secret)) payload["secret"] = req.Secret!;

                var (status, body) = await _client
                    .DoPostAsync("/sna/v1/entity/register", JsonSerializer.Serialize(payload), cancellationToken)
                    .ConfigureAwait(false);

                if (status != 200)
                {
                    _client.ParseError(status, body);
                }
                return RegisterResponse.From(_client.ParseJson(status, body));
            }
            catch (Exception err)
            {
                _client.LogWarn(err.Message);
                return RegisterResponse.Error(err.Message);
            }
        }

        public async Task<TokenResponse> GetTokenAsync(CancellationToken cancellationToken = default)
        {
            try
            {
                var (status, body) = await _client
                    .DoGetAsync("/sna/v1/entity/token/generate", cancellationToken)
                    .ConfigureAwait(false);

                if (status != 200)
                {
                    _client.ParseError(status, body);
                }
                return TokenResponse.From(_client.ParseJson(status, body));
            }
            catch (Exception err)
            {
                _client.LogWarn(err.Message);
                return TokenResponse.Error(err.Message);
            }
        }
    }
}
