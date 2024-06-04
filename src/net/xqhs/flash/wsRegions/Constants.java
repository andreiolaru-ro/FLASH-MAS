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
		 * Message sent from new created agent to their home Region-Server. The registered agent is the source of the
		 * message.
		 */
		REGISTER,
		/**
		 * Message sent from agent to the Region-Server for unregistering. The agent wanting to unregister
		 * is the source of the message.
		 */
		UNREGISTER,
		/**
		 * Message sent from new arrived agent to its host Region-Server.
		 */
		CONNECT,
		/**
		 * Message sent from agent to the Region-Server, when it wants to leave to another node.
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
		 * Message sent from Region-Server to the agent that wants to leave.
		 */
		REQ_ACCEPT,
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
