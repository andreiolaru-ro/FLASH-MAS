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
import net.xqhs.flash.core.interoperability.InteroperableMessagingPylonProxy;
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
 * @author Andrei Olaru
 */
public class WSRegionsPylon extends DefaultPylonImplementation {
	
	class WSRegionsPylonProxy implements WaveMessagingPylonProxy {

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
		}
		
		@Override
		public String getEntityName() {
			return getName();
		}
	}

	class InteroperableWSRegionsPylonProxy extends WSRegionsPylonProxy implements InteroperableMessagingPylonProxy {

		@Override
		public boolean registerBridge(String entityName, String platformPrefix) {
			send((AgentWave) new AgentWaveJson().addSourceElements(entityName, Constants.PROTOCOL)
					.add(Constants.EVENT_TYPE_KEY, Constants.MessageType.REGISTER.toString())
					.add(InteroperableMessagingPylonProxy.MESSAGE_BRIDGE_KEY, platformPrefix));

			return false;
		}

		@Override
		public String getPlatformPrefix() {
			return HomeServerAddressName.split(PLATFORM_PREFIX_SEPARATOR)[0];
		}
	}

	/**
	 * Agent list, that are located on this node.
	 */
	protected Map<String, WaveReceiver>	agentList				= new HashMap<>();

	/**
	 * The proxy offered by this pylon.
	 */
	public WaveMessagingPylonProxy		messagingProxy			= new InteroperableWSRegionsPylonProxy();
	
	/**
	 * The attribute name of server address of this instance.
	 */
	public static final String	REGION_SERVER_PARAMETER	= "connectTo";
	/**
	 * The attribute name for the server port.
	 */
	public static final String	IS_SERVER_PARAMETER		= "isServer";
	/**
	 * The attribute name for the list of other servers.
	 */
	public static final String	OTHER_SERVERS_PARAMETER	= "servers";
	
	/**
	 * <code>true</code> if a server should be created.
	 */
	protected boolean			hasServer				= false;
	/**
	 * The port on which to open the server.
	 */
	protected int				serverPort				= -1;
	/**
	 * The WS Regions server.
	 */
	protected RegionServer		serverEntity			= null;
	/**
	 * The other region servers the server should connect to.
	 */
	protected ArrayList<String>	serverList				= null;
	/**
	 * The address of this server.
	 */
	public String				HomeServerAddressName	= null;
	/**
	 * The wrapper over the WebSocket client.
	 */
	protected WSClient			wsClient				= null;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		
		if(configuration.isSimple(IS_SERVER_PARAMETER)) {
			hasServer = true;
			HomeServerAddressName = configuration.getAValue(IS_SERVER_PARAMETER);
			serverPort = Integer.parseInt(HomeServerAddressName.split(":")[1]);
			HomeServerAddressName = WebSocketPylon.WS_PROTOCOL_PREFIX + HomeServerAddressName;
		}
		else if(configuration.isSimple(REGION_SERVER_PARAMETER)) {
			HomeServerAddressName = WebSocketPylon.WS_PROTOCOL_PREFIX
					+ configuration.getAValue(REGION_SERVER_PARAMETER);
		}
		if(configuration.isSimple(OTHER_SERVERS_PARAMETER)) {
			String s = configuration.getAValue(OTHER_SERVERS_PARAMETER);
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
		return true;
	}
	
	@Override
	public boolean start() {
		if(hasServer) {
			serverEntity = new RegionServer(serverPort, serverList, (HomeServerAddressName.split("//"))[1]);
			serverEntity.start();
		}
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
		if(hasServer)
			serverEntity.stop();
		printStatus();
		return true;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		if(!super.addContext(context))
			return false;
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
	
	/**
	 * Processes a message received on the WebSocket connection.
	 * 
	 * @param message
	 */
	protected void processMessage(String message) {
		JsonObject json;
		try {
			json = JsonParser.parseString(message).getAsJsonObject();
		} catch(JsonSyntaxException e) {
			le("Exception [] when parsing []", e.getStackTrace(), message);
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
	
	/**
	 * Sends a message through the WebSocket connection.
	 * 
	 * @param wave
	 *            - the message to send (will be sent as a JSON).
	 * @return an indication of success.
	 */
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
				wsClient.client.getReadyState(), agentList.keySet());
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
	protected void lw(String message, Object... arguments) {
		super.lw(message, arguments);
	}
}
