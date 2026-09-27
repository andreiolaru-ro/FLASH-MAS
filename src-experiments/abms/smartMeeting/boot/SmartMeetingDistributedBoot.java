package abms.smartMeeting.boot;

import abms.common.JsonConfig;
import abms.smartMeeting.ScenarioTrace;
import net.xqhs.flash.FlashBoot;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartMeetingDistributedBoot {
    public static final String DEFAULT_CONFIG_PATH = SmartMeetingBoot.DEFAULT_CONFIG_PATH;
    public static final String WS_HOST_PROPERTY = "smartmeeting.ws.host";
    public static final String WS_PORT_PROPERTY = "smartmeeting.ws.port";

    private static final int DEFAULT_WS_PORT = 8899;
    private static final int EXTRA_NODE_KEEP_SECONDS = 120;

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        JsonConfig config = JsonConfig.load(configPath);
        String scenarioName = config.getString("scenarioName", "sm-unnamed");
        String scenarioVersion = config.getString("scenarioVersion", "1.0");
        System.setProperty("smartmeeting.mode", "distributed");
        System.setProperty("smartmeeting.scenario", scenarioName);
        System.setProperty("smartmeeting.scenario.version", scenarioVersion);
        ScenarioTrace.reset(0);

        int steps = config.getInt("steps", 60);
        long seed = config.getLong("baseSeed", 42);
        String wsHost = System.getProperty(WS_HOST_PROPERTY, "localhost");
        int wsPort = parseIntProperty(WS_PORT_PROPERTY, DEFAULT_WS_PORT);
        List<JSONObject> deploymentNodes = readDeploymentNodes(config);
        validateDeployment(config, deploymentNodes);
        String selectedNodeId = readNodeOverride(args);
        if (selectedNodeId != null) {
            deploymentNodes = deploymentNodes.stream()
                    .filter(node -> selectedNodeId.equals(JsonConfig.getString(node, "id", "")))
                    .collect(Collectors.toList());
            if (deploymentNodes.isEmpty())
                throw new IllegalArgumentException("Unknown deployment node: " + selectedNodeId);
        }

        StringBuilder boot = new StringBuilder();
        boot.append(" -load_order pylon;simulation;executor;context;SmartMeetingGroup");
        boot.append(" -package net.xqhs.flash.abms");
        boot.append(" -package net.xqhs.flash.webSocket");
        boot.append(" -package abms.smartMeeting");
        boot.append(" -loader SmartMeetingGroup classpath:abms.smartMeeting.SmartMeetingGroupLoader");

        JSONObject firstNode = deploymentNodes.get(0);
        String firstNodeId = JsonConfig.getString(firstNode, "id", "sm-node-1");
        boolean coordinator = firstNodeId.equals(JsonConfig.getString(readDeploymentNodes(config).get(0), "id", "sm-node-1"));
        boot.append(" -node ").append(firstNodeId).append(" keep:").append(EXTRA_NODE_KEEP_SECONDS);
        if (coordinator)
            boot.append(" -pylon webSocket:ws1 serverPort:").append(wsPort);
        else
            boot.append(" -pylon webSocket:ws1 connectTo:ws://").append(wsHost).append(":").append(wsPort);
        appendSmartMeetingPayload(boot, config, seed, steps, firstNode);
        if (selectedNodeId == null) {
            for (int i = 1; i < deploymentNodes.size(); i++) {
                JSONObject node = deploymentNodes.get(i);
                String nodeId = JsonConfig.getString(node, "id", "sm-node-" + (i + 1));
                boot.append(" -node ").append(nodeId).append(" keep:").append(EXTRA_NODE_KEEP_SECONDS);
                boot.append(" -pylon webSocket:ws").append(i + 1).append(" connectTo:ws://")
                        .append(wsHost).append(":").append(wsPort);
                appendSmartMeetingPayload(boot, config, seed, steps, node);
            }
        }

        FlashBoot.main(boot.toString().trim().split(" "));
    }

    private static String readNodeOverride(String[] args) {
        for (String arg : args)
            if (arg.startsWith("--node="))
                return arg.substring("--node=".length());
        return null;
    }

    private static void appendSmartMeetingPayload(StringBuilder a, JsonConfig config, long seed, int steps,
                                                  JSONObject deploymentNode) {
        JSONObject graph = config.getObject("graph");
        List<String> nodes = JsonConfig.getStringList(graph, "nodes");
        List<String> edges = JsonConfig.getStringList(graph, "edges");

        a.append(" -simulation sim classpath:Simulation");
        a.append(" -executor StepWise:StepWise steps:").append(steps)
                .append(" stepBarrierNodeId:")
                .append(JsonConfig.getString(deploymentNode, "id", "unknown"))
                .append(" stepBarrierCoordinator:stepbarrier-")
                .append(JsonConfig.getString(readDeploymentNodes(config).get(0), "id", "sm-node-1"))
                .append(" stepBarrierNodes:").append(String.join(",", barrierNodeIds(config)));
        a.append(" -context AgentManagement:agentManagement");
        a.append(" -context Random:random seed:").append(seed);
        a.append(" -context GraphCommunication:communication");
        a.append(" -context Space:space topology:graph");
        a.append(" nodes:").append(String.join(",", nodes));
        a.append(" edges:").append(String.join(",", edges));
        a.append(" -SmartMeetingGroup g");
        appendAssignedAgents(a, config, deploymentNode);
    }

    private static void appendAssignedAgents(StringBuilder a, JsonConfig config, JSONObject node) {
        String nodeId = JsonConfig.getString(node, "id", "unknown");
        List<String> assigned = JsonConfig.getStringList(node, "agents");
        appendAssignedKind(a, config, "Auction", assigned, nodeId);
        appendAssignedKind(a, config, "Person", assigned, nodeId);
        appendAssignedKind(a, config, "Room", assigned, nodeId);
    }

    private static void appendAssignedKind(StringBuilder a, JsonConfig config, String kind,
                                           List<String> assigned, String nodeId) {
        List<String> matching = new ArrayList<>();
        String prefix = kind.equals("Room") ? "r" : kind.toLowerCase();
        for (String name : assigned)
            if (name.startsWith(prefix))
                matching.add(name);
        if (matching.isEmpty())
            return;
        JSONObject definition = findAgentDefinition(config, kind);
        if (definition == null)
            throw new IllegalArgumentException("Deployment references " + kind + " agents but no definition exists");
        JSONObject params = (JSONObject) definition.get("params");
        a.append(" -agent ").append(kind).append(" n:").append(matching.size());
        if (kind.equals("Room")) {
            a.append(" roomIdList:").append(String.join("|", matching));
        }
        appendParams(a, params);
        a.append(" nodeId:").append(nodeId);
        if (kind.equals("Auction")) {
            a.append(" bidWaitSteps:20 roomTargets:").append(allRoomIds(config));
        }
    }

    private static String allRoomIds(JsonConfig config) {
        List<String> rooms = new ArrayList<>();
        for (JSONObject node : readDeploymentNodes(config))
            for (String agent : JsonConfig.getStringList(node, "agents"))
                if (agent.startsWith("r"))
                    rooms.add(agent);
        return String.join(",", rooms);
    }

    private static List<JSONObject> readDeploymentNodes(JsonConfig config) {
        JSONObject deployment = config.getObject("deployment");
        List<JSONObject> nodes = new ArrayList<>();
        Object configuredNodes = deployment == null ? null : deployment.get("nodes");
        if (configuredNodes instanceof JSONArray)
            for (Object value : (JSONArray) configuredNodes)
                if (value instanceof JSONObject)
                    nodes.add((JSONObject) value);
        if (nodes.isEmpty())
            throw new IllegalArgumentException(
                    "Distributed Smart Meeting requires deployment.nodes in the scenario configuration");
        return nodes;
    }

    private static List<String> barrierNodeIds(JsonConfig config) {
        List<String> ids = new ArrayList<>();
        for (JSONObject node : readDeploymentNodes(config))
            ids.add(JsonConfig.getString(node, "id", "unknown"));
        return ids;
    }

    private static void validateDeployment(JsonConfig config, List<JSONObject> nodes) {
        Set<String> assigned = new HashSet<>();
        for (JSONObject node : nodes) {
            String id = JsonConfig.getString(node, "id", "");
            if (id.isEmpty())
                throw new IllegalArgumentException("Deployment node is missing id");
            for (String agent : JsonConfig.getStringList(node, "agents"))
                if (!assigned.add(agent))
                    throw new IllegalArgumentException("Agent assigned to multiple nodes: " + agent);
        }
        for (JSONObject definition : config.getObjectList("agents")) {
            String kind = JsonConfig.getString(definition, "kind", "");
            int count = JsonConfig.getInt(definition, "count", 0);
            String prefix = kind.equalsIgnoreCase("Room") ? "r" : kind.toLowerCase();
            for (int i = 0; i < count; i++) {
                String expected = prefix + (kind.equalsIgnoreCase("Room") ? i + 1 : i);
                if (!assigned.contains(expected))
                    throw new IllegalArgumentException("Agent missing from deployment: " + expected);
            }
        }
    }

    private static JSONObject findAgentDefinition(JsonConfig config, String kind) {
        for (JSONObject agent : config.getObjectList("agents"))
            if (kind.equalsIgnoreCase(JsonConfig.getString(agent, "kind", "")))
                return agent;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void appendParams(StringBuilder a, JSONObject params) {
        if (params == null) return;
        for (Object keyObj : params.keySet()) {
            String key = keyObj.toString();
            Object v = params.get(key);
            a.append(' ').append(key).append(':').append(v);
        }
    }

    private static int parseIntProperty(String key, int fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty())
            return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
