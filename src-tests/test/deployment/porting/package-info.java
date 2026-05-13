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
/**
 * Tests for porting behavior in deployment configuration (Issue #31).
 * <p>
 * Tests what happens when non-predefined categories (agentarray) or predefined 
 * categories (shard) are declared between predefined hierarchy levels.
 * <p>
 * Scenarios tested:
 * <ul>
 * <li>scenario1.xml — agentarray with agent at deployment level</li>
 * <li>scenario2.xml — agentarray with agent inside a node</li>
 * <li>scenario3.xml — shard explicitly under each agent (correct baseline)</li>
 * <li>scenario4.xml — two pylons, two agents without in-context-of</li>
 * </ul>
 * <p>
 * <b>Verifies:</b> correct porting and lifting behavior in
 * {@link net.xqhs.flash.core.DeploymentConfiguration#postProcess}
 */
package test.deployment.porting;