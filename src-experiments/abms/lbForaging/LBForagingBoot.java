package abms.lbForaging;

import org.json.simple.JSONObject;

import abms.common.BatchRunner;
import abms.common.JsonConfig;
import net.xqhs.util.logging.Logger.Level;

/**
 * Entry point for the Level-Based Foraging simulation.
 *
 * The scenario configuration (grid size, agent counts, food levels, number of runs,
 * base seed, step count) is read from a JSON file. Per-run seed = baseSeed + runIndex
 * so the run sequence is reproducible while individual runs diverge.
 */
public class LBForagingBoot {
    public static final String DEFAULT_CONFIG_PATH = "resources/config/lbforaging/baseline-8x8.json";

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        final JsonConfig config = JsonConfig.load(configPath);
        final String scenarioName = config.getString("scenarioName", "lbf-unnamed");
        final int runs = config.getInt("runs", 1);
        final int steps = config.getInt("steps", 100);
        final long baseSeed = config.getLong("baseSeed", 42);
        Level logLevel = parseLogLevel(config.getString("logLevel", "ERROR"));

        System.out.println("LBForaging scenario: " + scenarioName + " (" + configPath + ")");
        System.out.println("Running " + runs + " run(s), " + steps + " step(s) each, baseSeed=" + baseSeed);

        BatchRunner.run(runs, logLevel,
                runIndex -> buildBootString(config, baseSeed + runIndex, steps),
                new LbfRunStats(scenarioName));
    }

    private static String buildBootString(JsonConfig config, long seed, int steps) {
        JSONObject grid = config.getObject("grid");
        int width = JsonConfig.getInt(grid, "width", 8);
        int height = JsonConfig.getInt(grid, "height", 8);

        StringBuilder a = new StringBuilder();
        a.append(" -load_order simulation;executor;context;LBForagingGroup");
        a.append(" -package net.xqhs.flash.abms");
        a.append(" -package abms.lbForaging");
        a.append(" -loader LBForagingGroup classpath:abms.lbForaging.LBForagingGroupLoader");
        a.append(" -node dummy -simulation sim classpath:Simulation");
        a.append(" -executor StepWise:StepWise steps:").append(steps);
        a.append(" -context AgentManagement:agentManagement");
        a.append(" -context Random:random seed:").append(seed);
        a.append(" -context ProximityCommunication:communication");
        a.append(" -context Space:space width:").append(width).append(" height:").append(height);
        a.append(" -context Foraging:foraging");
        a.append(" -LBForagingGroup g");

        for (JSONObject patch : config.getObjectList("patches")) {
            String kind = JsonConfig.getString(patch, "kind", "Food");
            int count = JsonConfig.getInt(patch, "count", 0);
            JSONObject params = (JSONObject) patch.get("params");
            a.append(" -patch ").append(kind).append(" n:").append(count);
            appendParams(a, params);
        }
        for (JSONObject agent : config.getObjectList("agents")) {
            String kind = JsonConfig.getString(agent, "kind", "Forager");
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

    private static Level parseLogLevel(String name) {
        try { return Level.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return Level.ERROR; }
    }
}
