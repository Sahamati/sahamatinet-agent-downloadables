package org.sahamati.snalib.errors;

public class SNAServerError extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;

    public SNAServerError(int httpStatus, String message, String errorCode) {
        super("sna server error (HTTP " + httpStatus + ", code " + errorCode + "): " + message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getErrorCode() { return errorCode; }
}
