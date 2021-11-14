package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.Map;

/**
 * The pylon needs to knoow the agents and the shadows
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

    public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return ShadowPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public boolean register(String entityName, MessageReceiver receiver) {
            //System.out.println("Register " + entityName);

            return true;
        }

        @Override
        public boolean send(String source, String destination, String content) {
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
    protected ShadowHomeServer      serverEntity    = null;

    /**
     * The server address itself.
     */
    protected String                HomeServerAddressName       = null;

    protected boolean useThread         = true;
    protected Thread messageThread      = null;

    /**
     * Information about agents and shadows
     */
    protected Map<ShadowHost, ArrayList<String>> shadowToAgents;

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
        this.name = "Shadow";
        return true;
    }

    @Override
    public boolean start() {
        /**
         * Starting server
         */
        if(hasServer) {
            serverEntity = new ShadowHomeServer(serverPort);
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

        /**
         * Stopping threads
         */
        if(useThread) {
            useThread = false;
            messageThread = null;
        }

        /**
         * Stopping server
         */
        if(hasServer)
            serverEntity.stop();
        return true;
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
