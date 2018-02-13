package net.xqhs.flash.core;

/**
 * An element in the deployment, be it a support infrastructure, an agent, a feature, etc. It needs to have some sort of
 * persistent presence in the system, and therefore it has a life-cycle that can be started, stopped, and checked upon.
 * <p>
 * It is characterized by a name or other form of identification in the system.
 * <p>
 * Things can be placed in the context of one another, but one thing can have only one type of context that directly
 * contains it.
 * <p>
 * Normally, before being started {@link Thing} instances are created by loaders.
 * 
 * @param <P>
 *            - the type of the thing that can contain this thing.
 * 
 * @author andreiolaru
 */
public interface Thing<P extends Thing<?>>
{
	/**
	 * Starts the life-cycle of the thing. If this goes well, from this moment on the thing should be executing
	 * normally.
	 * <p>
	 * The method must guarantee that once it has been started successfully, it can immediately begin to receive events,
	 * even if those events will not be processed immediately.
	 * 
	 * @return <code>true</code> if the thing was started without error. <code>false</code> otherwise.
	 */
	public boolean start();
	
	/**
	 * Stops the thing. After this method succeeds, the thing should not be executing any more.
	 * 
	 * @return <code>true</code> if the thing was stopped without error. <code>false</code> otherwise.
	 */
	public boolean stop();
	
	/**
	 * Queries the thing to check if it has completed its startup and is fully functional. The thing is running after it
	 * has fully started and until it is {@link #stop}ed.
	 * 
	 * @return <code>true</code> if the thing is currently running.
	 */
	public boolean isRunning();
	
	/**
	 * Retrieves the name (or other identification) of the thing.
	 * 
	 * @return the name.
	 */
	public String getName();
	
	/**
	 * Creates a link from a subordinate thing to a thing containing it in some way.
	 * 
	 * @param context
	 *            - a reference to the higher-level thing.
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	public boolean addContext(P context);
}
