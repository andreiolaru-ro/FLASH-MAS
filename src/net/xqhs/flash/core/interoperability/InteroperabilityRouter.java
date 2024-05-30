package net.xqhs.flash.core.interoperability;

import java.util.HashMap;
import java.util.Map;

public class InteroperabilityRouter {
	protected Map<String, String>	platformToBridge;
	private static final String		PLATFORM_PREFIX_SEPARATOR	= "/(?!/)";

	public boolean hasBridge(String entityName) {
		if (platformToBridge == null)
			return false;
		return platformToBridge.containsValue(entityName);
	}

	public boolean canRouteToPlatform(String platformPrefix) {
		if (platformToBridge == null)
			return false;
		return platformToBridge.containsKey(platformPrefix);
	}

	public void addBridge(String platformPrefix, String entityName) {
		if (platformToBridge == null)
			platformToBridge = new HashMap<>();

		platformToBridge.put(platformPrefix, entityName);
	}

	public String getBridgeName(String destination) {
		if (platformToBridge == null)
			return null;

		String bridgeEntity = platformToBridge.get(getPlatformPrefix(destination));
		if (bridgeEntity == null)
			return null;

		return bridgeEntity;
	}

	public static String getPlatformPrefix(String address) {
		return address.split(PLATFORM_PREFIX_SEPARATOR)[0];
	}
}
