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

/**
 * This interface should be implemented by any proxy to a {@link Pylon} that offers messaging services.
 * 
 * @author Vlad Tălmaciu, Andrei Olaru
 */
public interface MessagingPylonProxy extends PylonProxy
{
	/**
	 * Registers an agent with the specified name, associating with it a {@link MessageReceiver} instance.
	 * 
	 * @param agentName
	 *                      - the name of the agent.
	 * @param receiver
	 *                      - the {@link MessageReceiver} instance to receive messages.
	 * @return an indication of success.
	 */
	boolean register(String agentName, MessageReceiver receiver);
	
	/**
	 * Requests to the pylon to send a message.
	 * 
	 * @param source
	 *                        - the source endpoint.
	 * @param destination
	 *                        - the destination endpoint.
	 * @param content
	 *                        - the content of the message.
	 * @return an indication of success.
	 */
	boolean send(String source, String destination, String content);
}
