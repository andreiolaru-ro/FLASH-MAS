package net.xqhs.flash.core.interoperability;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class InteroperabilityRouter<T> {
	protected Map<String, T>	platformPrefixToRoutingDestination;

	private static final String	PLATFORM_PREFIX_SEPARATOR	= "/(?!/)";

	public void addEndpoint(String platformPrefix, T entityName) {
		if (platformPrefixToRoutingDestination == null)
			platformPrefixToRoutingDestination = new HashMap<>();

		platformPrefixToRoutingDestination.put(platformPrefix, entityName);
	}

	public T getRoutingDestination(String finalDestination) {
		if (platformPrefixToRoutingDestination == null || finalDestination == null)
			return null;

		T routingDestination = platformPrefixToRoutingDestination.get(getPlatformPrefixFromAddress(finalDestination));
		if (routingDestination == null)
			return null;

		return routingDestination;
	}

	private static String getPlatformPrefixFromAddress(String address) {
		return address.split(PLATFORM_PREFIX_SEPARATOR)[0];
	}

	public Set<String> getAllPlatformPrefixes() {
		return platformPrefixToRoutingDestination.keySet();
	}

	public Collection<T> getAllDestinations() {
		return platformPrefixToRoutingDestination.values();
	}

	public boolean removeBridge(String entityName) {
		if (!platformPrefixToRoutingDestination.containsValue(entityName))
			return false;

		Iterator<Entry<String, T>> iterator = platformPrefixToRoutingDestination.entrySet().iterator();
		boolean foundBridge = false;
		while (iterator.hasNext()) {
			Entry<String, T> entry = iterator.next();
			if (entityName.equals(entry.getValue())) {
				iterator.remove();
				foundBridge = true;
			}
		}

		return foundBridge;
	}
}
