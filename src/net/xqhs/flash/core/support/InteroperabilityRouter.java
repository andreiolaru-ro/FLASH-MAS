package net.xqhs.flash.core.support;

import java.util.HashMap;
import java.util.Map;

public class InteroperabilityRouter {
//	protected Map<String, Map<String, MessageReceiver>> bridgeToSupportInfrastructureIdentifier;
	protected Map<String, String> platformToBridge;


	public boolean hasBridge(String entityName) {
		if (platformToBridge == null)
			return false;
		return platformToBridge.containsValue(entityName);
	}

	public void addBridge(String identifier, String entityName) {
		if (platformToBridge == null)
			platformToBridge = new HashMap<>();

		platformToBridge.put(identifier, entityName);
	}

	public String getBridge(String destination) {
		if (platformToBridge == null)
			return null;

//		String[] info = destination.split(AgentWave.PROTOCOL_SEPARATOR); // cu tot cu protocol
//		String identifier = info[1].split(AgentWave.ADDRESS_SEPARATOR)[0];

//		String bridgeEntity = platformToBridge.get(identifier);
//		if (bridgeEntity == null)
//			return null;
//
//		return bridgeEntity;
		return null;
	}

}
