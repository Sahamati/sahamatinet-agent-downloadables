package org.sahamati.snalib;

public class SNAResponse {
    private boolean success = true;
    private String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    protected void markError(String msg) {
        this.success = false;
        this.errorMessage = msg;
    }
}
