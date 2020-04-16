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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.xqhs.flash.core.support.*;
import net.xqhs.flash.core.util.PlatformUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;
import net.xqhs.flash.core.monitoring.MonitoringNodeProxy;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalPylon;
import net.xqhs.util.logging.Unit;

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
	protected String						name;

	/**
	 * A collection of all entities added in the context of this node, indexed by their names.
	 */
	protected Map<String, List<Entity<?>>>	registeredEntities	= new HashMap<>();
	
	/**
	 * A {@link List} containing the entities added in the context of this node, in the order in which they were added.
	 */
	protected List<Entity<?>>				entityOrder			= new LinkedList<>();

	protected List<String>                  registeredNodes     = new LinkedList<>();

	protected MessagingShard messagingShard;

	protected CentralMonitoringAndControlEntity centralMonitoringEntity;

	protected String centralMonitoringEntityName;

    protected MessageReceiver centralMessagingReceiver;

    private boolean isRunning;

    private static final String SHARD_ENDPOINT                  = "control";

	private HashMap<String, List<String>> nodeToAgents          = new LinkedHashMap<>();

	private MonitoringNodeProxy powerfulProxy = new MonitoringNodeProxy() {
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
		public List<String> getAgentsFromOuterNodes() {
			List<String> list = nodeToAgents.entrySet()
					.stream()
					.flatMap(e -> e.getValue().stream())
					.collect(Collectors.toList());
			return list;
		}

		@Override
		public List<String> getOwnAgents() {
			List<Entity<?>> list = getTypeEntities("agent");
			List<String> ownEntities = list.stream().map(el -> el.getName()).collect(Collectors.toList());
			return ownEntities;
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
				if(jsonObject.get("agentName") != null) {
					String newAgent = (String)jsonObject.get("agentName");
					nodeToAgents.get(newNodeToRegister).add(newAgent);
				} else {
					registeredNodes.add(newNodeToRegister);
					nodeToAgents.put(newNodeToRegister, new LinkedList<>());
				}
			}
		}

		@Override
		public void postAgentEvent(AgentEvent event) {
			switch (event.getType())
			{
				case AGENT_WAVE:
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
	 * @param nodeConfiguration
	 *            the configuration of the node. Can be <code>null</code>.
	 */
	public Node(MultiTreeMap nodeConfiguration)
	{
		if(nodeConfiguration != null)
			name = nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		//setLoggerType(PlatformUtils.platformLogType());
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
		if(entityType.equals(DeploymentConfiguration.MONITORING_TYPE))
		{
			centralMonitoringEntity = (CentralMonitoringAndControlEntity) entity;
//			centralMonitoringEntity.addNodeProxy(asPowerfulContext());
//			centralMonitoringEntity.startGUIBoard();
		}

		entityOrder.add(entity);
		if(!registeredEntities.containsKey(entityType))
			registeredEntities.put(entityType, new LinkedList<Entity<?>>());
		registeredEntities.get(entityType).add(entity);
		lf("registered an entity of type []. Provided name was [].", entityType, entityName);
	}

	protected void registerNodeToCentralNode() {
		if(centralMonitoringEntity != null) return;
		JSONObject registerNode = new JSONObject();
		registerNode.put("nodeName", getName());
		messagingShard.sendMessage(
				AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(DeploymentConfiguration.CENTRAL_NODE_NAME, SHARD_ENDPOINT),
				registerNode.toString());
	}

	protected void registerAgentToCentralNode(String agentName) {
		if(centralMonitoringEntity != null) return;
		JSONObject registerAgent = new JSONObject();
		registerAgent.put("agentName", agentName);
		registerAgent.put("nodeName", getName());
		messagingShard.sendMessage(
				AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(DeploymentConfiguration.CENTRAL_NODE_NAME, SHARD_ENDPOINT),
				registerAgent.toString());
	}

	protected void registerNodeToPylon() {
		messagingShard.registerNode(getName(), centralMonitoringEntity != null);
	}
	
	@Override
	public boolean start()
	{
		li("Starting node [].", name);
		for(Entity<?> entity : entityOrder)
		{
			String entityName = entity.getName();
			lf("starting entity []...", entityName);
			if(entity.start())
			{
				lf("entity [] started successfully.", entityName);
				if(getName() != null && (entity instanceof DefaultPylonImplementation))
					registerNodeToPylon();
			}
			else
				le("failed to start entity [].", entityName);
		}
		isRunning = true;
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
		isRunning = false;
		li("Node [] stopped.", name);
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
		PylonProxy pylonProxy = (PylonProxy)context;
		String recommendedShard = pylonProxy
				.getRecommendedShardImplementation(
						AgentShardDesignation.standardShard(
								AgentShardDesignation.StandardAgentShard.MESSAGING));
		try
		{
			messagingShard = (MessagingShard) PlatformUtils
					.getClassFactory()
					.loadClassInstance(recommendedShard, null, true);
		} catch(ClassNotFoundException
				| InstantiationException | NoSuchMethodException
				| IllegalAccessException | InvocationTargetException e)
		{
			e.printStackTrace();
		}
		messagingShard.addContext(proxy);
		return messagingShard.addGeneralContext(context);
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

	public EntityProxy<Node> asPowerfulContext() {
		return powerfulProxy;
	}

	public List<Entity<?>> getEntities() {
		return entityOrder;
	}
}
