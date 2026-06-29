import { SNAConfig, VersionResponse } from './models.js';
import { EntityClient } from './entity.js';
import { AAClient } from './aa.js';
export declare class SNAClient {
    private readonly baseUrl;
    private readonly timeout;
    private readonly logger;
    readonly entity: EntityClient;
    readonly aa: AAClient;
    private constructor();
    static create(config: SNAConfig): Promise<SNAClient>;
    private connectWithRetry;
    private doPing;
    ping(): Promise<boolean>;
    version(): Promise<VersionResponse>;
    logWarn(msg: string): void;
    logInfo(msg: string): void;
    doGet(path: string): Promise<[number, string]>;
    doPost(path: string, body: string): Promise<[number, string]>;
    parseError(status: number, rawBody: string): never;
    parseJson(body: string): Record<string, unknown>;
}
//# sourceMappingURL=client.d.ts.map