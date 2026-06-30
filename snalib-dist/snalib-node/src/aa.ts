import type { SNAClient } from './client.js';
import { AARequest, DispatchResponse } from './models.js';

export class AAClient {
  private readonly client: SNAClient;

  constructor(client: SNAClient) {
    this.client = client;
  }

  async dispatch(req: AARequest): Promise<DispatchResponse> {
    try {
      // Accept body as object or raw JSON string.
      // If string, parse it first — otherwise JSON.stringify embeds it as a quoted string.
      let bodyValue: unknown;
      if (typeof req.body === 'string') {
        bodyValue = JSON.parse(req.body);
      } else {
        bodyValue = req.body;
      }

      const payload: Record<string, unknown> = {
        callType: req.callType,
        route: req.route,
        peerId: req.peerId,
        peerType: req.peerType,
        customerId: req.customerId,
        txnCorId: req.txnCorId,
        body: bodyValue,
      };
      if (req.httpStatus !== undefined) payload['httpStatus'] = req.httpStatus;
      if (req.addlAttr !== undefined) payload['addlAttr'] = req.addlAttr;

      const [status, body] = await this.client.doPost('/sna/v1/aa', JSON.stringify(payload));
      if (status !== 200) {
        this.client.parseError(status, body);
      }
      const data = this.client.parseJson(body);
      return DispatchResponse.from(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      this.client.logWarn(msg);
      return DispatchResponse.error(msg);
    }
  }
}
