package net.xqhs.flash.remoteOperation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.support.MessagingShard;
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
	}
	
	/**
	 * The definition of the operation to stop the agent, corresponding to {@link Operations#REMOTE_STOP}.
	 */
	public static final Operation REMOTE_STOP = new BaseOperation(Operations.REMOTE_STOP, new String[] {});
	
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
	 * The default remote interface for the entity, formed of a stop button.
	 */
	protected static final String	INTERFACE_SPECIFICATION	= "children: [{ type: button, value: Stop, port: "
			+ REMOTE_STOP.s() + ", when: [{ running_status: running_status_running, style: disabled }], "
		    + "notify: " + SHARD_ENDPOINT + "/" + REMOTE_STOP.s() + " }]";
	
	/**
	 * The interface specification for this agent, which will be sent to central monitoring entities / web
	 * application(s).
	 */
	protected Element					interfaceSpecification	= new Element();
	/**
	 * Local remotes.
	 */
	protected Set<RemoteLocalReceiver>	localRemotes			= new HashSet<>();
	/**
	 * Away remotes.
	 */
	protected Set<String>				awayRemotes				= new HashSet<>();
	/**
	 * Fields describing entity status.
	 */
	protected Fields[]					entityStatus			= new Fields[2];
	
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
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			// agent waves are either for this shard, or are ignored
			if(!(event instanceof AgentWave)
					|| !SHARD_ENDPOINT.equals(((AgentWave) event).getFirstDestinationElement()))
				break;
			AgentWave wave = ((AgentWave) event).removeFirstDestinationElement(); // this entity name
			// original wave destination endpoint was AgentX/remote/remote_stop
			if(wave.getFirstDestinationElement().toUpperCase().equals(Operations.REMOTE_STOP.toString())) {
				li("Agent stopping requested remotely.");
				getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
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
			break;
		case AGENT_STOP:
			entityStatus[0] = Fields.RUNNING_STATUS_STOPPED;
			break;
		case SIMULATION_START:
			entityStatus[1] = Fields.APPLICATION_STATUS_RUNNING;
			break;
		case SIMULATION_PAUSE:
			entityStatus[1] = Fields.APPLICATION_STATUS_STOPPED;
			break;
		default:
			break;
		}
		switch(event.getType()) {
		case AGENT_START:
			sendGuiUpdate();
			//$FALL-THROUGH$
		case AGENT_STOP:
		case SIMULATION_PAUSE:
		case SIMULATION_START: {
			String status = Arrays.stream(entityStatus).map(p -> p.toString()).collect(Collectors.joining(" | "));
			li("Status of [] is [].", getParentName(), status);
			AgentWave update = CentralMonitoringAndControlEntity.UPDATE_ENTITY_STATUS
					// FIXME this should be sent to each remote
					.instantiate(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, (Object[]) entityStatus)
					.addSourceElements(SHARD_ENDPOINT);
			if(!sendMessage(update))
				le("Status sending failed. Wave is []", update);
			// TODO: change interfaceSpecification to reflect the new status
			return;
		}
		default:
			// nothing to do
		}
		lf("Agent [] event unhandled [].", getParentName(), event);
	}
	
	/**
	 * Adds a GUI element to the interface specification
	 * 
	 * @param element
	 *            the element to add
	 */
	public void addGuiElement(Element element) {
		interfaceSpecification.addAllChildren(element.getChildren());
		// lf("Element [] added to interface specification", element, interfaceSpecification);
		sendGuiUpdate();
	}
	
	/**
	 * Removes a GUI element from the interface specification, and recursively removes all children
	 * 
	 * @param element
	 *            the element to remove
	 * @param _parent
	 *            the parent element to remove the element from (if null, the root element is used)
	 * @return true if the element was removed, false otherwise
	 */
	public boolean removeGuiElement(Element element, Element _parent) {
		Element parent = _parent != null ? _parent : interfaceSpecification;
		
		for(Element e : parent.getChildren()) {
			if(e.equals(element)) {
				if(e.getChildren() != null)
					for(Element child : e.getChildren())
						removeGuiElement(child, e);
				parent.getChildren().remove(e);
				// FIXME this could be more clearly implemented
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
	 * Send the GUI specification to the central monitoring entity
	 */
	public void sendGuiUpdate() {
		AgentWave update = CentralMonitoringAndControlEntity.UPDATE_ENTITY_GUI
				.instantiate(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, interfaceSpecification)
				.addSourceElements(SHARD_ENDPOINT);
		if(!sendMessage(update))
			le("Status sending failed. Wave is []", update);
		else
			lf("GUI update sent.");
	}
	
	/**
	 * Relays a GUI output from the GUIShard to the central monitoring entity.
	 *
	 * @param wave
	 *            the output to send
	 */
	public void sendOutput(AgentWave wave) {
		// TODO check if waves are cloned when sending. Must clone waves because their destination / source parameters
		// are processed and changed
		String port = wave.getFirstDestinationElement();
		
		// the source is this shard, within this agent
		wave.addSourceElements(getParentName(), SHARD_ENDPOINT);
		// the wave already has the destination as port and the content to output to the port
		wave.prependDestination(CentralMonitoringAndControlEntity.ENTITY_GUI_OUTPUT.s());
		// FIXME this should be sent to each remote
		wave.prependDestination(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
		if(!sendMessage(wave))
			le("Failed to send output [].", wave);
		// TODO: update the GUI for the entity
		interfaceSpecification.applyUpdate(port, wave);
	}
}
