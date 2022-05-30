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

import maria.NonSerializableShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import org.json.simple.JSONObject;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.OperationUtils;


/**
 * The {@link WebSocketMessagingShard} class manages the link between agent's messaging service and its pylon.
 *
 * @author Florina Nastasoiu
 */
public class WebSocketMessagingShard extends AbstractNameBasedMessagingShard implements NonSerializableShard {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 2L;
	
	/**
	 * Endpoint name for this shard (see {@link AgentWave}).
	 */
	protected static final String SHARD_ENDPOINT = "messaging";
	
	/**
	 * Reference to the local Websocket pylon.
	 */
	private MessagingPylonProxy	pylon;
	
	/**
	 * The proxy to this shard, to be used by the pylon.
	 */
	public transient MessageReceiver inbox;
	
	/**
	 * Default constructor.
	 */
	public WebSocketMessagingShard() {
		super();
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!(context instanceof MessagingPylonProxy)) {
			return false;
		}
		if(inbox == null)
			inbox = new MessageReceiver() {
				@Override
				public void receive(String source, String destination, String content) {
					receiveMessage(source, destination, content);
				}
			};
		pylon = (MessagingPylonProxy) context;
		System.out.println("Added pylon to messaging shard " + this + " " + pylon);
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType().equals(AgentEventType.AGENT_START))
			pylon.register(getAgent().getEntityName(), inbox);
		if(event.getType().equals(AgentEventType.AGENT_WAVE))
			if((((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT)) {
				JSONObject msg = OperationUtils.operationToJSON("message", "", ((AgentWave) event).getContent(),
						DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME);
				sendMessage(getAgent().getEntityName() + "/" + SHARD_ENDPOINT,
						DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME + "/control", msg.toString());
			}
	}
	
	@Override
	public boolean sendMessage(String target, String source, String content) {
//		System.out.println("sendmessage from target " + target + " to source " + source + " in messaging shard " + this);
		return pylon.send(target, source, content);
	}
	
	@Override
	protected void receiveMessage(String source, String destination, String content) {
		super.receiveMessage(source, destination, content);
	}
	
	/**
	 * This can be called by non-agent entities to register their messaging shard.
	 */
	@Override
	public void register(String entityName) {
		pylon.register(entityName, inbox);
	}

	@Override
	public MultiTreeMap getShardConfiguration() {
		return shardConfiguration;
	}

//	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
//		inbox = new MessageReceiver() {
//			@Override
//			public void receive(String source, String destination, String content) {
//				receiveMessage(source, destination, content);
//			}
//		};
//	}
}
