package net.xqhs.flash.core;

/**
 * An element in the deployment, be it a support infrastructure, an agent, a feature, etc. It needs to have some sort of
 * persistent presence in the system, and therefore it has a life-cycle that can be started, stopped, and checked upon.
 * <p>
 * It is characterized by a name or other form of identification in the system.
 * <p>
 * Entities can be placed one in the context of one another, but one entity can have only one type of context that directly
 * contains it.
 * <p>
 * Normally, before being started, {@link Entity} instances are created by loaders.
 * 
 * @param <P>
 *            - the type of the entity that can contain this entity.
 * 
 * @author andreiolaru
 */
public interface Entity<P extends Entity<?>>
{
	/**
	 * Starts the life-cycle of the entity. If this goes well, from this moment on the entity should be executing
	 * normally.
	 * <p>
	 * The method must guarantee that once it has been started successfully, it can immediately begin to receive events,
	 * even if those events will not be processed immediately.
	 * 
	 * @return <code>true</code> if the entity was started without error. <code>false</code> otherwise.
	 */
	public boolean start();
	
	/**
	 * Stops the entity. After this method succeeds, the entity should not be executing any more.
	 * 
	 * @return <code>true</code> if the entity was stopped without error. <code>false</code> otherwise.
	 */
	public boolean stop();
	
	/**
	 * Queries the entity to check if it has completed its startup and is fully functional. The entity is running after it
	 * has fully started and until it is {@link #stop}ed.
	 * 
	 * @return <code>true</code> if the entity is currently running.
	 */
	public boolean isRunning();
	
	/**
	 * Retrieves the name (or other identification) of the entity.
	 * 
	 * @return the name.
	 */
	public String getName();
	
	/**
	 * Creates a link from a subordinate entity to a entity containing it in some way.
	 * 
	 * @param context
	 *            - a reference to the higher-level entity.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addContext(P context);
}
