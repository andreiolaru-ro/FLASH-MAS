package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;

/**
 * 
 */
public class InteroperabilityBridge extends Unit implements Entity<Pylon> {

	/**
	 * The agent name, if given.
	 */
	protected String											entityName;

	/**
	 * Keeps track of the platform prefixes and their corresponding pylon proxies.
	 */
	InteroperabilityRouter<InteroperableMessagingPylonProxy>	interoperabilityRouter	= new InteroperabilityRouter<>();

	/**
	 * The proxy through which the bridge entity receives agent waves, to be used by the pylon.
	 */
	WaveReceiver												waveInbox;

	/**
	 * @param configuration
	 */
	public InteroperabilityBridge(MultiTreeMap configuration) {
		entityName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		setUnitName(entityName);

		waveInbox = new WaveReceiver() {
			@Override
			public void receive(AgentWave wave) {
				receiveWave(wave);
			}
		};
	}

	protected void receiveWave(AgentWave wave) {
		if (!getName().equals(wave.getFirstDestinationElement()))
			// FIXME use log
			throw new IllegalStateException(
					"The first element in destination endpoint (" + wave.getValues(AgentWave.DESTINATION_ELEMENT)
							+ ") is not the address of this agent (" + getName() + ")");
		
		// already routed to this agent
		wave.removeFirstDestinationElement();
		
		if(!wave.getCompleteSource().startsWith(wave.getCompleteSource().split(AgentWave.ADDRESS_SEPARATOR, 2)[0]))
			// FIXME use log
			throw new IllegalStateException(
					"Source endpoint (" + wave.getCompleteSource() + ") does not start with the address of this agent ("
							+ wave.getCompleteSource().split(AgentWave.ADDRESS_SEPARATOR, 2)[0] + ")");

		postBridgeEvent(wave);
	}

	public boolean postBridgeEvent(AgentEvent event) {
		li("Routing [] through bridge [].", event.toString(), getName());

		if (event.getType().equals(AgentEventType.AGENT_WAVE)) {
			String destination = ((AgentWave) event).getCompleteDestination();
			InteroperableMessagingPylonProxy pylonProxy = interoperabilityRouter.getEndpoint(destination);

			if (pylonProxy != null)
				return pylonProxy.send((AgentWave) event);

			le("Can't route to [].", destination);
		}

		le("Can't route [].", event.toString());
		return false;
	}

	@Override
	public boolean start() {
		for (InteroperableMessagingPylonProxy pylonProxy : interoperabilityRouter.getAllEndpoints()) {
			boolean registerEntity = true;
			for (String platformPrefix : interoperabilityRouter.getAllPlatformPrefixes()) {
				pylonProxy.registerBridge(getName(), registerEntity ? waveInbox : null, platformPrefix);
				registerEntity = false;
			}
		}

		li("Bridge started");
		return true;
	}

	@Override
	public boolean stop() {
		for (InteroperableMessagingPylonProxy pylonProxy : interoperabilityRouter.getAllEndpoints())
			pylonProxy.unregister(getName(), waveInbox);

		li("Bridge stopped");
		return false;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		if (!(context instanceof InteroperableMessagingPylonProxy))
			return false;

		InteroperableMessagingPylonProxy pylonProxy = (InteroperableMessagingPylonProxy) context;
		interoperabilityRouter.addEndpoint(pylonProxy.getPlatformPrefix(), pylonProxy);

		lf("Context added: ", context.getEntityName());
		return true;
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
	public String getName() {
		return entityName;
	}
}
