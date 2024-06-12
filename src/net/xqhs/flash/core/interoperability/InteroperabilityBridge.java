package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
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

	/**
	 * This method is called when the bridge receives a message in order to further route it.
	 *
	 * @param wave
	 *            - the message to route.
	 */
	protected void receiveWave(AgentWave wave) {
		li("Routing [] through bridge [].", wave.toString(), getName());

		if (!getName().equals(wave.getFirstDestinationElement()))
			throw new IllegalStateException("The first element in destination endpoint (" + wave.getValues(AgentWave.DESTINATION_ELEMENT) + ") is not the address of this agent (" + getName() + ")");
		wave.removeFirstDestinationElement();
		wave.recomputeCompleteDestination();

		String destination = wave.getCompleteDestination();
		InteroperableMessagingPylonProxy pylonProxy = interoperabilityRouter.getRoutingDestination(destination);

		if (pylonProxy != null) {
			le("Found routing destination [] for [].", pylonProxy.getPlatformPrefix(), wave.toString());
			pylonProxy.send(wave);
			return;
		}

		le("Can't find routing destination for [].", wave.toString());
	}

	@Override
	public boolean start() {
		li("Starting bridge entity [].", getName());
		if (interoperabilityRouter.getAllDestinations() == null || interoperabilityRouter.getAllDestinations().isEmpty())
			throw new IllegalStateException("Bridge has had no context added.");

		for (InteroperableMessagingPylonProxy pylonProxy : interoperabilityRouter.getAllDestinations()) {
			boolean registerEntity = true;
			for (String platformPrefix : interoperabilityRouter.getAllPlatformPrefixes()) {
				if (!platformPrefix.equals(pylonProxy.getPlatformPrefix())) {
					pylonProxy.registerBridge(getName(), registerEntity ? waveInbox : null, platformPrefix);
					registerEntity = false;
				}
			}
		}

		li("Bridge entity [] started successfully.", getName());
		return true;
	}

	@Override
	public boolean stop() {
		li("Stopping bridge entity [].", getName());
		for (InteroperableMessagingPylonProxy pylonProxy : interoperabilityRouter.getAllDestinations())
			pylonProxy.unregister(getName(), waveInbox);

		li("Bridge entity [] stopped successfully.", getName());
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
		interoperabilityRouter.addRoutingDestinationForPlatform(pylonProxy.getPlatformPrefix(), pylonProxy);

		lf("Context added for bridge entity []: []", getName(), context.getEntityName());
		return true;
	}

	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		if (!(context instanceof InteroperableMessagingPylonProxy))
			return false;

		InteroperableMessagingPylonProxy pylonProxy = (InteroperableMessagingPylonProxy) context;
		interoperabilityRouter.removeRoutingDestinationForPlatform(pylonProxy.getPlatformPrefix(), pylonProxy);

		lf("Context removed for bridge entity []: []", getName(), context.getEntityName());
		return false;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof InteroperableMessagingPylonProxy)
			return addContext((InteroperableMessagingPylonProxy) context);
		return false;
	}

	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof InteroperableMessagingPylonProxy)
			return removeContext((InteroperableMessagingPylonProxy) context);
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
