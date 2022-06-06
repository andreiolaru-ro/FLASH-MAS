package shadowProtocolDeployment;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.shadowProtocol.MessageFactory;
import net.xqhs.flash.shadowProtocol.ShadowAgentShard;

import java.util.List;

import static net.xqhs.flash.shadowProtocol.MessageFactory.createMonitorNotification;


public class SendMessageShard extends AgentShardGeneral {

    List<Action> actions;

    AgentShard mess_shard;

    /**
     * @param designation
     * @see AgentShardCore#AgentShardCore(AgentShardDesignation)
     */
    protected SendMessageShard(AgentShardDesignation designation, List<Action> actions) {
        super(designation);
        this.actions = actions;
    }

    protected void attachMessagingShard(AgentShard shard) {
        this.mess_shard = shard;
    }

    public boolean start() {
        if (actions != null) {
            for (Action action : actions) {
                switch (action.getType()) {
                    case MOVE_TO_ANOTHER_NODE:
                        break;
                    case SEND_MESSAGE:
                        if (mess_shard instanceof ShadowAgentShard shadow_shard) {
                           // System.out.println(action);
                            shadow_shard.sendMessage(action.getSource(), action.getDestination(), action.getContent());
                        }
                        break;
                }
            }
        }
        if (mess_shard instanceof ShadowAgentShard shadow_shard) {
            String content = createMonitorNotification(MessageFactory.ActionType.AGENT_READY_TO_STOP, null);
            shadow_shard.inbox.receive(null, null, content);
           // System.out.println("Send stop");
        }
        return true;
    }
}
