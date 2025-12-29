package andrei.abms;

import java.util.Set;

import andrei.abms.gridworld.GridTopology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
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
	protected GridTopology map;
	
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
			if(c instanceof GridTopology)
				map = (GridTopology) c;
		return true;
	}
	
	@Override
	public void preStep() {
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
	public void step() {
		li("state [] -> [] ", state, nextState);
		state = nextState;
	}
}
