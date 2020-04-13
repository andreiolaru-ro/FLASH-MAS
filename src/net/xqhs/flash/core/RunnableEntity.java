package net.xqhs.flash.core;

/**
 * This version of {@link Entity} adds the method {@link Runnable#run()}, enabling whoever holds a reference to this
 * entity to also control the thread on which the entity executes.
 * 
 * @param <P>
 *            - the type of the entity that can contain (be the context of) this entity.
 * 
 * @author Andrei Olaru
 */
public interface RunnableEntity<P extends Entity<?>> extends Entity<P>, Runnable
{
	/**
	 * The method will start the entity and will only returned after the entity has been {@link #stop()}ed.
	 */
	@Override
	public void run();
}
