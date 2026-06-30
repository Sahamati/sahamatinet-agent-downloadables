package main

import (
	"context"
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/sahamati/snalib/snalib-go/snalib"
)

func main() {
	// Instantiate once at application startup — reuse across your entire application lifetime.
	// NewClient calls /ping internally; it retries with backoff. If unreachable, a warning is logged
	// and the error is returned alongside a valid client — the SDK never halts caller execution.
	// API calls on a client that failed to connect will surface the failure as error return values.
	snaNewClient, err := snalib.NewClient(snalib.Config{
		BaseURL:    "http://localhost:4044",
		LogEnabled: true, // structured JSON logs to stderr; omit or set false in production if you handle logs yourself
		// OnWarning: func(msg string) { myLogger.Warn(msg) }, // optional: route SDK warnings to your own logger
		// MaxRetries:    3,              // default
		// RetryInterval: time.Second,    // default; doubles each attempt
		// MaxWait:       15*time.Second, // default total across all retries
		// Timeout:       30*time.Second, // default per-request
	})
	if err != nil {
		// SNAConnectionError — service unreachable after all retries; client is still valid and usable
		log.Printf("warning: SNA unreachable at startup: %v", err)
	}

	ctx := context.Background()

	// --- Liveness check ---
	if err := snaNewClient.Ping(ctx); err != nil {
		log.Fatalf("ping failed: %v", err)
	}
	fmt.Println("ping: OK")

	// --- Version ---
	ver, err := snaNewClient.Version(ctx)
	if err != nil {
		handleErr("version", err)
		return
	}
	fmt.Printf("version: %s (%s)\n", ver.AgentVersion, ver.Name)

	// --- FIP-initiated flow: requestIn → responseOut ---
	// FIP sends a request to AA; AA processes and responds back.
	// Generate one txnCorID for this flow — use the same value for both requestIn and responseOut.
	// SNA uses txnCorID to correlate the pair.
	fipTxnCorID := newTxnID()
	fipStatus := 200

	// Body accepts three forms — pass whichever you already have:
	//   json.RawMessage:  json.RawMessage(`{"ver":"2.0.0","txnid":"..."}`)
	//   struct:           MyRequest{Ver: "2.0.0", TxnID: "..."}
	//   map[string]any:   map[string]any{"ver": "2.0.0", "txnid": "..."}
	consentBody := json.RawMessage(`{"ver":"2.0.0","txnid":"` + newTxnID() + `"}`)

	incomingRequest := snalib.AARequest{
		CallType:   "requestIn",
		Route:      "/Consent/Notification",
		PeerID:     "fip-test-001",
		PeerType:   "FIP",
		CustomerID: "user-test@sahamati",
		TxnCorID:   fipTxnCorID,
		Body:       consentBody,
	}

	outgoingResponse := snalib.AARequest{
		CallType:   "responseOut",
		Route:      "/Consent/Notification",
		PeerID:     "fip-test-001",
		PeerType:   "FIP",
		CustomerID: "user-test@sahamati",
		HTTPStatus: &fipStatus,
		TxnCorID:   fipTxnCorID, // same txnCorID as requestIn
		Body:       consentBody,
	}

	handleIncoming(ctx, snaNewClient, incomingRequest)
	time.Sleep(300 * time.Millisecond)
	handleOutgoing(ctx, snaNewClient, outgoingResponse)

	// --- AA-initiated flow: requestOut → responseIn ---
	// AA sends a request to FIP; FIP responds back to AA.
	// Generate one txnCorID for this flow — use the same value for both requestOut and responseIn.
	// SNA uses txnCorID to correlate the pair.
	aaTxnCorID := newTxnID()
	aaStatus := 200

	// Body accepts three forms — pass whichever you already have:
	//   json.RawMessage:  json.RawMessage(`{"ver":"2.0.0","txnid":"..."}`)
	//   struct:           MyRequest{Ver: "2.0.0", TxnID: "..."}
	//   map[string]any:   map[string]any{"ver": "2.0.0", "txnid": "..."}
	fiBody := json.RawMessage(`{"ver":"2.0.0","txnid":"` + newTxnID() + `"}`)

	outgoingRequest := snalib.AARequest{
		CallType:   "requestOut",
		Route:      "/FI/request",
		PeerID:     "fip-test-001",
		PeerType:   "FIP",
		CustomerID: "user-test@sahamati",
		TxnCorID:   aaTxnCorID,
		Body:       fiBody,
	}

	incomingResponse := snalib.AARequest{
		CallType:   "responseIn",
		Route:      "/FI/request",
		PeerID:     "fip-test-001",
		PeerType:   "FIP",
		CustomerID: "user-test@sahamati",
		HTTPStatus: &aaStatus,
		TxnCorID:   aaTxnCorID, // same txnCorID as requestOut
		Body:       fiBody,
		AddlAttr:   map[string]any{"correlationId": "corr-test-789"},
	}

	handleOutgoing(ctx, snaNewClient, outgoingRequest)
	time.Sleep(300 * time.Millisecond)
	handleIncoming(ctx, snaNewClient, incomingResponse)

	// --- Per-request timeout using context ---
	tctx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	if err := snaNewClient.Ping(tctx); err != nil {
		handleErr("ping with timeout", err)
	}
}

// handleIncoming dispatches requestIn and responseIn calls to SNA.
func handleIncoming(ctx context.Context, client *snalib.SNAClient, req snalib.AARequest) {

	/*
		...
		...
		Application receives an incoming message.
		Handle validation, authentication, etc.
		...
		...
	*/

	out, err := client.AA.Dispatch(ctx, req)
	if err != nil {
		handleErr("incoming "+req.CallType, err)
		return
	}
	fmt.Printf("incoming %s: %s\n", req.CallType, out.Message)

	/*
		...
		...
		Application logic to handle the incoming message based on callType and route.
		...
		...
	*/
}

// handleOutgoing dispatches requestOut and responseOut calls to SNA.
func handleOutgoing(ctx context.Context, client *snalib.SNAClient, req snalib.AARequest) {

	/*
		...
		...
		Application prepares the outgoing message.
		Handle serialization, enrichment, etc.
		...
		...
	*/

	out, err := client.AA.Dispatch(ctx, req)
	if err != nil {
		handleErr("outgoing "+req.CallType, err)
		return
	}
	fmt.Printf("outgoing %s: %s\n", req.CallType, out.Message)

	/*
		...
		...
		Application logic after the outgoing message has been dispatched.
		...
		...
	*/
}

// newTxnID returns a random UUID v4 string for use as a transaction ID.
func newTxnID() string {
	b := make([]byte, 16)
	rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

// handleErr demonstrates how callers should inspect SDK errors.
func handleErr(op string, err error) {
	var connErr *snalib.SNAConnectionError
	var valErr *snalib.SNAValidationError
	var srvErr *snalib.SNAServerError
	var notImplErr *snalib.SNANotImplementedError
	var unexpectedErr *snalib.SNAUnexpectedResponseError

	switch {
	case errors.As(err, &connErr):
		fmt.Printf("[%s] connection error: %s\n", op, connErr.Message)
	case errors.As(err, &valErr):
		fmt.Printf("[%s] validation error (HTTP %d, code %s): %s\n", op, valErr.HTTPStatus, valErr.ErrorCode, valErr.Message)
	case errors.As(err, &srvErr):
		fmt.Printf("[%s] server error (HTTP %d, code %s): %s\n", op, srvErr.HTTPStatus, srvErr.ErrorCode, srvErr.Message)
	case errors.As(err, &notImplErr):
		fmt.Printf("[%s] not implemented (HTTP %d): %s\n", op, notImplErr.HTTPStatus, notImplErr.Message)
	case errors.As(err, &unexpectedErr):
		fmt.Printf("[%s] unexpected response (HTTP %d): %s\n", op, unexpectedErr.HTTPStatus, unexpectedErr.Message)
	default:
		fmt.Printf("[%s] unexpected error: %v\n", op, err)
	}
}

// getToken demonstrates token generation from the entity service.
// This function retrieves an access token required for entity operations.
// Token response includes expiration details and token type information.
//
// Purpose: Shows how to call the entity token generation API and handle
// errors using the (T, error) pattern.
//
// Note: This function is not called in the main flow — token generation
// is optional and not required for AA dispatch operations.
func getToken(ctx context.Context, client *snalib.SNAClient) {
	// --- Get token ---
	tok, err := client.Entity.GetToken(ctx)
	if err != nil {
		handleErr("get_token", err)
		return
	}
	fmt.Printf("token: type=%s  expires_in=%ds\n", tok.TokenType, tok.ExpiresIn)
}
