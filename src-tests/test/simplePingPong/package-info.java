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
 * The scenario of two agents which ping messages between them (one sends and one replies).
 * <p>
 * Run this with:
 * <p>
 * <code>-package examples.compositeAgent -loader agent:composite -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";</code>
 * <p>
 * Expect to see at each 2 seconds 2 events: one from AgentB and one from AgentA.
 * 
 * @author Andrei Olaru
 */
package test.simplePingPong;
