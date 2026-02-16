package abms.ca;

import java.util.Set;
import java.util.stream.Collectors;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class CAAgent extends BaseAgent implements SteppableEntity {

	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1453941340763410471L;
	public static final String	STATE_PARAM			= "state";
	protected int					state				= 0;
	protected int					nextState;
	protected GridTopology topology;
	protected Simulation simulation;
	protected SpaceContext<GridPosition> space;

	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		state = configuration.containsKey(STATE_PARAM) ? Integer.parseInt(configuration.getAValue(STATE_PARAM)) : 0;
		return true;
	}
	
	@Override
	public boolean start() {
		super.start();
		for(EntityProxy<? extends Entity<?>> c : getFullContext()) {
			if(c instanceof GridTopology)
				topology = (GridTopology) c;
			if(c instanceof Simulation)
				simulation = (Simulation) c;
			if(c instanceof SpaceContext)
				space = (SpaceContext<GridPosition>) c;
		}
		return true;
	}
	
	public void preStep() {
		GridPosition myPosition = space.getPosition(this.asContext());
		if (myPosition == null) {
			nextState = 0;
			return;
		}

		Set<GridPosition> vicinity = space.getVicinity(myPosition);
		Set<SteppableEntity> neighbors = vicinity.stream()
				.filter(topology::isValidPosition)
				.flatMap(pos -> space.getEntitiesAt(pos).stream())
				.filter(e -> e instanceof CAAgent)
				.map(e -> (SteppableEntity) e)
				.collect(Collectors.toSet());

		int liveNeighbors = neighbors.stream().mapToInt(a -> ((CAAgent) a).state).sum();
		switch(liveNeighbors) {
		case 2:
			nextState = state;
			break;
		case 3:
			nextState = 1;
			break;
		default:
			nextState = 0;
			break;
		}
	}
	
	@Override
	public void step() {
		li("state [] -> [] ", state, nextState);
		state = nextState;
	}

	public int getState() {
		return state;
	}
}
