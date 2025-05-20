package net.xqhs.flash.core.interoperability;

import java.util.Iterator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.json.AgentWaveJson;
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
		if (wave.get(InteroperableMessagingPylonProxy.MULTI_PLATFORM_ROUTING_INFORMATION) != null) {
			li("Received multi-platform routing info [].", wave.toString());

			String content = wave.get(InteroperableMessagingPylonProxy.MULTI_PLATFORM_ROUTING_INFORMATION);
			String sourceName = wave.getFirstSource();
			InteroperableMessagingPylonProxy sourceProxy = interoperabilityRouter.getRoutingDestination(sourceName);
			if (sourceProxy == null) {
				le("Cannot find routing destination for [].", sourceName);
				le("No reason to try updating routing info.");
				return;
			}

			JsonParser parser = new JsonParser();
			JsonObject json = null;
			json = (JsonObject) parser.parse(content);

			if (interoperabilityRouter.updateRoutingInformation(json, sourceProxy)) {
				li("Updated info for [].", getName());

				// send to other ERs
				for (Iterator<InteroperableMessagingPylonProxy> iterator = interoperabilityRouter.getAllDestinations().iterator(); iterator.hasNext();) {
					Object next = iterator.next();
					if (!(next instanceof InteroperableMessagingPylonProxy))
						continue;

					InteroperableMessagingPylonProxy pylonProxy = (InteroperableMessagingPylonProxy) next;
					String destinationName = pylonProxy.getPlatformPrefix();

					JsonObject updatedRoutingInfo = interoperabilityRouter.getRoutingInfo(destinationName);
					AgentWaveJson updatedWave = (AgentWaveJson) new AgentWaveJson().addSourceElements(getName());
					updatedWave.add(InteroperableMessagingPylonProxy.MULTI_PLATFORM_ROUTING_INFORMATION, updatedRoutingInfo.toString());
					pylonProxy.send(updatedWave);
					li("Sent routing info [] to []", AgentWaveJson.toJson(updatedWave).toString(), destinationName);
				}

			} else {
				li("Known routing info before update [].", interoperabilityRouter.getAllRoutingInfo());
				li("Did not update routing info.");
			}

			return;
		}

		li("Routing [] through bridge [].", wave.toString(), getName());

		if (!getName().equals(wave.getFirstDestinationElement()))
			throw new IllegalStateException("The first element in destination endpoint (" + wave.getValues(AgentWave.DESTINATION_ELEMENT) + ") is not the address of this agent (" + getName() + ")");
		wave.removeFirstDestinationElement();
		wave.recomputeCompleteDestination();

		String destination = wave.getFirstDestinationElement();
		InteroperableMessagingPylonProxy pylonProxy = interoperabilityRouter.getRoutingDestination(destination);

		if (pylonProxy != null) {
			li("Found routing destination [] for [].", pylonProxy.getPlatformPrefix(), wave.toString());

			pylonProxy.send(wave);
			return;
		}

		le("Can't find routing destination for [].", wave.toString());
		li("Known routing info [].", interoperabilityRouter.getAllRoutingInfo());
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

			// this is for inter-zone routing
			// for inter-zone routing
//			AgentWave wave = new AgentWaveJson().appendDestination(pylonProxy.getPlatformPrefix()).addSourceElements(getName());
//			
//			// create json with entries target -> {next_hop: <name>. distance: <dist>}
//			String jsonToSend = interoperabilityRouter.getRoutingInfo().getAsString();
//			wave.add("content", jsonToSend);
//			wave.add(InteroperableMessagingPylonProxy.MULTI_PLATFORM_ROUTING_INFORMATION, "true");
//			pylonProxy.send(wave);
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
		interoperabilityRouter.addRoutingDestinationForPlatform(pylonProxy.getPlatformPrefix(), pylonProxy, 1);

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
