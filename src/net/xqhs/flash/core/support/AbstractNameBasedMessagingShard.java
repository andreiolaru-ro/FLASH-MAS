/*******************************************************************************
 * Copyright (C) 2015 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.support;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.ShardContainer;

/**
 * A simple extension of {@link AbstractMessagingShard}, which leaves to implement only
 * {@link #sendMessage(String, String, String)} as an abstract method, and also the registration of a message receiver
 * to the pylon. This class is meant to be extended by messaging shards in support infrastructures that use agent names
 * as agent addresses (agents are addressed by name). Therefore, the address of the agent is always the same with its
 * name.
 * <p>
 * It is also presumed that agent names do not contain slashes. An exception is thrown if the shard is loaded in an
 * agent with a name containing a slash (or whatever {@link AgentWave#ADDRESS_SEPARATOR} is set to).
 * 
 * @author Andrei Olaru
 */
public abstract class AbstractNameBasedMessagingShard extends AbstractMessagingShard
{
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 13149367588469383L;
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent)
	{
		if(getAgent() != null && getAgent().getEntityName().contains(AgentWave.ADDRESS_SEPARATOR))
			throw new IllegalStateException(
					"Name-based messaging cannot support agent names containing " + AgentWave.ADDRESS_SEPARATOR);
		super.parentChangeNotifier(oldParent);
	}
	
	@Override
	public String getAgentAddress()
	{
		return getAgent().getEntityName();
	}
	
	@Override
	public String extractAgentAddress(String endpoint)
	{
		return endpoint.split(AgentWave.ADDRESS_SEPARATOR, 2)[0];
	}
}
