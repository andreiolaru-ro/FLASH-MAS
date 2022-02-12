package net.xqhs.flash.http;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.OperationUtils;
import org.json.simple.JSONObject;

public class HttpMessagingShard extends AbstractNameBasedMessagingShard {

    /**
     * The serial UID.
     */
    private static final long serialVersionUID = 2L;

    /**
     * Endpoint name for this shard (see {@link AgentWave}).
     */
    protected static final String SHARD_ENDPOINT = "messaging";

    /**
     * Reference to the local Websocket pylon.
     */
    private MessagingPylonProxy pylon;

    /**
     * The proxy to this shard, to be used by the pylon.
     */
    public MessageReceiver inbox;
    
    public HttpMessagingShard() {
        inbox = this::receiveMessage;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if(!(context instanceof MessagingPylonProxy))
            return false;
        pylon = (MessagingPylonProxy) context;
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START))
            pylon.register(getAgent().getEntityName(), inbox);
        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_WAVE))
            if((((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT)) {
                JSONObject msg = OperationUtils.operationToJSON("message", "", ((AgentWave) event).getContent(),
                        DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
                sendMessage(getAgent().getEntityName() + "/" + SHARD_ENDPOINT,
                        DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME + "/control", msg.toString());
            }
    }
    
    

    @Override
    public boolean sendMessage(String target, String source, String content) {
        return pylon.send(target, source, content);
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    /**
     * This can be called by non-agent entities to register their messaging shard.
     */
    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
    }
}
