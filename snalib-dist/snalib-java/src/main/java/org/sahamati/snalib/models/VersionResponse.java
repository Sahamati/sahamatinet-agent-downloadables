package org.sahamati.snalib.models;

import org.sahamati.snalib.SNAResponse;

public class VersionResponse extends SNAResponse {
    private String agentVersion;
    private String name;

    private VersionResponse() {}

    public String getAgentVersion() { return agentVersion; }
    public String getName() { return name; }

    public static VersionResponse error(String msg) {
        VersionResponse r = new VersionResponse();
        r.markError(msg);
        return r;
    }
}
