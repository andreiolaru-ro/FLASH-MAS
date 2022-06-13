package shadowProtocolDeployment;

import maria.MobileCompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.io.Serializable;

/**
 * The agent to use for testing.
 */
public class CompositeAgentTest extends MobileCompositeAgent implements Serializable
{

    protected String agentName;

    public CompositeAgentTest() {
    }

    public CompositeAgentTest(MultiTreeMap configuration) {
        super(configuration);
        if (configuration.getAValue("agent_name") != null) {
            this.agentName = configuration.getAValue("agent_name");
        }
    }

    public String getName() {
        return this.agentName;
    }

    @Override
    protected AgentShard getShard(AgentShardDesignation designation) {
        return super.getShard(designation);
    }
}
