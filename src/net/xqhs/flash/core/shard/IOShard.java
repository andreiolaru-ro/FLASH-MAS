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
package net.xqhs.flash.core.shard;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;

public abstract class IOShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2775487340051334684L;
	
	protected IOShard() {
		super(StandardAgentShard.IO.toAgentShardDesignation());
	}
	
	protected IOShard(AgentShardDesignation designation) {
		super(designation);
	}
	
	public void postActiveInput(String port, Map<String, String> values) {
		AgentWave inputWave = new AgentWave(null, "/");
		for(String role : values.keySet())
			inputWave.add(role, values.get(role));
		postActiveInput(port, inputWave);
	}
	
	public void postActiveInput(String port, AgentWave inputWave) {
		if(inputWave.getCompleteSource().length() == 0)
			inputWave.addSourceElementFirst(port);
		inputWave.addSourceElementFirst(getShardDesignation().toString());
		super.getAgent().postAgentEvent(inputWave);
	}
	
	public abstract AgentWave getInput(String portName);
	
	public abstract void sendOutput(AgentWave agentWave);
	
	public static HashMap<String, String> reducedInterfacesValues = new HashMap<>();
}
