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
package net.xqhs.flash.core.shard;

import java.io.Serializable;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.util.logging.Unit;

/**
 * This class serves as base for the implementation of agent shards.
 * <p>
 * A shard can belong to at most one {@link Agent}, which is its parent (and, in {@link Entity} terms, its context).
 * When created, the shard has no parent; a parent will be set afterwards and the shard notified.
 * <p>
 * In the life-cycle of a shard, it will be constructed and configured, then started and eventually stopped.
 * <p>
 * This implementation manages the configuration of the shard and the shard's state. It also extends {@link Unit}, so
 * extending classes can easily post logging messages.
 * <p>
 * This implementation exclusively manages the relation with the shard's parent, and as such:
 * <ul>
 * <li>the parent agent must be an {@link Agent}.
 * <li>{@link #addContext} and {@link #removeContext} methods are final; extending classes can override
 * {@link #parentChangeNotifier};
 * <li>extending classes can access the parent agent by means of {@link #getAgent};
 * </ul>
 * <p>
 * 
 * @author Andrei Olaru
 */
public class AgentShardCore extends Unit implements AgentShard, Serializable
{
	/**
	 * The class UID.
	 */
	private static final long		serialVersionUID	= -8282262747231347473L;
	
	/**
	 * The designation of the shard, as instance of {@link StandardAgentShard}.
	 */
	private AgentShardDesignation	shardDesignation;
	/**
	 * Creation data for the shard. The field is initialized with an empty structure, so that it is guaranteed that it
	 * will never be <code>null</code> after construction.
	 */
	private MultiTreeMap			shardConfiguration;
	/**
	 * The {@link CompositeAgent} instance that this instance is part of.
	 */
	private ShardContainer			parentAgent;
	/**
	 * Indicates the state of the shard.
	 */
	private boolean					isRunning;
	
	/**
	 * The constructor assigns the designation to the shard.
	 * <p>
	 * IMPORTANT: extending classes should only perform in the constructor initializations that do not depend on the
	 * parent agent or on other shards, as when the shard is created, the {@link AgentShardCore#parentAgent} member is
	 * <code>null</code>. The assignment of a parent (as any parent change) is notified to extending classes by calling
	 * the method {@link AgentShardCore#parentChangeNotifier}.
	 * <p>
	 * Event registration is not dependent on the parent, so it can be performed in the constructor or in the
	 * {@link #shardInitializer()} method.
	 * 
	 * @param designation
	 *            - the designation of the shard, as instance of {@link StandardAgentShard}.
	 */
	protected AgentShardCore(AgentShardDesignation designation)
	{
		shardDesignation = designation;
		
		// dummy shard data, in case no other is configured
		shardConfiguration = new MultiTreeMap();
		shardConfiguration.ensureLocked();
		
		shardInitializer();
	}
	
	/**
	 * Extending <b>anonymous</b> classes can override this method to perform actions when the shard is created. The
	 * method is called at the end of the constructor.
	 * <p>
	 * Extending classes should always call <code>super.shardInitializer()</code> first thing in their implementation of
	 * <code>shardInitializer()</code>.
	 * <p>
	 * IMPORTANT: The note in {@link #AgentShardCore(AgentShardDesignation)} regarding initializations also applies to
	 * this method.
	 * <p>
	 * VERY IMPORTANT: initializations done in this method are done before all initializations in extending
	 * constructors.
	 */
	protected void shardInitializer()
	{
		// this class does not do anything here.
	}
	
	/**
	 * Extending classes should override this method to verify and load agent-independent shard data, based on
	 * deployment data.
	 * <p>
	 * If the shard is surely not going to be able to load, <code>false</code> will be returned. For any non-fatal
	 * issues, the method should return <code>true</code> and output warnings in the specified log.
	 * <p>
	 * The method loads the parameters into {@link #shardConfiguration} and locks them.
	 * <p>
	 * IMPORTANT: The note in {@link #AgentShardCore(AgentShardDesignation)} regarding initializations also applies to
	 * this method.
	 * <p>
	 * ALSO IMPORTANT: overriding methods should always call <code>super.configure()</code> first.
	 * <p>
	 * 
	 * @param configuration
	 *            - parameters for creating the shard. The parameters will be locked (see {@link MultiValueMap#lock()}
	 *            from this moment on.
	 * @return <code>true</code> if no fatal issues were found; <code>false</code> otherwise.
	 */
	@Override
	public boolean configure(MultiTreeMap configuration)
	{
		if(configuration != null)
		{
			configuration.ensureLocked();
			shardConfiguration = configuration;
		}
		return true;
	}
	
	/**
	 * Overriding methods should always call <code>super.start()</code> first.
	 */
	@Override
	public boolean start()
	{
		if(isRunning)
			return ler(false, "Shard is already running");
		if(getAgent() == null)
			return ler(false, "Shards cannot start without being in context of an agent.");
		isRunning = true;
		return true;
	}
	
	/**
	 * Overriding methods should always call <code>super.stop()</code> first.
	 */
	@Override
	public boolean stop()
	{
		if(!isRunning)
			return ler(false, "Shard is not running");
		if(getAgent() == null)
			throw new IllegalStateException("Shard is " + getShardDesignation() + " not in the context of an agent.");
		isRunning = false;
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}
	
	/**
	 * Extending classes can override this method to perform actions when the parent of the shard changes, or when the
	 * shard is effectively integrated (added) in the agent.
	 * <p>
	 * The previous reference to the parent can be found in the first parameter. The current parent can be obtained by
	 * calling {@link #getAgent()}.
	 * <p>
	 * Such actions may be initializations that depend on the parent or on other shards of the same agent.
	 * <p>
	 * Extending classes should always call super.parentChangeNotifier() first.
	 * 
	 * @param oldParent
	 *            - the previous value for the parent, if any.
	 */
	protected void parentChangeNotifier(ShardContainer oldParent)
	{
		// this class does not do anything here.
	}
	
	/**
	 * @return the shard initialization data. It cannot be modified, and it is guaranteed to not be <code>null</code>.
	 */
	protected MultiTreeMap getShardData()
	{
		return shardConfiguration;
	}
	
	/**
	 * The method calls is called by the parent {@link Agent} when an event occurs.
	 * 
	 * @param event
	 *            - the event which occurred.
	 */
	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		// This method does nothing here.
	}
	
	@Override
	final public AgentShardDesignation getShardDesignation()
	{
		return shardDesignation;
	}
	
	/**
	 * The return value is based on the shard's designation.
	 */
	@Override
	public String getName()
	{
		return shardDesignation.toString();
	}
	
	/**
	 * Setter for the parent agent. If an agent instance is already a parent of this shard, <code>removeParent</code>
	 * must be called first.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 * 
	 * @param parent
	 *            - the {@link CompositeAgent} instance that this shard is part of.
	 */
	@Override
	public final boolean addContext(EntityProxy<Agent> parent)
	{
		if(parentAgent != null)
			return ler(false, "Parent already set");
		if(parent == null || !(parent instanceof ShardContainer))
			return ler(false, "Parent should be a ShardContainer instance");
		parentAgent = (ShardContainer) parent;
		parentChangeNotifier(null);
		return true;
	}
	
	/**
	 * Sets the parent of the shard to <code>null</code>, effectively eliminating the shard from the agent.
	 * <p>
	 * After assigning the parent, <code>the parentChangeNotifier</code> method will be called, so that extending
	 * classes can take appropriate action.
	 */
	@Override
	public final boolean removeContext(EntityProxy<Agent> parent)
	{
		if(parentAgent == null)
			return ler(false, "Parent is not set");
		if(parentAgent != parent)
			return ler(false, "Argument is not the same as actual parent.");
		parentAgent = null;
		parentChangeNotifier((ShardContainer) parent);
		return true;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		lw("No general context supported for shards by default.");
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		lw("No general context supported for shards by default.");
		return false;
	}
	
	/**
	 * Retrieves the parent of the shard.
	 * 
	 * @return the {@link CompositeAgent} that is the parent of this shard; <code>null</code> if there is no parent set.
	 */
	final protected ShardContainer getAgent()
	{
		return parentAgent;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<AgentShard> asContext()
	{
		throw new UnsupportedOperationException("The AgentSharCore cannot be a context of another entity.");
	}
}
