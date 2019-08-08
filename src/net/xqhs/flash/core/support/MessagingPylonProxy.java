package net.xqhs.flash.core.support;

import net.xqhs.flash.core.Entity.EntityProxy;

/**
 * This interface should be implemented by any proxy to a {@link Pylon} that offers messaging services.
 * 
 * @author Vlad Tălmaciu
 */
public interface MessagingPylonProxy extends EntityProxy<Pylon>
{
	boolean register(String agentName, MessageReceiver receiver);
	
	boolean send(String source, String destination, String content);
}
