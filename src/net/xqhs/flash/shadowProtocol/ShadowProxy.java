package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.util.logging.Unit;

import java.util.Map;

/**
 * Proxy used to keep track of the movement of all agents belonging to a shadow
 *  - the shadow is located where the agent was created
 *  - keep the agent's next destination
 */
public class ShadowProxy extends Unit {

    /**
     * Keeps a map of the agent and the next destination
     */
    private Map<String, MessagingPylonProxy> nextDestination;
}
