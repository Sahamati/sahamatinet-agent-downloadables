import type { SNAClient } from './client.js';
import { RegisterRequest, RegisterResponse, TokenResponse } from './models.js';

export class EntityClient {
  private readonly client: SNAClient;

  constructor(client: SNAClient) {
    this.client = client;
  }

  async register(req: RegisterRequest): Promise<RegisterResponse> {
    try {
      const payload: Record<string, string> = { entity_id: req.entityId };
      // Never log secret or token — protection is here at the callsite.
      if (req.token !== undefined) {
        payload['token'] = req.token;
      } else if (req.secret !== undefined) {
        payload['secret'] = req.secret;
      }

      const [status, body] = await this.client.doPost('/sna/v1/entity/register', JSON.stringify(payload));
      if (status !== 200) {
        this.client.parseError(status, body);
      }
      const data = this.client.parseJson(body);
      return RegisterResponse.from(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      this.client.logWarn(msg);
      return RegisterResponse.error(msg);
    }
  }

  async getToken(): Promise<TokenResponse> {
    try {
      const [status, body] = await this.client.doGet('/sna/v1/entity/token/generate');
      if (status !== 200) {
        this.client.parseError(status, body);
      }
      const data = this.client.parseJson(body);
      return TokenResponse.from(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      this.client.logWarn(msg);
      return TokenResponse.error(msg);
    }
  }
}
