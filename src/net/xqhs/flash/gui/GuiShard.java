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
package net.xqhs.flash.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.IOShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

/**
 * Class for any shard that models a GUI.
 * 
 * @author andreiolaru
 * @author Florin Mihalache
 */
public class GuiShard extends IOShard {
	/**
	 * Connects each component of each role in each port to an actual component of the interface (e.g. a button).
	 */
	protected interface ComponentConnect {
		/**
		 * @param value
		 *            the value to output to the component.
		 */
		void sendOutput(String value);
		
		/**
		 * @return the value in the component.
		 */
		String getInput();
	}
	
	/**
	 * The UID.
	 */
	private static final long									serialVersionUID	= -2769555908800271606L;
	/**
	 * The description of the structure of the interface.
	 */
	protected Element											interfaceStructure;
	/**
	 * A mapping of ports to roles to list of components.
	 */
	protected Map<String, Map<String, List<ComponentConnect>>>	portRoleComponents	= new HashMap<>();
	/**
	 * A reference to the {@link RemoteOperationShard} if one exists.
	 */
	protected RemoteOperationShard								remoteShard			= null;
	
	/**
	 * No-argument constructor.
	 */
	public GuiShard() {
		super(StandardAgentShard.GUI.toAgentShardDesignation());
	}
	
	/**
	 * Loads the interface structure, via {@link GUILoad}.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		interfaceStructure = GUILoad.load(configuration, getLogger());
		if(interfaceStructure == null) {
			le("Interface load failed");
			return false;
		}
		return true;
	}
	
	/**
	 * Handles the {@link AgentEventType#AGENT_START} event to register the UIG to the {@link MonitoringShard}.
	 */
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation()) != null) {
				remoteShard = (RemoteOperationShard) getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				// TODO add this shard endpoint as handler endpoint for ports in the interface
				remoteShard.addGuiElement(interfaceStructure);
			}
			break;
		case AGENT_WAVE:
			if(event instanceof AgentWave
					&& StandardAgentShard.GUI.shardName().equals(((AgentWave) event).getFirstDestinationElement())) {
				((AgentWave) event).popDestinationElement();
				// handle the event directly and don't repost
				postActiveInput(((AgentWave) event).getFirstDestinationElement(), (AgentWave) event);
			}
			break;
		default:
			// nothing to do
		}
	}
	
	@Override
	public AgentWave getInput(String sourcePort) {
		// TODO check for multiple sources (e.g. remote interfaces) ?
		if(!portRoleComponents.containsKey(sourcePort)) {
			le("Input source port [] not found.", sourcePort);
			return null;
		}
		AgentWave result = new AgentWave().addSourceElementFirst(sourcePort);
		for(String role : portRoleComponents.get(sourcePort).keySet())
			for(ComponentConnect comp : portRoleComponents.get(sourcePort).get(role))
				result.add(role, comp.getInput());
		return result;
	}
	
	@Override
	public boolean sendOutput(AgentWave wave) {
		String targetport = wave.getFirstDestinationElement();
		if(targetport.equals(getShardDesignation().toString()))
			targetport = wave.removeFirstDestinationElement().getFirstDestinationElement();
		
		if(!portRoleComponents.containsKey(targetport))
			return ler(false, "Output target port [] not found for content [].", targetport, wave.getContent());
		
		Map<String, List<ComponentConnect>> roleMap = portRoleComponents.get(targetport);
		
		for(String role : wave.getContentElements())
			if(roleMap.containsKey(role)) {
				List<ComponentConnect> targetList = roleMap.get(role);
				if(targetList.size() != wave.getValues(role).size())
					lw("Wave number of values for role []/[] and number of available components differ: [] / [].",
							targetport, role, Integer.valueOf(wave.getValues(role).size()),
							Integer.valueOf(targetList.size()));
				for(int i = 0; i < Math.min(targetList.size(), wave.getValues(role).size()); i++)
					targetList.get(i).sendOutput(wave.getValues(role).get(i));
			}
			else
				le("Output role []/[] cannot be found.", targetport, role);
		if(remoteShard != null)
			remoteShard.sendOutput(wave);
		return true;
	}
}
