/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.node;

import java.util.*;

import monitoringAndControl.CentralMonitoringAndControlEntity;
import monitoringAndControl.MonitoringNodeProxy;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * A {@link Node} instance embodies the presence of the framework on a machine, although multiple {@link Node} instances
 * may exist on the same machine.
 * <p>
 * There should be no "higher"-context entity than the node.
 * 
 * @author Andrei Olaru
 */
public class Node extends Unit implements Entity<Node>
{
	/**
	 * The name of the node.
	 */
	protected String						name				= null;
	

	/**
	 * A collection of all entities added in the context of this node, indexed by their names.
	 */
	protected Map<String, List<Entity<?>>>	registeredEntities	= new HashMap<>();
	
	/**
	 * A {@link List} containing the entities added in the context of this node, in the order in which they were added.
	 */
	protected List<Entity<?>>				entityOrder			= new LinkedList<>();

	protected List<String>                  registeredNodes     = new LinkedList<>();

	protected MessagingShard messagingShard                     = null;

	protected CentralMonitoringAndControlEntity centralMonitoringEntity   = null;

	protected boolean isCentralNode                             = false;

	protected String centralMonitoringEntityName;

    protected MessageReceiver centralMessagingReceiver;


    protected static final String SUPPORT                        = "support";

    protected static final String SHARD_ENDPOINT                 = "monitoring";

	protected MonitoringNodeProxy powerfulProxy = new MonitoringNodeProxy() {
		@Override
		public boolean register(String entityName, MessageReceiver receiver) {
			centralMonitoringEntityName = entityName;
			centralMessagingReceiver = receiver;
			return true;
		}

		@Override
		public boolean send(String source, String destination, String content) {
			return false;
		}

		@Override
		public Map<String, List<Entity<?>>> getEntities() {
			return registeredEntities;
		}

		@Override
		public List<Entity<?>> getTypeEntities(String entityType) {
			return registeredEntities.get(entityType);
		}

		@Override
		public List<Entity<?>> getEntitiesOrder() {
			return entityOrder;
		}

		@Override
		public String getEntityName() {
			return getName();
		}

	};

	protected ShardContainer proxy = new ShardContainer() {

		public void parseJSON(Object obj)
		{
			JSONObject jsonObject = (JSONObject) obj;
			if(jsonObject.get("nodeName") != null)
			{
				String newNodeToRegister = (String)jsonObject.get("nodeName");
				registeredNodes.add(newNodeToRegister);
			}
		}

		@Override
		public void postAgentEvent(AgentEvent event) {
			switch (event.getType())
			{
				case AGENT_WAVE:
					System.out.println(event.toString());
					String content = ((AgentWave) event).getContent();
					Object obj = JSONValue.parse(content);
					if(obj != null) parseJSON(obj);
					break;
				default:
					break;
			}
		}

		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation) {
			return null;
		}

		@Override
		public String getEntityName() {
			return getName();
		}
	};
	/**
	 * Creates a new {@link Node} instance.
	 * 
	 * @param name
	 *                 the name of the node, if any. Can be <code>null</code>.
	 */
	public Node(String name)
	{
		this.name = name;
		//setLoggerType(PlatformUtils.platformLogType());
		messagingShard = new LocalSupport.SimpleLocalMessaging();
	}
	
	/**
	 * Method used to register entities added in the context of this node.
	 * 
	 * @param entityType
	 *                       - the type of the entity.
	 * @param entity
	 *                       - a reference to the entity.
	 * @param entityName
	 *                       - the name of the entity.
	 */
	protected void registerEntity(String entityType, Entity<?> entity, String entityName)
	{
		if(name == null) return;
		if(entityType.equals(SUPPORT) && entity.asContext() != null)
		{
			messagingShard.addContext(proxy);
			messagingShard.addGeneralContext(entity.asContext());

			if(entity instanceof LocalSupport)
			{
				((LocalSupport) entity).setIsCentralNode(DeploymentConfiguration.isCentralNode);
				((LocalSupport) entity).registerNodeId(getName());
				isCentralNode = DeploymentConfiguration.isCentralNode;
				registerToCentralNode();
			}
		}
		if(entityType.equals(DeploymentConfiguration.MONITORING_TYPE))
		{
			centralMonitoringEntity = (CentralMonitoringAndControlEntity) entity;
			centralMonitoringEntity.startGUI();
		}
		entityOrder.add(entity);
		if(!registeredEntities.containsKey(entityType))
			registeredEntities.put(entityType, new LinkedList<Entity<?>>());
		registeredEntities.get(entityType).add(entity);
		lf("registered an entity of type []. Provided name was [].", entityType, entityName);
	}

	protected void registerToCentralNode() {
		if(isCentralNode) return;
		JSONObject registerNode = new JSONObject();
		registerNode.put("nodeName", getName());
		messagingShard.sendMessage(
				AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(DeploymentConfiguration.CENTRAL_NODE_NAME, SHARD_ENDPOINT),
				registerNode.toString());
	}
	
	@Override
	public boolean start()
	{
		li("Starting node [].", name);
		for(Entity<?> entity : entityOrder)
		{
			lf("starting an entity...");
			if(entity.start())
				lf("entity started successfully.");
			else
				le("failed to start entity.");
		}
		li("Node [] started.", name);
		return true;
	}
	
	@Override
	public boolean stop()
	{
		li("Stopping node [].", name);
		LinkedList<Entity<?>> reversed = new LinkedList<>(entityOrder);
		Collections.reverse(reversed);
		for(Entity<?> entity : reversed)
			if(entity.isRunning())
			{
				lf("stopping an entity...");
				if(entity.stop())
					lf("entity stopped successfully.");
				else
					le("failed to stop entity.");
			}
		li("Node [] stopped.", name);
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context)
	{
		// unsupported
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		// unsupported
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		// unsupported
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context)
	{
		// unsupported
		return false;
	}
	
	@Override
	public <C extends Entity<Node>> EntityProxy<C> asContext()
	{
		// no functionality offered
		return null;
	}

	/*
	 * Return the local proxy to the this node.
	 */
	public EntityProxy<Node> asPowerfulContext() {
		return powerfulProxy;
	}

	public List<Entity<?>> getEntities() {
		return entityOrder;
	}

	public List<Agent> getAgents() {
		LinkedList<Agent> agents = new LinkedList<>();
		for (Entity<?> e : entityOrder) {
			if (e instanceof Agent) {
				agents.add((Agent) e);
			}
		}
		return agents;
	}

}
