package net.xqhs.flash.core.agent;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation;

/**
 * A feature (also called a component) is characterized by its functionality, identified by means of its designation --
 * an instance of {@link AgentFeatureDesignation}.
 * 
 * @author andreiolaru
 */
public interface AgentFeature extends Entity<Agent>
{
	/**
	 * @return the designation of the feature (instance of {@link AgentFeatureDesignation}).
	 */
	AgentFeatureDesignation getFeatureDesignation();
}
