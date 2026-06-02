package org.sahamati.snalib.errors;

public class SNAValidationError extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;

    public SNAValidationError(int httpStatus, String message, String errorCode) {
        super("sna validation error (HTTP " + httpStatus + ", code " + errorCode + "): " + message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getErrorCode() { return errorCode; }
}
