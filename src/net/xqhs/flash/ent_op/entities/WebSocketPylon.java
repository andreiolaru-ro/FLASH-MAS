package net.xqhs.flash.ent_op.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.ent_op.entities.operations.RouteOperation;
import net.xqhs.flash.ent_op.impl.OperationCallWave;
import net.xqhs.flash.ent_op.impl.RelationChangeWave;
import net.xqhs.flash.ent_op.impl.ResultWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.util.logging.Unit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static net.xqhs.flash.ent_op.entities.Node.NODE_NAME;

public class WebSocketPylon extends Unit implements Pylon {

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
     * The key in the JSON object which is assigned to the name of the entity (for registration messages).
     */
    public static final String MESSAGE_ENTITY_KEY = "entityName";

    /**
     * The key in the JSON object which is assigned to the destination of the message.
     */
    public static final String MESSAGE_DESTINATION_KEY = "destination";

    /**
     * The key in the JSON object which is assigned to the content of the message.
     */

    public static final String MESSAGE_CONTENT_KEY = "content";

    /**
     * The key in the JSON object which is assigned to the wave type.
     */
    public static final String WAVE_TYPE_KEY = "type";

    /**
     * The attribute name of server address of this instance.
     */
    public static final String WEBSOCKET_SERVER_ADDRESS_NAME = "connectTo";

    /**
     * The attribute name for the server port.
     */
    public static final String WEBSOCKET_SERVER_PORT_NAME = "serverPort";

    /**
     * The attribute name for the server port.
     */
    public static final String WEBSOCKET_PYLON_NAME = "pylonName";

    /**
     * The attribute name for the webSocket configuration.
     */
    public static final String WEBSOCKET_PYLON_CONFIG = "wsPylonConfig";

    /**
     * The prefix for WebSocket server address.
     */
    public static final String WS_PROTOCOL_PREFIX = "ws://";

    /**
     * The OPERATION_CALL wave name.
     */
    public static final String OPERATION_CALL_WAVE = "OPERATION_CALL";

    /**
     * The RELATION_CHANGE wave name.
     */
    public static final String RELATION_CHANGE_WAVE = "RELATION_CHANGE";

    /**
     * The RESULT wave name.
     */
    public static final String RESULT_WAVE = "RESULT";

    /**
     * The name of the WebSocketPylon.
     */
    public static String pylonName = "default ws pylon";

    /**
     * <code>true</code> if there is a Websocket server configured on the local node.
     */
    protected boolean hasServer;

    /**
     * For the case in which a server must be created on this node, the port the server is bound to.
     */
    protected int serverPort = -1;

    /**
     * For the case in which a server must be created on this node, the entity that represents the server.
     */
    protected WebSocketServerEntity serverEntity;

    /**
     * The address of the Websocket server that the client should connect to.
     */
    protected String webSocketServerAddress;

    /**
     * The {@link WebSocketClient} instance to use.
     */
    protected WebSocketClient webSocketClient;

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    /**
     * The node name.
     */
    protected String nodeName;

    /**
     * The framework instance.
     */
    protected FMas fMas;

    /**
     * The corresponding entity tools for this instance.
     */
    protected EntityTools entityTools;

    /**
     * The object mapper.
     */
    protected ObjectMapper mapper = new ObjectMapper();

    /**
     * The route operation .
     */
    protected List<Operation> operations = new LinkedList<>();

    @Override
    public boolean setup(MultiTreeMap configuration) {
        if (configuration.isSimple(WEBSOCKET_SERVER_PORT_NAME)) {
            hasServer = true;
            serverPort = Integer.parseInt(configuration.getAValue(WEBSOCKET_SERVER_PORT_NAME));
            webSocketServerAddress = WS_PROTOCOL_PREFIX + PlatformUtils.getLocalHostURI() + ":" + serverPort;
        } else if (configuration.isSimple(WEBSOCKET_SERVER_ADDRESS_NAME)) {
            webSocketServerAddress = configuration.getAValue(WEBSOCKET_SERVER_ADDRESS_NAME);
        }
        if (configuration.isSimple(WEBSOCKET_PYLON_NAME))
            pylonName = configuration.getAValue(WEBSOCKET_PYLON_NAME);
        if (configuration.isSimple(NODE_NAME))
            nodeName = configuration.getAValue(NODE_NAME);
        setUnitName(pylonName);

        RouteOperation routeOperation = new RouteOperation();
        operations.add(routeOperation);
        entityTools = fMas.registerEntity(this);
        entityTools.createOperation(routeOperation);

        return true;
    }

    @Override
    public boolean start() {
        if (hasServer) {
            serverEntity = new WebSocketServerEntity(serverPort);
            serverEntity.setup(new MultiTreeMap());
            serverEntity.start();
        }
        try {
            int tries = 10;
            long spaceBetweenTries = 1000;
            while (tries > 0) {
                try {
                    li("Trying connection to WS server ", webSocketServerAddress);
                    webSocketClient = new WebSocketClient(new URI(webSocketServerAddress)) {
                        @Override
                        public void onOpen(ServerHandshake serverHandshake) {
                            li("New connection to server opened.");
                        }

                        /**
                         * Receives a message from the server. The message was previously routed to this websocket
                         * client address, and it is further routed to a specific entity using the {@link FMas} instance.
                         *
                         * @param message
                         */
                        @Override
                        public void onMessage(String message) {
                            var jsonMessage = new JSONObject(message);

                            if (jsonMessage.get("destination") == null) {
                                le("No destination entity received.");
                                return;
                            }

                            var content = jsonMessage.getString("content");
                            deserializeWave(content).ifPresent(wave -> fMas.route(wave));
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
                    return false;
                }
                if (webSocketClient.connectBlocking())
                    break;
                Thread.sleep(spaceBetweenTries);
                tries--;
                System.out.println("Tries:" + tries);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean stop() {
        if (hasServer)
            serverEntity.stop();
        try {
            webSocketClient.closeBlocking();
        } catch (InterruptedException x) {
            x.printStackTrace();
        }
        li("Stopped");
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCallWave) {
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return pylonName;
    }

    @Override
    public List<Operation> getOperations() {
        return operations;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return entityID != null && entityID.ID.contains("ws:");
    }

    @Override
    public EntityID getEntityID() {
        return null;
    }

    public boolean register(String entityName) {
        if (webSocketClient == null)
            return false;
        JSONObject messageToServer = new JSONObject();
        messageToServer.put(MESSAGE_NODE_KEY, getNodeName());
        messageToServer.put(MESSAGE_ENTITY_KEY, entityName);
        webSocketClient.send(messageToServer.toString());
        return true;
    }

    public boolean send(String source, String destination, String content) {
        JSONObject messageToServer = new JSONObject();
        messageToServer.put(MESSAGE_NODE_KEY, getNodeName());
        messageToServer.put(MESSAGE_SOURCE_KEY, source);
        messageToServer.put(MESSAGE_DESTINATION_KEY, destination);
        messageToServer.put(MESSAGE_CONTENT_KEY, content);
        webSocketClient.send(messageToServer.toString());
        return true;
    }

    protected String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setfMas(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean canRouteOpCall(String destinationTarget) {
        return destinationTarget.startsWith("ws:");
    }

    private Optional<Wave> deserializeWave(String content) {
        return getWaveTypeClass(content)
                .map(x -> {
                    try {
                        return mapper.readValue(content, x);
                    } catch (JsonProcessingException e) {
                        le("The wave couldn't be deserialized.");
                    }
                    return null;
                });
    }

    private Optional<Class<? extends Wave>> getWaveTypeClass(String content) {
        var jsonObject = new JSONObject(content);
        var waveTypeStr = jsonObject.getString(WAVE_TYPE_KEY);
        Class<? extends Wave> waveTypeClass = null;

        switch (waveTypeStr) {
            case OPERATION_CALL_WAVE:
                waveTypeClass = OperationCallWave.class;
                break;
            case RELATION_CHANGE_WAVE:
                waveTypeClass = RelationChangeWave.class;
                break;
            case RESULT_WAVE:
                waveTypeClass = ResultWave.class;
                break;
            default:
                le("The [] wave is not supported by FLASH-MAS.", waveTypeStr);
        }

        return Optional.ofNullable(waveTypeClass);
    }
}
