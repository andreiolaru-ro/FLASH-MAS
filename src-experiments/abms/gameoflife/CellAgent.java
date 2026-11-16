package abms.gameoflife;

import java.util.HashSet;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import testing.AgentPingPongPlain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Represents a single cell/agent in the Conway's Game of Life simulation,
 * built on top of AgentPingPongPlain, SteppableEntity, and ShardContainer.
 */
public class CellAgent extends AgentPingPongPlain implements SteppableEntity, ShardContainer {

	private static final long serialVersionUID = 1L;

	protected EnvironmentLinkShard envLink = new EnvironmentLinkShard();

	// Cell state: true = alive, false = dead
	private boolean isAlive = false;
	private boolean nextState = false;

	// Set containing the names of neighboring cells and monitors
	private Set<String> neighbors = new HashSet<>();
	private Set<String> monitors = new HashSet<>();

	// Counter for alive neighbors collected during the current generation
	private int aliveNeighborsCount = 0;

	// Step phase tracker: even = broadcast state, odd = compute next state
	private int stepPhase = 0;

	// Constructor with configuration
	public CellAgent(MultiTreeMap configuration) {
		super();
		super.configure(configuration);
		envLink.addContext(this);

		if (configuration.isSet("initialState")) {
			this.isAlive = Boolean.parseBoolean(configuration.getFirstValue("initialState"));
			this.nextState = this.isAlive;
		}
		if (configuration.isSet("neighbor")) {
			this.neighbors.addAll(configuration.getValues("neighbor"));
		}
		if (configuration.isSet("monitor")) {
			this.monitors.addAll(configuration.getValues("monitor"));
		}
	}

	@Override
	public boolean start() {
		if (!super.start())
			return false;

		// Send initial state to the monitor so the first step renders correctly
		reportStateToMonitors();
		return true;
	}

	private void reportStateToMonitors() {
		for (String monitorName : monitors) {
			JsonObject msg = new JsonObject();
			msg.addProperty("sender", getName());
			msg.addProperty("isAlive", isAlive);

			AgentWave wave = new AgentWave(msg.toString(), monitorName, "gol-endpoint")
					.addSourceElementFirst(getName());
			send(wave);
		}
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		envLink.addGeneralContext(context);

		if (context instanceof Simulation) {
			((Simulation) context).registerEntity(CategoryName.AGENT.s(), this, this.getName());
		}
		return super.addGeneralContext(context);
	}

	/**
	 * Executed at each step in StepWise mode.
	 * Even steps broadcast the current state; odd steps evaluate Conway's rules.
	 */
	@Override
	public void step() {
		if (!isRunning()) return;

		if (stepPhase % 2 == 0) {
			// Phase 1: Broadcast current state to all neighbors and monitors
			for (String neighborName : neighbors) {
				JsonObject msg = new JsonObject();
				msg.addProperty("sender", getName());
				msg.addProperty("isAlive", isAlive);

				AgentWave wave = new AgentWave(msg.toString(), neighborName, "gol-endpoint")
						.addSourceElementFirst(getName());

				if (!send(wave)) {
					le("Failed to send state message to neighbor: " + neighborName);
				}
			}
			reportStateToMonitors();
		} else {
			// Phase 2: Apply Conway's Game of Life rules based on collected alive neighbors count
			if (isAlive) {
				nextState = (aliveNeighborsCount == 2 || aliveNeighborsCount == 3);
			} else {
				nextState = (aliveNeighborsCount == 3);
			}

			isAlive = nextState;
			aliveNeighborsCount = 0; // Reset counter for the next generation

			reportStateToMonitors();
		}

		stepPhase++;
	}

	/**
	 * Overrides processEvent to handle incoming state messages from neighbors.
	 */
	@Override
	protected boolean processEvent(AgentWave wave) {
		try {
			JsonObject msg = JsonParser.parseString(wave.getContent()).getAsJsonObject();
			if (msg.has("isAlive") && msg.get("isAlive").getAsBoolean()) {
				aliveNeighborsCount++;
			}
		} catch (Exception e) {
			super.processEvent(wave);
		}
		return true;
	}

	@Override
	public boolean postAgentEvent(AgentEvent event) {
		if (event instanceof AgentWave) {
			return processEvent((AgentWave) event);
		}
		return false;
	}

	@Override
	public AgentShard getAgentShard(AgentShardDesignation designation) {
		return null;
	}

	@Override
	public String getEntityName() {
		return getName();
	}
}