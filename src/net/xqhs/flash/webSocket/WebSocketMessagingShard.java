package net.xqhs.flash.webSocket;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * The {@link WebSocketMessagingShard} class manages the link between agent's messaging service and its pylon.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketMessagingShard extends AbstractNameBasedMessagingShard implements Serializable {

    private static final long serialVersionUID = 2L;

    private transient MessagingPylonProxy pylon;

    public transient MessageReceiver inbox;

    public WebSocketMessagingShard() {
        super();
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                receiveMessage(source, destination, content);
            }
        };
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
    {
        if(!(context instanceof MessagingPylonProxy))
            return false;
        pylon = (MessagingPylonProxy) context;
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event)
    {
        super.signalAgentEvent(event);
        if(event.getType().equals(AgentEventType.AGENT_START) && pylon != null)
            pylon.register(getAgent().getEntityName(), inbox);
    }

    @Override
    public boolean sendMessage(String target, String source, String content) {
        return pylon.send(target,source, content);
    }

    @Override
    protected void receiveMessage(String source, String destination, String content)
    {
        super.receiveMessage(source, destination, content);
    }


    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                receiveMessage(source, destination, content);
            }
        };
    }
}