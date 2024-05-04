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
 * This interface should be implemented by any proxy to a {@link Pylon} that offers messaging services.
 * <p>
 * There are currently two types of messaging proxies: "classic", implementing {@link ClassicMessagingPylonProxy}, where
 * messages are described by a source/destination/content triple; and "wave", implementing
 * {@link WaveMessagingPylonProxy}, where messages are {@link AgentWave}s.
 * 
 * @author Vlad Tălmaciu, Andrei Olaru
 */
public interface MessagingPylonProxy extends PylonProxy {
	// functionality actually depends on whether the pylon supports Classic or Wave interface.
}
