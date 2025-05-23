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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
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
 * <p>
 * The node will stop when there are no more active entities in its registered entity list. By default, active entities
 * are the ones in {@link #DEFAULT_ACTIVE_ENTITIES}. All entities can be specified as active by passing the
 * <code>active:*</code> parameter in the configuration deployment of the node, leading to the node (most likely) never
 * stopping by default.
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
	private static final String		SHARD_ENDPOINT				= "node";
	/**
	 * The name of the operation in which a node receives a mobile agent.
	 */
	public static final String		RECEIVE_AGENT_OPERATION		= "receive_agent";
	/**
	 * The name of the parameter that indicates which entities keep the node alive.
	 */
	public static final String		ACTIVE_PARAMETER_NAME		= "active";
	/**
	 * Value for the active parameter indicating that all entities are active entities.
	 */
	public static final String		ACTIVE_ALWAYS_VALUE			= "*";
	/**
	 * The default "active" entities, which keep the node running while they are running.
	 */
	public static final String[]	DEFAULT_ACTIVE_ENTITIES		= new String[] { "agent" };
	/**
	 * The name of the parameter indicating when to make the first check for active entities.
	 */
	public static final String		ACTIVE_CHECK_PARAMETER		= "keep";
	/**
	 * Global (implementation-wide) switch to kill the node when there are no more running active entities.
	 */
	public static final boolean		EXIT_ON_NO_ACTIVE_ENTITIES	= true;
	/**
	 * The time (in seconds) after which to perform the first check of active entities, if not modified in the
	 * configuration.
	 */
	public static final int			INITIAL_ACTIVE_CHECK		= 5;
	
	/**
	 * The name of the node.
	 */
	protected String						name				= null;
	/**
	 * A collection of all entities added in the context of this node, indexed by their types.
	 */
	protected Map<String, List<Entity<?>>>	registeredEntities	= new HashMap<>();
	/**
	 * A {@link List} containing the entities added in the context of this node, in the order in which they were added.
	 */
	protected List<Entity<?>>				entityOrder			= new LinkedList<>();
	/**
	 * A {@link MessagingShard} of this node for message communication.
	 */
	protected MessagingShard				messagingShard;
	/**
	 * An indication if this entity is running.
	 */
	private boolean							isRunning;
	/**
	 * The set of entity types considered as "active" and keeping the node from exiting.
	 */
	protected Set<String>					activeEntities		= new HashSet<>(Arrays.asList(DEFAULT_ACTIVE_ENTITIES));
	/**
	 * The time (in seconds) to check for active entities. if negative, the node will never close.
	 */
	protected long							activeFor			= INITIAL_ACTIVE_CHECK;
	/**
	 * Monitors if all active entities still running.
	 */
	protected Timer							activeMonitor		= null;
	/**
	 * The pylon proxy of the node. This is used as a context for the node (and its {@link MessagingShard}) and for any
	 * mobile agents which arrive here.
	 */
	private PylonProxy						nodePylonProxy;
	
	/**
	 * Creates a new {@link Node} instance.
	 * 
	 * @param nodeConfiguration
	 *            the configuration of the node. Can be <code>null</code>.
	 */
	public Node(MultiTreeMap nodeConfiguration) {
		if(nodeConfiguration != null) {
			name = nodeConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			if(nodeConfiguration.isSimple(ACTIVE_PARAMETER_NAME))
				activeEntities = new HashSet<>(nodeConfiguration.getValues(ACTIVE_PARAMETER_NAME));
			if(nodeConfiguration.isSimple(ACTIVE_CHECK_PARAMETER))
				activeFor = nodeConfiguration.getAValue(ACTIVE_CHECK_PARAMETER) != null
						? Long.parseLong(nodeConfiguration.getAValue(ACTIVE_CHECK_PARAMETER))
						: -1; // keep node active if no value mentioned
		}
		setLoggerType(PlatformUtils.platformLogType());
		setUnitName(EntityIndex.register(CategoryName.NODE.s(), this)).lock();
		li("Active entitites:", activeEntities);
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
	protected JSONArray configureOperations() {
		JSONArray operations = new JSONArray();
		for(OperationUtils.ControlOperation op : OperationUtils.ControlOperation.values()) {
			JSONObject o = OperationUtils.operationToJSON(op.getOperation(), getName(), "", "");
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
		JSONArray operations = configureOperations();
		JSONArray entities = new JSONArray();
		registeredEntities.forEach((category, value) -> {
			for(Entity<?> entity : value) {
				JSONObject ent = OperationUtils.registrationToJSON(getName(), category, entity.getName(), operations);
				entities.add(ent);
			}
		});
		return false;
		// TODO revert to this when a monitoring entity is actually created.
		// return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, entities.toString());
	}
	
	@Override
	public boolean start() {
		li("Starting node [] with entities [].", name, entityOrder);
		for(Entity<?> entity : entityOrder) {
			String entityName = entity.getName();
			lf("starting entity []...", entityName);
			if(entity.start())
				lf("entity [] started successfully.", entityName);
			else
				le("failed to start entity [].", entityName);
		}
		isRunning = true;
		if(messagingShard != null)
			messagingShard.signalAgentEvent(new AgentEvent(AgentEventType.AGENT_START));
		sendStatusUpdate();
		li("Node [] started.", name);
		
		if(getName() != null && registerEntitiesToCentralEntity())
			lf("Entities successfully registered to control entity.");
		
		if(EXIT_ON_NO_ACTIVE_ENTITIES && activeFor >= 0) {
			activeMonitor = new Timer();
			activeMonitor.schedule(new TimerTask() {
				@Override
				public void run() {
					checkRunning();
				}
			}, activeFor * 1000, 1000);
		}
		
		return true;
	}
	
	@Override
	public boolean stop() {
		li("Stopping node [] with entities [].", name, entityOrder);
		activeMonitor.cancel();
		LinkedList<Entity<?>> reversed = new LinkedList<>(entityOrder);
		Collections.reverse(reversed);
		for(Entity<?> entity : reversed) {
			if(entity.isRunning()) {
				lf("stopping entity []...", entity.getName());
				if(entity.stop())
					lf("entity [] stopped successfully.", entity.getName());
				else
					le("failed to stop entity [].", entity.getName());
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
			public boolean postAgentEvent(AgentEvent event) {
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
				return true; // FIXME it always returns true
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
	
	protected void checkRunning() {
		boolean allActive = (activeEntities.size() == 1)
				&& activeEntities.iterator().next().equals(ACTIVE_ALWAYS_VALUE);
		for(String type : registeredEntities.keySet())
			if(activeEntities.contains(type) || allActive)
				for(Entity<?> e : registeredEntities.get(type))
					if(e.isRunning())
						// found an active entity still running
						return;
		li("Node [] will stop due to no more active entitites running. Active entity type list was [].", name,
				activeEntities);
		stop();
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
		// String status = isRunning ? "RUNNING" : "STOPPED";
		// JSONObject update = OperationUtils.operationToJSON(
		// OperationUtils.MonitoringOperation.STATUS_UPDATE.getOperation(), "", status, getName());
		// return sendMessage(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, update.toString());
		return false;
	}
	
	/**
	 * This method parses the content received and takes further control/monitoring decisions.
	 * 
	 * @param jo
	 *            - an object representing the content received with an {@link AgentEvent}
	 */
	private void parseReceivedMsg(JsonObject jo) {
		if(!jo.has(OperationUtils.NAME))
			return;
		String op = jo.get(OperationUtils.NAME).getAsString();
		if(OperationUtils.ControlOperation.START.getOperation().equals(op)) {
			String param = jo.get(OperationUtils.PARAMETERS).getAsString();
			if(param == null)
				return;
			Entity<?> entity = entityOrder.stream().filter(en -> en.getName().equals(param)).findFirst().orElse(null);
			if(entity == null) {
				le("[] entity not found in the context of [].", param, name);
				return;
			}
			if(entity.start()) {
				lf("[] was started by parent [].", param, name);
				return;
			}
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
		root.addProperty(OperationUtils.NAME, Node.RECEIVE_AGENT_OPERATION);
		root.addProperty(OperationUtils.PARAMETERS, agentData);
		
		lf("Send message with agent [] to []", agentName, destination);
		sendMessage(destination, root.toString());
	}
}
