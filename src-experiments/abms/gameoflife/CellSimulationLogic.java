package abms.gameoflife;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.remoteOperation.PropertyContainer;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

public class CellSimulationLogic extends AgentShardGeneral implements PropertyContainer {


	public static final Map<String, Boolean> gridState = new ConcurrentHashMap<>();
	private static final Map<String, CellSimulationLogic> cells = new ConcurrentHashMap<>();
	private static final Object simulationLock = new Object();

	private int row, col;
	private volatile boolean isAlive = false;
	private static Timer simulationTimer;
	private static final long SIMULATION_STEP_MS = Long.getLong("gol.stepMs", 300L);
	private static final Set<String> INITIAL_LIVE_CELLS = createGosperGliderGun();

	public CellSimulationLogic() {
		super(AgentShardDesignation.autoDesignation("CellSimulationLogic"));
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_START:
				String name = getAgent().getEntityName();
				String[] parts = name.split("_");
				this.row = Integer.parseInt(parts[1]);
				this.col = Integer.parseInt(parts[2]);

				this.isAlive = INITIAL_LIVE_CELLS.contains(row + ":" + col);

				gridState.put(name, isAlive);
				cells.put(name, this);

				// Use one timer for the whole board so every cell sees the same generation.
				startSimulation();

				RemoteOperationShard remoteOp = (RemoteOperationShard) getAgent().getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				if (remoteOp != null) {
					Set<String> properties = new HashSet<>();
					properties.add("x");
					properties.add("y");
					properties.add("isAlive");
					properties.add("statusVal");
					properties.add("state");
					properties.add("color");
					remoteOp.registerOutputProperties(this, properties);
				}

				break;

			case AGENT_STOP:
				String stoppedName = getAgent().getEntityName();
				synchronized (simulationLock) {
					cells.remove(stoppedName);
					gridState.remove(stoppedName);
					if (cells.isEmpty() && simulationTimer != null) {
						simulationTimer.cancel();
						simulationTimer = null;
					}
				}
				break;

			default:
				break;
		}
	}

	private static Set<String> createGosperGliderGun() {
		int[][] coordinates = {
				{5, 1}, {5, 2}, {6, 1}, {6, 2},
				{3, 13}, {3, 14}, {4, 12}, {4, 16}, {5, 11}, {5, 17},
				{6, 11}, {6, 15}, {6, 17}, {6, 18}, {7, 11}, {7, 17},
				{8, 12}, {8, 16}, {9, 13}, {9, 14},
				{1, 25}, {2, 23}, {2, 25}, {3, 21}, {3, 22}, {4, 21},
				{4, 22}, {5, 21}, {5, 22}, {6, 23}, {6, 25}, {7, 25},
				{3, 35}, {3, 36}, {4, 35}, {4, 36}
		};
		Set<String> liveCells = new HashSet<>();
		for (int[] coordinate : coordinates) {
			liveCells.add(coordinate[0] + ":" + coordinate[1]);
		}
		return liveCells;
	}

	private void startSimulation() {
		synchronized (simulationLock) {
			if (simulationTimer != null) return;

			simulationTimer = new Timer(true);
			simulationTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					advanceGeneration();
				}
		}, 300, SIMULATION_STEP_MS);
		}
	}

	private static void advanceGeneration() {
		synchronized (simulationLock) {
			if (cells.isEmpty()) return;

			Map<String, Boolean> snapshot = new HashMap<>(gridState);
			Map<String, Boolean> nextStates = new HashMap<>();

			for (Map.Entry<String, CellSimulationLogic> entry : cells.entrySet()) {
				CellSimulationLogic cell = entry.getValue();
				int aliveNeighbors = 0;

				for (int dr = -1; dr <= 1; dr++) {
					for (int dc = -1; dc <= 1; dc++) {
						if (dr == 0 && dc == 0) continue;

						String neighborName = "cell_" + (cell.row + dr) + "_" + (cell.col + dc);
						if (snapshot.getOrDefault(neighborName, false)) {
							aliveNeighbors++;
						}
					}
				}

				boolean nextState = cell.isAlive
						? aliveNeighbors == 2 || aliveNeighbors == 3
						: aliveNeighbors == 3;
				nextStates.put(entry.getKey(), nextState);
			}

			for (Map.Entry<String, Boolean> entry : nextStates.entrySet()) {
				CellSimulationLogic cell = cells.get(entry.getKey());
				if (cell != null) {
					cell.isAlive = entry.getValue();
					gridState.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	@Override
	public Map<String, String> getProperties(Set<String> properties) {
		Map<String, String> metrics = new HashMap<>();
		metrics.put("x", String.valueOf(col));
		metrics.put("y", String.valueOf(row));
		metrics.put("isAlive", String.valueOf(isAlive));
		metrics.put("statusVal", isAlive ? "1" : "0");
		metrics.put("state", isAlive ? "alive" : "dead");
		metrics.put("color", isAlive ? "#4CAF50" : "#E0E0E0");
		return metrics;
	}
}