package test.mobility;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

import java.util.Timer;
import java.util.TimerTask;

public class CountShard extends AgentShardCore {
    private boolean active = false;
    private transient Timer timer;
    public static final String DESIGNATION = "ALIVE_COUNT";
    int counter = 0;
    /**
     * The constructor assigns the designation to the shard.
     * <p>
     * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
     * parent agent or on other shards, as when the shard is created, the {@link AgentShardCore#parentAgent} member is
     * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
     * the method {@link AgentShardCore#parentChangeNotifier}.
     * <p>
     * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
     * {@link #shardInitializer()} method.
     *
     * @param designation - the designation of the shard, as instance of {@link StandardAgentShard}.
     */
    protected CountShard(AgentShardDesignation designation) {
        super(designation);
    }

    public CountShard() {
        this(AgentShardDesignation.autoDesignation(DESIGNATION));
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        switch (event.getType()) {
            case AGENT_START:
                active = true;
                setTimer();
                break;
            case AGENT_STOP:
                active = false;
                cancelTimer();
                break;
            default:
                // Nothing
        }
    }

    private void cancelTimer() {
        timer.cancel();
    }

    private void setTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                System.out.println(getAgent().getEntityName() + " up for " + counter + " seconds");
                counter++;
            }
        }, 0, 1000);

    }
}
