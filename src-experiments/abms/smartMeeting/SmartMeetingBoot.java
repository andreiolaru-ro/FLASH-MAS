package abms.smartMeeting;

import java.util.List;

import aggregate_logging.ALogging;
import benchmarking.Benchmark;
import org.json.simple.JSONObject;

import abms.common.BatchRunner;
import abms.common.JsonConfig;
import net.xqhs.util.logging.Logger.Level;

/**
 * Entry point for the Smart Meeting simulation. Scenario configuration (graph topology,
 * number of auction/room/person agents and the request parameter ranges) is read from a
 * JSON file under {@code resources/config/smartmeeting/}.
 */
public class SmartMeetingBoot {
    public static final String DEFAULT_CONFIG_PATH = "resources/config/smartmeeting/tree-7n-light.json";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        final JsonConfig config = JsonConfig.load(configPath);
        final String scenarioName = config.getString("scenarioName", "sm-unnamed");
        final int runs = config.getInt("runs", 1);
        final int steps = config.getInt("steps", 60);
        final long baseSeed = config.getLong("baseSeed", 42);
        Level logLevel = parseLogLevel(config.getString("logLevel", "ERROR"));

        System.out.println("SmartMeeting scenario: " + scenarioName + " (" + configPath + ")");
        System.out.println("Running " + runs + " run(s), " + steps + " step(s) each, baseSeed=" + baseSeed);
        BatchRunner.run(runs, logLevel,
                runIndex -> buildBootString(config, baseSeed + runIndex, steps),
                new SmRunStats(scenarioName));

        ALogging.getInstance().printAllAgr();
        Benchmark.addTime(System.currentTimeMillis() - startTime);
        Benchmark.printResults();
    }

    private static String buildBootString(JsonConfig config, long seed, int steps) {
        JSONObject graph = config.getObject("graph");
        List<String> nodes = JsonConfig.getStringList(graph, "nodes");
        List<String> edges = JsonConfig.getStringList(graph, "edges");

        StringBuilder a = new StringBuilder();
        a.append(" -load_order simulation;executor;context;SmartMeetingGroup");
        a.append(" -package net.xqhs.flash.abms");
        a.append(" -package abms.smartMeeting");
        a.append(" -loader SmartMeetingGroup classpath:abms.smartMeeting.SmartMeetingGroupLoader");
        a.append(" -node dummy -simulation sim classpath:Simulation");
        a.append(" -executor StepWise:StepWise steps:").append(steps);
        a.append(" -context AgentManagement:agentManagement");
        a.append(" -context Random:random seed:").append(seed);
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
            appendParams(a, params);
        }
        return a.toString();
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
        try { return Level.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return Level.ERROR; }
    }
}
