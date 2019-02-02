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

enum UnimplementedFeatures {
	/**
	 * The designation of a component extending {@link VisualizableComponent}.
	 */
	VISUALIZABLE(
			StandardAgentShard.AGENT_FEATURE_PACKAGE_ROOT + ".visualization.VisualizableComponent"),
	
	/**
	 * The designation of a component extending {@link CognitiveComponent}.
	 */
	COGNITIVE(StandardAgentShard.AGENT_FEATURE_PACKAGE_ROOT + ".kb.ContextComponent"),
	
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
	S_CLAIM(StandardAgentShard.AGENT_FEATURE_PACKAGE_ROOT + ".claim.ClaimComponent"),
	
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
