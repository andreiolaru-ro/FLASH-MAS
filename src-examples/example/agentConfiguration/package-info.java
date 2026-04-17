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
 * deployment for a configurable agent that outputs to its log "Hello World" and the time when it will stop.
 * <p>
 * The scenario can be started either via the deployment file (see {@link example.agentConfiguration.BootDeployment}) or via command line arguments (see {@link example.agentConfiguration.BootCLI})
 * <p>
 * Expected output: the agent outputs the fact that it is starting (it received the agent start event).
 * It outputs to its log "Hello World" and the time when it will stop.
 * If no configuration is done in the deployment, the agent will stop in 2 seconds.
 *
 * @author andreiolaru
 *
 */
package example.agentConfiguration;