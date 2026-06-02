package org.sahamati.snalib.models;

import java.util.Map;

public final class AARequest {
    private final String callType;
    private final String route;
    private final String peerId;
    private final String peerType;
    private final String customerId;
    private final Integer httpStatus;
    private final Map<String, Object> body;
    private final Map<String, Object> addlAttr;

    private AARequest(Builder b) {
        this.callType = b.callType;
        this.route = b.route;
        this.peerId = b.peerId;
        this.peerType = b.peerType;
        this.customerId = b.customerId;
        this.httpStatus = b.httpStatus;
        this.body = b.body;
        this.addlAttr = b.addlAttr;
    }

    public String getCallType() { return callType; }
    public String getRoute() { return route; }
    public String getPeerId() { return peerId; }
    public String getPeerType() { return peerType; }
    public String getCustomerId() { return customerId; }
    public Integer getHttpStatus() { return httpStatus; }
    public Map<String, Object> getBody() { return body; }
    public Map<String, Object> getAddlAttr() { return addlAttr; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String callType;
        private String route;
        private String peerId;
        private String peerType;
        private String customerId;
        private Integer httpStatus;
        private Map<String, Object> body;
        private Map<String, Object> addlAttr;

        public Builder callType(String callType) { this.callType = callType; return this; }
        public Builder route(String route) { this.route = route; return this; }
        public Builder peerId(String peerId) { this.peerId = peerId; return this; }
        public Builder peerType(String peerType) { this.peerType = peerType; return this; }
        public Builder customerId(String customerId) { this.customerId = customerId; return this; }
        public Builder httpStatus(Integer httpStatus) { this.httpStatus = httpStatus; return this; }
        public Builder body(Map<String, Object> body) { this.body = body; return this; }
        public Builder addlAttr(Map<String, Object> addlAttr) { this.addlAttr = addlAttr; return this; }

        public AARequest build() { return new AARequest(this); }
    }
}
