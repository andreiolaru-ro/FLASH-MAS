package net.xqhs.flash.ent_op;

/**
 * This interface serves as a base interface for any implementation which fits the <i>model</i> of an entity. For
 * instance, an interface called <code>AgentModel</code> extending {@link EntityModel}, can be used as interface for any
 * implementation that <i>models</i> an agent.
 * <p>
 * <b>The model is not the entity.</b> But a model of, for instance, a remote entity may be used to facilitate the
 * interaction with the remote entity by storing some known functionality of that remote entity.
 * 
 * @author Andrei Olaru
 */
public interface EntityModel {
	handleCall()
}
