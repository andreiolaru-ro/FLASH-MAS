package websockets;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.*;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

public class WebSocketPylon extends DefaultPylonImplementation {
    /**
     * The type of this support infrastructure (its 'kind')
     */

    public static final String       WEBSOCKET_SUPPORT_NAME = "WebSocket";


    /**
     * The default name of server address of this instance.
     */
    public static final String        WEBSOCKET_SERVER_ADDRESS = "WEBSOCKET_SERVER_ADDRESS";

    /**
     * The server address itself.
     */
    protected String                  webSocketServerAddressName;

    /**
     * The proxy to the webSocket server; this is actually a webSocket client.
     */
    protected WebSocketClientProxy webSocketClient;

    public WebSocketPylon(String serverAddress) {
        super();
        this.configure(new MultiTreeMap().addSingleValue(WEBSOCKET_SERVER_ADDRESS, serverAddress));
        try {
            webSocketClient = new WebSocketClientProxy(new URI(webSocketServerAddressName));
            webSocketClient.connect();
            Thread.sleep(1000);
        } catch (URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MessagingPylonProxy messagingProxy =  new MessagingPylonProxy() {
        /*
         * The agent is registered within the webSocket client.
         * */
        @Override
        public boolean register(String agentName, MessageReceiver receiver) {
            webSocketClient.addReceiverAgent(agentName, receiver);
            String registerMessage = "name=" + agentName;
            webSocketClient.send(registerMessage);
            return true;
        }

        @Override
        public boolean send(String source, String destination, String content) {
            String destAgent = getAgentNameFromAddress(getAgentAddress(destination));
            String message = source + "@" + destAgent + "@" + content;
            webSocketClient.send(message);
            return true;
        }

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return WebSocketPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public String getEntityName() {
            return getName();
        }
    };

    public static class WebSocketMessaging extends AbstractMessagingShard {

        private static final long serialVersionUID = 2L;

        private MessagingPylonProxy pylon;

        public MessageReceiver inbox;

        public WebSocketMessaging() {
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
            pylon.register(getAgent().getEntityName(), inbox);
            return true;
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
    }

    class MessageThread implements Runnable
    {
        @Override
        public void run()
        {
            // System.out.println("oops");
            while(useThread)
            {
                if(messageQueue.isEmpty())
                    try
                    {
                        synchronized(messageQueue)
                        {
                            messageQueue.wait();
                        }
                    } catch(InterruptedException e)
                    {
                        // do nothing
                    }
                else
                {
                    Map.Entry<WebSocketPylon.WebSocketMessaging, Vector<String>> event = messageQueue.poll();
                    event.getKey().receiveMessage(event.getValue().get(0), event.getValue().get(1),
                            event.getValue().get(2));
                }
            }
        }
    }

    protected Map<String, WebSocketMessaging>										             registry		= new HashMap<>();

    protected boolean																                 useThread		= true;

    protected LinkedBlockingQueue<Map.Entry<WebSocketPylon.WebSocketMessaging, Vector<String>>> messageQueue	= null;

    protected Thread																                 messageThread	= null;

    @Override
    public boolean start()
    {
        if(!super.start())
            return false;
        if(useThread)
        {
            messageQueue = new LinkedBlockingQueue<>();
            messageThread = new Thread(new MessageThread());
            messageThread.start();
        }
        return true;
    }

    @Override
    public boolean stop()
    {
        super.stop();
        if(useThread)
        {
            useThread = false; // signal to the thread
            synchronized(messageQueue)
            {
                messageQueue.clear();
                messageQueue.notifyAll();
            }
            try
            {
                messageThread.join();
            } catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            messageQueue = null;
            messageThread = null;
        }
        return true;
    }

    @Override
    public boolean configure(MultiTreeMap configuration)
    {
        if(configuration.isSingleton(WEBSOCKET_SERVER_ADDRESS))
            webSocketServerAddressName = configuration.getSingleValue(WEBSOCKET_SERVER_ADDRESS);
        if(configuration.isSimple("name"))
            name = configuration.get("name");
        return true;
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardName)
    {
        if (shardName.equals(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)))
            return WebSocketPylon.WebSocketMessaging.class.getName();
        return super.getRecommendedShardImplementation(shardName);
    }

    @Override
    public String getName()
    {
        return WEBSOCKET_SUPPORT_NAME;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Pylon> asContext()
    {
        return messagingProxy;
    }

}
