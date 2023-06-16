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
import net.xqhs.flash.core.monitoring.MonitoringShard;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.IOShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.Element;
import org.yaml.snakeyaml.Yaml;

public class GuiShard extends IOShard {
	protected interface ComponentConnect {
		void sendOutput(String value);

		String getInput();
	}

	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2769555908800271606L;

	protected Element interfaceStructure;

	protected Map<String, Map<String, List<ComponentConnect>>> portRoleComponents = new HashMap<>();

	protected MonitoringShard monitor = null;

	public GuiShard() {
		super(StandardAgentShard.GUI.toAgentShardDesignation());
	}

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

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType() == AgentEventType.AGENT_START)
			if(getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation()) != null) {
				monitor = (MonitoringShard) getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation());
				//monitor.sendGuiUpdate(new Yaml().dump(interfaceStructure));
				monitor.addGuiElement(interfaceStructure);
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
	public void sendOutput(AgentWave wave) {
		String targetport = wave.getFirstDestinationElement();
		if(targetport.equals(getShardDesignation().toString()))
			targetport = wave.removeFirstDestinationElement().getFirstDestinationElement();
		
		if(!portRoleComponents.containsKey(targetport)) {
			le("Output target port [] not found for content [].", targetport, wave.getContent());
			return;
		}
		
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
		if(monitor != null)
			monitor.sendOutput(wave);
	}
}
