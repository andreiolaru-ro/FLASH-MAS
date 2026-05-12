package net.xqhs.flash.abms.space.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GraphTopology implements Topology<GraphPosition> {

	public interface CellDisplayProvider {
		Character getDisplayChar(Set<EntityProxy<?>> entities);
	}

	public interface Heuristic {
		double estimate(GraphPosition from, GraphPosition to);
	}

	private static final Heuristic ZERO_HEURISTIC = new Heuristic() {
		@Override
		public double estimate(GraphPosition from, GraphPosition to) {
			return 0.0;
		}
	};

	private final Map<GraphPosition, Set<GraphPosition>> adjacency = new LinkedHashMap<>();
	private final Map<String, Double> edgeWeights = new HashMap<>();
	private CellDisplayProvider displayProvider;
	private Heuristic heuristic = ZERO_HEURISTIC;

	public GraphTopology() {
	}

	public GraphTopology(MultiTreeMap configuration) {
		if (configuration == null)
			return;
		String nodesStr = configuration.getAValue("nodes");
		String edgesStr = configuration.getAValue("edges");
		if (nodesStr != null)
			for (String nodeId : nodesStr.split(","))
				addNode(nodeId.trim());
		if (edgesStr != null)
			for (String edge : edgesStr.split(","))
				parseAndAddEdge(edge.trim());
	}

	public void addNode(String nodeId) {
		adjacency.putIfAbsent(new GraphPosition(nodeId), new HashSet<>());
	}

	public void addEdge(String fromId, String toId) {
		addEdge(fromId, toId, 1.0);
	}

	public void addEdge(String fromId, String toId, double weight) {
		GraphPosition from = new GraphPosition(fromId);
		GraphPosition to = new GraphPosition(toId);
		adjacency.computeIfAbsent(from, k -> new HashSet<>()).add(to);
		adjacency.computeIfAbsent(to, k -> new HashSet<>()).add(from);
		String edgeKey = edgeKey(fromId, toId);
		edgeWeights.put(edgeKey, weight);
	}

	public double getEdgeWeight(String fromId, String toId) {
		Double w = edgeWeights.get(edgeKey(fromId, toId));
		return w != null ? w : Double.POSITIVE_INFINITY;
	}

	public void setEdgeWeight(String fromId, String toId, double weight) {
		String key = edgeKey(fromId, toId);
		if (edgeWeights.containsKey(key))
			edgeWeights.put(key, weight);
	}

	@Override
	public Set<GraphPosition> getVicinity(GraphPosition pos) {
		Set<GraphPosition> neighbors = adjacency.get(pos);
		return neighbors != null ? Collections.unmodifiableSet(neighbors) : Collections.emptySet();
	}

	@Override
	public boolean isValidPosition(GraphPosition pos) {
		return pos != null && adjacency.containsKey(pos);
	}

	@Override
	public int getDistance(GraphPosition a, GraphPosition b) {
		if (a.equals(b))
			return 0;
		if (!adjacency.containsKey(a) || !adjacency.containsKey(b))
			return Integer.MAX_VALUE;

		Queue<GraphPosition> queue = new LinkedList<>();
		Map<GraphPosition, Integer> visited = new HashMap<>();
		queue.add(a);
		visited.put(a, 0);

		while (!queue.isEmpty()) {
			GraphPosition current = queue.poll();
			int dist = visited.get(current);
			Set<GraphPosition> neighbors = adjacency.get(current);
			if (neighbors == null)
				continue;
			for (GraphPosition neighbor : neighbors) {
				if (visited.containsKey(neighbor))
					continue;
				int nextDist = dist + 1;
				if (neighbor.equals(b))
					return nextDist;
				visited.put(neighbor, nextDist);
				queue.add(neighbor);
			}
		}
		return Integer.MAX_VALUE;
	}

	public List<GraphPosition> getShortestPath(GraphPosition from, GraphPosition to) {
		if (from.equals(to))
			return Collections.singletonList(from);
		if (!adjacency.containsKey(from) || !adjacency.containsKey(to))
			return Collections.emptyList();

		Queue<GraphPosition> queue = new LinkedList<>();
		Map<GraphPosition, GraphPosition> parent = new HashMap<>();
		queue.add(from);
		parent.put(from, null);

		while (!queue.isEmpty()) {
			GraphPosition current = queue.poll();
			Set<GraphPosition> neighbors = adjacency.get(current);
			if (neighbors == null)
				continue;
			for (GraphPosition neighbor : neighbors) {
				if (parent.containsKey(neighbor))
					continue;
				parent.put(neighbor, current);
				if (neighbor.equals(to)) {
					List<GraphPosition> path = new ArrayList<>();
					for (GraphPosition n = to; n != null; n = parent.get(n))
						path.add(n);
					Collections.reverse(path);
					return path;
				}
				queue.add(neighbor);
			}
		}
		return Collections.emptyList();
	}

	public void setHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic != null ? heuristic : ZERO_HEURISTIC;
	}

	public List<GraphPosition> findPathAStar(GraphPosition from, GraphPosition to) {
		if (from.equals(to))
			return Collections.singletonList(from);
		if (!adjacency.containsKey(from) || !adjacency.containsKey(to))
			return Collections.emptyList();

		final GraphPosition source = from;
		final GraphPosition target = to;
		final Map<GraphPosition, Double> gScore = new HashMap<>();
		final Map<GraphPosition, GraphPosition> cameFrom = new HashMap<>();
		final Set<GraphPosition> closedSet = new HashSet<>();

		gScore.put(source, 0.0);
		PriorityQueue<GraphPosition> openSet = new PriorityQueue<GraphPosition>(16,
				new Comparator<GraphPosition>() {
					@Override
					public int compare(GraphPosition a, GraphPosition b) {
						double fa = gScore.getOrDefault(a, Double.MAX_VALUE) + heuristic.estimate(a, target);
						double fb = gScore.getOrDefault(b, Double.MAX_VALUE) + heuristic.estimate(b, target);
						return Double.compare(fa, fb);
					}
				});
		openSet.add(source);

		while (!openSet.isEmpty()) {
			GraphPosition current = openSet.poll();
			if (current.equals(target))
				return reconstructPath(cameFrom, target);
			if (closedSet.contains(current))
				continue;
			closedSet.add(current);

			Set<GraphPosition> neighbors = adjacency.get(current);
			if (neighbors == null)
				continue;
			double currentG = gScore.getOrDefault(current, Double.MAX_VALUE);
			for (GraphPosition neighbor : neighbors) {
				if (closedSet.contains(neighbor))
					continue;
				double edgeWeight = getEdgeWeight(current.getNodeId(), neighbor.getNodeId());
				double tentativeG = currentG + edgeWeight;
				if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
					cameFrom.put(neighbor, current);
					gScore.put(neighbor, tentativeG);
					openSet.add(neighbor);
				}
			}
		}
		return Collections.emptyList();
	}

	public double getPathCost(List<GraphPosition> path) {
		if (path == null || path.size() < 2)
			return 0.0;
		double cost = 0.0;
		for (int i = 0; i < path.size() - 1; i++)
			cost += getEdgeWeight(path.get(i).getNodeId(), path.get(i + 1).getNodeId());
		return cost;
	}

	public int getDiameter() {
		int maxDist = 0;
		List<GraphPosition> nodes = new ArrayList<>(adjacency.keySet());
		for (int i = 0; i < nodes.size(); i++)
			for (int j = i + 1; j < nodes.size(); j++) {
				int dist = getDistance(nodes.get(i), nodes.get(j));
				if (dist != Integer.MAX_VALUE && dist > maxDist)
					maxDist = dist;
			}
		return maxDist;
	}

	private static List<GraphPosition> reconstructPath(Map<GraphPosition, GraphPosition> cameFrom,
			GraphPosition current) {
		List<GraphPosition> path = new ArrayList<>();
		for (GraphPosition n = current; n != null; n = cameFrom.get(n))
			path.add(n);
		Collections.reverse(path);
		return path;
	}

	public Set<GraphPosition> getAllNodes() {
		return Collections.unmodifiableSet(adjacency.keySet());
	}

	public int getNodeCount() {
		return adjacency.size();
	}

	public void setDisplayProvider(CellDisplayProvider provider) {
		this.displayProvider = provider;
	}

	@Override
	public String visualize(Map<GraphPosition, Set<EntityProxy<?>>> entityInPosition) {
		StringBuilder sb = new StringBuilder();
		sb.append("\nGraph Topology:\n");

		for (GraphPosition node : adjacency.keySet()) {
			Set<EntityProxy<?>> entities = entityInPosition.get(node);
			String entityLabel;
			if (entities == null || entities.isEmpty()) {
				entityLabel = ".";
			} else if (displayProvider != null) {
				Character ch = displayProvider.getDisplayChar(entities);
				entityLabel = ch != null ? String.valueOf(ch) : ".";
			} else {
				EntityProxy<?> first = entities.iterator().next();
				String name = first.getEntityName();
				entityLabel = (name != null && !name.isEmpty())
						? String.valueOf(Character.toUpperCase(name.charAt(0)))
						: "?";
			}

			Set<GraphPosition> neighbors = adjacency.get(node);
			StringBuilder neighborStr = new StringBuilder();
			for (GraphPosition neighbor : neighbors) {
				if (neighborStr.length() > 0)
					neighborStr.append(", ");
				neighborStr.append(neighbor.getNodeId());
			}
			sb.append("  ").append(node.getNodeId()).append(" [").append(entityLabel).append("] -> {")
					.append(neighborStr).append("}\n");
		}

		return sb.toString();
	}

	private void parseAndAddEdge(String edgeSpec) {
		int dashIndex = edgeSpec.indexOf('-');
		if (dashIndex < 0)
			return;
		String fromId = edgeSpec.substring(0, dashIndex).trim();
		String rest = edgeSpec.substring(dashIndex + 1).trim();

		double weight = 1.0;
		int colonIndex = rest.indexOf(':');
		String toId;
		if (colonIndex >= 0) {
			toId = rest.substring(0, colonIndex).trim();
			try {
				weight = Double.parseDouble(rest.substring(colonIndex + 1).trim());
			} catch (NumberFormatException ignored) {
			}
		} else {
			toId = rest;
		}
		addEdge(fromId, toId, weight);
	}

	private static String edgeKey(String a, String b) {
		return a.compareTo(b) <= 0 ? a + "-" + b : b + "-" + a;
	}
}
