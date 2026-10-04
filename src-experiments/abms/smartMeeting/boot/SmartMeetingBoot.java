package abms.smartMeeting.boot;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import abms.common.BatchRunner;
import abms.common.JsonConfig;
import abms.smartMeeting.SmRunStats;
import aggregate_logging.ALogging;
import benchmarking.Benchmark;
import net.xqhs.util.logging.Logger.Level;

/**
 * Entry point for the Smart Meeting simulation. Scenario configuration (graph topology,
 * number of auction/room/person agents and the request parameter ranges) is read from a
 * JSON file under {@code resources/config/smartmeeting/}.
 */
public class SmartMeetingBoot {
	public static final String DEFAULT_CONFIG_PATH = "resources/config/smartmeeting/tree-3n.json";

    public static void main(String[] args) {
        Benchmark.start("Total");
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        final JsonConfig config = JsonConfig.load(configPath);
        final String scenarioName = config.getString("scenarioName", "sm-unnamed");
        final String scenarioVersion = config.getString("scenarioVersion", "1.0");
        final int configuredRuns = config.getInt("runs", 1);
        final int runs = readRunsOverride(args, configuredRuns);
        final int steps = config.getInt("steps", 60);
        final long baseSeed = config.getLong("baseSeed", 42);
        System.setProperty("smartmeeting.scenario", scenarioName);
        System.setProperty("smartmeeting.scenario.version", scenarioVersion);
        Level logLevel = parseLogLevel(config.getString("logLevel", "ERROR"));
        System.out.println("SmartMeeting scenario: " + scenarioName + " (" + configPath + ")");
        System.out.println("Running " + runs + " run(s), " + steps + " step(s) each, baseSeed=" + baseSeed);

        BatchRunner.run(runs, logLevel,
                runIndex -> buildBootString(config, baseSeed + runIndex, steps),
                new SmRunStats(scenarioName));

        ALogging.getInstance().printAllAgr();
        Benchmark.stop("Total");
        Benchmark.printResults();
    }

    private static int readRunsOverride(String[] args, int fallback) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--runs".equals(arg) && i + 1 < args.length) {
                try {
                    return validateRuns(Integer.parseInt(args[i + 1]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("--runs requires a positive integer", e);
                }
            }
            if (arg.startsWith("--runs=")) {
                try {
                    return validateRuns(Integer.parseInt(arg.substring("--runs=".length())));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("--runs requires a positive integer", e);
                }
            }
        }
        return validateRuns(fallback);
    }

    private static int validateRuns(int runs) {
        if (runs < 1)
            throw new IllegalArgumentException("--runs must be at least 1");
        return runs;
    }

    private static String buildBootString(JsonConfig config, long seed, int steps) {
        JSONObject graph = config.getObject("graph");
        List<String> nodes = JsonConfig.getStringList(graph, "nodes");
        List<String> edges = JsonConfig.getStringList(graph, "edges");

        StringBuilder a = new StringBuilder();
        a.append(" -load_order simulation;executor;context;pylon;SmartMeetingGroup");
        a.append(" -package net.xqhs.flash.abms");
        a.append(" -package abms.smartMeeting");
        a.append(" -package net.xqhs.flash.webSocket");
        a.append(" -loader SmartMeetingGroup classpath:abms.smartMeeting.SmartMeetingGroupLoader");
        a.append(" -node dummy");
        a.append(" -simulation sim classpath:Simulation");
        a.append(" -executor StepWise:StepWise steps:").append(steps);
        a.append(" -context AgentManagement:agentManagement");
        a.append(" -context Random:random seed:").append(seed);
        a.append(" -context Temporal:temporal");
        a.append(" -context GraphCommunication:communication");
        a.append(" -context Space:space topology:graph");
        a.append(" nodes:").append(String.join(",", nodes));
        a.append(" edges:").append(String.join(",", edges));
        a.append(" -SmartMeetingGroup g");
        for (JSONObject agent : config.getObjectList("agents")) {
            String kind = JsonConfig.getString(agent, "kind", "Agent");
            int count = JsonConfig.getInt(agent, "count", 0);
            JSONObject params = (JSONObject) agent.get("params");
            a.append(" -agent ").append(kind).append(" n:").append(count);
            // name the rooms as in the distributed deployment, so that both modes give the same results
            List<String> roomIds = deploymentRoomIds(config);
            if (kind.equals("Room") && roomIds.size() == count)
                a.append(" roomIdList:").append(String.join("|", roomIds));
            appendParams(a, params);
        }
        return a.toString();
    }

    /**
     * @return the names of the room agents in the scenario's deployment section, in order (empty if there is none).
     */
    private static List<String> deploymentRoomIds(JsonConfig config) {
        List<String> rooms = new ArrayList<>();
        JSONObject deployment = config.getObject("deployment");
        Object nodes = deployment == null ? null : deployment.get("nodes");
        if (nodes instanceof JSONArray)
            for (Object node : (JSONArray) nodes)
                if (node instanceof JSONObject)
                    for (String agent : JsonConfig.getStringList((JSONObject) node, "agents"))
                        if (agent.startsWith("r"))
                            rooms.add(agent);
        return rooms;
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

    @SuppressWarnings("unused")
    private static Level parseLogLevel(String name) {
        try {
            return Level.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Level.ERROR;
        }
    }
}
