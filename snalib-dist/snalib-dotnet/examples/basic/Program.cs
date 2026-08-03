using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Sahamati.SnaLib;

namespace Examples.Basic
{
    public static class Program
    {
        public static async Task Main()
        {
            // Instantiate once at application startup — reuse across your entire application lifetime.
            // SNAClient.CreateAsync() calls /ping internally and retries with backoff. If unreachable,
            // a warning is logged — CreateAsync never throws. API calls will surface the failure as
            // error-state responses.
            var client = await SNAClient.CreateAsync(new SNAConfig
            {
                BaseUrl = "http://localhost:4044",
                LogEnabled = true, // structured JSON logs to stderr; omit or set false in production if you handle logs yourself
                // OnWarning = msg => myLogger.Warn(msg), // optional: route SDK warnings to your own logger
                // MaxRetries = 3,      // default
                // RetryInterval = 1000, // ms, default; doubles each attempt
                // MaxWait = 15000,     // ms, default total across all retries
                // Timeout = 30000,     // ms, default per-request
            });

            // --- Liveness check ---
            // PingAsync() never throws — returns false and logs a warning if unreachable
            var alive = await client.PingAsync();
            Console.WriteLine($"ping: {(alive ? "OK" : "FAILED — check SNA service")}");

            // --- Version ---
            var ver = await client.VersionAsync();
            if (!ver.IsSuccess())
            {
                Console.Error.WriteLine($"version warning: {ver.ErrorMessage}");
            }
            else
            {
                Console.WriteLine($"version: {ver.AgentVersion} ({ver.Name})");
            }

            // --- FIP-initiated flow: requestIn -> responseOut ---
            // FIP sends a request to AA; AA processes and responds back.
            // Generate one txnCorId for this flow — use the same value for both requestIn and responseOut.
            // SNA uses txnCorId to correlate the pair.
            var fipTxnCorId = Guid.NewGuid().ToString();

            // Body accepts two forms — pass whichever you already have:
            //   raw JSON string: "{\"ver\":\"2.0.0\",\"txnid\":\"...\"}"
            //   object:          new { ver = "2.0.0", txnid = "..." }
            var consentBody = $"{{\"ver\":\"2.0.0\",\"txnid\":\"{Guid.NewGuid()}\"}}";

            await HandleIncoming(client, new AARequest
            {
                CallType = "requestIn",
                Route = "/Consent/Notification",
                PeerId = "fip-test-001",
                PeerType = "FIP",
                CustomerId = "user-test@sahamati",
                TxnCorId = fipTxnCorId,
                Body = consentBody, // raw JSON string
            });

            await Task.Delay(300);

            await HandleOutgoing(client, new AARequest
            {
                CallType = "responseOut",
                Route = "/Consent/Notification",
                PeerId = "fip-test-001",
                PeerType = "FIP",
                CustomerId = "user-test@sahamati",
                TxnCorId = fipTxnCorId, // same txnCorId as requestIn
                HttpStatus = 200,
                Body = consentBody,
            });

            // --- AA-initiated flow: requestOut -> responseIn ---
            // AA sends a request to FIP; FIP responds back to AA.
            // Generate one txnCorId for this flow — use the same value for both requestOut and responseIn.
            // SNA uses txnCorId to correlate the pair.
            var aaTxnCorId = Guid.NewGuid().ToString();

            // Body accepts two forms — pass whichever you already have:
            //   raw JSON string: "{\"ver\":\"2.0.0\",\"txnid\":\"...\"}"
            //   object:          new { ver = "2.0.0", txnid = "..." }
            var fiBody = new { ver = "2.0.0", txnid = Guid.NewGuid().ToString() }; // object form

            await HandleOutgoing(client, new AARequest
            {
                CallType = "requestOut",
                Route = "/FI/request",
                PeerId = "fip-test-001",
                PeerType = "FIP",
                CustomerId = "user-test@sahamati",
                TxnCorId = aaTxnCorId,
                Body = fiBody,
            });

            await Task.Delay(300);

            await HandleIncoming(client, new AARequest
            {
                CallType = "responseIn",
                Route = "/FI/request",
                PeerId = "fip-test-001",
                PeerType = "FIP",
                CustomerId = "user-test@sahamati",
                TxnCorId = aaTxnCorId, // same txnCorId as requestOut
                HttpStatus = 200,
                Body = fiBody,
                AddlAttr = new Dictionary<string, object> { ["correlationId"] = "corr-test-789" },
            });
        }

        // HandleIncoming dispatches requestIn and responseIn calls to SNA.
        private static async Task HandleIncoming(SNAClient client, AARequest req)
        {
            /*
             * ...
             * Application receives an incoming message.
             * Handle validation, authentication, etc.
             * ...
             */

            // DispatchAsync() never throws — check IsSuccess() to detect SNA failures
            var outResp = await client.AA.DispatchAsync(req);
            if (!outResp.IsSuccess())
            {
                Console.Error.WriteLine($"incoming {req.CallType} warning: {outResp.ErrorMessage}");
                // application continues — SNA failure does not stop request processing
                return;
            }
            Console.WriteLine($"incoming {req.CallType}: {outResp.Message}");

            /*
             * ...
             * Application logic to handle the incoming message based on callType and route.
             * ...
             */
        }

        // HandleOutgoing dispatches requestOut and responseOut calls to SNA.
        private static async Task HandleOutgoing(SNAClient client, AARequest req)
        {
            /*
             * ...
             * Application prepares the outgoing message.
             * Handle serialization, enrichment, etc.
             * ...
             */

            // DispatchAsync() never throws — check IsSuccess() to detect SNA failures
            var outResp = await client.AA.DispatchAsync(req);
            if (!outResp.IsSuccess())
            {
                Console.Error.WriteLine($"outgoing {req.CallType} warning: {outResp.ErrorMessage}");
                // application continues — SNA failure does not stop request processing
                return;
            }
            Console.WriteLine($"outgoing {req.CallType}: {outResp.Message}");

            /*
             * ...
             * Application logic after the outgoing message has been dispatched.
             * ...
             */
        }

        // Demonstrates token generation from the entity service. Not called in the main
        // flow — token generation is optional and not required for AA dispatch operations.
        private static async Task GetToken(SNAClient client)
        {
            var tok = await client.Entity.GetTokenAsync();
            if (!tok.IsSuccess())
            {
                Console.Error.WriteLine($"get_token warning: {tok.ErrorMessage}");
            }
            else
            {
                Console.WriteLine($"token: type={tok.TokenType}  expires_in={tok.ExpiresIn}s");
            }
        }
    }
}
