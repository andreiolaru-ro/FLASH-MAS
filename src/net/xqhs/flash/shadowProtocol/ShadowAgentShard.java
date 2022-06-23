package net.xqhs.flash.shadowProtocol;

import maria.NonSerializableShard;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static net.xqhs.flash.shadowProtocol.MessageFactory.*;

public class ShadowAgentShard extends AbstractNameBasedMessagingShard implements NonSerializableShard {
    /**
     * Reference to the local pylon proxy
     */
    private MessagingPylonProxy pylon =     null;

    /**
     * The {@link MessageReceiver} instance of this shard.
     */
    public MessageReceiver inbox;
    /**
     * the Websocket object connected to Region server.
     */
    protected WebSocketClient client;
    String serverURI;
    String name;
    /**
     * Default constructor
     */
    public ShadowAgentShard() {
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                Object obj = JSONValue.parse(content);
                if(obj == null) return;
                JSONObject mesg = (JSONObject) obj;
                String type = (String) mesg.get("action");
                switch (MessageFactory.ActionType.valueOf(type)) {
                    case RECEIVE_MESSAGE:
                        String server = (String) mesg.get("server");
                        if (server != null) {
                            serverURI = server;
                            li("After moving connect to " + serverURI);
                            break;
                        }
                        receiveMessage(source, destination, (String) mesg.get("content"));
                        pylon.send(source, destination, content);
                        break;
                    case MOVE_TO_ANOTHER_NODE:
                    default:
                        break;
                }
            }
        };
    }

    public void startShadowAgentShard(MessageType connection_type) {
        {
            setUnitName(name);
            setLoggerType(PlatformUtils.platformLogType());
        }
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
                                case AGENT_CONTENT:
                                    li("Received agent from " + message.get("source"));
                                    AgentEvent arrived_agent = new AgentWave();
                                    arrived_agent.add("content", (String) message.get("content"));
                                    arrived_agent.add("destination-complete", (String) message.get("destination"));
                                    getAgent().postAgentEvent(arrived_agent);
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
        client.send(createMessage(pylon.getEntityName(), this.getName(), connection_type, null));
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(!super.configure(configuration))
            return false;
        if (configuration.getAValue("connectTo") != null) {
            this.serverURI = configuration.getAValue("connectTo");
        }
        if (configuration.getAValue("agent_name") != null) {
            this.name = configuration.getAValue("agent_name");
        }
        return true;
    }

    @Override
    public boolean sendMessage(String source, String target, String content) {
        li("Send message " + content + " from agent " + source + " to agent " + target);
        String notify_content = createMonitorNotification(ActionType.SEND_MESSAGE, content);
        pylon.send(this.getName(), target, notify_content);

        Map<String, String> data = new HashMap<>();
        data.put("destination", target);
        data.put("content", content);
        if (client != null) {
            if (target.contains("Monitoring")) {
                return true;
            }
            if (source.contains("node") || target.contains("node")) {
                client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.AGENT_CONTENT, data));
            } else {
                client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONTENT, data));
            }
        }
        return true;
    }

    @Override
    protected void receiveMessage(String source, String destination, String content) {
        super.receiveMessage(source, destination, content);
    }

    @Override
    public void register(String entityName) {
        pylon.register(entityName, inbox);
      //  client.send(createMessage(pylon.getEntityName(), this.getName(), MessageFactory.MessageType.REGISTER, null));
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
            if (event.get("TO_FROM_TRANSIENT") != null) {
                le("Agent started after move.");
                String entityName = getAgent().getEntityName();
                String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null);
                pylon.send(this.getName(), pylon.getEntityName(), notify_content);

                pylon.register(entityName, inbox);
                startShadowAgentShard(MessageType.CONNECT);
            } else {
                this.register(getAgent().getEntityName());
                startShadowAgentShard(MessageType.REGISTER);
            }

        }

        if(event.getType().equals(AgentEvent.AgentEventType.BEFORE_MOVE)) {
            String target = event.get("TARGET");
            lf("Agent " + this.getName() + " wants to move to another node " + target);
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

    @Override
    public String getAgentAddress() {
        return this.getName();
    }
}
