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
 * 
 */
/**
 * A simple deployment, testing the deployment (via XML file) of very basic scenarios. See <code>deployment.xml</code>.
 * <p>
 * An agent that outputs to its log all the events that happen, placed in a default environment.
 * <p>
 * Expected output: the agent outputs the fact that it is starting (it received the agent started event). The agent
 * stops after a short while.
 * 
 * @author andreiolaru
 *
 */
package example.echoAgent;
