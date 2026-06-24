package org.sahamati.snalib;

import java.util.function.Consumer;

public final class SNAConfig {
    private final String baseUrl;
    private final int maxRetries;
    private final long retryIntervalMs;
    private final long maxWaitMs;
    private final long timeoutMs;
    private final boolean logEnabled;
    private final Consumer<String> onWarning;

    private SNAConfig(Builder b) {
        this.baseUrl = b.baseUrl;
        this.maxRetries = b.maxRetries;
        this.retryIntervalMs = b.retryIntervalMs;
        this.maxWaitMs = b.maxWaitMs;
        this.timeoutMs = b.timeoutMs;
        this.logEnabled = b.logEnabled;
        this.onWarning = b.onWarning;
    }

    public String getBaseUrl() { return baseUrl; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryIntervalMs() { return retryIntervalMs; }
    public long getMaxWaitMs() { return maxWaitMs; }
    public long getTimeoutMs() { return timeoutMs; }
    public boolean isLogEnabled() { return logEnabled; }
    public Consumer<String> getOnWarning() { return onWarning; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String baseUrl;
        private int maxRetries = 3;
        private long retryIntervalMs = 1_000;
        private long maxWaitMs = 15_000;
        private long timeoutMs = 30_000;
        private boolean logEnabled = false;
        private Consumer<String> onWarning = null;

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder retryIntervalMs(long retryIntervalMs) { this.retryIntervalMs = retryIntervalMs; return this; }
        public Builder maxWaitMs(long maxWaitMs) { this.maxWaitMs = maxWaitMs; return this; }
        public Builder timeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; return this; }
        public Builder logEnabled(boolean logEnabled) { this.logEnabled = logEnabled; return this; }

        /** Optional. Receives all SDK warnings and errors. If not set, warnings are printed to stderr via java.util.logging. */
        public Builder onWarning(Consumer<String> onWarning) { this.onWarning = onWarning; return this; }

        public SNAConfig build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            return new SNAConfig(this);
        }
    }
}
