package net.xqhs.flash.core.interoperability;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonObject;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.webSocket.WebSocketPylon;

public class InteroperabilityRouter<T> {
	protected Map<String, T>	platformPrefixToRoutingDestination;

	public void addRoutingDestinationForPlatform(String platformPrefix, T entityName) {
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
		return address.split(InteroperableMessagingPylonProxy.PLATFORM_PREFIX_SEPARATOR)[0];
	}

	public Set<String> getAllPlatformPrefixes() {
		if (platformPrefixToRoutingDestination == null)
			return null;

		return platformPrefixToRoutingDestination.keySet();
	}

	public Collection<T> getAllDestinations() {
		if (platformPrefixToRoutingDestination == null)
			return null;

		return platformPrefixToRoutingDestination.values();
	}

	public boolean removeBridge(String entityName) {
		if (platformPrefixToRoutingDestination == null || !platformPrefixToRoutingDestination.containsValue(entityName))
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

	public boolean removeRoutingDestinationForPlatform(String platformPrefix, T pylonProxy) {
		if (platformPrefixToRoutingDestination == null)
			return true;

		return platformPrefixToRoutingDestination.remove(platformPrefix, pylonProxy);
	}

	public static JsonObject prependDestinationToMessage(JsonObject message, String bridgeDestination) {
		String nodeName = null;
		if (message.has(WebSocketPylon.MESSAGE_NODE_KEY))
			nodeName = message.get(WebSocketPylon.MESSAGE_NODE_KEY).getAsString();

		message.remove(WebSocketPylon.MESSAGE_NODE_KEY);
		AgentWave messageAsWave = AgentWaveJson.toAgentWave(message);
		messageAsWave.prependDestination(bridgeDestination);

		if (nodeName != null)
			AgentWaveJson.toJson(messageAsWave).addProperty(WebSocketPylon.MESSAGE_NODE_KEY, nodeName);

		return AgentWaveJson.toJson(messageAsWave);
	}
}
