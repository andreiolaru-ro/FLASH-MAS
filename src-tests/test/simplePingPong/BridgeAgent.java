package test.simplePingPong;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.webSocket.WebSocketMessagingShard;
import net.xqhs.util.logging.Unit;
import wsRegions.WSRegionsShard;

/**
 * 
 */
public class BridgeAgent extends Unit implements Agent {

	/**
	 * The name of the component parameter that contains the id of the ping agent.
	 */
	protected static final String	PING_AGENT_PARAMETER_NAME	= "ping";
	
	/**
	 * The name of the component parameter that contains the id of the ping back agent.
	 */
	protected static final String	PONG_AGENT_PARAMETER_NAME	= "pong";

	/**
	 * The agent name, if given.
	 */
	protected String				agentName;

	/**
	 * List of messagingShards.
	 */
	Map<String, MessagingShard>		messagingShards				= new HashMap<>();

	/**
	 * Cache for the name of the ping agents.
	 */
	List<String>					pingAgents					= null;

	/**
	 * Cache for the name of the ping agents.
	 */
	List<String>					pongAgents					= null;

	/**
	 * @param configuration
	 */
	public BridgeAgent(MultiTreeMap configuration) {
		agentName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		setUnitName(agentName);

		if (configuration.isSet(PING_AGENT_PARAMETER_NAME))
			pingAgents = configuration.getValues(PING_AGENT_PARAMETER_NAME);
		if (configuration.isSet(PONG_AGENT_PARAMETER_NAME))
			pongAgents = configuration.getValues(PONG_AGENT_PARAMETER_NAME);
	}

	protected boolean postAgentEvent(AgentEvent event) {
		li("received: " + event.toString());

		if (event.getType().equals(AgentEventType.AGENT_WAVE)) {
			String source = ((AgentWave) event).getCompleteSource();
			String target = "";
			MessagingShard msgShard = null;

			if (source.equals(AgentWave.makePath(pingAgents.get(0), PING_AGENT_PARAMETER_NAME))) {
				msgShard = messagingShards.get(WSRegionsShard.class.getName());
				if (msgShard == null)
					throw new IllegalStateException("No WSRegions messaging shard present");
				target = AgentWave.makePath(pongAgents.get(0), "pong");
			}

			if (source.equals(AgentWave.makePath(pongAgents.get(0), PONG_AGENT_PARAMETER_NAME))) {
				msgShard = messagingShards.get(WebSocketMessagingShard.class.getName());
				if (msgShard == null)
					throw new IllegalStateException("No websocket messaging shard present");
				target = AgentWave.makePath(pingAgents.get(0), "ping");
			}

			String replyContent = ((AgentWave) event).getContent() + " reply";
			return msgShard.sendMessage(AgentWave.makePath(getName(), "ping"),
					target, replyContent);
		}

		return false;
	}

	@Override
	public boolean start() {
		if (messagingShards == null)
			throw new IllegalStateException("No messaging shard present");

		for (MessagingShard msgShard : messagingShards.values())
			msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));

		li("Agent started");
		return true;
	}

	@Override
	public boolean stop() {
		for (MessagingShard msgShard : messagingShards.values())
			msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
		li("Agent stopped");
		return false;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String getName() {
		return agentName;
	}

	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		PylonProxy proxy = (PylonProxy) context;
		String recommendedShard = proxy
				.getRecommendedShardImplementation(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING));
		MessagingShard msgShard = null;

		try {
			msgShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard, null, true);
		} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
		}

		if (msgShard == null)
			return false;

		lf("Context added: ", context.getEntityName());

		messagingShards.put(recommendedShard, msgShard);
		msgShard.addContext(new ShardContainer() {
			@Override
			public String getEntityName() {
				return getName();
			}

			@Override
			public boolean postAgentEvent(AgentEvent event) {
				return BridgeAgent.this.postAgentEvent(event);
			}

			@Override
			public AgentShard getAgentShard(AgentShardDesignation designation) {
				return null;
			}
		});

		return msgShard.addGeneralContext(context);
	}

	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		return false;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof MessagingPylonProxy)
			return addContext((MessagingPylonProxy) context);
		return false;
	}

	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}

	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return null;
	}

	@Override
	protected void le(String message, Object... arguments) {
		super.le(message, arguments);
	}

	@Override
	protected void lf(String message, Object... arguments) {
		super.lf(message, arguments);
	}
}
