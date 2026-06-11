package net.xqhs.flash.core.interoperability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

	/**
	 * Returns all known routing destinations for the given final destination address, ordered by ascending distance
	 * (closest bridges first). This allows callers to try each candidate in order and skip any that are unavailable.
	 *
	 * @param finalDestination
	 *            - the address of the final destination
	 * @return an ordered {@link List} of routing destinations, or <code>null</code> if none are known
	 */
	public List<T> getRoutingDestinations(String finalDestination) {
		if (platformPrefixToRoutingDestination == null || finalDestination == null)
			return null;

		TreeMap<Integer, Set<T>> routesForPrefix = platformPrefixToRoutingDestination
				.get(getPlatformPrefixFromAddress(finalDestination));
		if (routesForPrefix == null || routesForPrefix.isEmpty())
			return null;

		List<T> result = new ArrayList<>();
		for (Set<T> candidates : routesForPrefix.values())
			result.addAll(candidates);
		return result;
	}

	public static String getPlatformPrefixFromAddress(String address) {
		String[] parts = address.split(InteroperableMessagingPylonProxy.PLATFORM_PREFIX_SEPARATOR);
		if (parts.length < 2)
			return address;

		int authorityStart = address.indexOf("://");
		if (authorityStart >= 0) {
			int pathStart = address.indexOf('/', authorityStart + 3);
			if (pathStart >= 0)
				return address.substring(0, pathStart);
			return address;
		}

		return parts[0];
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
		if (message.has(WebSocketPylon.MESSAGE_NODE_KEY)) {
			nodeName = message.get(WebSocketPylon.MESSAGE_NODE_KEY).getAsString();
			message.remove(WebSocketPylon.MESSAGE_NODE_KEY);
		}

		AgentWave messageAsWave = AgentWaveJson.toAgentWave(message);
		messageAsWave.prependDestination(destination);

		if (nodeName != null)
			AgentWaveJson.toJson(messageAsWave).addProperty(WebSocketPylon.MESSAGE_NODE_KEY, nodeName);

		return AgentWaveJson.toJson(messageAsWave);
	}

	public JsonObject getAllRoutingInfo() {
		if (platformPrefixToRoutingDestination == null || platformPrefixToRoutingDestination.isEmpty())
			return null;

		JsonArray routingInfo = new JsonArray();
		for (Map.Entry<String, TreeMap<Integer, Set<T>>> routesForDestination : platformPrefixToRoutingDestination.entrySet()) {
			JsonArray routesInfoForDestination = new JsonArray();
			String destination = routesForDestination.getKey();
			JsonObject object = new JsonObject();

			for (Map.Entry<Integer, Set<T>> routeInfo : routesForDestination.getValue().entrySet()) {
				Integer distance = routeInfo.getKey();
				JsonObject routeInfoJson = new JsonObject();

				// TODO: only send first value info ?
				for (T route : routeInfo.getValue()) {
					if (route instanceof String) {
						routeInfoJson.addProperty("route", (String) route);
					} else if (route instanceof InteroperableMessagingPylonProxy) {
						routeInfoJson.addProperty("route", ((InteroperableMessagingPylonProxy) route).getPlatformPrefix());
					}

					routeInfoJson.addProperty("distance", distance);
				}

				routesInfoForDestination.add(routeInfoJson);
			}

			object.addProperty("destination", destination);
			object.add("info", routesInfoForDestination);
			routingInfo.add(object);
		}

		JsonObject result = new JsonObject();
		result.add("routingInfo", routingInfo);
		return result;
	}

	public JsonObject getRoutingInfo(String nextHopEntityNameForExcludedRoute) {
		if (platformPrefixToRoutingDestination == null || platformPrefixToRoutingDestination.isEmpty())
			return null;

		JsonArray routingInfo = new JsonArray();
		for (Map.Entry<String, TreeMap<Integer, Set<T>>> routesForDestination : platformPrefixToRoutingDestination.entrySet()) {
			JsonArray routesInfoForDestination = new JsonArray();
			String destination = routesForDestination.getKey();
			JsonObject object = new JsonObject();

			boolean addInfo = false;
			for (Map.Entry<Integer, Set<T>> routeInfo : routesForDestination.getValue().entrySet()) {
				Integer distance = routeInfo.getKey();
				JsonObject routeInfoJson = new JsonObject();
				boolean addRoute = false;

				for (T route : routeInfo.getValue()) {
					String nextHop = "";

					if (route instanceof String) {
						nextHop = (String) route;
					} else if (route instanceof InteroperableMessagingPylonProxy) {
						nextHop = ((InteroperableMessagingPylonProxy) route).getPlatformPrefix();
					}

					if (!nextHop.equals(nextHopEntityNameForExcludedRoute)) {
						routeInfoJson.addProperty("route", nextHop);
						routeInfoJson.addProperty("distance", distance);
						if (!addRoute)
							addRoute = true;
					}
				}

				if (addRoute) {
					routesInfoForDestination.add(routeInfoJson);
					if (!addInfo)
						addInfo = true;
				}
			}

			if (addInfo) {
				object.addProperty("destination", destination);
				object.add("info", routesInfoForDestination);
				routingInfo.add(object);
			}
		}

		JsonObject result = new JsonObject();
		result.add("routingInfo", routingInfo);
		return result;
	}

	public boolean updateRoutingInformation(JsonObject json, T source) {
		if (json == null)
			return false;

		boolean infoUpdated = false;
		JsonElement jsonArray = json.get("routingInfo");
		for (JsonElement routesForDestination : jsonArray.getAsJsonArray()) {
			JsonObject object = routesForDestination.getAsJsonObject();
			String destination = object.get("destination").getAsString();
			JsonArray routes = object.getAsJsonArray("info");
			JsonObject shortestRoute = routes.get(0).getAsJsonObject();
			if (shortestRoute.keySet().isEmpty())
				continue;

			// distance reported by the source, plus 1 hop to reach the source itself
			int distance = shortestRoute.get("distance").getAsInt() + 1;

			// skip if source is already registered at a shorter or equal distance for this destination
			if (platformPrefixToRoutingDestination.get(destination) != null) {
				boolean alreadyKnownShorter = false;
				for (Map.Entry<Integer, Set<T>> entry : platformPrefixToRoutingDestination.get(destination).entrySet()) {
					if (entry.getValue().contains(source) && entry.getKey() <= distance) {
						alreadyKnownShorter = true;
						break;
					}
				}
				if (alreadyKnownShorter)
					continue;
			}

			// only flag as updated if this source+distance combination is actually new
			if (platformPrefixToRoutingDestination.get(destination) == null
					|| platformPrefixToRoutingDestination.get(destination).get(distance) == null
					|| !platformPrefixToRoutingDestination.get(destination).get(distance).contains(source)) {
				addRoutingDestinationForPlatform(destination, source, distance);
				infoUpdated = true;
			}
		}
		
		return infoUpdated;
	}
}
