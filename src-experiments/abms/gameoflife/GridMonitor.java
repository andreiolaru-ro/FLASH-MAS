package abms.gameoflife;

import java.util.HashMap;
import java.util.Map;

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
 * Monitors the Conway's Game of Life grid and renders a visual ASCII matrix at each step.
 */
public class GridMonitor extends AgentPingPongPlain implements SteppableEntity, ShardContainer {

	private static final long serialVersionUID = 1L;

	protected EnvironmentLinkShard envLink = new EnvironmentLinkShard();

	private int rows = 5;
	private int cols = 5;

	// Stores the latest live state of each cell by name
	private final Map<String, Boolean> cellStates = new HashMap<>();

	public GridMonitor(MultiTreeMap configuration) {
		super();
		super.configure(configuration);
		envLink.addContext(this);

		if (configuration.isSet("rows")) {
			this.rows = Integer.parseInt(configuration.getFirstValue("rows"));
		}
		if (configuration.isSet("cols")) {
			this.cols = Integer.parseInt(configuration.getFirstValue("cols"));
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
	 * Executed at each step to render the current 2D grid matrix in the console.
	 */
	@Override
	public void step() {
		if (!isRunning()) return;

		System.out.println("\n=================================");
		System.out.println("          GRID GENERATION        ");
		System.out.println("=================================");

		for (int r = 0; r < rows; r++) {
			StringBuilder sb = new StringBuilder();
			for (int c = 0; c < cols; c++) {
				String cellName = "cell_" + r + "_" + c;
				boolean isAlive = cellStates.getOrDefault(cellName, false);
				sb.append(isAlive ? " [O] " : " [ ] ");
			}
			System.out.println(sb.toString());
		}
		System.out.println("=================================\n");
	}

	@Override
	protected boolean processEvent(AgentWave wave) {
		try {
			JsonObject msg = JsonParser.parseString(wave.getContent()).getAsJsonObject();
			if (msg.has("sender") && msg.has("isAlive")) {
				String sender = msg.get("sender").getAsString();
				boolean isAlive = msg.get("isAlive").getAsBoolean();
				cellStates.put(sender, isAlive);
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