package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.xqhs.flash.shadowProtocol.MessageFactory.*;

public class ShadowAgentShard extends AbstractNameBasedMessagingShard {
    /**
     * Reference to the local pylon proxy
     */
    private MessagingPylonProxy pylon =     null;
    /**
     * The {@link MessageReceiver} instance of this shard.
     */
    public MessageReceiver inbox;

    public final Object lock = new Object();

    protected WebSocketClient client;
    /**
     * Default constructor
     */
    public ShadowAgentShard(String serverURI, String name) {
        super();
        {
            setUnitName(name);
            setLoggerType(PlatformUtils.platformLogType());
        }
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                Object obj = JSONValue.parse(content);
                if(obj == null) return;
                JSONObject mesg = (JSONObject) obj;
                String type = (String) mesg.get("action");
                switch (MessageFactory.ActionType.valueOf(type)) {
                    case RECEIVE_MESSAGE:
                        receiveMessage(source, destination, (String) mesg.get("content"));
                        pylon.send(source, destination, content);
                        break;
                    case MOVE_TO_ANOTHER_NODE:
                    case AGENT_READY_TO_STOP:
                        pylon.send(getName(), destination, content);
                        break;
                    default:
                        break;
                }
            }
        };
        try {
            int tries = 10;
            long space = 1000;
            while(tries > 0) {
                try {
                    client = new WebSocketClient(new URI(serverURI)) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {
                            li("New connection to server.");
                        }

                        @Override
                        public void onMessage(String s) {
                            Object obj = JSONValue.parse(s);
                            if(obj == null) return;
                            JSONObject message = (JSONObject) obj;
                            String str = (String) message.get("type");
                            String content;

                            switch (MessageFactory.MessageType.valueOf(str)) {
                                case CONTENT:
                                    content = createMonitorNotification(MessageFactory.ActionType.RECEIVE_MESSAGE, (String) message.get("content"));
                                    inbox.receive((String) message.get("source"), (String) message.get("destination"), content);
                                    li("Message from " + message.get("source") + ": " + message.get("content"));
                                    break;
                                case REQ_ACCEPT:
                                    li("[] Prepare to leave", getUnitName());
                                    content = createMonitorNotification(MessageFactory.ActionType.MOVE_TO_ANOTHER_NODE, null);
                                    inbox.receive(null, null, content);
                                    client.close();
                                    break;
                                default:
                                    le("Unknown type");
                            }
                        }

                        @Override
                        public void onClose(int i, String s, boolean b) {
                            lw("Closed with exit code " + i);
                        }

                        @Override
                        public void onError(Exception e) {
                            le(Arrays.toString(e.getStackTrace()));
                        }
                    };
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    le("Websocket didn't connect!");
                }
                if(client.connectBlocking())
                    break;
                Thread.sleep(space);
                tries--;
                System.out.println("Tries:" + tries);
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean sendMessage(String source, String target, String content) {
        li("Send message " + content + " from agent " + source + " to agent " + target);
        String notify_content = createMonitorNotification(ActionType.SEND_MESSAGE, content);
        pylon.send(this.getName(), target, notify_content);

        Map<String, String> data = new HashMap<>();
        data.put("destination", target);
        data.put("content", content);
        client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONTENT, data));
        return true;
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
        client.send(createMessage(pylon.getEntityName(), this.getName(), MessageFactory.MessageType.REGISTER, null));
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

        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START)) {
            this.register(getAgent().getEntityName());
        }

        if(event.getType().equals(AgentEvent.AgentEventType.BEFORE_MOVE)) {
            lf("Agent " + this.getName() + " wants to move to another node");
            client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.REQ_LEAVE, null));
        }

        if(event.getType().equals(AgentEvent.AgentEventType.AFTER_MOVE)) {
            String entityName = getAgent().getEntityName();
            String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null);
            pylon.send(this.getName(), pylon.getEntityName(), notify_content);

            pylon.register(entityName, inbox);
            client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONNECT, null));
        }
    }

    @Override
    public String getName() {
        return (getUnitName().split(".messaging"))[0];
    }
}
