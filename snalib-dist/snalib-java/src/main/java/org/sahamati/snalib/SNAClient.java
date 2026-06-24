package org.sahamati.snalib;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.sahamati.snalib.errors.SNAUnexpectedResponseError;
import org.sahamati.snalib.errors.SNAConnectionError;
import org.sahamati.snalib.errors.SNANotImplementedError;
import org.sahamati.snalib.errors.SNAServerError;
import org.sahamati.snalib.errors.SNAValidationError;
import org.sahamati.snalib.models.VersionResponse;
import org.sahamati.snalib.utils.SnaLibLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Instantiate once and reuse across your application lifetime.
public final class SNAClient {

    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final long timeoutMs;
    private final SnaLibLogger logger;
    private final EntityClient entity;
    private final AAClient aa;

    public SNAClient(SNAConfig config) {
        this.baseUrl = config.getBaseUrl().replaceAll("/+$", "");
        this.timeoutMs = config.getTimeoutMs();
        this.logger = new SnaLibLogger(config.isLogEnabled(), gson, config.getOnWarning());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.entity = new EntityClient(this, gson);
        this.aa = new AAClient(this, gson);
        try {
            connectWithRetry(config);
        } catch (SNAConnectionError e) {
            logger.warn(e.getMessage());
        }
    }

    public EntityClient entity() { return entity; }
    public AAClient aa() { return aa; }

    /**
     * Checks liveness of the SNA service. Returns false and logs a warning if unreachable — never throws.
     */
    public boolean ping() {
        try {
            doPing();
            return true;
        } catch (RuntimeException e) {
            logWarn(e.getMessage());
            return false;
        }
    }

    public VersionResponse version() {
        try {
            String body = get("/sna/v1/version");
            try {
                return gson.fromJson(body, VersionResponse.class);
            } catch (JsonSyntaxException e) {
                VersionResponse r = VersionResponse.error("failed to decode version response: " + e.getMessage());
                logWarn(r.getErrorMessage());
                return r;
            }
        } catch (RuntimeException e) {
            logWarn(e.getMessage());
            return VersionResponse.error(e.getMessage());
        }
    }

    // package-private — used by EntityClient, AAClient, and sub-methods
    String get(String path) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
        return execute(req);
    }

    String post(String path, String jsonBody) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
        return execute(req);
    }

    void logWarn(String message) {
        logger.warn(message);
    }

    private String execute(HttpRequest req) {
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status == 200) return resp.body();
            parseAndThrow(status, resp.body());
            return null; // unreachable
        } catch (SNAConnectionError | SNAValidationError | SNAServerError | SNANotImplementedError | SNAUnexpectedResponseError e) {
            throw e;
        } catch (IOException e) {
            throw new SNAConnectionError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SNAConnectionError("request interrupted");
        }
    }

    private void parseAndThrow(int status, String body) {
        ErrorResponse err = null;
        try {
            err = gson.fromJson(body, ErrorResponse.class);
        } catch (JsonSyntaxException ignored) {
            // best-effort — fall back to raw body as message
        }
        String message = (err != null && err.getMessage() != null) ? err.getMessage() : body;
        String errorCode = (err != null && err.getErrorCode() != null) ? err.getErrorCode() : String.valueOf(status);
        switch (status) {
            case 400 -> throw new SNAValidationError(status, message, errorCode);
            case 501 -> throw new SNANotImplementedError(status, message, errorCode);
            default  -> throw new SNAServerError(status, message, errorCode);
        }
    }

    private void doPing() {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/sna/v1/ping"))
                .GET()
                .timeout(Duration.ofMillis(timeoutMs))
                .build();
        try {
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() != 200) {
                throw new SNAConnectionError("ping returned HTTP " + resp.statusCode());
            }
        } catch (SNAConnectionError e) {
            throw e;
        } catch (IOException e) {
            throw new SNAConnectionError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SNAConnectionError("ping interrupted");
        }
    }

    private void connectWithRetry(SNAConfig config) {
        long deadline = System.currentTimeMillis() + config.getMaxWaitMs();
        long interval = config.getRetryIntervalMs();

        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                doPing();
                logger.info("connected to SNA service");
                return;
            } catch (SNAConnectionError e) {
                // continue to retry
            }

            if (attempt == config.getMaxRetries() || System.currentTimeMillis() + interval > deadline) {
                break;
            }

            logger.warn("SNA unreachable, retrying", "attempt", attempt + 1, "backoff_ms", interval);
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            interval *= 2;
        }

        throw new SNAConnectionError(
                "service at " + config.getBaseUrl() + " unreachable after " + (config.getMaxRetries() + 1) + " attempts"
        );
    }
}
