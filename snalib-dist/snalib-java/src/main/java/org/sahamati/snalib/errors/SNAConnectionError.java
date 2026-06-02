package org.sahamati.snalib.errors;

public class SNAConnectionError extends RuntimeException {
    public SNAConnectionError(String message) {
        super("sna connection error: " + message);
    }
}
