using System.Text.Json;

namespace Sahamati.SnaLib
{
    internal static class JsonHelpers
    {
        public static string GetString(JsonElement data, string key, string? fallbackKey = null)
        {
            return GetOptionalString(data, key)
                   ?? (fallbackKey != null ? GetOptionalString(data, fallbackKey) : null)
                   ?? "";
        }

        public static string? GetOptionalString(JsonElement data, string key)
        {
            if (data.ValueKind != JsonValueKind.Object) return null;
            if (!data.TryGetProperty(key, out var v)) return null;
            if (v.ValueKind == JsonValueKind.Null || v.ValueKind == JsonValueKind.Undefined) return null;

            // A non-string value is rendered via ToString() rather than dropped. The service
            // contract says these fields are strings; if one arrives as a number the raw text
            // is more use to the caller than an empty string.
            return v.ValueKind == JsonValueKind.String ? v.GetString() : v.ToString();
        }

        public static long GetInt64(JsonElement data, string key, long defaultValue = 0)
        {
            return GetOptionalInt64(data, key) ?? defaultValue;
        }

        public static long? GetOptionalInt64(JsonElement data, string key)
        {
            if (data.ValueKind != JsonValueKind.Object) return null;
            if (!data.TryGetProperty(key, out var v)) return null;
            if (v.ValueKind != JsonValueKind.Number) return null;

            // TryGetInt64 first — exact, and no precision loss on large values.
            if (v.TryGetInt64(out var n)) return n;

            // Fall back to double for values written with a fractional part (e.g. 3600.0),
            // which TryGetInt64 rejects but the previous GetDouble-and-cast accepted.
            return v.TryGetDouble(out var d) ? (long)d : null;
        }
    }
}
