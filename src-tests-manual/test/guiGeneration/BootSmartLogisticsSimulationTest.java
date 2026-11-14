package test.guiGeneration;

import net.xqhs.flash.FlashBoot;

/**
 * Production Case-Study Boot: Smart City Drone Logistics & Urban Airspace Management.
 * Simulates real physical trajectories, regional demand heatmaps, state-machines,
 * and ad-hoc communication graphs to evaluate visualization engine throughput.
 */
public class BootSmartLogisticsSimulationTest {

	private static final int DRONE_COUNT = 250;
	private static final int GRID_SIZE_X = 15;
	private static final int GRID_SIZE_Y = 15;

	private static final int DRONE_TICK_RATE_MS = 1000;
	private static final int SECTOR_TICK_RATE_MS = 2000;

	public static void main(String[] args) {
		StringBuilder testArgs = new StringBuilder();

		testArgs.append(" -loader agent:composite");
		testArgs.append(" -package test.guiGeneration");
		testArgs.append(" -node main central:web");
		testArgs.append(" -pylon webSocket:serverPylon serverPort:8886");

		// 1. Instantiating Drone Fleet
		for (int i = 1; i <= DRONE_COUNT; i++) {
			testArgs.append(" -agent composite:Drone_").append(i)
					.append(" -shard messaging")
					.append(" -shard remoteOperation update-frequency:").append(DRONE_TICK_RATE_MS)
					.append(" -shard DroneSimulationLogic");
		}

		// 2. Instantiating Urban Airspace Grid Sectors
		for (int x = 0; x < GRID_SIZE_X; x++) {
			for (int y = 0; y < GRID_SIZE_Y; y++) {
				testArgs.append(" -agent composite:Sector_").append(x).append("_").append(y)
						.append(" -shard messaging")
						.append(" -shard remoteOperation update-frequency:").append(SECTOR_TICK_RATE_MS)
						.append(" -shard GridSectorSimulationLogic");
			}
		}

		FlashBoot.main(testArgs.toString().split(" "));
	}
}