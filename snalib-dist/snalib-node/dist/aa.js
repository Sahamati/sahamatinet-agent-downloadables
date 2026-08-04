import { DispatchResponse } from './models.js';
export class AAClient {
    client;
    constructor(client) {
        this.client = client;
    }
    async dispatch(req) {
        try {
            // Accept body as object or raw JSON string.
            // If string, parse it first — otherwise JSON.stringify embeds it as a quoted string.
            let bodyValue;
            if (typeof req.body === 'string') {
                if (req.body.trim() === '') {
                    // The peer returned no payload. null, {} and an absent key are all len == 0 to
                    // SNA, and on responseIn that is the transport-failure signal. Re-encoding "" as
                    // null is lossless — JSON cannot carry a string into an object-typed field.
                    bodyValue = null;
                }
                else {
                    try {
                        bodyValue = JSON.parse(req.body);
                    }
                    catch {
                        // Not JSON. Send it unchanged and let SNA reject it — the SDK does not
                        // validate, and the parsed value's type is SNA's rule to enforce.
                        bodyValue = req.body;
                    }
                }
            }
            else {
                bodyValue = req.body ?? null;
            }
            const payload = {
                callType: req.callType,
                route: req.route,
                peerId: req.peerId,
                peerType: req.peerType,
                customerId: req.customerId,
                txnCorId: req.txnCorId,
                body: bodyValue,
            };
            if (req.httpStatus !== undefined)
                payload['httpStatus'] = req.httpStatus;
            if (req.addlAttr !== undefined)
                payload['addlAttr'] = req.addlAttr;
            const [status, body] = await this.client.doPost('/sna/v1/aa', JSON.stringify(payload));
            if (status !== 200) {
                this.client.parseError(status, body);
            }
            const data = this.client.parseJson(body);
            return DispatchResponse.from(data);
        }
        catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            this.client.logWarn(msg);
            return DispatchResponse.error(msg);
        }
    }
}
