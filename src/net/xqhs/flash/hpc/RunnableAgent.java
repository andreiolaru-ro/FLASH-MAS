package net.xqhs.flash.hpc;

import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Pylon;

/**
 * An agent that has the {@link RunnableEntity#run()} method.
 * 
 * @author Andrei Olaru
 */
public interface RunnableAgent extends Agent, RunnableEntity<Pylon> {
	// nothing additional to add
}
