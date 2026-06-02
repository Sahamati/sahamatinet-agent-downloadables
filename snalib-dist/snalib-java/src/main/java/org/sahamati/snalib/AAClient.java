package org.sahamati.snalib;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.sahamati.snalib.errors.SNAUnexpectedResponseError;
import org.sahamati.snalib.models.AARequest;
import org.sahamati.snalib.models.DispatchResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AAClient {

    private final SNAClient client;
    private final Gson gson;

    AAClient(SNAClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    // A successful return means the service accepted the request for telemetry processing. Please check the SNA logs and verify if the request has been processed successfully.
    public DispatchResponse dispatch(AARequest req) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callType", req.getCallType());
        payload.put("route", req.getRoute());
        payload.put("peerId", req.getPeerId());
        payload.put("peerType", req.getPeerType());
        payload.put("customerId", req.getCustomerId());
        payload.put("body", req.getBody());
        if (req.getHttpStatus() != null) payload.put("httpStatus", req.getHttpStatus());
        if (req.getAddlAttr() != null) payload.put("addlAttr", req.getAddlAttr());

        String body = client.post("/sna/v1/aa", gson.toJson(payload));
        try {
            return gson.fromJson(body, DispatchResponse.class);
        } catch (JsonSyntaxException e) {
            throw new SNAUnexpectedResponseError(200, "failed to decode dispatch response: " + e.getMessage(), body);
        }
    }
}
