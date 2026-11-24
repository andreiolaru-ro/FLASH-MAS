package Tiberiu.smartGrid.hybrid;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.recorder.RecorderService;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Automated experimental harness comparing:
 * 1. Nominal Baseline (100% LIVE, unperturbed) -> records tick-indexed trace and baseline prices.
 * 2. Perturbed Ground Truth (100% LIVE, perturbed) -> reference clearing prices under disturbance.
 * 3. Hybrid Dynamic Bubble (LIVE + MOCKED replay) -> evaluates fidelity error (RMSE/MAE),
 *    bubble activation ratio, catch-up overhead, and step reduction.
 */
public class HybridBenchmarkRunner {

    public static class RunResult {
        public final double						elapsedMs;
        public final double						catchUpMs;
        public final long						totalAgentStepsExecuted;
        public final List<Integer>				liveAgentsPerTick		= new ArrayList<>();
        public final Map<String, List<Double>>	clearingPricesByZone	= new HashMap<>();

        public RunResult(double elapsedMs, double catchUpMs, long totalAgentStepsExecuted) {
            this.elapsedMs = elapsedMs;
            this.catchUpMs = catchUpMs;
            this.totalAgentStepsExecuted = totalAgentStepsExecuted;
        }
    }

    public static void main(String[] args) throws Exception {
        int numZones = 4;
        int prosumersPerZone = 25;
        int totalAgents = numZones * (prosumersPerZone + 1); // 104 agents
        long maxTicks = 48; // 24h at 30-min Ausgrid resolution
        String perturbedAgent = "Prosumer_Zone_A_0";

        AusgridDatasetLoader dataset = new AusgridDatasetLoader();
        dataset.generateSyntheticAusgridProfiles(numZones * prosumersPerZone, (int) maxTicks, 42L);

        // 1. Nominal Baseline Run (records tick-indexed JSON trace)
        System.out.println("=== [1/3] Running Nominal Baseline (100% LIVE Recording) ===");
        System.setProperty("flash.recorder.enabled", "true");
        System.setProperty("flash.recorder.file_mode", "true");
        RecorderService.reinitialize();

        RunResult nominalBaseline = executeExperiment(dataset, numZones, prosumersPerZone, maxTicks,
                null, false, null, null);
        RecorderService.shutdown(); // Flush JSON array cleanly to disk
        File baselineFile = findLatestSimulationLog();

        // 2. Perturbed Ground Truth Run (100% LIVE with perturbation)
        System.out.println("=== [2/3] Running Perturbed Ground Truth (100% LIVE) ===");
        System.setProperty("flash.recorder.enabled", "false");
        RecorderService.reinitialize();

        RunResult groundTruth = executeExperiment(dataset, numZones, prosumersPerZone, maxTicks,
                null, false, perturbedAgent, null);

        // 3. Hybrid Dynamic Bubble Run
        System.out.println("=== [3/3] Running Hybrid Bubble Execution (LIVE + MOCKED Replay) ===");
        RunResult hybrid = executeExperiment(dataset, numZones, prosumersPerZone, maxTicks,
                baselineFile.getPath(), true, perturbedAgent, nominalBaseline);

        // Compute, display, and export validation metrics
        printAndExportMetrics(groundTruth, hybrid, totalAgents, maxTicks, "hybrid_benchmark_metrics.csv");
    }

    private static RunResult executeExperiment(AusgridDatasetLoader dataset, int numZones,
                                               int prosumersPerZone, long maxTicks, String tracePath, boolean hybridMode,
                                               String perturbedAgent, RunResult nominalBaseline) throws Exception {

        BubbleContext bubbleContext = new BubbleContext("SmartGridBubbleContext");
        HybridPylon pylon = new HybridPylon();
        pylon.configure(new MultiTreeMap());
        pylon.addGeneralContext(bubbleContext.asContext());
        pylon.start();

        TickTraceStore traceStore = new TickTraceStore();
        if (hybridMode && tracePath != null) {
            traceStore.loadFromJson(tracePath);
        }

        HybridSimulationExecutor executor = new HybridSimulationExecutor(pylon, traceStore, hybridMode);
        executor.setBubbleContext(bubbleContext);

        String[] zoneNames = { "Zone_A", "Zone_B", "Zone_C", "Zone_D" };
        Map<String, AggregatorAgent> aggregatorsByZone = new HashMap<>();
        int customerCounter = 1;

        for (int z = 0; z < numZones; z++) {
            String aggName = "Aggregator_" + zoneNames[z];
            List<String> localProsumers = new ArrayList<>();

            for (int p = 0; p < prosumersPerZone; p++) {
                String pName = "Prosumer_" + zoneNames[z] + "_" + p;
                localProsumers.add(pName);
                boolean isPerturbed = pName.equals(perturbedAgent);
                ProsumerAgent prosumer = new ProsumerAgent(pName, customerCounter++, aggName, dataset, isPerturbed);
                prosumer.addGeneralContext(pylon.getPylonProxy());
                executor.registerAgent(prosumer);
            }

            List<String> neighbors = new ArrayList<>();
            if (z > 0)
                neighbors.add("Aggregator_" + zoneNames[z - 1]);
            if (z < numZones - 1)
                neighbors.add("Aggregator_" + zoneNames[z + 1]);

            AggregatorAgent aggregator = new AggregatorAgent(aggName, localProsumers, neighbors);
            aggregator.addGeneralContext(pylon.getPylonProxy());
            executor.registerAgent(aggregator);
            aggregatorsByZone.put(zoneNames[z], aggregator);
        }

        Set<String> initialLive = new HashSet<>();
        if (perturbedAgent != null) {
            initialLive.add(perturbedAgent);
        }

        long tStart = System.nanoTime();
        executor.runSimulation(maxTicks, initialLive);
        double elapsedMs = (System.nanoTime() - tStart) / 1_000_000.0;

        pylon.stop();

        RunResult result = new RunResult(elapsedMs, executor.getCatchUpTimeMs(), executor.getTotalStepsExecuted());
        result.liveAgentsPerTick.addAll(executor.getLiveCountHistory());

        // Reconstruct full per-zone clearing price trajectory (using nominal baseline values whenever an aggregator was MOCKED)
        for (String zone : zoneNames) {
            AggregatorAgent agg = aggregatorsByZone.get(zone);
            List<Double> recordedLivePrices = agg.getClearingPriceHistory();
            List<Double> baselinePrices = nominalBaseline != null
                    ? nominalBaseline.clearingPricesByZone.getOrDefault(zone, Collections.emptyList())
                    : Collections.emptyList();

            List<Double> fullSeries = new ArrayList<>((int) maxTicks);
            for (int t = 0; t < maxTicks; t++) {
                if (t < recordedLivePrices.size()) {
                    fullSeries.add(recordedLivePrices.get(t));
                } else if (t < baselinePrices.size()) {
                    fullSeries.add(baselinePrices.get(t));
                } else {
                    fullSeries.add(0.25);
                }
            }
            result.clearingPricesByZone.put(zone, fullSeries);
        }

        return result;
    }

    private static void printAndExportMetrics(RunResult groundTruth, RunResult hybrid,
                                              int totalAgents, long maxTicks, String csvOutPath) throws Exception {

        double sumSqErr = 0.0;
        double sumAbsErr = 0.0;
        int priceSamples = 0;

        for (String zone : groundTruth.clearingPricesByZone.keySet()) {
            List<Double> gtPrices = groundTruth.clearingPricesByZone.get(zone);
            List<Double> hybPrices = hybrid.clearingPricesByZone.getOrDefault(zone, Collections.emptyList());
            int len = Math.min(gtPrices.size(), hybPrices.size());
            for (int t = 0; t < len; t++) {
                double diff = gtPrices.get(t) - hybPrices.get(t);
                sumSqErr += diff * diff;
                sumAbsErr += Math.abs(diff);
                priceSamples++;
            }
        }

        double rmse = priceSamples > 0 ? Math.sqrt(sumSqErr / priceSamples) : 0.0;
        double mae = priceSamples > 0 ? (sumAbsErr / priceSamples) : 0.0;
        double avgBubbleRatio = hybrid.liveAgentsPerTick.stream()
                .mapToDouble(c -> (double) c / totalAgents)
                .average().orElse(0.0);
        double stepReductionPct = 100.0 * (1.0 - ((double) hybrid.totalAgentStepsExecuted / groundTruth.totalAgentStepsExecuted));

        System.out.println("\n================ VALIDATION SUMMARY ================");
        System.out.printf(Locale.US, "Ground Truth (100%% LIVE) Time  : %.2f ms (%d agent steps)%n",
                groundTruth.elapsedMs, groundTruth.totalAgentStepsExecuted);
        System.out.printf(Locale.US, "Hybrid Execution Time          : %.2f ms (%d agent steps | Catch-up: %.2f ms)%n",
                hybrid.elapsedMs, hybrid.totalAgentStepsExecuted, hybrid.catchUpMs);
        System.out.printf(Locale.US, "Computational Step Reduction   : %.2f%%%n", stepReductionPct);
        System.out.printf(Locale.US, "Mean Bubble Activation Ratio   : %.2f%% (%d total agents)%n",
                avgBubbleRatio * 100.0, totalAgents);
        System.out.printf(Locale.US, "Clearing Price Fidelity RMSE   : %.6f%n", rmse);
        System.out.printf(Locale.US, "Clearing Price Fidelity MAE    : %.6f%n", mae);
        System.out.println("====================================================\n");

        try (PrintWriter pw = new PrintWriter(new FileWriter(csvOutPath))) {
            pw.println("tick,live_agents,total_agents,bubble_ratio,gt_price_zone_a,hybrid_price_zone_a");
            List<Double> gtZoneA = groundTruth.clearingPricesByZone.get("Zone_A");
            List<Double> hybZoneA = hybrid.clearingPricesByZone.get("Zone_A");
            for (int t = 0; t < hybrid.liveAgentsPerTick.size(); t++) {
                int live = hybrid.liveAgentsPerTick.get(t);
                pw.printf(Locale.US, "%d,%d,%d,%.4f,%.4f,%.4f%n",
                        t, live, totalAgents, (double) live / totalAgents, gtZoneA.get(t), hybZoneA.get(t));
            }
        }
        System.out.println("Exported per-tick benchmark telemetry to: " + csvOutPath);
    }

    private static File findLatestSimulationLog() {
        File dir = new File(".");
        File[] logs = dir.listFiles((d, name) -> name.startsWith("simulation_log_") && name.endsWith(".json"));
        if (logs == null || logs.length == 0) {
            throw new IllegalStateException("No baseline simulation_log_*.json file found.");
        }
        return Arrays.stream(logs).max(Comparator.comparingLong(File::lastModified)).orElse(logs[0]);
    }
}