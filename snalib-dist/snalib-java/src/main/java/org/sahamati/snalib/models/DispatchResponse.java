package org.sahamati.snalib.models;

import org.sahamati.snalib.SNAResponse;

public class DispatchResponse extends SNAResponse {
    private String message;

    private DispatchResponse() {}

    public String getMessage() { return message; }

    public static DispatchResponse error(String msg) {
        DispatchResponse r = new DispatchResponse();
        r.markError(msg);
        return r;
    }
}
