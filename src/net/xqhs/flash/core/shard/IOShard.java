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

import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;

/**
 * Parent for all shards that support communicating with the exterior via "ports". An example is a graphical user
 * interface. An IO interface is organized in <i>ports</i>, each port containing one or more <i>roles</i>. Ports and
 * roles are identified by strings.
 * 
 * <li>output -- information goes out through the port
 * <li>'passive' input -- information is read through the port at request, when {@link #getInput(String)} is called.
 * <li>'active' input -- the method {@link #postActiveInput} is called when input comes in through the port (acts as a
 * callback).
 * </ul>
 * Information is sent through ports by means of {@link AgentWave} instances containing value key-pairs. The key of each
 * pair is called a <i>role</i>.
 * 
 * @author andreiolaru
 */
public abstract class IOShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2775487340051334684L;
	
	/**
	 * No-argument constructor.
	 */
	protected IOShard() {
		super(StandardAgentShard.IO.toAgentShardDesignation());
	}
	
	/**
	 * Constructor with the given designation.
	 * 
	 * @param designation
	 *            - the designation of the shard.
	 */
	protected IOShard(AgentShardDesignation designation) {
		super(designation);
	}
	
	/**
	 * The methods is called from the exterior of the shard / of the shard container when an event generates input for
	 * the shard (e.g. a user clicks on a button in the GUI).
	 * 
	 * @param port
	 *            - the port that generated the input.
	 * @param values
	 *            - the roles in the port and their corresponding values that characterize the input event.
	 */
	public void postActiveInput(String port, Map<String, String> values) {
		AgentWave inputWave = new AgentWave(null, "/");
		for(String role : values.keySet())
			inputWave.add(role, values.get(role));
		postActiveInput(port, inputWave);
	}
	
	/**
	 * The methods is called from the exterior of the shard / of the shard container when an event generates input for
	 * the shard (e.g. a user clicks on a button in the GUI). In this version, the roles and values are already
	 * assembled in an {@link AgentWave} instance.
	 * 
	 * @param port
	 *            - the port that generated the input.
	 * @param inputWave
	 *            - an {@link AgentWave} containing the roles in the port and their corresponding values that
	 *            characterize the input event.
	 */
	public void postActiveInput(String port, AgentWave inputWave) {
		if(inputWave.getCompleteSource().length() == 0)
			inputWave.addSourceElementFirst(port);
		inputWave.addSourceElementFirst(getShardDesignation().toString());
		super.getAgent().postAgentEvent(inputWave);
	}
	
	/**
	 * The method should be called from within the shard to retrieve input from the port.
	 * 
	 * @param portName
	 *            - the port to get input from.
	 * @return an {@link AgentWave} containing the information obtained from the port. The wave should contain the name
	 *         of the port as {@link AgentWave#SOURCE_ELEMENT}.
	 */
	public abstract AgentWave getInput(String portName);
	
	/**
	 * The method should be called from within the shard to send output through a port. The port is found in the
	 * {@link AgentWave#DESTINATION_ELEMENT} of the wave.
	 * 
	 * @param agentWave
	 *            - the wave containing the information to be sent out.
	 * @return an indication of success in processing and conveying the output to the IO interface.
	 */
	public abstract boolean sendOutput(AgentWave agentWave);
}
