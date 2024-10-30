package net.xqhs.flash.remoteOperation;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.OperationUtils.MonitoringOperation;

/**
 * Receiver for updates from {@link RemoteOperationShard}.
 * <p>
 * An update is an {@link AgentWave} in which the first destinations are the update operation (e.g.
 * {@link MonitoringOperation#STATUS_UPDATE}), the entity for which there is an update, the port, and optionally the
 * role.
 */
public interface RemoteLocalReceiver extends PylonProxy {
	/**
	 * @param update
	 *            an {@link AgentWave} containing relevant update information (see {@link RemoteLocalReceiver}).
	 */
	void receiveUpdate(AgentWave update);
}
