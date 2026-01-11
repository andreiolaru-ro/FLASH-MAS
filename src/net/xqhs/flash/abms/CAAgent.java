package net.xqhs.flash.abms;

import java.util.Set;

import net.xqhs.flash.abms.gridworld.GridPosition;
import net.xqhs.flash.abms.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class CAAgent extends BaseAgent implements StepAgent {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1453941340763410471L;
	protected static final String	STATE_PARAM			= "state";
	protected int					state				= 0;
	protected int					nextState;
	protected GridTopology topology;
	protected Simulation<GridPosition> simulation;

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
				simulation = (Simulation<GridPosition>) c;
		}
		return true;
	}
	
	@Override
	public void preStep() {
		GridPosition myPosition = simulation.getAgentPosition(this);
		if (myPosition == null) {
			nextState = 0;
			return;
		}

		Set<StepAgent> neighbors = topology.getNeighbors(myPosition, pos -> simulation.getAgentAt(pos));
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
}
