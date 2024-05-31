package net.xqhs.flash.core.interoperability;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
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
import net.xqhs.util.logging.Unit;

/**
 * 
 */
public class InteroperabilityBridge extends Unit implements Entity<Pylon>, ShardContainer {

	/**
	 * The agent name, if given.
	 */
	protected String						entityName;

	/**
	 * Router for routing to messagingShards.
	 */
	InteroperabilityRouter<MessagingShard>	interoperabilityRouter		= new InteroperabilityRouter<>();

	/**
	 * List of messagingShards for communication with each platform.
	 */
	Map<String, MessagingShard>				messagingShards				= new HashMap<>();

	/**
	 * List of interoperable pylon proxies for registering this bridge within each pylon.
	 */
	List<InteroperableMessagingPylonProxy>	interoperablePylonProxies	= new ArrayList<>();

	/**
	 * @param configuration
	 */
	public InteroperabilityBridge(MultiTreeMap configuration) {
		entityName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		setUnitName(entityName);
	}

	@Override
	public boolean postAgentEvent(AgentEvent event) {
		li("Routing [] through bridge [].", event.toString(), getName());

		if (event.getType().equals(AgentEventType.AGENT_WAVE)) {
			String destination = ((AgentWave) event).getCompleteDestination();
			MessagingShard msgShard = interoperabilityRouter.getEndpoint(destination);

			if (msgShard != null)
				return msgShard.sendMessage((AgentWave) event);
		}

		return false;
	}

	@Override
	public boolean start() {
		if (messagingShards == null)
			throw new IllegalStateException("No messaging shard present");

		for (MessagingShard msgShard : messagingShards.values())
			msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));

		for (InteroperableMessagingPylonProxy pylonProxy : interoperablePylonProxies) {
			for (String platformPrefix : interoperabilityRouter.getAllPlatformPrefixes()) {
				pylonProxy.registerBridge(getName(), platformPrefix, null); // TODO
			}
		}

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
	public boolean addContext(EntityProxy<Pylon> context) {
		PylonProxy proxy = (PylonProxy) context;
		String recommendedShard = proxy.getRecommendedShardImplementation(AgentShardDesignation.standardShard(StandardAgentShard.MESSAGING));
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
				return InteroperabilityBridge.this.postAgentEvent(event);
			}

			@Override
			public AgentShard getAgentShard(AgentShardDesignation designation) {
				return null;
			}
		});

		if (proxy instanceof InteroperableMessagingPylonProxy)
			interoperablePylonProxies.add((InteroperableMessagingPylonProxy) proxy);

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

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getName() {
		return entityName;
	}

	@Override
	public AgentShard getAgentShard(AgentShardDesignation designation) {
		// TODO Auto-generated method stub
		return null;
	}
}
