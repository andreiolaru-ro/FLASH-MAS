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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import monitoringAndControl.CentralMonitoringAndControlEntity;
import monitoringAndControl.MonitoringNodeProxy;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.MessageReceiver;
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

	/*
	 * The name of the CentralMonitoringAndControlEntity available.
	 */
	protected String centralMonitoringEntityName;

	/*
	* The receiver for CentralMonitoringAndControlEntity.
	* */

    protected MessageReceiver centralMessagingReceiver;

	/*
	* The central node will have this monitoring entity.
	* */
    protected CentralMonitoringAndControlEntity monitoringEntity = null;

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

	/**
	 * Creates a new {@link Node} instance.
	 * 
	 * @param name
	 *                 the name of the node, if any. Can be <code>null</code>.
	 * @param isCentralNode
	 * 					if the node possesses the central monitoring entity,
	 * 				    therefore this will be the central monitoring node
	 */
	public Node(String name, boolean isCentralNode)
	{
		this.name = name;
		//setLoggerType(PlatformUtils.platformLogType());
		if(isCentralNode) {
			/*
			* TODO: The name SET for monitoringEntity should be the same as centralMonitoringEntityName
			*  used by the shard when registered.
			* */
			monitoringEntity = new CentralMonitoringAndControlEntity(name + "MonitoringEntity");
			monitoringEntity.addGeneralContext(powerfulProxy);
			registerEntity("monitoring", monitoringEntity, centralMonitoringEntityName);
		}

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
