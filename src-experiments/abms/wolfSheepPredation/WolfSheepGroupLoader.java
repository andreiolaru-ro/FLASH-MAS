package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.xqhs.flash.abms.SimulationExecutor;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.util.MultiTreeMap;

public class WolfSheepGroupLoader implements Loader<WolfSheepGroup> {
	private static final int DEFAULT_SHEEP = 10;
	private static final int DEFAULT_WOLVES = 6;
	private static final String SHEEP_COUNT = "numberOfSheep";
	private static final String WOLF_COUNT = "numberOfWolves";

	@Override
	public boolean configure(MultiTreeMap multiTreeMap, LoadPack loadPack) {
		return true;
	}

	@Override
	public boolean preload(MultiTreeMap multiTreeMap, List<EntityProxy<? extends Entity<?>>> context) {
		return preload(multiTreeMap);
	}

	@Override
	public boolean preload(MultiTreeMap multiTreeMap) {
		return true;
	}

	@Override
	public WolfSheepGroup load(MultiTreeMap multiTreeMap, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities) {
		List<EntityProxy<? extends Entity<?>>> safeContext = context != null ? context : new ArrayList<>();
		GridTopology topology = (GridTopology) Loader.getClosestContext(safeContext, GridTopology.class);
		SimulationExecutor executor = (SimulationExecutor) Loader.getClosestContext(safeContext, SimulationExecutor.class);
		if (topology == null || executor == null) {
			return null;
		}

		int sheepCount = readInt(multiTreeMap, SHEEP_COUNT, DEFAULT_SHEEP);
		int wolfCount = readInt(multiTreeMap, WOLF_COUNT, DEFAULT_WOLVES);
		int totalCells = topology.getWidth() * topology.getHeight();
		if (sheepCount + wolfCount > totalCells) {
			return null;
		}

		Simulation<GridPosition> simulation = new Simulation<>(topology);

		List<GridPosition> positions = new ArrayList<>(totalCells);
		for (int y = 0; y < topology.getHeight(); y++) {
			for (int x = 0; x < topology.getWidth(); x++) {
				positions.add(new GridPosition(x, y));
			}
		}
		Collections.shuffle(positions, new Random());

		List<Agent> agents = new ArrayList<>();
		int idx = 0;
		for (int i = 0; i < sheepCount; i++) {
			SheepAgent sheep = new SheepAgent();
			configureAgentName(sheep, "sheep" + i);
			sheep.addGeneralContext(simulation.asContext());
			sheep.addGeneralContext(topology.asContext());
			simulation.placeAgent(sheep, positions.get(idx++));
			executor.register(sheep);
			agents.add(sheep);
		}
		for (int i = 0; i < wolfCount; i++) {
			WolfAgent wolf = new WolfAgent();
			configureAgentName(wolf, "wolf" + i);
			wolf.addGeneralContext(simulation.asContext());
			wolf.addGeneralContext(topology.asContext());
			simulation.placeAgent(wolf, positions.get(idx++));
			executor.register(wolf);
			agents.add(wolf);
		}

		WolfSheepGroup group = new WolfSheepGroup(agents, simulation, topology.getWidth(), topology.getHeight());
		group.configure(multiTreeMap);
		executor.register(group);
		return group;
	}

	@Override
	public WolfSheepGroup load(MultiTreeMap multiTreeMap) {
		return load(multiTreeMap, null, null);
	}

	private static int readInt(MultiTreeMap multiTreeMap, String key, int fallback) {
		if (multiTreeMap == null || !multiTreeMap.containsKey(key)) {
			return fallback;
		}
		try {
			return Integer.parseInt(multiTreeMap.getAValue(key));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static void configureAgentName(BaseAgent agent, String name) {
		MultiTreeMap multiTreeMap = new MultiTreeMap();
		multiTreeMap.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
		agent.configure(multiTreeMap);
	}
}
