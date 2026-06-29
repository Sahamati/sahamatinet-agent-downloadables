import { SNAClient, AARequest, DispatchResponse } from '../../src/index.js';

// Instantiate once at application startup — reuse across your entire application lifetime.
// SNAClient.create() calls /ping internally and retries with backoff. If unreachable, a warning
// is logged — create() never rejects. API calls will surface the failure as error-state responses.
const client = await SNAClient.create({
  baseUrl: 'http://localhost:4044',
  logEnabled: true, // structured JSON logs to stderr; omit or set false in production if you handle logs yourself
  // onWarning: (msg) => myLogger.warn(msg), // optional: route SDK warnings to your own logger
  // maxRetries:    3,     // default
  // retryInterval: 1000,  // ms, default; doubles each attempt
  // maxWait:       15000, // ms, default total across all retries
  // timeout:       30000, // ms, default per-request
});

// --- Liveness check ---
// ping() never rejects — returns false and logs a warning if unreachable
const alive = await client.ping();
console.log('ping:', alive ? 'OK' : 'FAILED — check SNA service');

// --- Version ---
const ver = await client.version();
if (!ver.isSuccess()) {
  console.error('version warning:', ver.errorMessage);
} else {
  console.log(`version: ${ver.agentVersion} (${ver.name})`);
}

// --- Register entity (secret path) ---
const reg = await client.entity.register({
  entityId: 'TEST-1-AA',
  secret: 'vDC2f9XsoijNROu4lrf4iIYewZxQ0NRW',
  // token: 'eyJhbGci...',  // provide instead of secret, or alongside (service uses token if both present)
});
if (!reg.isSuccess()) {
  console.error('register warning:', reg.errorMessage);
} else {
  console.log(`register: entity=${reg.entityId}  status=${reg.status}`);
}

// --- FIP-initiated flow: requestIn → responseOut ---
// FIP sends a request to AA; AA processes and responds back.
// Generate one txnCorId for this flow — use the same value for both requestIn and responseOut.
// SNA uses txnCorId to correlate the pair.
const fipTxnCorId = crypto.randomUUID();

// body accepts two forms — pass whichever you already have:
//   raw JSON string:   '{"ver":"2.0.0","txnid":"..."}'
//   object:            { ver: '2.0.0', txnid: '...' }
const consentBody = `{"ver":"2.0.0","txnid":"${crypto.randomUUID()}"}`;

await handleIncoming(client, {
  callType: 'requestIn',
  route: '/Consent/Notification',
  peerId: 'fip-test-001',
  peerType: 'FIP',
  customerId: 'user-test@sahamati',
  txnCorId: fipTxnCorId,
  body: consentBody,   // raw JSON string
});

await new Promise(r => setTimeout(r, 300));

await handleOutgoing(client, {
  callType: 'responseOut',
  route: '/Consent/Notification',
  peerId: 'fip-test-001',
  peerType: 'FIP',
  customerId: 'user-test@sahamati',
  txnCorId: fipTxnCorId, // same txnCorId as requestIn
  httpStatus: 200,
  body: consentBody,
});

// --- AA-initiated flow: requestOut → responseIn ---
// AA sends a request to FIP; FIP responds back to AA.
// Generate one txnCorId for this flow — use the same value for both requestOut and responseIn.
// SNA uses txnCorId to correlate the pair.
const aaTxnCorId = crypto.randomUUID();

// body accepts two forms — pass whichever you already have:
//   raw JSON string:   '{"ver":"2.0.0","txnid":"..."}'
//   object:            { ver: '2.0.0', txnid: '...' }
const fiBody = { ver: '2.0.0', txnid: crypto.randomUUID() }; // object form

await handleOutgoing(client, {
  callType: 'requestOut',
  route: '/FI/request',
  peerId: 'fip-test-001',
  peerType: 'FIP',
  customerId: 'user-test@sahamati',
  txnCorId: aaTxnCorId,
  body: fiBody,
});

await new Promise(r => setTimeout(r, 300));

await handleIncoming(client, {
  callType: 'responseIn',
  route: '/FI/request',
  peerId: 'fip-test-001',
  peerType: 'FIP',
  customerId: 'user-test@sahamati',
  txnCorId: aaTxnCorId, // same txnCorId as requestOut
  httpStatus: 200,
  body: fiBody,
  addlAttr: { correlationId: 'corr-test-789' },
});

// handleIncoming dispatches requestIn and responseIn calls to SNA.
async function handleIncoming(client: SNAClient, req: AARequest): Promise<void> {

  /*
   * ...
   * Application receives an incoming message.
   * Handle validation, authentication, etc.
   * ...
   */

  // dispatch() never rejects — check isSuccess() to detect SNA failures
  const out: DispatchResponse = await client.aa.dispatch(req);
  if (!out.isSuccess()) {
    console.error(`incoming ${req.callType} warning: ${out.errorMessage}`);
    // application continues — SNA failure does not stop request processing
    return;
  }
  console.log(`incoming ${req.callType}: ${out.message}`);

  /*
   * ...
   * Application logic to handle the incoming message based on callType and route.
   * ...
   */
}

// handleOutgoing dispatches requestOut and responseOut calls to SNA.
async function handleOutgoing(client: SNAClient, req: AARequest): Promise<void> {

  /*
   * ...
   * Application prepares the outgoing message.
   * Handle serialization, enrichment, etc.
   * ...
   */

  // dispatch() never rejects — check isSuccess() to detect SNA failures
  const out: DispatchResponse = await client.aa.dispatch(req);
  if (!out.isSuccess()) {
    console.error(`outgoing ${req.callType} warning: ${out.errorMessage}`);
    // application continues — SNA failure does not stop request processing
    return;
  }
  console.log(`outgoing ${req.callType}: ${out.message}`);

  /*
   * ...
   * Application logic after the outgoing message has been dispatched.
   * ...
   */
}

/**
 * Demonstrates token generation from the entity service.
 * Retrieves an access token; the response includes expiration details and token type.
 *
 * Purpose: shows how to call the entity token generation API and handle both
 * success and failure cases using the isSuccess() pattern.
 *
 * Note: this function is not called in the main flow — token generation is
 * optional and not required for AA dispatch operations.
 */
async function getToken(client: SNAClient): Promise<void> {
  // --- Get token ---
  const tok = await client.entity.getToken();
  if (!tok.isSuccess()) {
    console.error('get_token warning:', tok.errorMessage);
  } else {
    console.log(`token: type=${tok.tokenType}  expires_in=${tok.expiresIn}s`);
  }
}
