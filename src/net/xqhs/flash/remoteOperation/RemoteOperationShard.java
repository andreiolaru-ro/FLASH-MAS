package net.xqhs.flash.remoteOperation;

import java.util.HashSet;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.JsonObject;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.structure.Element;

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
	
	{
		setUnitName(SHARD_ENDPOINT);
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * No-argument constructor.
	 */
	public RemoteOperationShard() {
		super(StandardAgentShard.REMOTE.toAgentShardDesignation());
	}
	
	/**
	 * Adds a GUI element to the interface specification
	 * 
	 * @param element
	 *            the element to add
	 */
	public void addGuiElement(Element element) {
		this.interfaceSpecification.addAllChildren(element.getChildren());
		// lf("Element [] added to interface specification", element, interfaceSpecification);
		sendGuiUpdate(new Yaml().dump(this.interfaceSpecification));
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
		Element parent = _parent != null ? _parent : this.interfaceSpecification;
		
		for(Element e : parent.getChildren()) {
			if(e.equals(element)) {
				if(e.getChildren() != null)
					for(Element child : e.getChildren())
						removeGuiElement(child, e);
				parent.getChildren().remove(e);
				if(parent == this.interfaceSpecification)
					sendGuiUpdate(new Yaml().dump(this.interfaceSpecification));
				lf("Element [] removed from interface specification", element, interfaceSpecification);
				return true;
			}
			else if(e.getChildren() != null)
				if(removeGuiElement(element, e))
					return true;
		}
		if(parent == this.interfaceSpecification)
			le("Element [] not found", element.toString());
		return false;
	}
	
	/**
	 * send a gui update to the central monitoring entity
	 * 
	 * @param specification
	 *            the interface specification to send
	 */
	public void sendGuiUpdate(String specification) {
		JsonObject update = OperationUtils.operationToJSON(OperationUtils.MonitoringOperation.GUI_UPDATE.getOperation(),
				"", specification, getAgent().getEntityName());
		sendMessage(update.toString(), SHARD_ENDPOINT, DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
	}
	
	/**
	 * Relays a GUI output from the GUIShard to the central monitoring entity.
	 *
	 * @param output
	 *            the output to send
	 */
	public void sendOutput(AgentWave output) {
		// the wave has the destination as port and the content to output to the port
		
		// the source is this shard, within this agent
		output.addSourceElements(getParentName(), SHARD_ENDPOINT);
		// the destination is the port in the interface for this agent, in the central entity
		output.prependDestination();
		// TODO check if waves are cloned when sending. Must clone waves because their destination / source parameters
		// are processed and changed
		
		JsonObject update = new JsonObject();
		update.addProperty(OperationUtils.OPERATION_NAME, OperationUtils.MonitoringOperation.GUI_OUTPUT.getOperation());
		update.addProperty(OperationUtils.PARAMETERS, getAgent().getEntityName());
		update.addProperty(OperationUtils.VALUE, output.toSerializedString());
		update.addProperty(OperationUtils.PROXY, "");
		sendMessage(update.toString(), SHARD_ENDPOINT, DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
	}
}
