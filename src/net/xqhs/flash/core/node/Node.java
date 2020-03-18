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

import net.xqhs.flash.core.Entity;
import net.xqhs.util.logging.Unit;

public class Node extends Unit implements Entity<Node>
{
	/**
	 * The name of the node.
	 */
	protected String						name				= null;
	

	protected Map<String, List<Entity<?>>>	registeredEntities	= new HashMap<>();
	
	protected List<Entity<?>>				entityOrder			= new LinkedList<>();
	
	/**
	 * Creates a new {@link Node} instance.
	 * 
	 * @param name
	 *                 the name of the node, if any. Can be <code>null</code>.
	 */
	public Node(String name)
	{
		this.name = name;
	}
	
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
	
}
