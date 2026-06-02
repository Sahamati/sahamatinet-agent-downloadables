package org.sahamati.snalib.models;

public final class TokenResponse {
    private String ver;
    private String timestamp;
    private String txnId;
    private String accessToken;
    private int expiresIn;
    private int refreshExpiresIn;
    private String tokenType;
    private int notBeforePolicy;
    private String scope;

    private TokenResponse() {}

    public String getVer() { return ver; }
    public String getTimestamp() { return timestamp; }
    public String getTxnId() { return txnId; }
    public String getAccessToken() { return accessToken; }
    public int getExpiresIn() { return expiresIn; }
    public int getRefreshExpiresIn() { return refreshExpiresIn; }
    public String getTokenType() { return tokenType; }
    public int getNotBeforePolicy() { return notBeforePolicy; }
    public String getScope() { return scope; }
}
