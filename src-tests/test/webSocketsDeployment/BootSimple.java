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
package test.webSocketsDeployment;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.webSocket.WebSocketMessagingShard;
import net.xqhs.flash.webSocket.WebSocketPylon;

/**
 * Tests websockets support works for a manually-constructed deployment.
 * 
 * @author Andrei Olaru
 */
public class BootSimple
{
	/**
	 * The agent to use for testing.
	 */
	public static class AgentTest implements Agent
	{
		/**
		 * Agent name.
		 */
		private String					name;
		/**
		 * Agent messaging shard.
		 */
		private AbstractMessagingShard	messagingShard;
		/**
		 * The pylon the agent is in the context of.
		 */
		private MessagingPylonProxy		pylon;
		
		/**
		 * @param name
		 *                 the name
		 */
		public AgentTest(String name)
		{
			this.name = name;
		}
		
		@Override
		public boolean start()
		{
			messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
			if(name.equals("Two"))
				messagingShard.sendMessage(messagingShard.getAgentAddress(), "One", "Hello from the other side!");
			return true;
		}
		
		@Override
		public boolean stop()
		{
			return true;
		}
		
		@Override
		public boolean isRunning()
		{
			return true;
		}
		
		@Override
		public String getName()
		{
			return name;
		}
		
		@Override
		public boolean addContext(EntityProxy<Pylon> context)
		{
			pylon = (MessagingPylonProxy) context;
			if(messagingShard != null)
			{
				messagingShard.addGeneralContext(pylon);
			}
			return true;
		}
		
		@Override
		public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
		{
			return true;
		}
		
		@Override
		public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
		{
			return false;
		}
		
		@Override
		public boolean removeContext(EntityProxy<Pylon> context)
		{
			pylon = null;
			return true;
		}
		
		@Override
		public <C extends Entity<Pylon>> EntityProxy<C> asContext()
		{
			return null;
		}
		
		/**
		 * @param shard
		 *                  messaging shard to add.
		 */
		public void addMessagingShard(AbstractMessagingShard shard)
		{
			messagingShard = shard;
			shard.addContext(new ShardContainer() {
				@Override
				public void postAgentEvent(AgentEvent event)
				{
					if(event instanceof AgentWave)
						System.out.println(
								((AgentWave) event).getContent() + " de la " + ((AgentWave) event).getCompleteSource()
										+ " la " + ((AgentWave) event).getCompleteDestination());
				}
				
				@Override
				public AgentShard getAgentShard(AgentShardDesignation designation)
				{
					return null;
				}
				
				@Override
				public String getEntityName()
				{
					return getName();
				}
				
			});
			if(pylon != null)
				messagingShard.addGeneralContext(pylon);
		}
		
		/**
		 * @return relay for the messaging shard.
		 */
		protected AbstractMessagingShard getMessagingShard()
		{
			return messagingShard;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		WebSocketPylon pylon = new WebSocketPylon();
		pylon.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
						.addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME, "8885"));
		pylon.start();
		AgentTest one = new AgentTest("One");
		one.addContext(pylon.asContext());
		one.addMessagingShard(new WebSocketMessagingShard());
		
		WebSocketPylon pylon2 = new WebSocketPylon();
		pylon2.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885"));
		pylon2.start();
		AgentTest two = new AgentTest("Two");
		two.addContext(pylon2.asContext());
		two.addMessagingShard(new WebSocketMessagingShard());
		
		one.start();
		two.start();
		
		pylon2.stop();
		pylon.stop();
	}
	
}
