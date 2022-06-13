package shadowProtocolDeployment;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.*;
import java.util.stream.Collectors;

import static shadowProtocolDeployment.Action.jsonStringToAction;

public class SendMessageShard extends AgentShardGeneral {

    /**
     * Send message another agent.
     */
    class ActionTimer extends TimerTask {

        @Override
        public void run() {
            if (actions == null) {
                return;
            }
            if (index == actions.size()) {
                action_timer.cancel();
                return;
            }
            switch (actions.get(index).getType()) {
                case MOVE_TO_ANOTHER_NODE:
                    getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.BEFORE_MOVE));
                    break;
                case SEND_MESSAGE:
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
    public SendMessageShard() {
        super(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.CONTROL));
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(!super.configure(configuration))
            return false;
        if (configuration.getAValue("agent_name") != null) {
            TestClass test = new TestClass("src-testing/shadowProtocolDeployment/RandomTestCases/Test1.json");
            List<Action> testCase = test.generateTest(5, 0);
            Map<String, List<Action>> sortActions = test.filterActionsBySources(testCase);
            System.out.println(testCase);
            actions = sortActions.get(configuration.getAValue("agent_name"));
        }

        if (configuration.getAValue("Actions_List") != null) {
            List<String> test = new ArrayList<>(Arrays.asList(configuration.getAValue("Actions_List").split(";")));
            actions = test.stream().map(Action::jsonStringToAction).collect(Collectors.toList());
        }
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
                action_timer.schedule(new ActionTimer(), 10, 5000);
                break;
            case AGENT_STOP:
                action_timer.cancel();
                break;
            default:
                break;
        }
    }
}
