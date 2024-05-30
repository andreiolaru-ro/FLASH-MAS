package net.xqhs.flash.core.interoperability;

import net.xqhs.flash.core.agent.AgentEvent;

public class BridgeAgentEvent extends AgentEvent {
	public final String PLATFORM_PREFIX = "platformPrefix";

	public BridgeAgentEvent(AgentEventType eventType, String platformPrefix) {
		super(eventType);
		add(PLATFORM_PREFIX, platformPrefix);
	}

	/**
	 * @return the platform prefix.
	 */
	public String getPlatformPrefix() {
		return getValue(PLATFORM_PREFIX);
	}
}
