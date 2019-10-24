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
package net.xqhs.flash.core.shard;

import java.io.Serializable;

/**
 * Implementation for shard designations. A shard may either have a standard designation, or a custom designation.
 * 
 * @author Andrei Olaru
 */
public class AgentShardDesignation implements Serializable
{
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Enumeration of standard shard names / functionalities.
	 * <p>
	 * The enumeration entries also contain information about the default implementation of the specified shard. The
	 * name of the implementation class can be given when creating the entry, or can be inferred based on the name of
	 * the entry and the constants in the enumeration.
	 * 
	 * @author andreiolaru
	 */
	public enum StandardAgentShard {
		
		/**
		 * The designation of a shard offering messaging services.
		 */
		MESSAGING,
		
		;
		
		/**
		 * Suffix for shard implementation.
		 */
		private static final String	AGENT_SHARD_CLASS_SUFFIX	= "Shard";
		
		/**
		 * The fully qualified class name of the default shard implementation.
		 */
		String						shardClass;
		
		/**
		 * The name of the shard, as appearing in the deployment file.
		 */
		String						shardName;
		
		/**
		 * Specifies the fully qualified class name of the shard implementation.
		 * 
		 * @param classname
		 *                      - the fully qualified class name.
		 */
		private StandardAgentShard(String classname)
		{
			// FIXME: check that package and class exist
			shardClass = classname;
			shardName = this.name().toLowerCase();
		}
		
		/**
		 * Infers the class of the shard implementation based on the name of the shard and constants in this class.
		 */
		private StandardAgentShard()
		{
			this(null);
			// FIXME: check that package and class exist
			// String shardPackage = AGENT_SHARD_PACKAGE_ROOT + "." + shardName;
			// shardClass = shardPackage + "." + shardName.substring(0, 1).toUpperCase() + shardName.substring(1)
			// + AGENT_SHARD_CLASS_SUFFIX;
		}
		
		/**
		 * Gets the specified or inferred class name for the default implementation of the shard.
		 * 
		 * @return the class name.
		 */
		public String getClassName()
		{
			return shardClass;
		}
		
		/**
		 * Gets the name of the shard, as appearing in the deployment file (in lowercase).
		 * 
		 * @return the name of the shard.
		 */
		public String shardName()
		{
			return shardName;
		}
		
		/**
		 * @return the {@link AgentShardDesignation} corresponding to this standard shard.
		 */
		public AgentShardDesignation toAgentShardDesignation()
		{
			return AgentShardDesignation.standardShard(this);
		}

		/**
		 * Returns the {@link StandardAgentShard} instance that corresponds to the specified name.
		 * 
		 * @param shardName
		 *                      - the name of the shard, as appearing in the deployment file.
		 * @return the corresponding {@link StandardAgentShard} instance.
		 */
		public static StandardAgentShard toStandardAgentShard(String shardName)
		{
			try
			{
				return StandardAgentShard.valueOf(shardName.toUpperCase());
			} catch(Exception e)
			{
				return null;
			}
		}
		
		/**
		 * Combines the functionality of {@link #toStandardAgentShard} and {@link #toAgentShardDesignation} to return an
		 * {@link AgentShardDesignation} based on a shard name. If no appropriate standard shard is found,
		 * <code>null</code> is returned.
		 * 
		 * @param shardName
		 *                      - the name of the shard, as appearing in the deployment file.
		 * @return the {@link AgentShardDesignation} corresponding to this standard shard with the name;
		 *         <code>null</code> if none found.
		 */
		public static AgentShardDesignation toStandardAgentShardDesignation(String shardName)
		{
			StandardAgentShard std = toStandardAgentShard(shardName);
			return std != null ? std.toAgentShardDesignation() : null;
		}
	}
	
	/**
	 * This field is used if the designation designates a standard shard.
	 */
	protected StandardAgentShard	standardShard;
	
	/**
	 * This field is used if the designation designates a custom shard.
	 */
	protected String				customShard;
	
	/**
	 * Private constructor. One and only one argument must be non-<code>null</code> at a time.
	 * 
	 * @param standardsName
	 *                          - the standard shard.
	 * @param customName
	 *                          - the custom shard name.
	 */
	private AgentShardDesignation(StandardAgentShard standardsName, String customName)
	{
		if(standardsName != null && customName != null)
			throw new IllegalArgumentException("Both arguments cannot be non-null.");
		if(standardsName == null && customName == null)
			throw new IllegalArgumentException("Both arguments cannot be null.");
		standardShard = standardsName;
		customShard = customName;
	}
	
	/**
	 * Creates a custom shard designation with the specified name.
	 * <p>
	 * The name must not be the same as an existing {@link StandardAgentShard}.
	 * 
	 * @param shardName
	 *                      - the custom name.
	 * @return the designation.
	 */
	public static AgentShardDesignation customShard(String shardName)
	{
		if(StandardAgentShard.toStandardAgentShard(shardName) != null)
			throw new IllegalArgumentException("There already is a standard shard with the name " + shardName);
		return new AgentShardDesignation(null, shardName);
	}
	
	/**
	 * Creates a standard designation instance.
	 * <p>
	 * An easier way to do this is to call {@link StandardAgentShard#toAgentShardDesignation()}.
	 * 
	 * @param standardShard
	 *                          - the standard shard.
	 * @return the designation.
	 */
	public static AgentShardDesignation standardShard(StandardAgentShard standardShard)
	{
		return new AgentShardDesignation(standardShard, null);
	}
	
	/**
	 * Creates a designation that is either a standard designation, if one such designation is found, or a custom
	 * designation, otherwise.
	 * 
	 * @param shardName
	 *                      - the name of the desired shard.
	 * @return the designation.
	 */
	public static AgentShardDesignation autoDesignation(String shardName)
	{
		StandardAgentShard std = StandardAgentShard.toStandardAgentShard(shardName);
		if(std != null)
			return standardShard(std);
		return customShard(shardName);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof AgentShardDesignation))
			return false;
		if(customShard != null)
			return customShard.equals(((AgentShardDesignation) obj).customShard);
		return standardShard.equals(((AgentShardDesignation) obj).standardShard);
	}
	
	@Override
	public int hashCode()
	{
		if(customShard != null)
			return customShard.hashCode();
		return standardShard.hashCode();
	}
	
	@Override
	public String toString()
	{
		// TODO: decide if there should be different rendering for the two cases.
		if(customShard != null)
			return customShard;
		return standardShard.shardName();
	}
}
