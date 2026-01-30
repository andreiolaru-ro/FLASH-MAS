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
package test.webSocketDeployment;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.node.Node;
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
 * Tests WebSocket support works for a manually-constructed deployment.
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
				public boolean postAgentEvent(AgentEvent event)
				{
					if(event instanceof AgentWave)
						System.out.println(
								((AgentWave) event).getContent() + " from " + ((AgentWave) event).getCompleteSource()
										+ " to " + ((AgentWave) event).getCompleteDestination());
					return true;
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
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
	{
		Node node1 = new Node();
		node1.configure(new MultiTreeMap().addFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "node1"));
		WebSocketPylon pylon = new WebSocketPylon();
		pylon.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
						.addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME, "8885"));
		pylon.addContext(node1.asContext());
		pylon.start();
		AgentTest one = new AgentTest("One");
		one.addMessagingShard(new WebSocketMessagingShard());
		one.addContext(pylon.asContext());
		
		Node node2 = new Node();
		node2.configure(new MultiTreeMap().addFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "node2"));
		WebSocketPylon pylon2 = new WebSocketPylon();
		pylon2.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885"));
		pylon2.addContext(node2.asContext());
		pylon2.start();
		AgentTest two = new AgentTest("Two");
		two.addMessagingShard(new WebSocketMessagingShard());
		two.addContext(pylon2.asContext());
		
		Thread.sleep(1000);
		
		one.start();
		Thread.sleep(1000);
		two.start();
		
		Thread.sleep(1000);
		
		pylon2.stop();
		pylon.stop();
	}
	
}
