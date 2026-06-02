package org.sahamati.snalib.models;

import com.google.gson.annotations.SerializedName;

public final class RegisterResponse {
    private String message;
    @SerializedName("entity_id")
    private String entityId;
    private String status;

    private RegisterResponse() {}

    public String getMessage() { return message; }
    public String getEntityId() { return entityId; }
    public String getStatus() { return status; }
}
