package org.sahamati.snalib.models;

import com.google.gson.annotations.SerializedName;
import org.sahamati.snalib.SNAResponse;

public class RegisterResponse extends SNAResponse {
    private String message;
    @SerializedName("entity_id")
    private String entityId;
    private String status;

    private RegisterResponse() {}

    public String getMessage() { return message; }
    public String getEntityId() { return entityId; }
    public String getStatus() { return status; }

    public static RegisterResponse error(String msg) {
        RegisterResponse r = new RegisterResponse();
        r.markError(msg);
        return r;
    }
}
