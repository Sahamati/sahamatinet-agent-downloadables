export class SNAConnectionError extends Error {
    name = 'SNAConnectionError';
    constructor(message) {
        super(`sna connection error: ${message}`);
    }
}
export class SNAValidationError extends Error {
    httpStatus;
    errorCode;
    name = 'SNAValidationError';
    constructor(httpStatus, message, errorCode) {
        super(`sna validation error (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
export class SNAServerError extends Error {
    httpStatus;
    errorCode;
    name = 'SNAServerError';
    constructor(httpStatus, message, errorCode) {
        super(`sna server error (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
export class SNANotImplementedError extends Error {
    httpStatus;
    errorCode;
    name = 'SNANotImplementedError';
    constructor(httpStatus, message, errorCode) {
        super(`sna not implemented (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
export class SNAUnexpectedResponseError extends Error {
    httpStatus;
    rawBody;
    name = 'SNAUnexpectedResponseError';
    constructor(httpStatus, message, rawBody) {
        super(`sna unexpected response (HTTP ${httpStatus}): ${message}`);
        this.httpStatus = httpStatus;
        this.rawBody = rawBody;
    }
}
