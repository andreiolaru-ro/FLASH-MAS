package net.xqhs.flash.wsRegions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.webSocket.WebSocketPylon;

/**
 * {@link Pylon} for the WS Regions protocol.
 * 
 * @author Monica Pricope
 */
public class WSRegionsPylon extends DefaultPylonImplementation {
	/**
	 * Agent list, that are located on this node.
	 */
	protected Map<String, WaveReceiver> agentList = new HashMap<>();
	
	/**
	 * The proxy offered by this pylon.
	 */
	public WaveMessagingPylonProxy messagingProxy = new WaveMessagingPylonProxy() {
		
		@Override
		public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
			return WSRegionsPylon.this.getRecommendedShardImplementation(shardType);
		}
		
		@Override
		public boolean register(String entityName, WaveReceiver receiver) {
			if(agentList.containsKey(entityName)) {
				lw("Entity [] was already registered.", entityName);
				return false;
			}
			agentList.put(entityName, receiver);
			lf("Registered entity [].", entityName);
			printStatus();
			
			// Map<String, String> data = new HashMap<>();
			// data.put("server", HomeServerAddressName);
			// data.put("action", String.valueOf(ActionType.RECEIVE_MESSAGE));
			// receiver.receive(new AgentWave(
			// MessageFactory.createMessage(getEntityName(), entityName, MessageType.CONTENT, data), entityName));
			return true;
		}
		
		@Override
		public boolean unregister(String entityName, WaveReceiver registeredReceiver) {
			if(!agentList.containsKey(entityName)) {
				lw("Entity [] was not registered but tried to unregister.", entityName);
				printStatus();
				return false;
			}
			agentList.remove(entityName);
			lf("Entity [] unregistered.", entityName);
			printStatus();
			return true;
		}
		
		@Override
		public boolean send(AgentWave wave) {
			return WSRegionsPylon.this.send(wave);
			
			// FIXME this is monitoring actions, fix later.
			// Object obj = JSONValue.parse(content);
			// if(obj == null)
			// return false;
			// JSONObject mesg = (JSONObject) obj;
			// String type = (String) mesg.get("action");
			// switch(MessageFactory.ActionType.valueOf(type)) {
			// case RECEIVE_MESSAGE:
			// case SEND_MESSAGE:
			// case ARRIVED_ON_NODE:
			// monitor.inbox.receive(source, destination, content);
			// break;
			// case MOVE_TO_ANOTHER_NODE:
			// monitor.inbox.receive(source, destination, content);
			//
			// break;
			// default:
			// break;
			// }
		}
		
		@Override
		public String getEntityName() {
			return getName();
		}
	};
	
	/**
	 * The attribute name of server address of this instance.
	 */
	public static final String	HOME_SERVER_ADDRESS_NAME	= "connectTo";
	/**
	 * The attribute name for the server port.
	 */
	public static final String	HOME_SERVER_PORT_NAME		= "isServer";
	
	protected boolean			hasServer				= false;
	protected int				serverPort				= -1;
	protected RegionServer		serverEntity			= null;
	protected ArrayList<String>	serverList				= null;
	public String				HomeServerAddressName	= null;
	protected WSClient			wsClient				= null;
	
	// protected boolean useThread = true;
	// protected Thread messageThread = null;
	
	// protected MonitoringEntity monitor = null;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		
		if(configuration.isSimple(HOME_SERVER_PORT_NAME)) {
			hasServer = true;
			HomeServerAddressName = configuration.getAValue(HOME_SERVER_PORT_NAME);
			serverPort = Integer.parseInt(HomeServerAddressName.split(":")[1]);
			HomeServerAddressName = WebSocketPylon.WS_PROTOCOL_PREFIX + HomeServerAddressName;
		}
		else if(configuration.isSimple(HOME_SERVER_ADDRESS_NAME)) {
			HomeServerAddressName = WebSocketPylon.WS_PROTOCOL_PREFIX
					+ configuration.getAValue(HOME_SERVER_ADDRESS_NAME);
		}
		if(configuration.isSimple("servers")) {
			String s = configuration.getAValue("servers");
			// s = s.substring(1, s.length()-1);
			serverList = new ArrayList<>(Arrays.asList(s.split("\\|")));
			String nickname = null;
			if(HomeServerAddressName != null) {
				nickname = (HomeServerAddressName.split("//"))[1];
			}
			serverList.remove(nickname);
		}
		else {
			serverList = new ArrayList<>(0);
		}
		setUnitName(getName());
		// setLoggerType(PlatformUtils.platformLogType());
		return true;
	}
	
	@Override
	public boolean start() {
		if(hasServer) {
			serverEntity = new RegionServer(serverPort, serverList, (HomeServerAddressName.split("//"))[1]);
			serverEntity.start();
		}
		
		// if(monitor == null) {
		// monitor = new MonitoringEntity(getName() + "-monitor");
		// monitor.start();
		// }
		
		if(!super.start())
			return false;
		
		wsClient = new WSClient(URI.create(HomeServerAddressName), 10, 10000, getLogger()) {
			@Override
			public void onMessage(String message) {
				processMessage(message);
			}
		};
		printStatus();
		
		return true;
	}
	
	@Override
	public boolean stop() {
		super.stop();
		// if(useThread) {
		// useThread = false;
		// messageThread = null;
		// }
		if(hasServer)
			serverEntity.stop();
		// monitor.stop();
		printStatus();
		return true;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		if(!super.addContext(context))
			return false;
		// String nodeName = context.getEntityName();
		// lf("Added node context ", nodeName);
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
			return WSRegionsShard.class.getName();
		return super.getRecommendedShardImplementation(shardName);
	}
	
	protected void processMessage(String jsonString) {
		JsonObject json;
		try {
			json = JsonParser.parseString(jsonString).getAsJsonObject();
		} catch(JsonSyntaxException e) {
			le("Exception [] when parsing []", e.getStackTrace(), jsonString);
			return;
		}
		
		String destination;
		try {
			destination = json.get(AgentWave.DESTINATION_ELEMENT).getAsJsonArray().get(0).getAsString();
		} catch(Exception e) {
			le("Unable to parse destination in ", json);
			return;
		}
		if(!agentList.containsKey(destination) || agentList.get(destination) == null)
			le("Entity [] does not exist in the scope of this pylon. Current entities: ", destination,
					agentList.keySet());
		else {
			AgentWave wave;
			try {
				wave = AgentWaveJson.toAgentWave(json);
			} catch(Exception e) {
				le("Unable to convert message to AgentWave because []: []", e, json);
				return;
			}
			agentList.get(destination).receive(wave);
		}
	}
	
	protected boolean send(AgentWave wave) {
		JsonObject json = wave instanceof AgentWaveJson ? ((AgentWaveJson) wave).getJson() : AgentWaveJson.toJson(wave);
		wsClient.send(json.toString());
		return true;
	}
	
	/**
	 * Prints the status of this pylon.
	 */
	protected void printStatus() {
		lf("Current state: [] [] client: []. Current entities: ", isRunning() ? "RUNNING" : "STOPPED",
				hasServer ? "server: " + (serverEntity.isRunning() ? "RUNNING" : "STOPPED") : "no server",
				wsClient.client.getReadyState(),
				agentList.keySet());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Pylon> asContext() {
		return messagingProxy;
	}
	
	@Override
	protected void lf(String message, Object... arguments) {
		super.lf(message, arguments);
	}
	
	@Override
	protected void li(String message, Object... arguments) {
		super.li(message, arguments);
	}
	
	@Override
	protected void lw(String message, Object... arguments) {
		super.lw(message, arguments);
	}
}
