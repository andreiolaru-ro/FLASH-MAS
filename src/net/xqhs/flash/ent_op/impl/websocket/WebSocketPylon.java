package net.xqhs.flash.ent_op.impl.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.ent_op.entities.EntityCore;
import net.xqhs.flash.ent_op.entities.Node;
import net.xqhs.flash.ent_op.entities.Pylon;
import net.xqhs.flash.ent_op.impl.operations.RouteOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.RelationChangeResultWave;
import net.xqhs.flash.ent_op.impl.waves.RelationChangeWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Relation.RelationChangeType;
import net.xqhs.flash.ent_op.model.Wave;
import net.xqhs.flash.ent_op.model.Wave.WaveType;

public class WebSocketPylon extends EntityCore implements Pylon {
	
	/**
	 * The key in the JSON object which is assigned to the source of the message.
	 */
	public static final String	MESSAGE_SOURCE_KEY		= "source";
	/**
	 * The key in the JSON object which is assigned to the name of the node on which an entity executes (for
	 * registration messages).
	 */
	public static final String	MESSAGE_NODE_KEY		= "nodeName";
	/**
	 * The key in the JSON object which is assigned to the name of the entity (for registration messages).
	 */
	public static final String	MESSAGE_ENTITY_KEY		= "entityName";
	/**
	 * The key in the JSON object which is assigned to the destination of the message.
	 */
	public static final String	MESSAGE_DESTINATION_KEY	= "destination";
	/**
	 * The key in the JSON object which is assigned to the content of the message.
	 */
	public static final String	MESSAGE_CONTENT_KEY		= "content";
	/**
	 * The key in the JSON object which is assigned to the wave type.
	 */
	public static final String	WAVE_TYPE_KEY			= "type";
	
	/**
	 * The attribute name of server address of this instance.
	 */
	public static final String	WEBSOCKET_SERVER_ADDRESS_NAME	= "connectTo";
	/**
	 * The attribute name for the server port.
	 */
	public static final String	WEBSOCKET_SERVER_PORT_NAME		= "serverPort";
	/**
	 * The attribute name for the server port.
	 */
	public static final String	WEBSOCKET_PYLON_NAME			= "pylonName";
	/**
	 * The attribute name for the webSocket configuration.
	 */
	public static final String	WEBSOCKET_PYLON_CONFIG			= "wsPylonConfig";
	/**
	 * The name of the node in the context of which this pylon executes.
	 */
	public static final String	NODE_NAME						= "node_name";
	/**
	 * The prefix for WebSocket server address.
	 */
	public static final String	WS_PROTOCOL_PREFIX				= "ws://";
	
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
	 * The node name.
	 */
	protected String nodeName;
	
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
		if(configuration.isSimple(WEBSOCKET_PYLON_NAME))
			pylonName = configuration.getAValue(WEBSOCKET_PYLON_NAME);
		if(!configuration.containsSimpleName(EntityID.ENTITY_ID_ATTRIBUTE_NAME))
			configuration.addSingleValue(EntityID.ENTITY_ID_ATTRIBUTE_NAME, pylonName);
		super.setup(configuration);
		if(configuration.isSimple(WEBSOCKET_SERVER_PORT_NAME)) {
			hasServer = true;
			serverPort = Integer.parseInt(configuration.getAValue(WEBSOCKET_SERVER_PORT_NAME));
			webSocketServerAddress = WS_PROTOCOL_PREFIX + PlatformUtils.getLocalHostURI() + ":" + serverPort;
		}
		else if(configuration.isSimple(WEBSOCKET_SERVER_ADDRESS_NAME)) {
			webSocketServerAddress = configuration.getAValue(WEBSOCKET_SERVER_ADDRESS_NAME);
		}
		// FIXME: probably not right to use MESSAGE_NODE_KEY
		// if(configuration.isSimple(MESSAGE_NODE_KEY))
		// nodeName = configuration.getAValue(MESSAGE_NODE_KEY);
		return true;
	}
	
	@Override
	public boolean connectTools(OutboundEntityTools entityTools) {
		super.connectTools(entityTools);
		RouteOperation routeOperation = new RouteOperation();
		framework.createOperation(routeOperation);
		framework.createOperation(new Operation() {
			protected ArrayList<Value> arguments;
			
			@Override
			public boolean hasResult() {
				return false;
			}
			
			@Override
			public Value getResultType() {
				return null;
			}
			
			@Override
			public Set<Restriction> getRestrictions() {
				return null;
			}
			
			@Override
			public String getName() {
				return RouteOperation.REGISTER_OPERATION;
			}
			
			@Override
			public Description getDescription() {
				return null;
			}
			
			@Override
			public List<Value> getArguments() {
				if(arguments != null)
					return arguments;
				arguments = new ArrayList<>();
				arguments.add(new Value() {
					@Override
					public String getType() {
						return EntityID.class.getName();
					}
					
					@Override
					public Description getDescription() {
						return () -> "The ID of the entity to register";
					}
				});
				return null;
			}
		});
		return true;
	}
	
	@Override
	public boolean handleRelationChange(RelationChangeType changeType, Relation relation) {
		super.handleRelationChange(changeType, relation);
		if(changeType == RelationChangeType.CREATE && relation.getRelationName() == Node.EXECUTES_ON_RELATION)
			nodeName = relation.getTo().ID;
		li("Registered the node name []", nodeName);
		return true;
	}
	
	@Override
	public boolean start() {
		if(isRunning() && ((hasServer && serverEntity != null) || (!hasServer && webSocketClient != null)))
			// no need to re-start
			return false;
		super.start();
		if(hasServer) {
			serverEntity = new WebSocketServerEntity(serverPort);
			serverEntity.setup(new MultiTreeMap());
			serverEntity.start();
		}
		try {
			int tries = 10;
			long spaceBetweenTries = 1000;
			while(tries > 0) {
				try {
					li("Trying connection to WS server ", webSocketServerAddress);
					webSocketClient = new WebSocketClient(new URI(webSocketServerAddress)) {
						@Override
						public void onOpen(ServerHandshake serverHandshake) {
							li("New connection to server opened.");
						}
						
						/**
						 * Receives a message from the server. The message was previously routed to this websocket
						 * client address, and it is further routed to a specific entity using the {@link FMas}
						 * instance.
						 *
						 * @param message
						 */
						@Override
						public void onMessage(String message) {
							var jsonMessage = new JSONObject(message);
							
							if(jsonMessage.get("destination") == null) {
								le("No destination entity received.");
								return;
							}
							
							var content = jsonMessage.getString("content");
							deserializeWave(content).ifPresent(wave -> getFramework().handleOutgoingWave(wave));
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
				} catch(URISyntaxException e) {
					e.printStackTrace();
					return false;
				}
				if(webSocketClient.connectBlocking())
					break;
				Thread.sleep(spaceBetweenTries);
				tries--;
				System.out.println("Tries:" + tries);
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean stop() {
		if(hasServer)
			serverEntity.stop();
		try {
			webSocketClient.closeBlocking();
		} catch(InterruptedException x) {
			x.printStackTrace();
		}
		li("Stopped");
		return true;
	}
	
	@Override
	public Object handleIncomingOperationCall(OperationCallWave operationCallWave) {
		if(webSocketClient == null)
			return Boolean.FALSE;
		JSONObject messageToServer = new JSONObject();
		switch(operationCallWave.getTargetOperation()) {
		
		case RouteOperation.REGISTER_OPERATION:
			messageToServer.put(MESSAGE_NODE_KEY, nodeName);
			messageToServer.put(MESSAGE_ENTITY_KEY, operationCallWave.getArgumentValues().get(0));
			webSocketClient.send(messageToServer.toString());
			return Boolean.TRUE;
		
		case RouteOperation.ROUTE_OPERATION_NAME:
			Wave embeddedWave = (Wave) operationCallWave.getArgumentValues().get(0);
			messageToServer.put(MESSAGE_NODE_KEY, nodeName);
			messageToServer.put(MESSAGE_SOURCE_KEY, embeddedWave.getSourceEntity().ID);
			messageToServer.put(MESSAGE_DESTINATION_KEY, embeddedWave.getTargetEntity().ID);
			messageToServer.put(MESSAGE_CONTENT_KEY, serializeWave(embeddedWave));
			webSocketClient.send(messageToServer.toString());
			return Boolean.TRUE;
		default:
		
		}
		return null;
	}
	
	@Override
	public boolean canRoute(EntityID entityID) {
		return entityID != null && entityID.ID.startsWith("ws://");
	}
	
	@Override
	public boolean canRouteOpCall(String destinationTarget) {
		return destinationTarget.startsWith("ws:");
	}
	
	private Optional<Wave> deserializeWave(String content) {
		return getWaveTypeClass(content).map(aClass -> {
			try {
				return mapper.readValue(content, aClass);
			} catch(JsonProcessingException e) {
				le("The wave couldn't be deserialized []", e);
			}
			return null;
		});
	}
	
	private Optional<Class<? extends Wave>> getWaveTypeClass(String content) {
		var jsonObject = new JSONObject(content);
		var waveType = WaveType.valueOf(jsonObject.getString(WAVE_TYPE_KEY));
		Class<? extends Wave> waveTypeClass = null;
		
		switch(waveType) {
		case OPERATION_CALL:
			waveTypeClass = OperationCallWave.class;
			break;
		case RELATION_CHANGE:
			waveTypeClass = RelationChangeWave.class;
			break;
		case RESULT:
			waveTypeClass = ResultWave.class;
			break;
		case RELATION_CHANGE_RESULT:
			waveTypeClass = RelationChangeResultWave.class;
			break;
		default:
			le("The [] wave is not supported by FLASH-MAS.", waveType);
		}
		return Optional.ofNullable(waveTypeClass);
	}
	
	/**
	 * @return the entity tools.
	 */
	OutboundEntityTools getFramework() {
		return framework;
	}
	
	protected String serializeWave(Wave wave) {
		try {
			return mapper.writeValueAsString(wave);
		} catch(JsonProcessingException e) {
			le("The wave couldn't be serialized.");
			return null;
		}
	}
}
