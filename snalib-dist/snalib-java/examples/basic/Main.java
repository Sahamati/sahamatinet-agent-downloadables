package examples.basic;

import org.sahamati.snalib.SNAClient;
import org.sahamati.snalib.SNAConfig;
import org.sahamati.snalib.models.AARequest;
import org.sahamati.snalib.models.DispatchResponse;
import org.sahamati.snalib.models.TokenResponse;
import org.sahamati.snalib.models.VersionResponse;

import java.util.Map;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Instantiate once at application startup — reuse across your entire application lifetime.
        // SNAClient calls /ping internally and retries with backoff. If unreachable, a warning is
        // logged — the constructor never throws. API calls will surface the failure as error-state responses.
        SNAClient client = new SNAClient(SNAConfig.builder()
                .baseUrl("http://localhost:4044")
                .logEnabled(true) // structured JSON logs to stderr; omit or set false in production
                // .onWarning(msg -> myLogger.warn(msg))  // optional: route SDK warnings to your own logger
                // .maxRetries(3)
                // .retryIntervalMs(1_000)
                // .maxWaitMs(15_000)
                // .timeoutMs(30_000)
                .build());

        // --- Liveness check ---
        // ping() never throws — returns false and logs a warning if unreachable
        boolean alive = client.ping();
        System.out.println("ping: " + (alive ? "OK" : "FAILED — check SNA service"));

        // --- Version ---
        VersionResponse ver = client.version();
        if (!ver.isSuccess()) {
            System.err.println("version warning: " + ver.getErrorMessage());
        } else {
            System.out.printf("version: %s (%s)%n", ver.getAgentVersion(), ver.getName());
        }

        // --- FIP-initiated flow: requestIn → responseOut ---
        // FIP sends a request to AA; AA processes and responds back.
        // Generate one txnCorId for this flow — use the same value for both requestIn and responseOut.
        // SNA uses txnCorId to correlate the pair.
        String fipTxnCorId = UUID.randomUUID().toString();

        // body accepts three forms — pass whichever you already have:
        //   raw JSON string:  .body("{\"ver\":\"2.0.0\",\"txnid\":\"...\"}")
        //   POJO:             .body(myConsentNotificationObject)
        //   Map:              .body(Map.of("ver", "2.0.0", "txnid", "..."))
        String consentBody = "{\"ver\":\"2.0.0\",\"txnid\":\"" + UUID.randomUUID() + "\"}";

        AARequest incomingRequest = AARequest.builder()
                .callType("requestIn")
                .route("/Consent/Notification")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .txnCorId(fipTxnCorId)
                .body(consentBody)
                .build();

        AARequest outgoingResponse = AARequest.builder()
                .callType("responseOut")
                .route("/Consent/Notification")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .httpStatus(200)
                .txnCorId(fipTxnCorId) // same txnCorId as requestIn
                .body(consentBody)
                .build();

        handleIncoming(client, incomingRequest);
        Thread.sleep(300);
        handleOutgoing(client, outgoingResponse);

        // --- AA-initiated flow: requestOut → responseIn ---
        // AA sends a request to FIP; FIP responds back to AA.
        // Generate one txnCorId for this flow — use the same value for both requestOut and responseIn.
        // SNA uses txnCorId to correlate the pair.
        String aaTxnCorId = UUID.randomUUID().toString();

        // body accepts three forms — pass whichever you already have:
        //   raw JSON string:  .body("{\"ver\":\"2.0.0\",\"txnid\":\"...\"}")
        //   POJO:             .body(myFIRequestObject)
        //   Map:              .body(Map.of("ver", "2.0.0", "txnid", "..."))
        String fiRequestBody = "{\"ver\":\"2.0.0\",\"txnid\":\"" + UUID.randomUUID() + "\"}";

        AARequest outgoingRequest = AARequest.builder()
                .callType("requestOut")
                .route("/FI/request")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .txnCorId(aaTxnCorId)
                .body(fiRequestBody)
                .build();

        AARequest incomingResponse = AARequest.builder()
                .callType("responseIn")
                .route("/FI/request")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .httpStatus(200)
                .txnCorId(aaTxnCorId) // same txnCorId as requestOut
                .body(fiRequestBody)
                .addlAttr(Map.of("correlationId", "corr-test-789"))
                .build();

        handleOutgoing(client, outgoingRequest);
        Thread.sleep(300);
        handleIncoming(client, incomingResponse);
    }

    // handleIncoming dispatches requestIn and responseIn calls to SNA.
    static void handleIncoming(SNAClient client, AARequest req) {

        /*
         * ...
         * Application receives an incoming message.
         * Handle validation, authentication, etc.
         * ...
         */

        // dispatch() never throws — check isSuccess() to detect SNA failures
        DispatchResponse out = client.aa().dispatch(req);
        if (!out.isSuccess()) {
            System.err.printf("incoming %s warning: %s%n", req.getCallType(), out.getErrorMessage());
            // application continues — SNA failure does not stop request processing
            return;
        }
        System.out.printf("incoming %s: %s%n", req.getCallType(), out.getMessage());

        /*
         * ...
         * Application logic to handle the incoming message based on callType and route.
         * ...
         */
    }

    // handleOutgoing dispatches requestOut and responseOut calls to SNA.
    static void handleOutgoing(SNAClient client, AARequest req) {

        /*
         * ...
         * Application prepares the outgoing message.
         * Handle serialization, enrichment, etc.
         * ...
         */

        // dispatch() never throws — check isSuccess() to detect SNA failures
        DispatchResponse out = client.aa().dispatch(req);
        if (!out.isSuccess()) {
            System.err.printf("outgoing %s warning: %s%n", req.getCallType(), out.getErrorMessage());
            // application continues — SNA failure does not stop request processing
            return;
        }
        System.out.printf("outgoing %s: %s%n", req.getCallType(), out.getMessage());

        /*
         * ...
         * Application logic after the outgoing message has been dispatched.
         * ...
         */
    }

    /**
     * Demonstrates token generation from the entity service.
     * This function retrieves an access token required for entity operations.
     * Token response includes expiration details and token type information.
     *
     * Purpose: Shows how to call the entity token generation API and handle
     * both success and failure cases using the isSuccess() pattern.
     *
     * Note: This function is not called in the main flow — token generation
     * is optional and not required for AA dispatch operations.
     */
    static void getToken(SNAClient client) {
        // --- Get token ---
        TokenResponse tok = client.entity().getToken();
        if (!tok.isSuccess()) {
            System.err.println("get_token warning: " + tok.getErrorMessage());
        } else {
            System.out.printf("token: type=%s  expires_in=%ds%n", tok.getTokenType(), tok.getExpiresIn());
        }
    }
}
