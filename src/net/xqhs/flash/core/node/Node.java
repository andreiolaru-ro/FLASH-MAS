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
package net.xqhs.flash.core.node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.OperationUtils.ControlOperation;
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
public class Node extends Unit implements Entity<Node> {
	/**
	 * Proxy for a {@link Node}.
	 */
	public class NodeProxy implements EntityProxy<Node> {
		@Override
		public String getEntityName() {
			return name;
		}
		
		/**
		 * Instructs the node to move the given serialized agent to a different node.
		 * 
		 * @param destination
		 *            - name of the destination node
		 * @param agentName
		 *            - name of the agent that wants to move
		 * @param agentData
		 *            - serialization of the agent
		 */
		public void moveAgent(String destination, String agentName, String agentData) {
			sendAgent(destination, agentName, agentData);
		}
	}
	
	/**
	 * The endpoint for messages sent between nodes.
	 */
	private static final String	SHARD_ENDPOINT			= "node";
	/**
	 * The name of the operation in which a node receives a mobile agent.
	 */
	public static final String	RECEIVE_AGENT_OPERATION	= "receive_agent";
	
	/**
	 * The name of the node.
	 */
	protected String						name						= null;
	/**
	 * A collection of all entities added in the context of this node, indexed by their names.
	 */
	protected Map<String, List<Entity<?>>>	registeredEntities			= new HashMap<>();
	/**
	 * A {@link List} containing the entities added in the context of this node, in the order in which they were added.
	 */
	protected List<Entity<?>>				entityOrder					= new LinkedList<>();
	/**
	 * A {@link MessagingShard} of this node for message communication.
	 */
	protected MessagingShard				messagingShard;
	/**
	 * monitors if the messaging shard has been registered with its pylon.
	 */
	protected boolean						messagingShardRegistered	= false;
	/**
	 * An indication if this entity is running.
	 */
	private boolean							isRunning;
	/**
	 * The pylon proxy of the node. This is used as a context for the node (and its {@link MessagingShard}) and for any
	 * mobile agents which arrive here.
	 */
	private PylonProxy						nodePylonProxy;
	protected String						serverURI					= null;					// FIXME: Remove this
	
	/**
	 * Creates a new {@link Node} instance.
	 * 
	 * @param nodeConfiguration
	 *            the configuration of the node. Can be {@code null}.
	 */
	public Node(MultiTreeMap nodeConfiguration) {
		if(nodeConfiguration != null) {
			name = nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			this.serverURI = nodeConfiguration.get("region-server");
		}
		setLoggerType(PlatformUtils.platformLogType());
		setUnitName(EntityIndex.register(CategoryName.NODE.s(), this)).lock();
	}
	
	/**
	 * Method used to register entities added in the context of this node.
	 * 
	 * @param entityType
	 *            - the type of the entity.
	 * @param entity
	 *            - a reference to the entity.
	 * @param entityName
	 *            - the name of the entity.
	 */
	protected void registerEntity(String entityType, Entity<?> entity, String entityName) {
		entityOrder.add(entity);
		if(!registeredEntities.containsKey(entityType))
			registeredEntities.put(entityType, new LinkedList<>());
		registeredEntities.get(entityType).add(entity);
		lf("registered an entity of type []. Provided name was [].", entityType, entityName);
	}
	
	/**
	 * It takes all available {@link ControlOperation} and build up for each of them a {@link JSONObject} containing
	 * relevant information.
	 * 
	 * @return - a json array indicating all details about each operation.
	 */
	protected JsonArray configureOperations() {
		JsonArray operations = new JsonArray();
		for(OperationUtils.ControlOperation op : OperationUtils.ControlOperation.values()) {
			JsonObject o = OperationUtils.operationToJSON(op.getOperation(), getName(), "", "");
			operations.add(o);
		}
		return operations;
	}
	
	/**
	 * Method used to send registration messages to {@link CentralMonitoringAndControlEntity} This lets it know what
	 * entities are in the content of current node and what operations can be performed on them.
	 *
	 * @return - an indication of success.
	 */
	protected boolean registerEntitiesToCentralEntity() {
		JsonArray operations = configureOperations();
		JsonArray entities = new JsonArray();
		registeredEntities.forEach((category, value) -> {
			for(Entity<?> entity : value) {
				JsonObject ent = OperationUtils.registrationToJSON(getName(), category, entity.getName(), operations);
				entities.add(ent);
			}
		});
		return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, entities.toString());
	}
	
	@Override
	public boolean start() {
		li("Starting node [] with entities [].", name, entityOrder);
		// must start entities before sending messages to M&C because M&C must be started too
		for(Entity<?> entity : entityOrder)
			startEntity(entity);
		isRunning = true;
		if(messagingShard != null)
			messagingShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		sendStatusUpdate();
		li("Node [] started.", name);
		
		if(getName() != null && registerEntitiesToCentralEntity())
			lf("Entities successfully registered to control entity: ", entityOrder);
		return true;
	}

	/**
	 * Method to start one entity in the context of this node.
	 *
	 * @param entity
	 * 		  - the entity to start.
	 *
	 * @return boolean - an indication of success.
	 */
	public boolean startEntity(Entity<?> entity) {
		lf("starting entity []...", entity.getName());
		if(entity.start()) {
			lf("entity [] started successfully.", entity.getName());
			EntityProxy<?> ctx = entity.asContext();
			if(!messagingShardRegistered && getName() != null && messagingShard != null
					&& (ctx instanceof MessagingPylonProxy)) {
				messagingShard.register(getName());
				messagingShardRegistered = true;
			}
		}
		else
			le("failed to start entity [].", entity.getName());
		return false;
	}

	@Override
	public boolean stop() {
		li("Stopping node [] with entities [].", name, entityOrder);
		LinkedList<Entity<?>> reversed = new LinkedList<>(entityOrder);
		Collections.reverse(reversed);
		for(Entity<?> entity : reversed) {
			if(entity.isRunning()) {
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
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return addGeneralContext(context);
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(nodePylonProxy != null)
			// only one pylon can be added as context for the node
			return false;
		PylonProxy pylonProxy = (PylonProxy) context;
		nodePylonProxy = pylonProxy;
		String recommendedShard = pylonProxy.getRecommendedShardImplementation(
				AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
		try {
			messagingShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard, null,
					true);
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			le("Unable to construct node messaging shard: ", PlatformUtils.printException(e));
		}
		messagingShard.addContext(new ShardContainer() {
			@Override
			public void postAgentEvent(AgentEvent event) {
				switch(event.getType()) {
				case AGENT_WAVE:
					String localAddr = ((AgentWave) event).getCompleteDestination();
					if(!(localAddr.split(AgentWave.ADDRESS_SEPARATOR)[0]).equals(getName()))
						break;
					JsonObject msg = new Gson().fromJson(((AgentWave) event).getContent(), JsonObject.class);
					if(msg == null)
						break;
					parseReceivedMsg(msg);
					break;
				default:
					break;
				}
			}
			
			@Override
			public AgentShard getAgentShard(AgentShardDesignation designation) {
				// no other shards in this container (in the node)
				return null;
			}
			
			@Override
			public String getEntityName() {
				return getName();
			}
		});
		// FIXME: remove this protocol-specific code
		messagingShard.configure(
				new MultiTreeMap().addSingleValue("connectTo", this.serverURI).addSingleValue("agent_name", getName()));
		lf("Messaging shard added, affiliated with pylon []", pylonProxy.getEntityName());
		return messagingShard.addGeneralContext(context);
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(nodePylonProxy != context)
			return false;
		nodePylonProxy = null;
		messagingShard = null;
		return true;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
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
	 * 
	 * @param destination
	 *            - the name of the destination entity
	 * @param content
	 *            - the content to be sent
	 * @return - an indication of success
	 */
	public boolean sendMessage(String destination, String content) {
		return messagingShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(destination, SHARD_ENDPOINT), content);
	}
	
	/**
	 * Build a {@link JSONObject} to send updates about the new status of the node.
	 * 
	 * @return - an indication of success
	 */
	private boolean sendStatusUpdate() {
		if(getName() == null)
			return false;
		String status = isRunning ? "RUNNING" : "STOPPED";
		JsonObject update = OperationUtils.operationToJSON(
				OperationUtils.MonitoringOperation.STATUS_UPDATE.getOperation(), "", status, getName());
		return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, update.toString());
	}
	
	/**
	 * This method parses the content received and takes further control/monitoring decisions.
	 * 
	 * @param jo
	 *            - an object representing the content received with an {@link AgentEvent}
	 */
	private void parseReceivedMsg(JsonObject jo) {
		String op = jo.get(OperationUtils.OPERATION_NAME).getAsString();
		if(OperationUtils.ControlOperation.fromOperation(op) != null) {
			String param = jo.get(OperationUtils.PARAMETERS).getAsString();
			if(param == null)
				return;
			Entity<?> entity = entityOrder.stream().filter(en -> en.getName().equals(param)).findFirst().orElse(null);
			if(entity == null) {
				le("[] entity not found in the context of [].", param, name);
				return;
			}
			switch (ControlOperation.fromOperation(op)) {
				case START:
					if(startEntity(entity)) {
						lf("[] was started by parent [].", param, name);
						return;
					}
					break;
				case KILL:
					if(entity.stop()) {
						lf("[] was stopped by parent [].", param, name);
						return;
					}
					break;
				default:
					le("Unknown operation: ", op);
					break;

			}/*
			if(entity.start()) {
				lf("[] was started by parent [].", param, name);
				return;
			}*/
		}
		else if(RECEIVE_AGENT_OPERATION.equals(op)) {
			String agentData = jo.get(OperationUtils.PARAMETERS).getAsString();
			
			MobileCompositeAgent agent = MobileCompositeAgent.deserializeAgent(agentData);
			registerEntity(CategoryName.AGENT.toString(), agent, agent.getName());
			lf("Starting agent [] after moving...", agent.getName());
			agent.addGeneralContext(asContext());
			agent.addContext(nodePylonProxy);
			agent.start();
		}
	}
	
	/**
	 * Removes the agent from the list of entities and sends it to a different node.
	 * 
	 * @param destination
	 *            - name of the destination node
	 * @param agentName
	 *            - name of the agent that wants to move
	 * @param agentData
	 *            - serialization of the agent
	 */
	protected void sendAgent(String destination, String agentName, String agentData) {
		entityOrder.stream()
				.filter(entity -> entity instanceof MobileCompositeAgent && entity.getName().equals(agentName))
				.findAny().ifPresent(entity -> entityOrder.remove(entity));
		JsonObject root = new JsonObject();
		root.addProperty(OperationUtils.OPERATION_NAME, Node.RECEIVE_AGENT_OPERATION);
		root.addProperty(OperationUtils.PARAMETERS, agentData);
		
		lf("Send message with agent [] to []", agentName, destination);
		sendMessage(destination, root.toString());
	}
}
