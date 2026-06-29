import type { SNAClient } from './client.js';
import { AARequest, DispatchResponse } from './models.js';
export declare class AAClient {
    private readonly client;
    constructor(client: SNAClient);
    dispatch(req: AARequest): Promise<DispatchResponse>;
}
//# sourceMappingURL=aa.d.ts.map