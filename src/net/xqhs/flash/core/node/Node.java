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
import java.util.*;

import net.xqhs.flash.core.support.*;
import net.xqhs.flash.core.util.PlatformUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;
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
	protected String						 name;

	/**
	 * A collection of all entities added in the context of this node, indexed by their names.
	 */
	protected Map<String, List<Entity<?>>>	registeredEntities	= new HashMap<>();
	
	/**
	 * A {@link List} containing the entities added in the context of this node, in the order in which they were added.
	 */
	protected List<Entity<?>>				entityOrder			= new LinkedList<>();

	protected MessagingShard                messagingShard;

    private boolean isRunning;

    private static final String             SHARD_ENDPOINT                  = "control";

    // operations that entities in the context of the node can perform themselves
	List<String> operations = new LinkedList<>();

	protected ShardContainer proxy = new ShardContainer() {

		private boolean parseJSON(Object obj) {
			if(obj instanceof JSONObject) {
				JSONObject jo = (JSONObject) obj;
				String child   = (String)jo.get("child");
				String command = (String)jo.get("command");
				Entity<?> entity = entityOrder.stream()
						.filter(en -> en.getName().equals(child))
						.findFirst().orElse(null);
				if(entity == null) {
					le("[] child not found in the context of [].", child, name);
					return false;
				}
				if(command.equals("start"))
					if(entity.start())
						lf("[] was started by parent [].", child, name);

			}
			return true;
		}

		@Override
		public void postAgentEvent(AgentEvent event) {
			switch (event.getType())
			{
				case AGENT_WAVE:
					if(!((AgentWave)event).getFirstDestinationElement().equals(SHARD_ENDPOINT)) break;
					Object obj = JSONValue.parse(((AgentWave)event).getContent());
					if(obj == null) break;
					parseJSON(obj);
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
		entityOrder.add(entity);
		if(!registeredEntities.containsKey(entityType))
			registeredEntities.put(entityType, new LinkedList<Entity<?>>());
		registeredEntities.get(entityType).add(entity);
		lf("registered an entity of type []. Provided name was [].", entityType, entityName);
	}

	/**
	 * Method used to send monitoring message to central entity: the message covers necessary information
	 * about all entities registered and started in the context of current node.
	 *
	 * @return
	 * 				- an indication of success.
	 */
	protected boolean registerEntitiesToControlEntity() {
		if(operations.isEmpty()) {
			operations.add("stop");
			operations.add("simulation");
		}

		JSONArray entities = new JSONArray();
		registeredEntities.entrySet().forEach(entry-> {
			String category = entry.getKey();
			for(Entity<?> entity : entry.getValue())
			{
				JSONObject ent = new JSONObject();
				ent.put("node", getName());
				ent.put("category", category);
				ent.put("name", entity.getName());
				ent.put("operations", operations);
				entities.add(ent);
			}
		});
		return messagingShard.sendMessage(
				AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, SHARD_ENDPOINT),
				entities.toString());
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
					messagingShard.registerNode(getName());
			}
			else
				le("failed to start entity [].", entityName);
		}
		isRunning = true;
		li("Node [] started.", name);

		if(getName() != null && registerEntitiesToControlEntity())
			lf("Entities successfully registered to control entity.");
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

	public List<Entity<?>> getEntities() {
		return entityOrder;
	}
}
