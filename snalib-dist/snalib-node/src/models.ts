// ---- Config & Request interfaces ----

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

// ---- Base response class ----

export class SNAResponse {
  readonly success: boolean;
  readonly errorMessage: string | undefined;

  isSuccess(): boolean {
    return this.success;
  }

  protected constructor(success: boolean, errorMessage?: string) {
    this.success = success;
    this.errorMessage = errorMessage;
  }
}

// ---- Response classes ----

export class VersionResponse extends SNAResponse {
  readonly agentVersion: string;
  readonly name: string;

  private constructor(
    success: boolean,
    agentVersion: string,
    name: string,
    errorMessage?: string,
  ) {
    super(success, errorMessage);
    this.agentVersion = agentVersion;
    this.name = name;
  }

  static from(data: Record<string, unknown>): VersionResponse {
    return new VersionResponse(
      true,
      String(data['agentVersion'] ?? data['agent_version'] ?? ''),
      String(data['name'] ?? ''),
    );
  }

  static error(message: string): VersionResponse {
    return new VersionResponse(false, '', '', message);
  }
}

export class RegisterResponse extends SNAResponse {
  readonly message: string;
  readonly entityId: string;
  readonly status: string;

  private constructor(
    success: boolean,
    message: string,
    entityId: string,
    status: string,
    errorMessage?: string,
  ) {
    super(success, errorMessage);
    this.message = message;
    this.entityId = entityId;
    this.status = status;
  }

  static from(data: Record<string, unknown>): RegisterResponse {
    return new RegisterResponse(
      true,
      String(data['message'] ?? ''),
      String(data['entity_id'] ?? ''),
      String(data['status'] ?? ''),
    );
  }

  static error(message: string): RegisterResponse {
    return new RegisterResponse(false, '', '', '', message);
  }
}

export class TokenResponse extends SNAResponse {
  readonly accessToken: string;
  readonly expiresIn: number;
  readonly tokenType: string | undefined;
  readonly refreshExpiresIn: number | undefined;
  readonly notBeforePolicy: number | undefined;
  readonly scope: string | undefined;
  readonly ver: string | undefined;
  readonly timestamp: string | undefined;
  readonly txnId: string | undefined;

  private constructor(
    success: boolean,
    accessToken: string,
    expiresIn: number,
    tokenType?: string,
    refreshExpiresIn?: number,
    notBeforePolicy?: number,
    scope?: string,
    ver?: string,
    timestamp?: string,
    txnId?: string,
    errorMessage?: string,
  ) {
    super(success, errorMessage);
    this.accessToken = accessToken;
    this.expiresIn = expiresIn;
    this.tokenType = tokenType;
    this.refreshExpiresIn = refreshExpiresIn;
    this.notBeforePolicy = notBeforePolicy;
    this.scope = scope;
    this.ver = ver;
    this.timestamp = timestamp;
    this.txnId = txnId;
  }

  static from(data: Record<string, unknown>): TokenResponse {
    return new TokenResponse(
      true,
      String(data['accessToken'] ?? ''),
      Number(data['expiresIn'] ?? 0),
      data['tokenType'] !== undefined ? String(data['tokenType']) : undefined,
      data['refreshExpiresIn'] !== undefined ? Number(data['refreshExpiresIn']) : undefined,
      data['notBeforePolicy'] !== undefined ? Number(data['notBeforePolicy']) : undefined,
      data['scope'] !== undefined ? String(data['scope']) : undefined,
      data['ver'] !== undefined ? String(data['ver']) : undefined,
      data['timestamp'] !== undefined ? String(data['timestamp']) : undefined,
      data['txnId'] !== undefined ? String(data['txnId']) : undefined,
    );
  }

  static error(message: string): TokenResponse {
    return new TokenResponse(false, '', 0, undefined, undefined, undefined, undefined, undefined, undefined, undefined, message);
  }
}

export class DispatchResponse extends SNAResponse {
  readonly message: string;

  private constructor(success: boolean, message: string, errorMessage?: string) {
    super(success, errorMessage);
    this.message = message;
  }

  static from(data: Record<string, unknown>): DispatchResponse {
    return new DispatchResponse(true, String(data['message'] ?? ''));
  }

  static error(message: string): DispatchResponse {
    return new DispatchResponse(false, '', message);
  }
}

// ---- Internal only — not exported from index.ts ----

export interface ErrorPayload {
  message?: string;
  errorCode?: string;
}
