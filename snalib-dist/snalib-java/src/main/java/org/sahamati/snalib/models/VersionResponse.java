package org.sahamati.snalib.models;

public final class VersionResponse {
    private String agentVersion;
    private String name;

    private VersionResponse() {}

    public String getAgentVersion() { return agentVersion; }
    public String getName() { return name; }
}
