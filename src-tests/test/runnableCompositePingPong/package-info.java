/**
 * Same example as in compositePingPong, but using the Runnable property of composite agents.
 * <p>
 * <b>Verifies:</b>
 * <ul>
 * <li>correct access to the thread on which {@link net.xqhs.flash.core.composite.CompositeAgent}s run.
 * <li>correct stopping of entity threads.
 * <li>auxiliary: the possibility of different {@link net.xqhs.flash.core.node.Node} implementation and access to
 * entities inside it.
 * </ul>
 */
package test.runnableCompositePingPong;