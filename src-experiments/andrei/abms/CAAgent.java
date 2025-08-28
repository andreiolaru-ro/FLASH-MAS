package andrei.abms;

import java.util.Set;

import andrei.abms.gridworld.GridMap;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class CAAgent extends StepAgent {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1453941340763410471L;
	protected static final String	STATE_PARAM			= "state";
	protected int					state				= 0;
	protected int					nextState;
	protected GridMap				map;
	
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
		for(EntityProxy<? extends Entity<?>> c : getFullContext())
			if(c instanceof GridMap)
				map = (GridMap) c;
		return true;
	}
	
	@Override
	void preStep() {
		Set<Agent> neighbors = map.getNeighbors(this);
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
	void step() {
		li("state [] -> [] ", state, nextState);
		state = nextState;
	}
}
