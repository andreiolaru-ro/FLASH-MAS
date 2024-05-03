package net.xqhs.flash.wsRegions;

import net.xqhs.util.logging.Debug.DebugItem;

/**
 * Constants used in the WS Regions protocol.
 * 
 * @author Andrei Olaru
 * @author Monica Pricope
 */
public class Constants {
	/**
	 * Debugging settings for messaging shards.
	 */
	public static enum Dbg implements DebugItem {
		/**
		 * General messaging debugging switch.
		 */
		DEBUG_WSREGIONS(true),
		
		;
		
		/**
		 * Activation state.
		 */
		boolean isset;
		
		/**
		 * @param set
		 *            - activation state.
		 */
		private Dbg(boolean set) {
			isset = set;
		}
		
		@Override
		public boolean toBool() {
			return isset;
		}
	}
	
	/**
	 * Types of messages.
	 * 
	 * @author Monica Pricope
	 */
	public enum MessageType {
		/**
		 * Message sent from new created agent to the Region-Server from their birth region. Contains the next fields:
		 * type, node (pylon name), source (sender agent name)
		 */
		REGISTER,
		/**
		 * Message sent from new arrived agent to the Region-Server. Contains the next fields: type, node (pylon name),
		 * source (sender agent name)
		 */
		CONNECT,
		/**
		 * Standard message, sent from one agent to another agent. Contains the next fields: type, node (pylon name),
		 * source (sender agent name), destination (receiver agent name), content (message content)
		 */
		CONTENT,
		/**
		 * Message sent from agent to the Region-Server, when it wants to leave to another node. Contains the next
		 * fields: type, node (pylon name), source (sender agent name)
		 */
		REQ_LEAVE,
		/**
		 * Message sent from current Region-Server to the home Region-Server of an entity, when that agent wants to
		 * leave to another node. The name of the entity is in the content of the wave.
		 */
		REQ_BUFFER,
		/**
		 * Message sent from a host Region-Server to the home Region-Server of an entity, when the agent arrives in the
		 * new region. The name of the entity is in the content of the wave.
		 */
		AGENT_UPDATE,
		/**
		 * Message sent from Region-Server to the agent Contains the next fields: type, node (pylon name), source
		 * (sender server name)
		 */
		REQ_ACCEPT,
		/**
		 * Message sent from a Node to another Node Contains the agent in serialized form
		 */
		AGENT_CONTENT,
	}
	
	/**
	 * Key for the type of protocol-internal event.
	 */
	public static final String	EVENT_TYPE_KEY	= "wsregions-event";
	/**
	 * Wave element for messages which are internal to the protocol.
	 */
	public static final String	PROTOCOL		= "wsregions-protocol";
}
