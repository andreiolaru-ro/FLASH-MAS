package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.OperationUtils;
import org.json.simple.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Agent it's a messaging shard that
 *  - comunicates with the Shadow for the regular checks
 *  - decrements the ttl
 *  - creates the proxy if necessary
 */

public class ShadowAgent extends AbstractNameBasedMessagingShard {

    /**
     * Actual value of the ttl
     */
    private int ttl;

    /**
     * The original value of the ttl
     */
    private int originalttl;

    /**
     * Reference to the local pylon proxy
     */
    private MessagingPylonProxy pylon =     null;

    /**
     * The {@link MessageReceiver} instance of this shard.
     */
    public MessageReceiver inbox;

    /**
     * The shadow instance of the agent
     */
    private ShadowHost shadow =     null;

    /**
     * Default constructor
     */
    public ShadowAgent(String serverURI) {
        super();
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                receiveMessage(source, destination, content);
            }
        };
        if (shadow == null) {
            System.out.println("The shadow doesn't exist!!");
            try {
                int tries = 10;
                long space = 1000;
                while(tries > 0) {
                    try {
                        shadow = new ShadowHost(new URI(serverURI));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        System.out.println("Shadow didn't connect");
                    }
                    if(shadow.connectBlocking())
                        break;
                    Thread.sleep(space);
                    tries--;
                    System.out.println("Tries:" + tries);
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("The shadow exist!!");
        }
    }

    @Override
    public boolean sendMessage(String source, String target, String content) {
        return pylon.send(target,source, content);
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
        System.out.println("On pylon " + pylon.getEntityName() + " we have agent " + entityName);
        shadow.addReceiverAgent(entityName, inbox);
        JSONObject messageToServer = new JSONObject();
        messageToServer.put("nodeName", pylon.getEntityName());
        messageToServer.put("entityName", entityName);
        shadow.send(messageToServer.toString());
    }

    /**
     * Get the pylon from context
     * @param context
     * @return
     */
    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        System.out.println("General context");
        if(!(context instanceof MessagingPylonProxy))
            return false;
        pylon = (MessagingPylonProxy) context;
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);

        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START))
            this.register(getAgent().getEntityName());
    }
}
