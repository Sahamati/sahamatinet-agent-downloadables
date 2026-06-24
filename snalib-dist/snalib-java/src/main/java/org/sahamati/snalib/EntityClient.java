package org.sahamati.snalib;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.sahamati.snalib.errors.SNAUnexpectedResponseError;
import org.sahamati.snalib.models.RegisterRequest;
import org.sahamati.snalib.models.RegisterResponse;
import org.sahamati.snalib.models.TokenResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityClient {

    private final SNAClient client;
    private final Gson gson;

    EntityClient(SNAClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    // Check isSuccess() on the returned response to detect failures — never throws.
    public RegisterResponse register(RegisterRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entity_id", req.getEntityId());
            if (req.getSecret() != null && !req.getSecret().isBlank()) payload.put("secret", req.getSecret());
            if (req.getToken() != null && !req.getToken().isBlank()) payload.put("token", req.getToken());

            String responseBody = client.post("/sna/v1/entity/register", gson.toJson(payload));
            try {
                return gson.fromJson(responseBody, RegisterResponse.class);
            } catch (JsonSyntaxException e) {
                throw new SNAUnexpectedResponseError(200, "failed to decode register response: " + e.getMessage(), responseBody);
            }
        } catch (RuntimeException e) {
            client.logWarn(e.getMessage());
            return RegisterResponse.error(e.getMessage());
        }
    }

    // Check isSuccess() on the returned response to detect failures — never throws.
    public TokenResponse getToken() {
        try {
            String responseBody = client.get("/sna/v1/entity/token/generate");
            try {
                return gson.fromJson(responseBody, TokenResponse.class);
            } catch (JsonSyntaxException e) {
                throw new SNAUnexpectedResponseError(200, "failed to decode token response: " + e.getMessage(), responseBody);
            }
        } catch (RuntimeException e) {
            client.logWarn(e.getMessage());
            return TokenResponse.error(e.getMessage());
        }
    }
}
