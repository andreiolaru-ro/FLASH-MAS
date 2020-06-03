package net.xqhs.flash.core.composite;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class ScheduledAgent extends CompositeAgent {
    /**
     * Constructor for {@link CompositeAgent} instances.
     * <p>
     * The configuration is used to extract the name of the agent from it (as the value associated with the
     * {@link DeploymentConfiguration#NAME_ATTRIBUTE_NAME} name).
     * <p>
     * Although the name may be null, it is strongly recommended that the agent is given a (unique) name, even one that
     * is automatically generated.
     *
     * @param configuration - the configuration, from which the name of the agent will be taken.
     */
    public ScheduledAgent(MultiTreeMap configuration) {
        super(configuration);
    }


    private long  runningTime = 0;

    private long startedTime = 0;

    public boolean isReady = false;


    public void setRunningTime(long time) {
        runningTime = time;
    }

    public long getRunningTime() {
        return runningTime;
    }

    public long getStartedTime () {
        return startedTime;
    }

    private void setStartedTime(long time) {
        startedTime = time;
    }

    @Override
    public void run()
    {
        setStartedTime(System.nanoTime());
        super.run();
    }

    public boolean suspend()
    {
        AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        event.addObject(TRANSIENT_EVENT_PARAMETER, true);
        return postAgentEvent(event);
    }

    //de supraincarcarcat pentru suspend() ca sa returneze TRUE
    //nu cred ca treb suprascie ca oricum returneaza TRUE
   /* @Override
    protected boolean FSMEventOut(AgentEvent.AgentEventType eventType, boolean toFromTransient)
    {}*/

}
