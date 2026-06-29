// ---- Config & Request interfaces ----
// ---- Base response class ----
export class SNAResponse {
    success;
    errorMessage;
    isSuccess() {
        return this.success;
    }
    constructor(success, errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
// ---- Response classes ----
export class VersionResponse extends SNAResponse {
    agentVersion;
    name;
    constructor(success, agentVersion, name, errorMessage) {
        super(success, errorMessage);
        this.agentVersion = agentVersion;
        this.name = name;
    }
    static from(data) {
        return new VersionResponse(true, String(data['agentVersion'] ?? data['agent_version'] ?? ''), String(data['name'] ?? ''));
    }
    static error(message) {
        return new VersionResponse(false, '', '', message);
    }
}
export class RegisterResponse extends SNAResponse {
    message;
    entityId;
    status;
    constructor(success, message, entityId, status, errorMessage) {
        super(success, errorMessage);
        this.message = message;
        this.entityId = entityId;
        this.status = status;
    }
    static from(data) {
        return new RegisterResponse(true, String(data['message'] ?? ''), String(data['entity_id'] ?? ''), String(data['status'] ?? ''));
    }
    static error(message) {
        return new RegisterResponse(false, '', '', '', message);
    }
}
export class TokenResponse extends SNAResponse {
    accessToken;
    expiresIn;
    tokenType;
    refreshExpiresIn;
    notBeforePolicy;
    scope;
    ver;
    timestamp;
    txnId;
    constructor(success, accessToken, expiresIn, tokenType, refreshExpiresIn, notBeforePolicy, scope, ver, timestamp, txnId, errorMessage) {
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
    static from(data) {
        return new TokenResponse(true, String(data['accessToken'] ?? ''), Number(data['expiresIn'] ?? 0), data['tokenType'] !== undefined ? String(data['tokenType']) : undefined, data['refreshExpiresIn'] !== undefined ? Number(data['refreshExpiresIn']) : undefined, data['notBeforePolicy'] !== undefined ? Number(data['notBeforePolicy']) : undefined, data['scope'] !== undefined ? String(data['scope']) : undefined, data['ver'] !== undefined ? String(data['ver']) : undefined, data['timestamp'] !== undefined ? String(data['timestamp']) : undefined, data['txnId'] !== undefined ? String(data['txnId']) : undefined);
    }
    static error(message) {
        return new TokenResponse(false, '', 0, undefined, undefined, undefined, undefined, undefined, undefined, undefined, message);
    }
}
export class DispatchResponse extends SNAResponse {
    message;
    constructor(success, message, errorMessage) {
        super(success, errorMessage);
        this.message = message;
    }
    static from(data) {
        return new DispatchResponse(true, String(data['message'] ?? ''));
    }
    static error(message) {
        return new DispatchResponse(false, '', message);
    }
}
