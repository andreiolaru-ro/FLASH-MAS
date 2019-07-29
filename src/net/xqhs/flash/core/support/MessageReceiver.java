package net.xqhs.flash.core.support;

public interface MessageReceiver {
	public boolean receive(String source, String destination, String content);
}
