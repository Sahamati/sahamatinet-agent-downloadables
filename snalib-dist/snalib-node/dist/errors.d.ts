export declare class SNAConnectionError extends Error {
    name: string;
    constructor(message: string);
}
export declare class SNAValidationError extends Error {
    readonly httpStatus: number;
    readonly errorCode: string;
    name: string;
    constructor(httpStatus: number, message: string, errorCode: string);
}
export declare class SNAServerError extends Error {
    readonly httpStatus: number;
    readonly errorCode: string;
    name: string;
    constructor(httpStatus: number, message: string, errorCode: string);
}
export declare class SNANotImplementedError extends Error {
    readonly httpStatus: number;
    readonly errorCode: string;
    name: string;
    constructor(httpStatus: number, message: string, errorCode: string);
}
export declare class SNAUnexpectedResponseError extends Error {
    readonly httpStatus: number;
    readonly rawBody: string;
    name: string;
    constructor(httpStatus: number, message: string, rawBody: string);
}
//# sourceMappingURL=errors.d.ts.map