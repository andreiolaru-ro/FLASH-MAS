package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.xqhs.flash.shadowProtocol.MessageFactory.*;


public class ShadowPylon extends DefaultPylonImplementation {

    static class MessageThread implements Runnable {
        @Override
        public void run() {

        }
    }

    /**
     * Agents list, that are located on this node.
     */
    protected Map<String, MessageReceiver> agentList = new HashMap<>();

    public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return ShadowPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public boolean register(String entityName, MessageReceiver receiver) {
            lf("On pylon " + getEntityName() + " arrived the agent " + entityName);
            if(!agentList.containsKey(entityName)) {
                agentList.put(entityName, receiver);
            }
            Map<String, String> data = new HashMap<>();
            data.put("server", HomeServerAddressName);
            data.put("action", String.valueOf(ActionType.RECEIVE_MESSAGE));
            receiver.receive(getEntityName(), entityName, createMessage(getEntityName(), entityName, MessageType.CONTENT, data));
            return true;
        }

        @Override
        public boolean send(String source, String destination, String content) {
            Object obj = JSONValue.parse(content);
            if(obj == null) return false;
            JSONObject mesg = (JSONObject) obj;
            String type = (String) mesg.get("action");
            switch (MessageFactory.ActionType.valueOf(type)) {
                case RECEIVE_MESSAGE:
                case SEND_MESSAGE:
                case ARRIVED_ON_NODE:
                    monitor.inbox.receive(source, destination, content);
                    break;
                case MOVE_TO_ANOTHER_NODE:
                    monitor.inbox.receive(source, destination, content);
                    lf("Agent " + source + " is leaving");
                    agentList.remove(source);
                    break;
                default:
                    break;
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
    protected RegionServer          serverEntity    = null;
    protected ArrayList<String>     serverList      = null;
    public String                   HomeServerAddressName = null;

    protected boolean               useThread       = true;
    protected Thread                messageThread   = null;

    protected MonitoringEntity         monitor      = null;

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if(!super.configure(configuration)) return false;

        if(configuration.isSimple(HOME_SERVER_ADDRESS_NAME)) {
            HomeServerAddressName = configuration.getAValue(HOME_SERVER_ADDRESS_NAME);
        }
        if(configuration.isSimple(HOME_SERVER_PORT_NAME)) {
            hasServer = true;
            serverPort = Integer.parseInt(configuration.getAValue(HOME_SERVER_PORT_NAME));
        }
        if(configuration.isSimple("servers")) {
            String s = configuration.getAValue("servers");
            //s = s.substring(1, s.length()-1);
            serverList = new ArrayList<>(Arrays.asList(s.split("\\|")));
            String nickname = null;
            if (HomeServerAddressName != null) {
                nickname = (HomeServerAddressName.split("//"))[1];
            }
            serverList.remove(nickname);
        }
        if(configuration.isSimple("pylon_name")) {
            setUnitName(configuration.getAValue("pylon_name"));
            setLoggerType(PlatformUtils.platformLogType());
        }
        return true;
    }

    @Override
    public boolean start() {
        if(hasServer) {
            serverEntity = new RegionServer(serverPort, serverList);
            serverEntity.start();
        }

        if (monitor == null) {
            monitor = new MonitoringEntity(getName() + "-monitor");
            monitor.start();
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
        if(useThread) {
            useThread = false;
            messageThread = null;
        }
        if(hasServer)
            serverEntity.stop();
        monitor.stop();
        return true;
    }

    @Override
    public boolean addContext(EntityProxy<Node> context) {
        if(!super.addContext(context))
            return false;
        String nodeName = context.getEntityName();
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
    public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
        if(shardName.equals(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)))
            return ShadowAgentShard.class.getName();
        return super.getRecommendedShardImplementation(shardName);
    }

    @Override
    public EntityProxy<Pylon> asContext() {
        return messagingProxy;
    }

    @Override
    public String getName() { return getUnitName(); }
}
