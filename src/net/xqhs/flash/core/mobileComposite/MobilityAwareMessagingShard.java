package net.xqhs.flash.core.mobileComposite;

import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.support.MessagingShard;

/**
 * Messaging shards should implement this interface if the process of migration needs any sort of confirmation from the
 * support infrastructure that the entity is ready to move / is ready to restart after move, and this confirmation is
 * expected to take some time (e.g. communication with other machines is needed).
 * 
 * Messaging shards implementing this interface commit to partially manage the migration of the entity by actually
 * issuing the {@link AgentEventType#AGENT_STOP} and the {@link AgentEventType#AFTER_MOVE}. If these events are not
 * posted correctly (or at all) the agent will not function.
 * 
 * @author Andrei Olaru
 */
public interface MobilityAwareMessagingShard extends MessagingShard {
	// this is only a marker interface.
}
