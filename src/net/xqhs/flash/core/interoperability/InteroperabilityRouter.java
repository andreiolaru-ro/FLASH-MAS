package net.xqhs.flash.core.interoperability;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class InteroperabilityRouter<T> {
	protected Map<String, T>	platformPrefixToEndpoint;				// rename ?

	private static final String	PLATFORM_PREFIX_SEPARATOR	= "/(?!/)";

	public void addEndpoint(String platformPrefix, T entityName) {
		if (platformPrefixToEndpoint == null)
			platformPrefixToEndpoint = new HashMap<>();

		platformPrefixToEndpoint.put(platformPrefix, entityName);
	}

	public T getEndpoint(String destination) {
		if (platformPrefixToEndpoint == null || destination == null)
			return null;

		T endpoint = platformPrefixToEndpoint.get(getPlatformPrefixFromAddress(destination));
		if (endpoint == null)
			return null;

		return endpoint;
	}

	private static String getPlatformPrefixFromAddress(String address) {
		return address.split(PLATFORM_PREFIX_SEPARATOR)[0];
	}

	public Set<String> getAllPlatformPrefixes() {
		return platformPrefixToEndpoint.keySet();
	}

	public Collection<T> getAllEndpoints() {
		return platformPrefixToEndpoint.values();
	}

	public void removeBridge(String entityName) {
		if (!platformPrefixToEndpoint.containsValue(entityName))
			return;

		Iterator<Entry<String, T>> iterator = platformPrefixToEndpoint.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, T> entry = iterator.next();
			if (entityName.equals(entry.getValue()))
				iterator.remove();
		}
	}
}
