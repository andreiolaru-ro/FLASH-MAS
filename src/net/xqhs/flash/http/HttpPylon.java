package net.xqhs.flash.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import net.xqhs.flash.core.agent.AgentWave;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class HttpPylon extends DefaultPylonImplementation
{

    private final ExecutorService asyncMessagesExecutorService = Executors.newFixedThreadPool(10);
    /**
     * The key in the JSON object which is assigned to the source of the message.
     */
    public static final String MESSAGE_SOURCE_KEY = "source";
    /**
     * The key in the JSON object which is assigned to the name of the node on which an entity executes (for
     * registration messages).
     */
    public static final String MESSAGE_NODE_KEY = "nodeName";
    /**
     * The key in the JSON object which is assigned to the destination of the message.
     */
    public static final String MESSAGE_DESTINATION_KEY = "destination";
    /**
     * The key in the JSON object which is assigned to the content of the message.
     */
    public static final String MESSAGE_CONTENT_KEY = "content";

    /**
     * The receivers for each agent.
     */
    protected Map<String, MessageReceiver> messageReceivers = new HashMap<>();
    
    protected Map<String, String> serviceRegistry = new ConcurrentHashMap<>();

    /**
     * The attribute name of server address of this instance.
     */
    public static final String HTTP_CONNECT_TO_SERVER_ADDRESS_NAME = "connectTo";
    /**
     * The attribute name for the server port.
     */
    public static final String HTTP_SERVER_PORT_NAME = "serverPort";
    
    public static final String HTTP_SERVER_HOST_NAME = "serverHost";
    
    /**
     * The prefix for Http server address.
     */
    public static final String HTTP_PROTOCOL_PREFIX = "http://";

    protected int serverPort = -1;
    
    protected String serverHostname = "localhost";

    /**
     * For the case in which a server must be created on this node, the entity that represents the server.
     */
    protected HttpServerEntity serverEntity;

    /**
     * The {@link HttpClient} instance to use.
     */
    protected HttpClient httpClient;
    
    private List<String> resourceNames = new ArrayList<>();
    
    public int getServerPort()
    {
        return serverPort;
    }
    
    public List<String> getResourceNames()
    {
        return resourceNames;
    }

    /**
     * The proxy to this pylon, to be referenced by any entities in the scope of this pylon.
     */
    public MessagingPylonProxy messagingProxy = new MessagingPylonProxy() {

        @Override
        public boolean register(String entityName, MessageReceiver receiver) {
            messageReceivers.put(entityName, receiver);
            return true;
        }

        /**
         * Send a message to the server.
         *
         * @param source
         *            - the source endpoint
         * @param destination
         *            - the destination endpoint
         * @param content
         *            - the content of the message
         * @return - an indication of success
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean send(String source, String destination, String content) {
            if(messageReceivers.containsKey(destination)) {
                messageReceivers.get(destination).receive(source, destination, content);
                return true;
            }
            JSONObject messageToServer = new JSONObject();
            messageToServer.put(MESSAGE_NODE_KEY, getNodeName());
            messageToServer.put(MESSAGE_SOURCE_KEY, source);
            messageToServer.put(MESSAGE_DESTINATION_KEY, destination);
            messageToServer.put(MESSAGE_CONTENT_KEY, content);
            
            String remoteDestinationUrl = serviceRegistry.get(destination.split(AgentWave.ADDRESS_SEPARATOR)[0]);
            if (remoteDestinationUrl == null)
            {
                le("The remote url of the destination " + destination + " is not known.");
                return false;
            }
            try
            {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Content-Type", "text/plain;charset=UTF-8")
                    .header("sourceAddress", HTTP_PROTOCOL_PREFIX + serverHostname + ":" + serverPort)
                    .uri(new URI(remoteDestinationUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(messageToServer.toString()))
                    .build();
                httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
            return HttpPylon.this.getRecommendedShardImplementation(shardType);
        }

        @Override
        public String getEntityName() {
            return getName();
        }
    };
    
    void receiveMessage(JSONObject jsonObject) {
        if(jsonObject == null) {
            le("null message received");
        }

        if(jsonObject.get("destination") == null) {
            le("No destination entity received.");
        }
        String destination = (String) jsonObject.get("destination");
        String localAddr = destination.split(AgentWave.ADDRESS_SEPARATOR)[0];
        if(!messageReceivers.containsKey(localAddr) || messageReceivers.get(localAddr) == null)
            le("Entity [] does not exist in the scope of this pylon.", localAddr);
        else {
            String source = (String) jsonObject.get("source");
            String sourceAddress = (String) jsonObject.get("sourceAddress");
            String content = (String) jsonObject.get("content");
            serviceRegistry.computeIfAbsent(source.split(AgentWave.ADDRESS_SEPARATOR)[0], a -> sourceAddress + "/" + a);
            messageReceivers.get(localAddr).receive(source, destination, content);
        }
    }
    
    @Override
    public boolean start() {
        serverEntity = new HttpServerEntity(this);
        serverEntity.start();
        
        Properties properties = System.getProperties();
        properties.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        
        httpClient = HttpClient.newBuilder()
            .executor(asyncMessagesExecutorService)
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        return true;
    }

    @Override
    public boolean stop() {
        super.stop();
        serverEntity.stop();
        li("Stopped");
        return true;
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if(!super.configure(configuration))
            return false;
        if(configuration.isSimple(HTTP_SERVER_PORT_NAME)) {
            serverPort = Integer.parseInt(configuration.getAValue(HTTP_SERVER_PORT_NAME));
        }
        if(configuration.isSimple(HTTP_SERVER_HOST_NAME)) {
            serverHostname = configuration.getAValue(HTTP_SERVER_HOST_NAME);
        }
        String agentKey = "agent";
        if (configuration.isHierarchical(agentKey))
        {
            resourceNames = new ArrayList<>(configuration.getATree(agentKey).getKeys());
        }
        if(configuration.isSimple(HTTP_CONNECT_TO_SERVER_ADDRESS_NAME)) {
            String remoteUrl = configuration.getAValue(HTTP_CONNECT_TO_SERVER_ADDRESS_NAME);
            String remoteDestination = remoteUrl.substring(remoteUrl.lastIndexOf("/") + 1);
            serviceRegistry.put(remoteDestination, remoteUrl);
        }
        if(configuration.isSimple(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)) {
            name = configuration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        }
        return true;
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardName) {
        if(shardName.equals(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)))
            return HttpMessagingShard.class.getName();
        return super.getRecommendedShardImplementation(shardName);
    }

    /**
     * @return the name of the local node, as configured in {@link DefaultPylonImplementation}.
     */
    protected String getNodeName() {
        return nodeName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Pylon> asContext() {
        return messagingProxy;
    }

    @Override
    protected void le(String message, Object... arguments) {
        super.le(message, arguments);
    }

    @Override
    protected void lw(String message, Object... arguments) {
        super.lw(message, arguments);
    }

    @Override
    protected void li(String message, Object... arguments) {
        super.li(message, arguments);
    }
}
