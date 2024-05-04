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
package net.xqhs.flash.core.support;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * This interface should be implemented by any entity which is able to receive messages as waves, from a support
 * infrastructure (and, more concretely, from a {@link Pylon} using a {@link WaveMessagingPylonProxy} as a proxy).
 * <p>
 * In practice, instances of this interface should allow the link between a {@link Pylon} and a {@link MessagingShard},
 * without exposing to the Pylon a reference to the shard.
 * 
 * @author Andrei Olaru
 */
public interface WaveReceiver {
	/**
	 * The method to be called when a message is received.
	 * 
	 * @param wave
	 *            - the wave that was received.
	 */
	public void receive(AgentWave wave);
}
