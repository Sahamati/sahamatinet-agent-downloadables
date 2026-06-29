import type { SNAClient } from './client.js';
import { RegisterRequest, RegisterResponse, TokenResponse } from './models.js';
export declare class EntityClient {
    private readonly client;
    constructor(client: SNAClient);
    register(req: RegisterRequest): Promise<RegisterResponse>;
    getToken(): Promise<TokenResponse>;
}
//# sourceMappingURL=entity.d.ts.map