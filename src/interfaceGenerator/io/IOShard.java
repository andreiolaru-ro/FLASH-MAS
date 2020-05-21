package interfaceGenerator.io;

import interfaceGenerator.Pair;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;

public abstract class IOShard extends AgentShardCore {
    /**
     * The constructor assigns the designation to the shard.
     * <p>
     * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
     * parent agent or on other shards, as when the shard is created, the  member is
     * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
     * the method {@link AgentShardCore#parentChangeNotifier}.
     * <p>
     * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
     * {@link #shardInitializer()} method.
     */
    protected IOShard(AgentShardDesignation designation) {
        super(designation);
    }

    protected IOShard() {
        super(AgentShardDesignation.autoDesignation("IO"));
    }

    protected IOShard(MultiTreeMap configuration) {
        this();
    }

    public void getActiveInput(ArrayList<Pair<String, String>> values) throws Exception {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (Pair<String, String> value : values) {
            activeInput.add(value.getKey(), value.getValue());
        }
        super.getAgent().postAgentEvent(activeInput);
    }

    public abstract AgentWave getInput(String portName);

    public abstract void sendOutput(AgentWave agentWave);
}
