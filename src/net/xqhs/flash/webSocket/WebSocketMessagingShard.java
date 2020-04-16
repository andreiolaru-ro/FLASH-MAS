package net.xqhs.flash.webSocket;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;

public class WebSocketMessagingShard extends AbstractNameBasedMessagingShard {

    private static final long serialVersionUID = 2L;

    private MessagingPylonProxy pylon;

    public MessageReceiver inbox;

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
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
        return true;
    }
	
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		if(event.getType().equals(AgentEventType.AGENT_START))
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
    public void registerNode(String nodeName, boolean isCentral) {
        pylon.registerNode(nodeName, isCentral);
    }
}