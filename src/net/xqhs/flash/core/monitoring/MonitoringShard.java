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
package net.xqhs.flash.core.monitoring;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.JsonObject;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.IOShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.OperationUtils.MonitoringOperation;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.gui.structure.Element;

/**
 * Shard that allows monitoring, potentially remotely, the container of this shard.
 * <p>
 * The shard handles monitoring events in the agent, and builds the unified interface structure which is sent to
 * centralized monitoring entities. It also manages remote activations for active inputs (see {@link IOShard}).
 * <p>
 * When relevant events are signaled to the shard (e.g. agent start/stop and simulation start/stop) these state changes
 * are signaled to the central monitoring entity (via {@link #sendStatusUpdate}).
 * <p>
 * When changes occur to the interface specification or interface content #TODO
 * 
 * @author andreiolaru
 * @author Florina Nastasoiu
 */
public class MonitoringShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID	= 521488201837501593L;
	/**
	 * Endpoint element of this shard.
	 */
	protected static final String	SHARD_ENDPOINT		= StandardAgentShard.MONITORING.shardName();
	
	/**
	 * The interface for this agent, which will be sent to central monitoring entities / web application(s).
	 */
	private Element	interfaceSpecification	= new Element();
	/**
	 * Cache for the name of this agent.
	 */
	String			thisAgent				= null;
	
	{
		setUnitName("monitor");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Constructor.
	 */
	public MonitoringShard() {
		super(StandardAgentShard.MONITORING.toAgentShardDesignation());
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			if(!(event instanceof AgentWave)
					|| !SHARD_ENDPOINT.equals(((AgentWave) event).getFirstDestinationElement()))
				break;
			AgentWave wave = ((AgentWave) event).removeFirstDestinationElement();
			if(MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation().equals(wave.getFirstDestinationElement())) {
				wave.removeFirstDestinationElement();
				wave.removeKey(AgentWave.SOURCE_ELEMENT);
				String port = wave.getFirstDestinationElement();
				wave.addSourceElements(port);
				wave.resetDestination(AgentWave.ADDRESS_SEPARATOR);
				((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation())).postActiveInput(port,
						wave);
				return;
			}
			return;
		case AGENT_START:
			li("Shard []/[] started.", thisAgent, SHARD_ENDPOINT);
			sendStatusUpdate(event.getType().toString());
			return;
		case AGENT_STOP:
			li("Shard []/[] stopped.", thisAgent, SHARD_ENDPOINT);
			sendStatusUpdate(event.getType().toString());
			return;
		case SIMULATION_START:
			li("Shard []/[] started simulation.", thisAgent, SHARD_ENDPOINT);
			sendStatusUpdate(event.getType().toString());
			return;
		case SIMULATION_PAUSE:
			li("Shard []/[] paused simulation.", thisAgent, SHARD_ENDPOINT);
			sendStatusUpdate(event.getType().toString());
			return;
		default:
			break;
		}
		lf("Agent [] event [].", thisAgent, event);
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null)
			thisAgent = getAgent().getEntityName();
		else
			thisAgent = "<null>";
	}
	
	private void sendStatusUpdate(String status) {
		JsonObject update = OperationUtils.operationToJSON(
				OperationUtils.MonitoringOperation.STATUS_UPDATE.getOperation(), "", status,
				getAgent().getEntityName());
		sendMessage(update.toString(), SHARD_ENDPOINT, DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
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
		JsonObject update = new JsonObject();
		update.addProperty(OperationUtils.OPERATION_NAME, OperationUtils.MonitoringOperation.GUI_OUTPUT.getOperation());
		update.addProperty(OperationUtils.PARAMETERS, getAgent().getEntityName());
		output.prependDestination(thisAgent);
		update.addProperty(OperationUtils.VALUE, output.toSerializedString());
		update.addProperty(OperationUtils.PROXY, "");
		sendMessage(update.toString(), SHARD_ENDPOINT, DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
	}
	
	@Override
	protected MultiTreeMap getShardData() {
		return super.getShardData();
	}
}
