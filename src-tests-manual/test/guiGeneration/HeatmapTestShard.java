package test.guiGeneration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.remoteOperation.PropertyContainer;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

/**
 * Shard for a proper 2D heatmap demonstration.
 * <p>
 * Each agent occupies a <b>fixed cell</b> on a grid (position does not change).
 * Only the cell's intensity value (energy) varies over time, producing a real
 * heatmap where colours shift without agents moving around.
 * <p>
 * Grid layout for N agents: ceiling(sqrt(N)) columns x as many rows as needed.
 * With 50 agents this gives a 8x7 grid (only last row is partial).
 * <p>
 * Sent via {@link RemoteOperationShard#registerOutputProperties}: x, y, energy.
 */
public class HeatmapTestShard extends AgentShardGeneral implements PropertyContainer {

	private static final long serialVersionUID = 1L;
	private final Random random = new Random();

	/** Fixed grid column (0-based). Set once on AGENT_START, never changes. */
	private int gridX;
	/** Fixed grid row (0-based). Set once on AGENT_START, never changes. */
	private int gridY;

	/** Current intensity value [0..100]. Changes every simulation step. */
	private volatile int energy = 50 + random.nextInt(50);

	/**
	 * Simulated "temperature" influence: each agent has a natural resting value
	 * it drifts toward, making neighbouring cells look correlated over time.
	 */
	private final int restingValue = 20 + random.nextInt(60);

	/** Simulation timer — advances energy independently of send rate. */
	private Timer simulationTimer = null;
	private static final long SIMULATION_STEP_MS = 500;

	/** Number of columns in the grid — must match BootHeatmapTest. */
	public static final int GRID_COLS = 8;

	public HeatmapTestShard() {
		super(AgentShardDesignation.autoDesignation("HeatmapTest"));
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_START: {
				// Compute fixed grid position from agent name (e.g. "Agent1" or "CompositeAgent1" → index 0)
				String myName = getAgent().getEntityName();
				int index = 0;
				try {
				    Matcher m = Pattern.compile("\\d+").matcher(myName);
				    if (m.find()) {
					    index = Integer.parseInt(m.group()) - 1;
					} else {
					    throw new NumberFormatException("No digits found");
					}
				} catch(NumberFormatException e) {
					le("Cannot parse agent index from name []; defaulting to 0.", myName);
				}
				gridX = index % GRID_COLS;
				gridY = index / GRID_COLS;
				li("Fixed grid position: ({}, {})", gridX, gridY);

				RemoteOperationShard remoteOp = (RemoteOperationShard)
						getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				if(remoteOp == null) {
					le("RemoteOperationShard not found.");
					return;
				}

				startSimulation();

				remoteOp.registerOutputProperties(
						this,
						Set.of("x", "y", "energy"));
				break;
			}
			case AGENT_STOP:
				if(simulationTimer != null) {
					simulationTimer.cancel();
					simulationTimer = null;
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Advances energy toward {@link #restingValue} with small random noise.
	 * This creates natural-looking "hot" and "cold" zones on the heatmap,
	 * since neighbours don't share the same resting value.
	 */
	private void startSimulation() {
		simulationTimer = new Timer(true);
		simulationTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// Drift toward resting value + noise
				int diff = restingValue - energy;
				energy += (int)(diff * 0.1) + (random.nextInt(7) - 3);
				energy = Math.max(0, Math.min(100, energy));
			}
		}, 0, SIMULATION_STEP_MS);
	}

	/**
	 * Returns the current cell values. x and y are fixed integers —
	 * the heatmap grid cell never moves.
	 */
	@Override
	public Map<String, String> getProperties(Set<String> properties) {
		Map<String, String> result = new HashMap<>();
		result.put("x",      String.valueOf(gridX));
		result.put("y",      String.valueOf(gridY));
		result.put("energy", String.valueOf(energy));
		return result;
	}
}