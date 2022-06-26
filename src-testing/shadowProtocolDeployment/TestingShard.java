package shadowProtocolDeployment;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.*;
import java.util.stream.Collectors;

public class TestingShard extends AgentShardGeneral {

    public static final String TARGET = "TARGET";

    /**
     * Send message another agent.
     */
    class ActionTimer extends TimerTask {

        @Override
        public void run() {
            if (actions == null) {
                li("NO ACTIONS!");
                return;
            }
            if (index == actions.size()) {
                li("DONE!");
                //action_timer.cancel();
                return;
            }
            if (actions.get(index) == null) {
                li("NO ACTION " + index + " " + actions.size());
                //action_timer.cancel();
                return;
            }
            li("INDEX " + index + " " +  actions.get(index).getSource() + " " + actions.get(index));
            switch (actions.get(index).getType()) {
                case MOVE_TO_ANOTHER_NODE:
                    li("MOVE_TO_ANOTHER_NODE");
                    action_timer.cancel();
                    AgentEvent move_event = new AgentEvent(AgentEvent.AgentEventType.BEFORE_MOVE);
                    move_event.add(TARGET, "node-" + actions.get(index).getDestination());
                    move_event.add("pylon_destination", actions.get(index).getDestination().split("-")[0]);
                    getAgent().postAgentEvent(move_event);
                    break;
                case SEND_MESSAGE:
                    li("SEND_MESSAGE");
                    sendMessage(actions.get(index).getContent(), "", actions.get(index).getDestination());
                    break;
            }
            index++;
        }
    }

    /**
     * The message index.
     */
    int index = 0;

    /**
     * The list of actions that the node needs to execute.
     */
    List<Action> actions = new ArrayList<>();

    /**
     * Timer for messaging.
     */
    transient Timer action_timer = null;

    /**
     * @see AgentShardCore#AgentShardCore(AgentShardDesignation)
     */
    public TestingShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.CONTROL));
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(!super.configure(configuration))
            return false;
        if (configuration.getAValue("agent_name") != null) {
            TestClass test = new TestClass("src-testing/shadowProtocolDeployment/RandomTestCases/topology1_2_servers_2_pylons_2_agents.json");
            List<Action> testCase = test.generateTest(5, 0);
            Map<String, List<Action>> sortActions = test.filterActionsBySources(testCase);
            actions = sortActions.get(configuration.getAValue("agent_name"));
        }

        if (configuration.getAValue("Actions_List") != null) {
            List<String> test = new ArrayList<>(Arrays.asList(configuration.getAValue("Actions_List").split(";")));
            actions = test.stream().map(Action::jsonStringToAction).collect(Collectors.toList());
        }
        li("SIZE " + actions.size());
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        super.signalAgentEvent(event);
        switch(event.getType())
        {
            case AGENT_START:
                action_timer = new Timer();
                action_timer.schedule(new ActionTimer(), 1000, 5000);
                break;
            case AGENT_STOP:
                action_timer.cancel();
                break;
            default:
                break;
        }
    }
}
