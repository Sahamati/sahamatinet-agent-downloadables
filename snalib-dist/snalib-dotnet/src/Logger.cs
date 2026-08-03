using System;
using System.Collections.Generic;
using System.Text.Json;

namespace Sahamati.SnaLib
{
    public class SnaLibLogger
    {
        private readonly bool _logEnabled;
        private readonly Action<string>? _onWarning;

        public SnaLibLogger(bool logEnabled, Action<string>? onWarning = null)
        {
            _logEnabled = logEnabled;
            _onWarning = onWarning;
        }

        public void Info(string msg, params object?[] kv)
        {
            if (!_logEnabled) return;
            Write(BuildJson("info", msg, kv));
        }

        public void Warn(string msg, params object?[] kv)
        {
            var json = BuildJson("warn", msg, kv);

            if (_onWarning != null)
            {
                try
                {
                    _onWarning(json);
                }
                catch
                {
                    // A caller callback that throws must not propagate. Warn() is called from
                    // inside the catch blocks of the public API methods, so an exception here
                    // would escape the SDK and fault the caller's task — breaking the
                    // never-throw contract (CLAUDE.md section 10). Fall back to stderr so the
                    // warning is still surfaced rather than swallowed.
                    Write(json);
                }
                return;
            }

            Write(json);
        }

        private static void Write(string line)
        {
            try
            {
                Console.Error.WriteLine(line);
            }
            catch
            {
                // stderr closed or redirected. Logging must never break the caller.
            }
        }

        private static string BuildJson(string level, string msg, object?[] kv)
        {
            var entry = new Dictionary<string, object?> { ["level"] = level, ["msg"] = msg };
            for (var i = 0; i + 1 < kv.Length; i += 2)
            {
                var key = kv[i]?.ToString();
                if (string.IsNullOrEmpty(key)) continue;
                entry[key!] = kv[i + 1];
            }

            try
            {
                return JsonSerializer.Serialize(entry);
            }
            catch
            {
                // A value that will not serialise must not cost us the log line.
                return JsonSerializer.Serialize(new Dictionary<string, object?>
                {
                    ["level"] = level,
                    ["msg"] = msg,
                });
            }
        }
    }
}
