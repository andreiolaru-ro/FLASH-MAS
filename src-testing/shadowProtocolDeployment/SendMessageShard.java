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
        /**
         * The message index.
         */
        int index = 0;

        @Override
        public void run() {
            switch (actions.get(index).getType()) {
                case MOVE_TO_ANOTHER_NODE:
                    break;
                case SEND_MESSAGE:
                    sendMessage(actions.get(index).getContent(), "", actions.get(index).getDestination());
                    break;
            }
            index++;
            if (index == actions.size()) {
                action_timer.cancel();
            }
        }
    }

    /**
     * The list of actions that the node needs to execute.
     */
    List<Action> actions;

    /**
     * Timer for messaging.
     */
    Timer action_timer = null;

    /**
     * @param designation - the shard type
     * @see AgentShardCore#AgentShardCore(AgentShardDesignation)
     */
    protected SendMessageShard(AgentShardDesignation designation) {
        super(designation);
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(!super.configure(configuration))
            return false;
        List<String> test = new ArrayList<>(Arrays.asList(configuration.getAValue("Actions_List").split(";")));
        actions = test.stream().map(Action::jsonStringToAction).collect(Collectors.toList());
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
