package net.xqhs.flash.core.agent.composite;

import net.xqhs.flash.core.agent.composite.AgentFeatureDesignation.StandardAgentFeature;

enum UnimplementedFeatures {
	/**
	 * The designation of a component extending {@link VisualizableComponent}.
	 */
	VISUALIZABLE(
			StandardAgentFeature.AGENT_FEATURE_PACKAGE_ROOT + ".visualization.VisualizableComponent"),
	
	/**
	 * The designation of a component extending {@link CognitiveComponent}.
	 */
	COGNITIVE(StandardAgentFeature.AGENT_FEATURE_PACKAGE_ROOT + ".kb.ContextComponent"),
	
	/**
	 * The designation of a component extending {@link MovementComponent}.
	 */
	MOVEMENT,
	
	/**
	 * The designation of a component extending {@link BehaviorComponent}.
	 */
	BEHAVIOR,
	
	/**
	 * The designation of a component extending {@link WebserviceComponent}.
	 */
	WEBSERVICE,
	
	/**
	 * The designation of a component extending {@link HierarchicalComponent}.
	 */
	HIERARCHICAL,
	
	/**
	 * The designation of a component extending {@link ClaimComponent}.
	 */
	S_CLAIM(StandardAgentFeature.AGENT_FEATURE_PACKAGE_ROOT + ".claim.ClaimComponent"),
	
	/**
	 * TEMPORARY type for testing. TODO: remove this type.
	 */
	TESTING_COMPONENT,
	
	;
	
	/**
	 * Dummy constructor.
	 * 
	 * @param cls
	 *            - unused
	 */
	private UnimplementedFeatures(String cls)
	{
		// does nothing
	}
	
	/**
	 * Dummy constructor.
	 */
	private UnimplementedFeatures()
	{
		// does nothing
	}
}