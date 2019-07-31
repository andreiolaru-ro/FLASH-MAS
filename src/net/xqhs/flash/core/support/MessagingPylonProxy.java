package net.xqhs.flash.core.support;

import net.xqhs.flash.core.Entity.EntityProxy;

public interface MessagingPylonProxy extends EntityProxy<Pylon> {
	boolean register(String agentName, MessageReceiver receiver);

	boolean send(String source, String destination, String content);
}
