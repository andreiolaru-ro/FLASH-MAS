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
package net.xqhs.flash.webSocket;

import net.xqhs.flash.core.mobileComposite.NonSerializableShard;
import net.xqhs.flash.core.support.NameBasedMessagingShard;


/**
 * The {@link WebSocketMessagingShard} class manages the link between agent's messaging service and its pylon.
 * <p>
 * it is currently standard {@link NameBasedMessagingShard}.
 */
public class WebSocketMessagingShard extends NameBasedMessagingShard implements NonSerializableShard {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 2L;
	
	@Override
	protected void receiveMessage(String source, String destination, String content) {
		super.receiveMessage(source, destination, content);
	}
}
