package net.xqhs.flash.core.support;

import net.xqhs.flash.core.Entity.Context;

public interface MessagingPylonProxy extends Context<Pylon> {
	boolean register(String agentName, MessageReceiver receiver);

	boolean send(String source, String destination, String content);
}
