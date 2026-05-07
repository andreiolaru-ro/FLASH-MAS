package test.guiGeneration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.remoteOperation.PropertyContainer;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

/**
 * Test shard demonstrating both RemoteOperationShard output paths.
 *
 * <p><b>Two concerns are deliberately separated:</b>
 * <ul>
 *   <li>The <b>simulation</b> runs on its own 1-second internal timer,
 *       updating x, y, energy, state and color continuously — independently of sends.
 *   <li>The <b>send rate</b> is controlled entirely by RemoteOperationShard's
 *       {@code update-frequency} parameter. {@link #getProperties} is a pure
 *       reader — it never advances state itself.
 * </ul>
 *
 * <p><b>Two send paths via RemoteOperationShard:</b>
 * <ul>
 *   <li>{@link RemoteOperationShard#registerOutputProperties} — x, y, energy, state, color
 *       are sent periodically to {@code RECEIVE_METRIC}.
 *   <li>{@link RemoteOperationShard#sendOutput} — neighbours (once on start) are pushed
 *       through {@code ENTITY_GUI_OUTPUT}.
 * </ul>
 */
public class ComprehensiveTestShard extends AgentShardGeneral implements PropertyContainer {

	private static final long serialVersionUID = 1L;
	private final Random random = new Random();

	// ── simulated state (written by simulation timer, read by getProperties) ──
	private volatile double x      = random.nextDouble() * 100;
	private volatile double y      = random.nextDouble() * 100;
	private volatile int    energy = 50 + random.nextInt(50);
	private volatile String state  = "healthy";
	private volatile String color  = stateToColor("healthy");
	private JsonArray neighbours;

	/** Drives state updates every second — decoupled from the send rate. */
	private Timer simulationTimer = null;
	/** How often the simulation advances, in ms. Independent of update-frequency. */
	private static final long SIMULATION_STEP_MS = 1000;

	/** Reference to RemoteOperationShard — set on AGENT_START. */
	private RemoteOperationShard remoteOp = null;

	// ── property name constants ───────────────────────────────────────────────
	/** Sent via registerOutputProperties → RECEIVE_METRIC (periodic snapshots). */
	public static final String PROP_X      = "x";
	public static final String PROP_Y      = "y";
	public static final String PROP_ENERGY = "energy";
	public static final String PROP_STATE  = "state";
	public static final String PROP_COLOR  = "color";

	/** Sent via sendOutput → ENTITY_GUI_OUTPUT (immediate / event-driven). */
	public static final String PORT_NEIGHBOURS = "neighbours";

	public ComprehensiveTestShard() {
		super(AgentShardDesignation.autoDesignation("ComprehensiveTest"));
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_START:
				remoteOp = (RemoteOperationShard)
						getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				if(remoteOp == null) {
					le("RemoteOperationShard not found.");
					return;
				}
				buildNeighbours();
				startSimulation();

				remoteOp.registerOutputProperties(
						this,
						Set.of(PROP_X, PROP_Y, PROP_ENERGY, PROP_STATE, PROP_COLOR));

				sendNeighbours();
				break;

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

	// ── Simulation timer (advances state, does NOT send) ─────────────────────

	/**
	 * Starts a 1-second timer that advances x/y/energy/state/color.
	 * Runs independently of {@code update-frequency} — simulation always steps
	 * every second; only the sends are rate-limited.
	 */
	private void startSimulation() {
		simulationTimer = new Timer(true);
		simulationTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				x += (random.nextDouble() - 0.5) * 8;
				y += (random.nextDouble() - 0.5) * 8;
				x = Math.max(0, Math.min(100, x));
				y = Math.max(0, Math.min(100, y));

				energy += random.nextInt(11) - 5;
				energy = Math.max(0, Math.min(100, energy));

				String newState;
				if      (energy < 20) newState = "critical";
				else if (energy < 60) newState = "warning";
				else                  newState = "healthy";

				if(!newState.equals(state)) {
					state = newState;
					color = stateToColor(state);
				}
			}
		}, 0, SIMULATION_STEP_MS);
	}

	// ── PropertyContainer (PATH 1) ────────────────────────────────────────────

	/**
	 * Called by {@link RemoteOperationShard} every {@code update-frequency} ms.
	 * Returns the current state snapshot — does not advance the simulation.
	 */
	@Override
	public Map<String, String> getProperties(Set<String> properties) {
		Map<String, String> result = new HashMap<>();
		result.put(PROP_X,      String.valueOf(x));
		result.put(PROP_Y,      String.valueOf(y));
		result.put(PROP_ENERGY, String.valueOf(energy));
		result.put(PROP_STATE,  state);
		result.put(PROP_COLOR,  color);
		return result;
	}

	// ── sendOutput helpers (PATH 2) ───────────────────────────────────────────

	/**
	 * Sends the neighbours list once via {@link RemoteOperationShard#sendOutput}.
	 * Neighbours are fixed at agent start so this is only called once.
	 */
	private void sendNeighbours() {
		if(remoteOp == null) return;
		AgentWave wave = new AgentWave();
		wave.resetDestination(PORT_NEIGHBOURS);
		for(int i = 0; i < neighbours.size(); i++)
			wave.add("neighbour", neighbours.get(i).getAsString());
		remoteOp.sendOutput(wave);
		li("Sent neighbours via sendOutput: []", neighbours);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private void buildNeighbours() {
		neighbours = new JsonArray();
		String myName = getAgent().getEntityName();
		try {
			int myId     = Integer.parseInt(myName.replace("Agent", ""));
			int maxNodes = 20;
			int nextId   = (myId % maxNodes) + 1;
			neighbours.add("Agent" + nextId);
			if(random.nextDouble() > 0.6) {
				int rnd = random.nextInt(maxNodes) + 1;
				if(rnd != myId && rnd != nextId)
					neighbours.add("Agent" + rnd);
			}
		} catch(Exception e) {
			neighbours.add("Agent1");
		}
	}

	private static String stateToColor(String s) {
		switch(s) {
			case "critical": return "#e74c3c";
			case "warning":  return "#f39c12";
			default:         return "#2ecc71";
		}
	}
}