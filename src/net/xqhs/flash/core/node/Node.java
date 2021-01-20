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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.DefaultPylonImplementation;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.PlatformUtils;
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
	public class NodeProxy implements EntityProxy<Node> {
		@Override
		public String getEntityName() {
			return name;
		}
	}

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

	/**
	 *  A {@link MessagingShard} of this node for message communication.
	 */
	protected MessagingShard                messagingShard;

	/**
	 *  An indication if this entity is running.
	 */
    private boolean isRunning;

    private static final String             SHARD_ENDPOINT      = "control";

	protected ShardContainer proxy = new ShardContainer() {

		/**
		 * This method parses the content received and takes further control/monitoring decisions.
		 * @param obj
		 * 				- an object representing the content received with an {@link AgentEvent}
		 */
		private void parseReceivedMsg(Object obj) {
			if(obj instanceof JSONObject) {
				JSONObject jo = (JSONObject) obj;
				if(jo.get(OperationUtils.NAME) != null && jo.get(OperationUtils.PARAMETERS) != null) {
					String operation  = (String)jo.get(OperationUtils.NAME);
					String param      = (String)jo.get(OperationUtils.PARAMETERS);
					Entity<?> entity = entityOrder.stream()
							.filter(en -> en.getName().equals(param))
							.findFirst().orElse(null);
					if(entity == null) {
						le("[] entity not found in the context of [].", param, name);
						return;
					}
					if(operation.equals(OperationUtils.ControlOperation.START.getOperation()))
						if(entity.start()) {
							lf("[] was started by parent [].", param, name);
							return;
						}
				}
				le("[] cannot properly parse received message.", name);
			}
		}

		@Override
		public void postAgentEvent(AgentEvent event) {
			switch (event.getType())
			{
				case AGENT_WAVE:
					String localAddr = ((AgentWave)event).getCompleteDestination();
					if(!(localAddr.split(AgentWave.ADDRESS_SEPARATOR)[0]).equals(getName()))
						break;
					Object msg = JSONValue.parse(((AgentWave)event).getContent());
					if(msg == null) break;
					parseReceivedMsg(msg);
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
		setLoggerType(PlatformUtils.platformLogType());
		setUnitName(EntityIndex.register(CategoryName.NODE.s(), this)).lock();
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
	 * It takes all available {@link OperationUtils.ControlOperation} and build up for each of them
	 * a {@link JSONObject} containing relevant information.
     * @return
     *          - a json array indicating all details about each operation.
     */
	protected JSONArray configureOperations() {
        JSONArray operations = new JSONArray();
        for(OperationUtils.ControlOperation op : OperationUtils.ControlOperation.values()) {
			JSONObject o = OperationUtils.operationToJSON(op.getOperation(), getName(), "", "");
			operations.add(o);
		}
        return operations;
    }

	/**
	 * Method used to send registration messages to
	 * {@link net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity}
	 * This lets it know what entities are in the content of current node and what
	 * operations can be performed on them.
	 *
	 * @return
	 * 				- an indication of success.
	 */
	protected boolean registerEntitiesToCentralEntity() {
	    JSONArray operations = configureOperations();
		JSONArray entities = new JSONArray();
		registeredEntities.entrySet().forEach(entry-> {
			String category = entry.getKey();
			for(Entity<?> entity : entry.getValue()) {
				JSONObject ent = OperationUtils.registrationToJSON(getName(), category, entity.getName(), operations);
				entities.add(ent);
			}
		});
		return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, entities.toString());
	}

	/**
	 * Build a {@link JSONObject} to send updates about the new status of the node.
	 * @return
	 * 			- an indication of success
	 */
	private boolean sendStatusUpdate() {
		if(getName() == null) return false;
		String status = isRunning ? "RUNNING" : "STOPPED";
		JSONObject update = OperationUtils.operationToJSON(
				OperationUtils.MonitoringOperation.STATUS_UPDATE.getOperation(),
				"", status, getName());
		return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, update.toString());
	}

	@Override
	public boolean start()
	{
		li("Starting node [].", name);
		for(Entity<?> entity : entityOrder) {
			String entityName = entity.getName();
			lf("starting entity []...", entityName);
			if(entity.start()) {
				lf("entity [] started successfully.", entityName);
				if(getName() != null && (entity instanceof DefaultPylonImplementation))
					messagingShard.register(getName());
			}
			else
				le("failed to start entity [].", entityName);
		}
		isRunning = true;
		sendStatusUpdate();
		li("Node [] started.", name);
		
		if(getName() != null && registerEntitiesToCentralEntity())
			lf("Entities successfully registered to control entity.");
		return true;
	}
	
	@Override
	public boolean stop()
	{
		li("Stopping node [].", name);
		LinkedList<Entity<?>> reversed = new LinkedList<>(entityOrder);
		Collections.reverse(reversed);
		for(Entity<?> entity : reversed) {
			if(entity.isRunning())
			{
				lf("stopping an entity...");
				if(entity.stop())
					lf("entity stopped successfully.");
				else
					le("failed to stop entity.");
			}
		}
		isRunning = false;
		sendStatusUpdate();
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
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Node> asContext() {
		return new NodeProxy();
	}

	/**
	 * Send a message via {@link MessagingShard}.
	 * @param destination
	 * 						- the name of the destination entity
	 * @param content
	 * 						- the content to be sent
	 * @return
	 * 						- an indication of success
	 */
	public boolean sendMessage(String destination, String content) {
		return messagingShard.sendMessage(
				AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(destination, SHARD_ENDPOINT),
				content);
	}
}
