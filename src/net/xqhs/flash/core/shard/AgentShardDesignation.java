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
 * Implementation for feature designations. A feature may either have a standard designation, or a custom designation.
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
	 * Enumeration of standard feature names / functionalities.
	 * <p>
	 * The enumeration entries also contain information about the default implementation of the specified feature. The
	 * name of the implementation class can be given when creating the entry, or can be inferred based on the name of
	 * the entry and the constants in the enumeration.
	 * 
	 * @author andreiolaru
	 */
	public enum StandardAgentShard {
		
		/**
		 * The designation of a feature extending {@link MessagingComponent}.
		 */
		MESSAGING,
		
		;
		
		/**
		 * Suffix for feature implementation.
		 */
		private static final String	AGENT_FEATURE_CLASS_SUFFIX	= "Feature";
		/**
		 * Default parent package containing default feature implementations.
		 */
		static final String			AGENT_FEATURE_PACKAGE_ROOT	= "tatami.core.features";
		
		/**
		 * The fully qualified class name of the default feature implementation.
		 */
		String						featureClass;
		
		/**
		 * The name of the feature, as appearing in the deployment file.
		 */
		String						featureName;
		
		/**
		 * Specifies the fully qualified class name of the feature implementation.
		 * 
		 * @param classname
		 *            - the fully qualified class name.
		 */
		private StandardAgentShard(String classname)
		{
			// FIXME: check that package and class exist
			featureClass = classname;
			featureName = this.name().toLowerCase();
		}
		
		/**
		 * Infers the class of the feature implementation based on the name of the feature and constants in this class.
		 */
		private StandardAgentShard()
		{
			this(null);
			// FIXME: check that package and class exist
//			String featurePackage = AGENT_FEATURE_PACKAGE_ROOT + "." + featureName;
//			featureClass = featurePackage + "." + featureName.substring(0, 1).toUpperCase() + featureName.substring(1)
//					+ AGENT_FEATURE_CLASS_SUFFIX;
		}
		
		/**
		 * Gets the specified or inferred class name for the default implementation of the feature.
		 * 
		 * @return the class name.
		 */
		public String getClassName()
		{
			return featureClass;
		}
		
		/**
		 * Gets the name of the feature, as appearing in the deployment file (in lowercase).
		 * 
		 * @return the name of the feature.
		 */
		public String featureName()
		{
			return featureName;
		}
		
		/**
		 * @return the {@link AgentShardDesignation} corresponding to this standard feature.
		 */
		public AgentShardDesignation toAgentFeatureDesignation()
		{
			return AgentShardDesignation.standardFeature(this);
		}

		/**
		 * Returns the {@link StandardAgentShard} instance that corresponds to the specified name.
		 * 
		 * @param featureName
		 *            - the name of the feature, as appearing in the deployment file.
		 * @return the corresponding {@link StandardAgentShard} instance.
		 */
		public static StandardAgentShard toStandardAgentFeature(String featureName)
		{
			try
			{
				return StandardAgentShard.valueOf(featureName.toUpperCase());
			} catch(Exception e)
			{
				return null;
			}
		}
		
		/**
		 * Combines the functionality of {@link #toStandardAgentFeature} and {@link #toAgentFeatureDesignation} to
		 * return an {@link AgentShardDesignation} based on a feature name. If no appropriate standard feature is
		 * found, <code>null</code> is returned.
		 * 
		 * @param featureName
		 *            - the name of the feature, as appearing in the deployment file.
		 * @return the {@link AgentShardDesignation} corresponding to this standard feature with the name;
		 *         <code>null</code> if none found.
		 */
		public static AgentShardDesignation toStandardAgentFeatureDesignation(String featureName)
		{
			StandardAgentShard std = toStandardAgentFeature(featureName);
			return std != null ? std.toAgentFeatureDesignation() : null;
		}
	}
	
	/**
	 * This field is used if the designation designates a standard feature.
	 */
	protected StandardAgentShard	standardFeature;
	
	/**
	 * This field is used if the designation designates a custom feature.
	 */
	protected String				customFeature;
	
	/**
	 * Private constructor. One and only one argument must be non-<code>null</code> at a time.
	 * 
	 * @param standardsName
	 *            - the standard feature.
	 * @param customName
	 *            - the custom feature name.
	 */
	private AgentShardDesignation(StandardAgentShard standardsName, String customName)
	{
		if(standardsName != null && customName != null)
			throw new IllegalArgumentException("Both arguments cannot be non-null.");
		if(standardsName == null && customName == null)
			throw new IllegalArgumentException("Both arguments cannot be null.");
		standardFeature = standardsName;
		customFeature = customName;
	}
	
	/**
	 * Creates a custom feature designation with the specified name.
	 * <p>
	 * The name must not be the same as an existing {@link StandardAgentShard}.
	 * 
	 * @param featureName
	 *            - the custom name.
	 * @return the designation.
	 */
	public static AgentShardDesignation customFeature(String featureName)
	{
		if(StandardAgentShard.toStandardAgentFeature(featureName) != null)
			throw new IllegalArgumentException("There already is a standard feature with the name " + featureName);
		return new AgentShardDesignation(null, featureName);
	}
	
	/**
	 * Creates a standard designation instance.
	 * <p>
	 * An easier way to do this is to call {@link StandardAgentShard#toAgentFeatureDesignation()}.
	 * 
	 * @param standardFeature
	 *            - the standard feature.
	 * @return the designation.
	 */
	public static AgentShardDesignation standardFeature(StandardAgentShard standardFeature)
	{
		return new AgentShardDesignation(standardFeature, null);
	}
	
	/**
	 * Creates a designation that is either a standard designation, if one such designation is found, or a custom
	 * designation, otherwise.
	 * 
	 * @param featureName
	 *            - the name of the desired feature.
	 * @return the designation.
	 */
	public static AgentShardDesignation autoFeature(String featureName)
	{
		StandardAgentShard std = StandardAgentShard.toStandardAgentFeature(featureName);
		if(std != null)
			return standardFeature(std);
		return customFeature(featureName);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof AgentShardDesignation))
			return false;
		if(customFeature != null)
			return customFeature.equals(((AgentShardDesignation) obj).customFeature);
		return standardFeature.equals(((AgentShardDesignation) obj).standardFeature);
	}
	
	@Override
	public int hashCode()
	{
		if(customFeature != null)
			return customFeature.hashCode();
		return standardFeature.hashCode();
	}
	
	@Override
	public String toString()
	{
		// TODO: decide if there should be different rendering for the two cases.
		if(customFeature != null)
			return customFeature;
		return standardFeature.featureName();
	}
}
