package shadowProtocolDeployment;

import java.util.List;
import java.util.Map;

/**
 * Class used in parsing the Json file that contains the topology.
 */
class Topology {
	private Map<String, Map<String, List<String>>> topology;

	public enum GetterType {
		GET_PYLON_FOR_AGENT,
		GET_SERVER_FOR_AGENT,
		GET_SERVER_FOR_PYLON,
	}

	public Map<String, Map<String, List<String>>> getTopology() {
		return topology;
	}

	public void setTopology(Map<String, Map<String, List<String>>> topology) {
		this.topology = topology;
	}

	public String getter(GetterType type, String entity) {
		for (Map.Entry<String, Map<String, List<String>>> region : topology.entrySet()) {
			for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
				switch (type) {
					case GET_PYLON_FOR_AGENT:
						if ((pylon.getValue()).contains(entity)) return pylon.getKey();
						break;
					case GET_SERVER_FOR_AGENT:
						if ((pylon.getValue()).contains(entity)) return region.getKey();
						break;
					case GET_SERVER_FOR_PYLON:
						if ((pylon.getKey()).equals(entity)) return region.getKey();
						break;
				}
			}
		}
		return null;
	}

	public void topologyAfterMove(Action moveAction) {
		var getPylon = moveAction.getDestination().split("-");
		String nextPylon = getPylon[0];
		String agent = (moveAction.source.split("-"))[0];
		String prevPylon = getter(GetterType.GET_PYLON_FOR_AGENT, agent);
		topology.get(getter(GetterType.GET_SERVER_FOR_PYLON, nextPylon)).get(nextPylon).add(agent);
		topology.get(getter(GetterType.GET_SERVER_FOR_PYLON, prevPylon)).get(prevPylon).remove(agent);
		//System.out.println(topology);
	}
}
