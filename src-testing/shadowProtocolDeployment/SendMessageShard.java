package shadowProtocolDeployment;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

import java.util.List;

public class SendMessageShard extends AgentShardGeneral {

    List<Action> actions;

    /**
     * @param designation
     * @see AgentShardCore#AgentShardCore(AgentShardDesignation)
     */
    protected SendMessageShard(AgentShardDesignation designation, List<Action> actions) {
        super(designation);
        this.actions = actions;
    }

    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        super.signalAgentEvent(event);
        switch(event.getType())
        {
            case AGENT_START:
                this.start();
                break;
            default:
                break;
        }
    }

    public boolean start() {
        if (actions != null) {
            for (Action action : actions) {
                switch (action.getType()) {
                    case MOVE_TO_ANOTHER_NODE:
                        break;
                    case SEND_MESSAGE:
                        sendMessage(action.getContent(), "", action.getDestination());
                        break;
                }
            }
        }
//        if (mess_shard instanceof ShadowAgentShard shadow_shard) {
//            String content = createMonitorNotification(MessageFactory.ActionType.AGENT_READY_TO_STOP, null);
//            shadow_shard.inbox.receive(null, null, content);
//           // System.out.println("Send stop");
//        }
        return true;
    }
}
