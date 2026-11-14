package net.xqhs.flash.abms;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.support.Pylon;

public interface Patch extends Entity<Pylon> {
    void step();

    boolean postAgentEvent(AgentEvent event);
}
