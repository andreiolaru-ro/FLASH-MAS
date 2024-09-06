package net.xqhs.flash.core.interoperability;

import java.util.*;

import com.google.gson.JsonObject;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.webSocket.WebSocketPylon;

public class InteroperabilityRouter<T> {
//	private Map<String, T> platformPrefixToRoutingDestination;
	private Map<String, TreeMap<Integer, Set<T>>> platformPrefixToRoutingDestination;

	public void addRoutingDestinationForPlatform(String platformPrefix, T entityName, Integer distance) {
//		if (platformPrefixToRoutingDestination == null)
//			platformPrefixToRoutingDestination = new HashMap<>();
//
//		platformPrefixToRoutingDestination.put(platformPrefix, entityName);

		if (platformPrefixToRoutingDestination == null) {
			platformPrefixToRoutingDestination = new HashMap<>();
		}

		if (platformPrefixToRoutingDestination.get(platformPrefix) == null) {
			platformPrefixToRoutingDestination.put(platformPrefix, new TreeMap<>());
		}

		if (platformPrefixToRoutingDestination.get(platformPrefix).get(distance) == null) {
			platformPrefixToRoutingDestination.get(platformPrefix).put(distance, new HashSet<>());
		}

		platformPrefixToRoutingDestination.get(platformPrefix).get(distance).add(entityName);

	}

	public T getRoutingDestination(String finalDestination) {
//		if (platformPrefixToRoutingDestination == null || finalDestination == null)
//			return null;
//
//		T routingDestination = platformPrefixToRoutingDestination.get(getPlatformPrefixFromAddress(finalDestination));
//		if (routingDestination == null)
//			return null;
//
//		return routingDestination;

		if (platformPrefixToRoutingDestination == null || finalDestination == null) {
			return null;
		}

		if (platformPrefixToRoutingDestination.get(getPlatformPrefixFromAddress(finalDestination)) == null) {
			return null;
		}

		return platformPrefixToRoutingDestination.get(getPlatformPrefixFromAddress(finalDestination)).firstEntry().getValue().iterator().next();
	}

	public static String getPlatformPrefixFromAddress(String address) {
		return address.split(InteroperableMessagingPylonProxy.PLATFORM_PREFIX_SEPARATOR)[0];
	}

	public Set<String> getAllPlatformPrefixes() {
		if (platformPrefixToRoutingDestination == null)
			return null;

		return platformPrefixToRoutingDestination.keySet();
	}

	public Collection<T> getAllDestinations() {
//		if (platformPrefixToRoutingDestination == null)
//			return null;
//
//		return platformPrefixToRoutingDestination.values();

		if (platformPrefixToRoutingDestination == null)
			return null;

		Set<T> destinations = new HashSet<>();
		for (TreeMap<Integer, Set<T>> treeMap : platformPrefixToRoutingDestination.values()) {
			for (Set<T> tSet : treeMap.values()) {
				destinations.addAll(tSet);
			}
		}

		return destinations;
	}

	public boolean removeBridge(String bridgeAddress) {
//		if (platformPrefixToRoutingDestination == null || !platformPrefixToRoutingDestination.containsValue(bridgeAddress))
//			return false;
//
//		Iterator<Entry<String, T>> iterator = platformPrefixToRoutingDestination.entrySet().iterator();
//		boolean foundBridge = false;
//		while (iterator.hasNext()) {
//			Entry<String, T> entry = iterator.next();
//			if (bridgeAddress.equals(entry.getValue())) {
//				iterator.remove();
//				foundBridge = true;
//			}
//		}
//
//		return foundBridge;
		return false;
	}

	public boolean removeRoutingDestinationForPlatform(String platformPrefix, T routingDestination) {
		if (platformPrefixToRoutingDestination == null)
			return true;

		return platformPrefixToRoutingDestination.remove(platformPrefix, routingDestination);
	}

	public static JsonObject prependDestinationToMessage(JsonObject message, String destination) {
		String nodeName = null;
		if (message.has(WebSocketPylon.MESSAGE_NODE_KEY))
			nodeName = message.get(WebSocketPylon.MESSAGE_NODE_KEY).getAsString();

		message.remove(WebSocketPylon.MESSAGE_NODE_KEY);
		AgentWave messageAsWave = AgentWaveJson.toAgentWave(message);
		messageAsWave.prependDestination(destination);

		if (nodeName != null)
			AgentWaveJson.toJson(messageAsWave).addProperty(WebSocketPylon.MESSAGE_NODE_KEY, nodeName);

		return AgentWaveJson.toJson(messageAsWave);
	}
}
