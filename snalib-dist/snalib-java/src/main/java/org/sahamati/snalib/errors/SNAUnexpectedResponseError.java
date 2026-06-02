package org.sahamati.snalib.errors;

public class SNAUnexpectedResponseError extends RuntimeException {
    private final int httpStatus;
    private final String rawBody;

    public SNAUnexpectedResponseError(int httpStatus, String message, String rawBody) {
        super("sna unexpected response (HTTP " + httpStatus + "): " + message);
        this.httpStatus = httpStatus;
        this.rawBody = rawBody;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getRawBody() { return rawBody; }
}
