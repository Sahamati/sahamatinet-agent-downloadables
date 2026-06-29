export class SNAConnectionError extends Error {
  override name = 'SNAConnectionError';

  constructor(message: string) {
    super(`sna connection error: ${message}`);
  }
}

export class SNAValidationError extends Error {
  override name = 'SNAValidationError';

  constructor(
    readonly httpStatus: number,
    message: string,
    readonly errorCode: string,
  ) {
    super(`sna validation error (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
  }
}

export class SNAServerError extends Error {
  override name = 'SNAServerError';

  constructor(
    readonly httpStatus: number,
    message: string,
    readonly errorCode: string,
  ) {
    super(`sna server error (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
  }
}

export class SNANotImplementedError extends Error {
  override name = 'SNANotImplementedError';

  constructor(
    readonly httpStatus: number,
    message: string,
    readonly errorCode: string,
  ) {
    super(`sna not implemented (HTTP ${httpStatus}, code ${errorCode}): ${message}`);
  }
}

export class SNAUnexpectedResponseError extends Error {
  override name = 'SNAUnexpectedResponseError';

  constructor(
    readonly httpStatus: number,
    message: string,
    readonly rawBody: string,
  ) {
    super(`sna unexpected response (HTTP ${httpStatus}): ${message}`);
  }
}
