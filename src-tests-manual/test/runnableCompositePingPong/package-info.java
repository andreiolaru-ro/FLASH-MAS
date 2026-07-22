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
 * Same example as in compositePingPong, but using the Runnable property of composite agents.
 * <p>
 * <b>Verifies:</b>
 * <ul>
 * <li>correct access to the thread on which {@link net.xqhs.flash.core.composite.CompositeAgent}s run.
 * <li>correct stopping of entity threads.
 * <li>auxiliary: the possibility of different {@link net.xqhs.flash.core.node.Node} implementation and access to
 * entities inside it.
 * </ul>
 */
package test.runnableCompositePingPong;
