package andrei.abms;

import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;

import java.util.*;

public class Simulation<P> extends EntityCore<Node> implements EntityProxy<Simulation<P>> {
	private final Topology<P> topology;
	private final Map<P, StepAgent> agentPositions; //maybe use a BiMap instead of keeping 2 maps
	private final Map<StepAgent, P> positionsByAgent;
	private final boolean allowMultipleAgentsPerCell;

	public Simulation(Topology<P> topology) {
		this(topology, false);
	}

	public Simulation(Topology<P> topology, boolean allowMultipleAgentsPerCell) {
		this.topology = topology;
		this.allowMultipleAgentsPerCell = allowMultipleAgentsPerCell;
		this.agentPositions = new HashMap<>();
		this.positionsByAgent = new HashMap<>();
	}

	public boolean placeAgent(StepAgent agent, P position) {
		if (!topology.isValidPosition(position)) {
			return false;
		}

		if (!allowMultipleAgentsPerCell && agentPositions.containsKey(position)) {
			return false;
		}

		P oldPosition = positionsByAgent.get(agent);
		if (oldPosition != null) {
			agentPositions.remove(oldPosition);
		}

		agentPositions.put(position, agent);
		positionsByAgent.put(agent, position);
		return true;
	}

	public boolean moveAgent(StepAgent agent, P newPosition) {
		P currentPosition = positionsByAgent.get(agent);
		if (currentPosition == null) {
			return false;
		}

		if (!topology.isValidPosition(newPosition)) {
			return false;
		}

		if (!topology.canMoveInOneStep(currentPosition, newPosition)) {
			return false;
		}

		if (!allowMultipleAgentsPerCell && agentPositions.containsKey(newPosition)) {
			return false;
		}

		agentPositions.remove(currentPosition);
		agentPositions.put(newPosition, agent);
		positionsByAgent.put(agent, newPosition);
		return true;
	}

	public P getAgentPosition(StepAgent agent) {
		return positionsByAgent.get(agent);
	}

	public StepAgent getAgentAt(P position) {
		return agentPositions.get(position);
	}

	public Set<P> getFreeNeighbors(P position) {
		Set<P> neighbors = topology.getVicinity(position);
		Set<P> free = new HashSet<>();
		for (P neighbor : neighbors) {
			if (topology.isValidPosition(neighbor) && !agentPositions.containsKey(neighbor)) {
				free.add(neighbor);
			}
		}
		return free;
	}


	public List<StepAgent> getAllAgents() {
		return new ArrayList<>(positionsByAgent.keySet());
	}

	public Topology<P> getTopology() {
		return topology;
	}

	@Override
	public String getEntityName() {
		return getName();
	}

	@Override
	public EntityProxy<Simulation<P>> asContext() {
		return this;
	}
}
