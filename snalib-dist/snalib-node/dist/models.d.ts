export interface SNAConfig {
    baseUrl: string;
    maxRetries?: number;
    retryInterval?: number;
    maxWait?: number;
    timeout?: number;
    logEnabled?: boolean;
    onWarning?: (msg: string) => void;
}
export interface RegisterRequest {
    entityId: string;
    secret?: string;
    token?: string;
}
export interface AARequest {
    callType: string;
    route: string;
    peerId: string;
    peerType: string;
    customerId: string;
    txnCorId: string;
    body: object | string;
    httpStatus?: number;
    addlAttr?: Record<string, unknown>;
}
export declare class SNAResponse {
    readonly success: boolean;
    readonly errorMessage: string | undefined;
    isSuccess(): boolean;
    protected constructor(success: boolean, errorMessage?: string);
}
export declare class VersionResponse extends SNAResponse {
    readonly agentVersion: string;
    readonly name: string;
    private constructor();
    static from(data: Record<string, unknown>): VersionResponse;
    static error(message: string): VersionResponse;
}
export declare class RegisterResponse extends SNAResponse {
    readonly message: string;
    readonly entityId: string;
    readonly status: string;
    private constructor();
    static from(data: Record<string, unknown>): RegisterResponse;
    static error(message: string): RegisterResponse;
}
export declare class TokenResponse extends SNAResponse {
    readonly accessToken: string;
    readonly expiresIn: number;
    readonly tokenType: string | undefined;
    readonly refreshExpiresIn: number | undefined;
    readonly notBeforePolicy: number | undefined;
    readonly scope: string | undefined;
    readonly ver: string | undefined;
    readonly timestamp: string | undefined;
    readonly txnId: string | undefined;
    private constructor();
    static from(data: Record<string, unknown>): TokenResponse;
    static error(message: string): TokenResponse;
}
export declare class DispatchResponse extends SNAResponse {
    readonly message: string;
    private constructor();
    static from(data: Record<string, unknown>): DispatchResponse;
    static error(message: string): DispatchResponse;
}
export interface ErrorPayload {
    message?: string;
    errorCode?: string;
}
//# sourceMappingURL=models.d.ts.map