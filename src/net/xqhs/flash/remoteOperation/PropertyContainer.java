package net.xqhs.flash.remoteOperation;

import java.util.Map;
import java.util.Set;

/**
 * Callback interface for entities that wish to provide property values at each call.
 * <p>
 * Implement this interface and register via {@link RemoteOperationShard#registerOutputProperties} to have your
 * property values automatically included in every periodic or deferred update sent to the monitoring entity.
 */
public interface PropertyContainer {
	/**
	 * Called by {@link RemoteOperationShard#sendUpdate()} to collect the current values of the registered
	 * properties.
	 *
	 * @param properties
	 *            - the set of property names that this container was registered for.
	 * @return a map from property name to current string value. May return {@code null} if no data is available.
	 */
	Map<String, String> getProperties(Set<String> properties);
}
