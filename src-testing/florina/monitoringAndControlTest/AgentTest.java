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
package florina.monitoringAndControlTest;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import example.simplePingPong.AgentPingPong;


/**
 * The implementation of the agent.
 */
public class AgentTest extends Unit implements Agent
{
	/**
	 * The messaging shard.
	 */
	MessagingShard					msgShard					= null;
	/**
	 * The name of this agent.
	 */
	String							agentName					= null;

	/**
	 * The name of the component parameter that contains the id of the other agent.
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "sendTo";
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	SHARD_ENDPOINT				= "control";

	/**
	 * Endpoint element for other shards.
	 */
	protected static final String   OTHER_SHARD_ENDPOINT        = "control";

	/**
	 * Cache for the name of the other agent.
	 */
	List<String> otherAgents					= null;

	private static boolean isRunning;

	public AgentTest(MultiTreeMap configuration)
	{
		agentName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		setUnitName(agentName);
		if(configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
	}
	
	@Override
	public boolean start()
	{
		if(msgShard == null)
			throw new IllegalStateException("No messaging shard present");
		msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		if(otherAgents != null) {
			for(String otherAgent : otherAgents) {
				lf("Sending the message to ", otherAgent);
				if(!msgShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT),
						AgentWave.makePath(otherAgent, OTHER_SHARD_ENDPOINT), "hello dear"))
					le("Message sending failed");
			}
		}

		isRunning = true;
		return true;
	}

	/**
	 * @param event
	 *            - the event received.
	 */
	protected void postAgentEvent(AgentEvent event) {
		li("received: " + event.toString());
		if(event.getType().equals(AgentEventType.AGENT_WAVE)) {
			String content = ((AgentWave) event).getContent();
			if(content.equals("start_simulation"))
				lf("[] started simulation.", getName());
			else if(content.equals("stop"))
				stop();
			else if(otherAgents == null) {
				String replyContent = content + " reply";
				msgShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT), ((AgentWave) event).getCompleteSource(),
						replyContent);
			}
		}
	}
	
	@Override
	public boolean stop()
	{
		msgShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
		lf("[] stopped.", getName());
		isRunning = false;
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}
	
	@Override
	public String getName()
	{
		return agentName;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context)
	{
		PylonProxy proxy = (PylonProxy) context;
		String recommendedShard = proxy
				.getRecommendedShardImplementation(AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
		try
		{
			msgShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard, null, true);
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e)
		{
			e.printStackTrace();
		}
		msgShard.addContext(new ShardContainer() {
			@Override
			public String getEntityName()
			{
				return getName();
			}

			@Override
			public void postAgentEvent(AgentEvent event)
			{
				AgentTest.this.postAgentEvent(event);
			}

			@Override
			public AgentShard getAgentShard(AgentShardDesignation designation)
			{
				return null;
			}
		});
		lf("Context added: ", context.getEntityName());
		return msgShard.addGeneralContext(context);
	}
	
	@Override
	public boolean removeContext(EntityProxy<Pylon> context)
	{
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		if(!(context instanceof MessagingPylonProxy))
			return false;
		return addContext((MessagingPylonProxy) context);
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		return false;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext()
	{
		return null;
	}
	
}
