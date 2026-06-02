package org.sahamati.snalib.errors;

public class SNANotImplementedError extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;

    public SNANotImplementedError(int httpStatus, String message, String errorCode) {
        super("sna not implemented (HTTP " + httpStatus + ", code " + errorCode + "): " + message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getErrorCode() { return errorCode; }
}
