package examples.basic;

import org.sahamati.snalib.SNAClient;
import org.sahamati.snalib.SNAConfig;
import org.sahamati.snalib.errors.SNAConnectionError;
import org.sahamati.snalib.errors.SNANotImplementedError;
import org.sahamati.snalib.errors.SNAUnexpectedResponseError;
import org.sahamati.snalib.errors.SNAServerError;
import org.sahamati.snalib.errors.SNAValidationError;
import org.sahamati.snalib.models.AARequest;
import org.sahamati.snalib.models.DispatchResponse;
import org.sahamati.snalib.models.RegisterRequest;
import org.sahamati.snalib.models.RegisterResponse;
import org.sahamati.snalib.models.TokenResponse;
import org.sahamati.snalib.models.VersionResponse;

import java.util.Map;
import java.util.UUID;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Instantiate once at application startup — reuse across your entire application lifetime.
        // SNAClient calls /ping internally; it retries with backoff and throws SNAConnectionError if unreachable.
        SNAClient client;
        try {
            client = new SNAClient(SNAConfig.builder()
                    .baseUrl("http://localhost:4044")
                    .logEnabled(true) // structured JSON logs to stderr; omit or set false in production
                    // .maxRetries(3)
                    // .retryIntervalMs(1_000)
                    // .maxWaitMs(15_000)
                    // .timeoutMs(30_000)
                    .build());
        } catch (SNAConnectionError e) {
            System.err.println("failed to connect to SNA: " + e.getMessage());
            return;
        }

        // --- Liveness check ---
        client.ping();
        System.out.println("ping: OK");

        // --- Version ---
        VersionResponse ver = client.version();
        System.out.printf("version: %s (%s)%n", ver.getAgentVersion(), ver.getName());

        // --- Register entity (secret path) ---
        RegisterResponse reg = client.entity().register(RegisterRequest.builder()
                .entityId("-enter-your-entity-id-here-")
                .secret("-enter-your-entity-secret-here-")
                // .token("eyJhbGci...")  // provide instead of secret, or alongside (service uses token if both present)
                .build());
        System.out.printf("register: entity=%s  status=%s%n", reg.getEntityId(), reg.getStatus());

        // --- Get token ---
        TokenResponse tok = client.entity().getToken();
        System.out.printf("token: type=%s  expires_in=%ds%n", tok.getTokenType(), tok.getExpiresIn());

        // --- FIP-initiated flow: requestIn → responseOut ---
        // FIP sends a request to AA; AA processes and responds back.
        // txnId and route must be identical across the pair — SNA keys the lookup on peerId+txnId+route.
        String fipTxnId = UUID.randomUUID().toString();

        // Pass your existing body as-is — a POJO, Map, or raw JSON string, whatever you already have.
        String consentBody = "{\"ver\":\"2.0.0\",\"txnid\":\"" + fipTxnId + "\"}";

        AARequest incomingRequest = AARequest.builder()
                .callType("requestIn")
                .route("/Consent/Notification")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .body(consentBody)
                .build();

        AARequest outgoingResponse = AARequest.builder()
                .callType("responseOut")
                .route("/Consent/Notification")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .httpStatus(200)
                .body(consentBody)
                .build();

        handleIncoming(client, incomingRequest);
        Thread.sleep(300);
        handleOutgoing(client, outgoingResponse);

        // --- AA-initiated flow: requestOut → responseIn ---
        // AA sends a request to FIP; FIP responds back to AA.
        // txnId and route must be identical across the pair — SNA keys the lookup on peerId+txnId+route.
        String aaTxnId = UUID.randomUUID().toString();

        String fiRequestBody = "{\"ver\":\"2.0.0\",\"txnid\":\"" + aaTxnId + "\"}";

        AARequest outgoingRequest = AARequest.builder()
                .callType("requestOut")
                .route("/FI/request")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .body(fiRequestBody)
                .build();

        AARequest incomingResponse = AARequest.builder()
                .callType("responseIn")
                .route("/FI/request")
                .peerId("fip-test-001")
                .peerType("FIP")
                .customerId("user-test@sahamati")
                .httpStatus(200)
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

        DispatchResponse out = client.aa().dispatch(req);
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

        DispatchResponse out = client.aa().dispatch(req);
        System.out.printf("outgoing %s: %s%n", req.getCallType(), out.getMessage());

        /*
         * ...
         * Application logic after the outgoing message has been dispatched.
         * ...
         */
    }

    static void handleErr(String op, Exception e) {
        if (e instanceof SNAConnectionError err) {
            System.err.printf("[%s] connection error: %s%n", op, err.getMessage());
        } else if (e instanceof SNAValidationError err) {
            System.err.printf("[%s] validation error (HTTP %d, code %s): %s%n", op, err.getHttpStatus(), err.getErrorCode(), err.getMessage());
        } else if (e instanceof SNAServerError err) {
            System.err.printf("[%s] server error (HTTP %d, code %s): %s%n", op, err.getHttpStatus(), err.getErrorCode(), err.getMessage());
        } else if (e instanceof SNANotImplementedError err) {
            System.err.printf("[%s] not implemented (HTTP %d): %s%n", op, err.getHttpStatus(), err.getMessage());
        } else if (e instanceof SNAUnexpectedResponseError err) {
            System.err.printf("[%s] unexpected response (HTTP %d): %s%n", op, err.getHttpStatus(), err.getMessage());
        } else {
            System.err.printf("[%s] unexpected error: %s%n", op, e.getMessage());
        }
    }
}
