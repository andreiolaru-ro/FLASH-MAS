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
import java.util.HashMap;
import java.util.Map;

import static net.xqhs.flash.shadowProtocol.MessageFactory.*;

public class AgentShard extends AbstractNameBasedMessagingShard {

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

    private String agent_name = null;

    /**
     * Default constructor
     */
    public AgentShard(String serverURI, String name) {
        super();
        this.agent_name = name;
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                if (content.equals("stop")) {
                    pylon.send(agent_name, destination, content);
                } else {
                    receiveMessage(source, destination, content);
                }
            }
        };
        if (shadow == null) {
            String short_name = (name.split("-"))[0];
            li("The shadow doesn't exist!!");
            try {
                int tries = 10;
                long space = 1000;
                while(tries > 0) {
                    try {
                        shadow = new ShadowHost(new URI(serverURI), short_name);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        le("Shadow didn't connect!");
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
        li("Send message");
        Map<String, String> data = new HashMap<>();
        data.put("destination", target);
        data.put("content", content);
        return shadow.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONTENT, data));
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
        lf("On pylon " + pylon.getEntityName() + " we have agent " + entityName);
        shadow.addReceiverAgent(entityName, inbox);
        shadow.send(createMessage(pylon.getEntityName(), this.getName(), MessageFactory.MessageType.REGISTER, new HashMap<>()));
    }

    /**
     * Get the pylon from context
     */
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

        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START)) {
            this.register(getAgent().getEntityName());
        }

        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_WAVE)) {
            li("Received message");
        }

        if(event.getType().equals(AgentEvent.AgentEventType.BEFORE_MOVE)) {
            li("Agent wants to move to another pylon");
            shadow.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.REQ_LEAVE, new HashMap<>()));
        }

        if(event.getType().equals(AgentEvent.AgentEventType.AFTER_MOVE)) {
            String entityName = getAgent().getEntityName();
            pylon.register(entityName, inbox);
            lf("On pylon " + pylon.getEntityName() + " arrived the agent " + entityName);
            shadow.addReceiverAgent(entityName, inbox);
            shadow.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONNECT, new HashMap<>()));
        }


    }

    @Override
    public String getName() {
        return this.agent_name;
    }
}
