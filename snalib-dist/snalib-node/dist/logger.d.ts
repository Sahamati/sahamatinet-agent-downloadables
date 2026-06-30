export declare class SnaLibLogger {
    private readonly logEnabled;
    private readonly onWarning;
    constructor(logEnabled: boolean, onWarning?: (msg: string) => void);
    info(msg: string, ...kv: unknown[]): void;
    warn(msg: string, ...kv: unknown[]): void;
    private buildJson;
}
//# sourceMappingURL=logger.d.ts.map