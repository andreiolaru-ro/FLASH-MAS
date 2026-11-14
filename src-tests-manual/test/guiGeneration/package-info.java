/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this
 * project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
/**
 * The test shows if GUI-based (and remote GUI-based) interaction work.
 * <p>
 * When started, Agent 1 spawns a Swing GUI, and a central monitoring web server is available at http://localhost:8080.
 * Interaction can go in two directions:
 * <ul>
 * <li>whenever the field in the swing GUI is updated, the updated is reflected in the web interface (because
 * {@link test.guiGeneration.TestShard} calls
 * {@link net.xqhs.flash.gui.GuiShard#sendOutput(net.xqhs.flash.core.agent.AgentWave)}).
 * <li>when the button is pressed in the web interface, an event is received by the {@link test.guiGeneration.TestShard}
 * and the value is updated in all interfaces.
 * <li>the agent regularly increments the value in the text field, if the autocount configuration parameter is "on".
 * </ul>
 * 
 * @author Andrei Olaru
 */
package test.guiGeneration;