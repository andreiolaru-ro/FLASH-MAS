package test.guiGeneration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.remoteOperation.PropertyContainer;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

public class GridSectorSimulationLogic extends AgentShardGeneral implements PropertyContainer {
	private int sectorX, sectorY;
	private static final double SECTOR_SIZE = 500.0 / 15.0; // 33.3333

	public GridSectorSimulationLogic() {
		super(AgentShardDesignation.autoDesignation("GridSectorSimulationLogic"));
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_START:
				String[] parts = getAgent().getEntityName().split("_");
				this.sectorX = Integer.parseInt(parts[1]);
				this.sectorY = Integer.parseInt(parts[2]);

				RemoteOperationShard remoteOp = (RemoteOperationShard) getAgent().getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				if (remoteOp != null) {
					Set<String> properties = new HashSet<>();
					properties.add("sectorX");
					properties.add("sectorY");
					properties.add("congestion");
					remoteOp.registerOutputProperties(this, properties);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public Map<String, String> getProperties(Set<String> properties) {
		int droneCount = 0;

		// Loop over the static registry tracking map directly
		for (double[] pos : DroneSimulationLogic.dronePositions.values()) {
			int droneSectorX = (int) (pos[0] / SECTOR_SIZE);
			int droneSectorY = (int) (pos[1] / SECTOR_SIZE);

			if (droneSectorX == this.sectorX && droneSectorY == this.sectorY) {
				droneCount++;
			}
		}

		Map<String, String> sectorMetrics = new HashMap<>();
		sectorMetrics.put("sectorX", String.valueOf(sectorX));
		sectorMetrics.put("sectorY", String.valueOf(sectorY));
		sectorMetrics.put("congestion", String.valueOf(droneCount * 10));
		return sectorMetrics;
	}
}