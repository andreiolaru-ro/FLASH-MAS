package net.xqhs.flash.core.interoperability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InteroperabilityRouter<T> {
	protected Map<String, T>	platformPrefixToEndpoint;				// ?

	private static final String	PLATFORM_PREFIX_SEPARATOR	= "/(?!/)";

	public void addEndpoint(String platformPrefix, T entityName) {
		if (platformPrefixToEndpoint == null)
			platformPrefixToEndpoint = new HashMap<>();

		platformPrefixToEndpoint.put(platformPrefix, entityName);
	}

	public T getEndpoint(String destination) {
		if (platformPrefixToEndpoint == null || destination == null)
			return null;

		T endpoint = platformPrefixToEndpoint.get(getPlatformPrefix(destination));
		if (endpoint == null)
			return null;

		return endpoint;
	}

	public static String getPlatformPrefix(String address) {
		return address.split(PLATFORM_PREFIX_SEPARATOR)[0];
	}

	public Set<String> getAllPlatformPrefixes() {
		return platformPrefixToEndpoint.keySet();
	}
}
