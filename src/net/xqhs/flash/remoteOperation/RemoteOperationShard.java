/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 *
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 *
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.remoteOperation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.Operation;
import net.xqhs.flash.core.util.Operation.BaseOperation;
import net.xqhs.flash.core.util.Operation.OperationName;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.GUILoad;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity.Fields;

/**
 * There can be more than one remote. There are two types of remotes:
 * <ul>
 * <li>"Away" remotes are contacted via messaging support infrastructures and updates are sent via the
 * {@link MessagingShard}.
 * <li>"Local" remotes are context for this entity, implement {@link RemoteLocalReceiver} and updates are relayed to
 * them directly. All context which implements {@link RemoteLocalReceiver} is automatically added to the list of
 * remotes.
 * </ul>
 *
 * @author Andrei Olaru
 * @author Florina Nastasoiu -- for the MonitoringShard and ControlShard that this is based on.
 */
public class RemoteOperationShard extends AgentShardGeneral {
	/**
	 * Operations supported by {@link RemoteOperationShard}.
	 */
	@SuppressWarnings("hiding")
	public enum Operations implements OperationName {
		/**
		 * Name of the operation to stop the agent remotely.
		 */
		REMOTE_STOP,
		START_APPLICATION,
		STOP_APPLICATION,
		PAUSE_APPLICATION;
	}

	/**
	 * The UID.
	 */
	private static final long		serialVersionUID		= 7298833269072319823L;
	/**
	 * Endpoint element of this shard.
	 */
	protected static final String	SHARD_ENDPOINT			= StandardAgentShard.REMOTE.shardName();
	/**
	 * Parameter in the configuration designating away remotes to add.
	 */
	public static final String		AWAY_REMOTE_PARAMETER	= "remote";
	/**
	 * Parameter for the initial delay before the first keepalive/GUI registration.
	 */
	public static final String		WAIT_PARAMETER_NAME		= "wait";
	/**
	 * Parameter for the minimum poll interval (in ms) for {@link PropertyContainer}s.
	 * Controls how often {@link #sendUpdate()} is called for the metric data path.
	 * Use {@code -1} to disable periodic property polling.
	 */
	public static final String		UPDATE_FREQUENCY_PARAM	= "update-frequency";
	/**
	 * The definition of the operation to stop the agent, corresponding to {@link Operations#REMOTE_STOP}.
	 */
	public static final Operation	REMOTE_STOP				= new BaseOperation(Operations.REMOTE_STOP, new String[] {});
	/**
	 * The default remote interface for the entity, formed of a stop button.
	 */
	protected static final String	INTERFACE_SPECIFICATION	= "children: [{ type: button, value: Stop, port: "
			+ REMOTE_STOP.s() + ", role: stop" + ", when: [{ standard-status: " + Fields.RUNNING_STATUS_STOPPED.name()
			+ ", style: disabled }], notify: " + SHARD_ENDPOINT + "/" + REMOTE_STOP.s() + " }]";

	/**
	 * The interface specification for this agent, which will be sent to central monitoring entities / web
	 * application(s).
	 */
	protected Element						interfaceSpecification	= new Element();
	/**
	 * Local remotes.
	 */
	protected Set<RemoteLocalReceiver>		localRemotes			= new HashSet<>();
	/**
	 * Away remotes.
	 */
	protected Set<String>					awayRemotes				= new HashSet<>();
	/**
	 * Fields describing entity status.
	 */
	protected Fields[]						entityStatus			= new Fields[2];
	/**
	 * Keepalive period in milliseconds.
	 */
	protected long							timeDelay				= 10000;
	/**
	 * Minimum time (ms) between two updates sent to the M&amp;C entity. {@code -1} means send immediately.
	 */
	protected long							updateFrequency			= -1;
	/**
	 * Buffered output waves per port, for the {@link #sendOutput} path.
	 * Each port keeps only the latest wave — a new {@link #sendOutput} call on the same port overwrites completely.
	 */
	protected Map<String, AgentWave>		bufferedOutputs			= new LinkedHashMap<>();
	/**
	 * Combined keepalive + initial GUI-registration timer (replaces the former separate remoteDelay timer).
	 * On the first tick it also sends the GUI update; on subsequent ticks it only sends keepalives.
	 */
	Timer									agentKeepAliveTimer		= new Timer();
	/**
	 * Periodic timer that fires {@link #sendUpdate()} when {@link PropertyContainer}s are registered.
	 */
	protected Timer							periodicSendUpdateTimer	= null;
	/**
	 * Maps property name to the {@link PropertyContainer} that registered it, used to detect duplicates.
	 */
	protected Map<String, PropertyContainer>		registeredProperties	= new HashMap<>();
	/**
	 * Maps each {@link PropertyContainer} to the set of property names it owns.
	 */
	protected Map<PropertyContainer, Set<String>>	propertyContainers		= new LinkedHashMap<>();

	{
		setUnitName(SHARD_ENDPOINT);
		setLoggerType(PlatformUtils.platformLogType());
	}

	/**
	 * No-argument constructor.
	 */
	public RemoteOperationShard() {
		super(StandardAgentShard.REMOTE.toAgentShardDesignation());
		entityStatus[0] = Fields.STATUS_UNKNOWN;
		entityStatus[1] = Fields.STATUS_UNKNOWN;
		interfaceSpecification = GUILoad.fromYaml(INTERFACE_SPECIFICATION);
	}

	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSimple(UPDATE_FREQUENCY_PARAM)) {
			try {
				updateFrequency = Long.parseLong(configuration.getAValue(UPDATE_FREQUENCY_PARAM));
			} catch(NumberFormatException e) {
				le("Parameter [] has non-numeric value: []", UPDATE_FREQUENCY_PARAM,
						configuration.getAValue(UPDATE_FREQUENCY_PARAM));
			}
		}
		return true;
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_WAVE:
				if(!(event instanceof AgentWave)
						|| !SHARD_ENDPOINT.equals(((AgentWave) event).getFirstDestinationElement()))
					break;
				AgentWave incomingWave = ((AgentWave) event).removeFirstDestinationElement();
				String command = incomingWave.getFirstDestinationElement().toUpperCase();
				switch(command) {
					case "REMOTE_STOP":
						li("Agent stopping requested remotely.");
						getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
						break;
					case "START_APPLICATION":
						li("Application Start requested remotely.");
						getAgent().postAgentEvent(new AgentEvent(AgentEventType.APPLICATION_START));
						break;
					case "STOP_APPLICATION":
						li("Application Stop requested remotely.");
						getAgent().postAgentEvent(new AgentEvent(AgentEventType.APPLICATION_STOP));
						break;
					case "PAUSE_APPLICATION":
						li("Application Pause requested remotely.");
						getAgent().postAgentEvent(new AgentEvent(AgentEventType.APPLICATION_PAUSE));
						break;
					default:
						lf("Unknown command received: []", command);
						break;
				}
				// // TODO this is not needed anymore, routing should be done directly
				// if(CentralMonitoringAndControlEntity.Operations.GUI_INPUT_TO_ENTITY ==
				// CentralMonitoringAndControlEntity.Operations
				// .getRoute(wave)) {
				// wave.removeFirstDestinationElement();
				// wave.removeKey(AgentWave.SOURCE_ELEMENT);
				// String port = wave.getFirstDestinationElement();
				// wave.addSourceElements(port);
				// wave.resetDestination(AgentWave.ADDRESS_SEPARATOR);
				// ((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation())).postActiveInput(port,
				// wave);
				// }
				return;
			case AGENT_START:
				entityStatus[0] = Fields.RUNNING_STATUS_RUNNING;
				long initialDelay = 0;
				if(getShardData().containsKey(WAIT_PARAMETER_NAME))
					initialDelay = Long.parseLong(getShardData().get(WAIT_PARAMETER_NAME));
				// Combined timer: on first tick performs the initial GUI registration (what the former
				// remoteDelay timer did), then sends keepalives on every subsequent tick.
				agentKeepAliveTimer.scheduleAtFixedRate(new TimerTask() {
					private boolean isFirstRun = true;

					@Override
					public void run() {
						if(isFirstRun) {
							sendGuiUpdate();
							isFirstRun = false;
						}
						AgentWave keepAliveWave = CentralMonitoringAndControlEntity.KEEP_ALIVE
								.instantiate(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME)
								.addSourceElements(SHARD_ENDPOINT);
						sendMessage(keepAliveWave);
					}
				}, initialDelay, timeDelay);
				sendStatusUpdate();
				return;
			case AGENT_STOP:
				entityStatus[1] = Fields.RUNNING_STATUS_STOPPED;
				agentKeepAliveTimer.cancel();
				if(periodicSendUpdateTimer != null) {
					periodicSendUpdateTimer.cancel();
					periodicSendUpdateTimer = null;
				}
				sendStatusUpdate();
				return;
			case APPLICATION_START:
				entityStatus[1] = Fields.APPLICATION_STATUS_RUNNING;
				sendStatusUpdate();
				return;
			case APPLICATION_PAUSE:
				entityStatus[1] = Fields.APPLICATION_STATUS_PAUSED;
				sendStatusUpdate();
				return;
			case APPLICATION_STOP:
				entityStatus[1] = Fields.APPLICATION_STATUS_STOPPED;
				sendStatusUpdate();
				return;
			default:
				break;
		}
		lf("Agent [] event unhandled [].", getAgent().getEntityName(), event);
	}

	/**
	 * Sends the current entity status to the M&amp;C entity.
	 */
	private void sendStatusUpdate() {
		String status = Arrays.stream(entityStatus).map(Fields::toString).collect(Collectors.joining(" | "));
		li("Status of [] is [].", getAgent().getEntityName(), status);
		AgentWave update = CentralMonitoringAndControlEntity.UPDATE_ENTITY_STATUS
				// FIXME this should be sent to each remote
				.instantiate(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, (Object[]) entityStatus)
				.addSourceElements(SHARD_ENDPOINT);
		if(!sendMessage(update))
			le("Status sending failed. Wave is []", update);
	}

	/**
	 * Adds a GUI element to the interface specification and sends a GUI update.
	 *
	 * @param element
	 *            the element to add.
	 */
	public void addGuiElement(Element element) {
		interfaceSpecification.addAllChildren(element.getChildren());
		sendGuiUpdate();
	}

	/**
	 * Removes a GUI element (and its children recursively) from the interface specification.
	 *
	 * @param element
	 *            the element to remove.
	 * @param _parent
	 *            the parent to search in; {@code null} means the root specification.
	 * @return {@code true} if the element was found and removed.
	 */
	public boolean removeGuiElement(Element element, Element _parent) {
		Element parent = _parent != null ? _parent : interfaceSpecification;
		for(Element e : parent.getChildren()) {
			if(e.equals(element)) {
				if(e.getChildren() != null)
					for(Element child : e.getChildren())
						removeGuiElement(child, e);
				parent.getChildren().remove(e);
				if(parent == interfaceSpecification)
					sendGuiUpdate();
				lf("Element [] removed from interface specification", element, interfaceSpecification);
				return true;
			}
			else if(e.getChildren() != null)
				if(removeGuiElement(element, e))
					return true;
		}
		if(parent == interfaceSpecification)
			le("Element [] not found", element.toString());
		return false;
	}

	/**
	 * Sends the full GUI specification to the central monitoring entity.
	 */
	public void sendGuiUpdate() {
		AgentWave update = CentralMonitoringAndControlEntity.UPDATE_ENTITY_GUI
				.instantiate(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, interfaceSpecification)
				.addSourceElements(SHARD_ENDPOINT);
		if(!sendMessage(update))
			le("GUI update failed. Wave is []", update);
		else
			lf("GUI update sent.");
	}

	/**
	 * Buffers a GUI output wave for the given port and sends it immediately to M&amp;C.
	 * <p>
	 * If a wave was already buffered for the same port, it is replaced completely (latest-wins per port).
	 * No rate limiting is applied here — {@link net.xqhs.flash.web.WebEntity} handles batching at its
	 * own frequency before forwarding to the GUI.
	 *
	 * @param wave
	 *            the output wave; its first destination element must be the target port.
	 */
	public void sendOutput(AgentWave wave) {
		// TODO check if waves are cloned when sending. Must clone waves because their destination / source parameters
		// are processed and changed
		String port = wave.getFirstDestinationElement();
		
		// the source is this shard, within this agent
		wave.addSourceElements(getAgent().getEntityName(), SHARD_ENDPOINT);
		// the wave already has the destination as port and the content to output to the port
		wave.prependDestination(CentralMonitoringAndControlEntity.ENTITY_GUI_OUTPUT.s());
		// FIXME this should be sent to each remote
		wave.prependDestination(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
		interfaceSpecification.applyUpdate(port, wave);
		bufferedOutputs.put(port, wave);
		sendUpdate();
	}

	/**
	 * Flushes all buffered output waves (one per port) and collects properties from all registered
	 * {@link PropertyContainer}s, sending one wave per container to {@code RECEIVE_METRIC}.
	 * <p>
	 * If {@link PropertyContainer}s are registered but {@link #updateFrequency} is negative, an error is logged.
	 */
	public void sendUpdate() {
		if(!bufferedOutputs.isEmpty()) {
			for(AgentWave buffered : bufferedOutputs.values()) {
				if(!sendMessage(buffered))
					le("sendUpdate (buffered output) failed. Wave is []", buffered);
				else
					lf("Buffered output wave sent.");
			}
			bufferedOutputs.clear();
		}
		if(!propertyContainers.isEmpty()) {
			if(updateFrequency < 0)
				le("PropertyContainers are registered but update-frequency is -1; periodic sends not possible.");
			else
				for(Map.Entry<PropertyContainer, Set<String>> entry : propertyContainers.entrySet()) {
					Map<String, String> props = entry.getKey().getProperties(entry.getValue());
					if(props == null)
						continue;
					AgentWave containerWave = new AgentWave(null,
							DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME,
							CentralMonitoringAndControlEntity.Operations.RECEIVE_METRIC.toString());
					for(Map.Entry<String, String> prop : props.entrySet())
						containerWave.add(prop.getKey(), prop.getValue());
					containerWave.addSourceElements(getAgent().getEntityName());
					if(!sendMessage(containerWave))
						le("sendUpdate (PropertyContainer) failed. Wave is []", containerWave);
					else
						lf("PropertyContainer update sent.");
				}
		}
	}

	/**
	 * Registers a {@link PropertyContainer} that will supply values for the given properties at each
	 * {@link #sendUpdate()} call. Properties are always sent to {@code RECEIVE_METRIC} on the central
	 * monitoring entity.
	 * <p>
	 * Logs an error and returns without registering if any property is already owned by another container. On the
	 * first successful registration a periodic {@link #sendUpdate()} timer is started (error if
	 * {@link #updateFrequency} is negative).
	 *
	 * @param callback
	 *            the container that will provide property values.
	 * @param properties
	 *            the set of property names this container is responsible for.
	 */
	public void registerOutputProperties(PropertyContainer callback, Set<String> properties) {
		for(String prop : properties)
			if(registeredProperties.containsKey(prop)) {
				le("Property [] is already registered by another container – registration aborted.", prop);
				return;
			}
		for(String prop : properties)
			registeredProperties.put(prop, callback);
		propertyContainers.put(callback, properties);
		lf("PropertyContainer registered for properties: []", properties);
		if(periodicSendUpdateTimer == null) {
			if(updateFrequency < 0)
				le("PropertyContainer registered but update-frequency is -1; cannot schedule periodic sendUpdate.");
			else {
				periodicSendUpdateTimer = new Timer(true);
				periodicSendUpdateTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						sendUpdate();
					}
				}, updateFrequency, updateFrequency);
				lf("Periodic sendUpdate timer started with period [] ms.", updateFrequency);
			}
		}
	}
}