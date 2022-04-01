package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The pylon needs to know the agents and the shadows
 */
public class ShadowPylon extends DefaultPylonImplementation {
    /**
     *  The thread that manages the message queue.
     */
    class MessageThread implements Runnable {

        @Override
        public void run() {

        }
    }

    protected Map<String, MessageReceiver> agentList = new HashMap<>();

    public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return ShadowPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public boolean register(String entityName, MessageReceiver receiver) {
           // System.out.println("Register " + entityName);
            if(!agentList.containsKey(entityName)) {
                agentList.put(entityName, receiver);
            }
           // System.out.println(agentList.toString());
            return true;
        }

        @Override
        public boolean send(String source, String destination, String content) {
            if (content.equals("stop")) {
                li("Agent " + source + " is leaving");
                agentList.remove(source);
            }
            return false;
        }

        @Override
        public String getEntityName() {
            return getName();
        }
    };

    /**
     * The attribute name of server address of this instance.
     */
    public static final String		HOME_SERVER_ADDRESS_NAME	= "connectTo";
    /**
     * The attribute name for the server port.
     */
    public static final String		HOME_SERVER_PORT_NAME		= "serverPort";

    protected boolean				hasServer       = false;
    protected int					serverPort      = -1;
    protected RegionServer serverEntity    = null;
    protected ArrayList<String>     serverList      = null;

    /**
     * The server address itself.
     */
    public String                HomeServerAddressName       = null;

    protected boolean useThread         = true;
    protected Thread messageThread      = null;

    protected String nodeName       = null;

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if(!super.configure(configuration))
            return false;
        if(configuration.isSimple(HOME_SERVER_ADDRESS_NAME)) {
            HomeServerAddressName = configuration.getAValue(HOME_SERVER_ADDRESS_NAME);
        }
        if(configuration.isSimple(HOME_SERVER_PORT_NAME)) {
            hasServer = true;
            serverPort = Integer.parseInt(configuration.getAValue(HOME_SERVER_PORT_NAME));
        }
        if(configuration.isSimple("servers")) {
            String s = configuration.getAValue("servers");
            s = s.substring(1, s.length()-1);
            serverList = new ArrayList<>(Arrays.asList(s.split(", ")));
            serverList.remove(HomeServerAddressName);
        }
        if(configuration.isSimple("pylon_name")) {
            this.name = configuration.getAValue("pylon_name");
        }
        return true;
    }

    @Override
    public boolean start() {
        /*
         * Starting server
         */
        if(hasServer) {
            serverEntity = new RegionServer(serverPort, serverList);
            serverEntity.start();
        }

        if(!super.start())
            return false;

        if(useThread) {
            messageThread = new Thread(new MessageThread());
            messageThread.start();
        }
        li("Started" + (useThread ? " with thread." : ""));

        return true;
    }

    @Override
    public boolean stop() {
        super.stop();

        /*
         * Stopping threads
         */
        if(useThread) {
            useThread = false;
            messageThread = null;
        }

        /*
         * Stopping server
         */
        if(hasServer)
            serverEntity.stop();
        return true;
    }

    @Override
    public boolean addContext(EntityProxy<Node> context) {
        if(!super.addContext(context))
            return false;
        nodeName = context.getEntityName();
        lf("Added node context ", nodeName);
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<?> context) {
        if(context instanceof Node.NodeProxy)
            return addContext((Node.NodeProxy) context);
        return false;
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardDesignation) {
        return null;
    }

    @Override
    public EntityProxy<Pylon> asContext() {
        return messagingProxy;
    }

}
