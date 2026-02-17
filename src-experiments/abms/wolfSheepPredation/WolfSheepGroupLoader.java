package abms.wolfSheepPredation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.xqhs.flash.abms.EntityGroup.EntityGroupLoader;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class WolfSheepGroupLoader extends EntityGroupLoader {
	private static final int	DEFAULT_SHEEP	= 10;
	private static final int	DEFAULT_WOLVES	= 6;
	private static final String	SHEEP_COUNT		= "sheepCount";
	private static final String	WOLF_COUNT		= "wolfCount";
	
	@Override
	public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
		return true;
	}
	
	@Override
	public WolfSheepGroup load(MultiTreeMap multiTreeMap, List<EntityProxy<? extends Entity<?>>> context,
			List<MultiTreeMap> subordinateEntities) {
		if(context == null)
			return null;
		
		int sheepCount = readInt(multiTreeMap, SHEEP_COUNT, DEFAULT_SHEEP);
		int wolfCount = readInt(multiTreeMap, WOLF_COUNT, DEFAULT_WOLVES);
		
		@SuppressWarnings("unchecked")
		SpaceContext<GridPosition> space = (SpaceContext<GridPosition>) Loader.getClosestContext(context,
				SpaceContext.class);
		GridTopology topology = (GridTopology) space.getTopology();
		if(topology == null) {
			return null;
		}
		int totalCells = topology.getWidth() * topology.getHeight();
		if(sheepCount + wolfCount > totalCells) {
			return null;
		}
		
		List<GridPosition> positions = new ArrayList<>(totalCells);
		for(int y = 0; y < topology.getHeight(); y++) {
			for(int x = 0; x < topology.getWidth(); x++) {
				positions.add(new GridPosition(x, y));
			}
		}
		Collections.shuffle(positions, new Random());
		
		Simulation sim = (Simulation) Loader.getClosestContext(context, Simulation.class);
		
		List<Entity<?>> agents = new ArrayList<>();
		int idx = 0;
		for(int i = 0; i < sheepCount; i++) {
			SheepAgent sheep = new SheepAgent();
			String name = "sheep" + i;
			configureAgentName(sheep, name);
			for(EntityProxy<? extends Entity<?>> c : context)
				sheep.addGeneralContext(c);
			space.place(sheep.asContext(), positions.get(idx++));
			agents.add(sheep);
			sim.registerEntity("agent", sheep, name);
		}
		for(int i = 0; i < wolfCount; i++) {
			WolfAgent wolf = new WolfAgent();
			String name = "wolf" + i;
			configureAgentName(wolf, name);
			for(EntityProxy<? extends Entity<?>> c : context)
				wolf.addGeneralContext(c);
			space.place(wolf.asContext(), positions.get(idx++));
			agents.add(wolf);
			sim.registerEntity("agent", wolf, name);
		}
		
		WolfSheepGroup group = new WolfSheepGroup(agents);
		group.configure(multiTreeMap);
		return group;
	}
	
	@Override
	public WolfSheepGroup load(MultiTreeMap multiTreeMap) {
		return load(multiTreeMap, null, null);
	}
	
	private static int readInt(MultiTreeMap multiTreeMap, String key, int fallback) {
		if(multiTreeMap == null || !multiTreeMap.containsKey(key)) {
			return fallback;
		}
		try {
			return Integer.parseInt(multiTreeMap.getAValue(key));
		} catch(NumberFormatException e) {
			return fallback;
		}
	}
	
	private static void configureAgentName(BaseAgent agent, String name) {
		MultiTreeMap multiTreeMap = new MultiTreeMap();
		multiTreeMap.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
		agent.configure(multiTreeMap);
	}
}
