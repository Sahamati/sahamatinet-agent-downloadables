package org.sahamati.snalib.models;

public final class RegisterRequest {
    private final String entityId;
    private final String secret;
    private final String token;

    private RegisterRequest(Builder b) {
        this.entityId = b.entityId;
        this.secret = b.secret;
        this.token = b.token;
    }

    public String getEntityId() { return entityId; }
    public String getSecret() { return secret; }
    public String getToken() { return token; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String entityId;
        private String secret;
        private String token;

        public Builder entityId(String entityId) { this.entityId = entityId; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder token(String token) { this.token = token; return this; }

        public RegisterRequest build() {
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("entityId is required");
            }
            return new RegisterRequest(this);
        }
    }
}
