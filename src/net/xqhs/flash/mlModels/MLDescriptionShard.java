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
package net.xqhs.flash.mlModels;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * The {@link MLDescriptionShard} class manages the SPARQL queries and transfering of ML models.
 *
 *  @author Daniel Liurca
 */
public class MLDescriptionShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long	serialVersionUID	= 5214882018809437402L;
	/**
	 * Endpoint element for this shard.
	 */
	public static final String	SHARD_ENDPOINT		= "ml_description";

	public static final String	FUNCTIONALITY	    = "ML_DESCRIPTION";

	/**
	 * Cache for the name of this agent.
	 */
	String						thisAgent		    = null;

	{
		setUnitName("ml_description-shard");
		setLoggerType(PlatformUtils.platformLogType());
	}

	/**
	 * Default constructor
	 */
	public MLDescriptionShard()
	{
		super(AgentShardDesignation.customShard(FUNCTIONALITY));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		switch(event.getType())
		{
			case AGENT_WAVE:
				if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
					break;
				String replyContent = ((AgentWave) event).getContent() + " reply";
				sendMessage(replyContent, SHARD_ENDPOINT, ((AgentWave) event).getCompleteSource());
				break;
			case AGENT_START:
				break;
			case AGENT_STOP:
				break;
			case SIMULATION_START:
				break;
			case SIMULATION_PAUSE:
				break;
			default:
				break;
		}
	}

	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null)
			thisAgent = getAgent().getEntityName();
	}
}
