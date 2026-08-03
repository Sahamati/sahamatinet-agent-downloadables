using System;
using System.Collections.Generic;
using System.Text.Json;

namespace Sahamati.SnaLib
{
    // ---- Config & Request types ----

    public class SNAConfig
    {
        public string BaseUrl { get; set; } = "";
        public int? MaxRetries { get; set; }
        public int? RetryInterval { get; set; }
        public int? MaxWait { get; set; }
        public int? Timeout { get; set; }
        public bool? LogEnabled { get; set; }
        public Action<string>? OnWarning { get; set; }
    }

    // Unset string fields default to null, not "". The SDK sends the request exactly as the
    // caller built it and lets the service decide (CLAUDE.md section 3.3) — substituting ""
    // for an omitted field would change the payload the service sees.
    public class RegisterRequest
    {
        public string EntityId { get; set; } = null!;
        public string? Secret { get; set; }
        public string? Token { get; set; }
    }

    public class AARequest
    {
        public string CallType { get; set; } = null!;
        public string Route { get; set; } = null!;
        public string PeerId { get; set; } = null!;
        public string PeerType { get; set; } = null!;
        public string CustomerId { get; set; } = null!;
        public string TxnCorId { get; set; } = null!;

        /// <summary>Accepts a raw JSON string or any object serializable by System.Text.Json.</summary>
        public object? Body { get; set; }

        public int? HttpStatus { get; set; }
        public Dictionary<string, object>? AddlAttr { get; set; }
    }

    // ---- Base response class ----

    public abstract class SNAResponse
    {
        private readonly bool _success;

        public string? ErrorMessage { get; }

        public bool IsSuccess() => _success;

        protected SNAResponse(bool success, string? errorMessage = null)
        {
            _success = success;
            ErrorMessage = errorMessage;
        }
    }

    // ---- Response classes ----

    public sealed class VersionResponse : SNAResponse
    {
        public string AgentVersion { get; }
        public string Name { get; }

        private VersionResponse(bool success, string agentVersion, string name, string? errorMessage = null)
            : base(success, errorMessage)
        {
            AgentVersion = agentVersion;
            Name = name;
        }

        internal static VersionResponse From(JsonElement data) =>
            new VersionResponse(
                true,
                JsonHelpers.GetString(data, "agentVersion", "agent_version"),
                JsonHelpers.GetString(data, "name"));

        internal static VersionResponse Error(string message) =>
            new VersionResponse(false, "", "", message);
    }

    public sealed class RegisterResponse : SNAResponse
    {
        public string Message { get; }
        public string EntityId { get; }
        public string Status { get; }

        private RegisterResponse(bool success, string message, string entityId, string status, string? errorMessage = null)
            : base(success, errorMessage)
        {
            Message = message;
            EntityId = entityId;
            Status = status;
        }

        internal static RegisterResponse From(JsonElement data) =>
            new RegisterResponse(
                true,
                JsonHelpers.GetString(data, "message"),
                JsonHelpers.GetString(data, "entity_id"),
                JsonHelpers.GetString(data, "status"));

        internal static RegisterResponse Error(string message) =>
            new RegisterResponse(false, "", "", "", message);
    }

    public sealed class TokenResponse : SNAResponse
    {
        public string AccessToken { get; }
        public long ExpiresIn { get; }
        public string? TokenType { get; }
        public long? RefreshExpiresIn { get; }
        public long? NotBeforePolicy { get; }
        public string? Scope { get; }
        public string? Ver { get; }
        public string? Timestamp { get; }
        public string? TxnId { get; }

        private TokenResponse(
            bool success,
            string accessToken,
            long expiresIn,
            string? tokenType,
            long? refreshExpiresIn,
            long? notBeforePolicy,
            string? scope,
            string? ver,
            string? timestamp,
            string? txnId,
            string? errorMessage = null)
            : base(success, errorMessage)
        {
            AccessToken = accessToken;
            ExpiresIn = expiresIn;
            TokenType = tokenType;
            RefreshExpiresIn = refreshExpiresIn;
            NotBeforePolicy = notBeforePolicy;
            Scope = scope;
            Ver = ver;
            Timestamp = timestamp;
            TxnId = txnId;
        }

        internal static TokenResponse From(JsonElement data) =>
            new TokenResponse(
                true,
                JsonHelpers.GetString(data, "accessToken"),
                JsonHelpers.GetInt64(data, "expiresIn"),
                JsonHelpers.GetOptionalString(data, "tokenType"),
                JsonHelpers.GetOptionalInt64(data, "refreshExpiresIn"),
                JsonHelpers.GetOptionalInt64(data, "notBeforePolicy"),
                JsonHelpers.GetOptionalString(data, "scope"),
                JsonHelpers.GetOptionalString(data, "ver"),
                JsonHelpers.GetOptionalString(data, "timestamp"),
                JsonHelpers.GetOptionalString(data, "txnId"));

        internal static TokenResponse Error(string message) =>
            new TokenResponse(false, "", 0, null, null, null, null, null, null, null, message);
    }

    public sealed class DispatchResponse : SNAResponse
    {
        public string Message { get; }

        private DispatchResponse(bool success, string message, string? errorMessage = null)
            : base(success, errorMessage)
        {
            Message = message;
        }

        internal static DispatchResponse From(JsonElement data) =>
            new DispatchResponse(true, JsonHelpers.GetString(data, "message"));

        internal static DispatchResponse Error(string message) =>
            new DispatchResponse(false, "", message);
    }
}
