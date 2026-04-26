package test.guiGeneration;

import net.xqhs.flash.FlashBoot;

/**
 * Combined boot for all visualization types:
 * <ul>
 *   <li><b>Agent1..Agent50</b> – use {@link ComprehensiveTestShard}; feed Scatter, Line, Bar and Graph charts.
 *   <li><b>Cell1..Cell50</b>  – use {@link HeatmapTestShard}; feed the Heatmap chart with fixed grid positions.
 * </ul>
 *
 * <p>UI setup:
 * <ul>
 *   <li>Scatter – X: {@code x}, Y: {@code y}, color by {@code state}
 *   <li>Heatmap – X: {@code x}, Y: {@code y}, Z: {@code energy}  (subscribe to Cell* agents)
 *   <li>Line / Bar – any numeric property from Agent* agents
 *   <li>Graph – {@code neighbours} property from Agent* agents
 * </ul>
 */
public class BootAllVisualizationsTest {

	/** How often Agent* agents send metric updates to the UI (ms). */
	private static final int AGENT_UPDATE_FREQUENCY_MS = 5000;
	/** How often Cell* agents send heatmap updates to the UI (ms). */
	private static final int CELL_UPDATE_FREQUENCY_MS  = 2000;

	private static final int AGENT_COUNT = 50;
	private static final int CELL_COUNT  = 50;

	public static void main(String[] args) {
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";
		test_args += " -node main central:web";
		test_args += " -pylon webSocket:serverPylon serverPort:8886";

		for(int i = 1; i <= AGENT_COUNT; i++) {
			test_args += " -agent composite:Agent" + i
					+ " -shard messaging"
					+ " -shard remoteOperation update-frequency:" + AGENT_UPDATE_FREQUENCY_MS
					+ " -shard ComprehensiveTest";
		}


		for(int i = 1; i <= CELL_COUNT; i++) {
			test_args += " -agent composite:Cell" + i
					+ " -shard messaging"
					+ " -shard remoteOperation update-frequency:" + CELL_UPDATE_FREQUENCY_MS
					+ " -shard HeatmapTest";
		}

		FlashBoot.main(test_args.split(" "));
	}
}