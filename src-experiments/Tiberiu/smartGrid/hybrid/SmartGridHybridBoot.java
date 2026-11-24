package Tiberiu.smartGrid.hybrid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Entry point for running multi-zone Smart-Grid experiments comparing full baseline recording
 * against hybrid bubble-based execution.
 */
public class SmartGridHybridBoot {

    public static void main(String[] args) throws Exception {
        int numZones = 4;
        int prosumersPerZone = 25;
        long maxTicks = 48; // 24 hours at 30-minute Ausgrid resolution

        AusgridDatasetLoader dataset = new AusgridDatasetLoader();
        // To use the official Ausgrid CSV dataset:
        // dataset.loadFromCsv("data/2012-2013_Solar_home_electricity_data.csv", "1/07/2012");
        dataset.generateSyntheticAusgridProfiles(numZones * prosumersPerZone, (int) maxTicks, 42L);

        System.out.println("=== PHASE 1: Baseline Run (100% LIVE Recording) ===");
        runScenario(dataset, numZones, prosumersPerZone, maxTicks, null, false, null);

        // Once baseline trace is saved to disk (e.g., "baseline_trace.json"),
        // invoke PHASE 2 in hybrid mode with a perturbed prosumer in Zone_A:
        // System.out.println("=== PHASE 2: Hybrid Run (Dynamic Bubble) ===");
        // runScenario(dataset, numZones, prosumersPerZone, maxTicks, "baseline_trace.json", true, "Prosumer_Zone_A_0");
    }

    public static void runScenario(AusgridDatasetLoader dataset,
                                   int numZones,
                                   int prosumersPerZone,
                                   long maxTicks,
                                   String traceJsonPath,
                                   boolean hybridMode,
                                   String perturbedAgentName) throws Exception {

        HybridPylon pylon = new HybridPylon();
        pylon.configure(new MultiTreeMap());
        pylon.start();

        TickTraceStore traceStore = new TickTraceStore();
        if (hybridMode && traceJsonPath != null) {
            traceStore.loadFromJson(traceJsonPath);
        }

        HybridSimulationExecutor executor = new HybridSimulationExecutor(pylon, traceStore, hybridMode);

        String[] zoneNames = { "Zone_A", "Zone_B", "Zone_C", "Zone_D" };
        int customerCounter = 1;

        for (int z = 0; z < numZones; z++) {
            String aggName = "Aggregator_" + zoneNames[z];
            List<String> localProsumers = new ArrayList<>();

            for (int p = 0; p < prosumersPerZone; p++) {
                String pName = "Prosumer_" + zoneNames[z] + "_" + p;
                localProsumers.add(pName);

                boolean isPerturbed = pName.equals(perturbedAgentName);
                ProsumerAgent prosumer = new ProsumerAgent(pName, customerCounter++, aggName, dataset, isPerturbed);
                prosumer.addContext(pylon.asContext());
                executor.registerAgent(prosumer);
            }

            // Linear inter-zone topology: Zone_A <-> Zone_B <-> Zone_C <-> Zone_D
            List<String> neighbors = new ArrayList<>();
            if (z > 0)
                neighbors.add("Aggregator_" + zoneNames[z - 1]);
            if (z < numZones - 1)
                neighbors.add("Aggregator_" + zoneNames[z + 1]);

            AggregatorAgent aggregator = new AggregatorAgent(aggName, localProsumers, neighbors);
            aggregator.addContext(pylon.asContext());
            executor.registerAgent(aggregator);
        }

        Set<String> initialLive = new HashSet<>();
        if (perturbedAgentName != null) {
            initialLive.add(perturbedAgentName);
        }

        long startTime = System.nanoTime();
        executor.runSimulation(maxTicks, initialLive);
        double elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0;

        pylon.stop();
        System.out.printf(Locale.US, "Simulation completed in %.2f ms | Final LIVE agents: %d / %d%n",
                elapsedMs, pylon.getLiveAgents().size(), numZones * (prosumersPerZone + 1));
    }
}