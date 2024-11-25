package net.xqhs.flash.core.node.clientApp;

import java.io.Serializable;

public class Agent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String agentName;
    private String shardName;

    public Agent(String agentName, String shardName) {
        this.agentName = agentName;
        this.shardName = shardName;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getShardName() {
        return shardName;
    }

    @Override
    public String toString() {
        return "Agent{name='" + agentName + "', shard='" + shardName + "'}";
    }
}
