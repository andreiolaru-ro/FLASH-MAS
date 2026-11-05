package Tiberiu.energyMarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import net.xqhs.flash.FlashBoot;

public class FinalMASPerformanceSuite {

    private static final String EXP_DIR = "src-experiments/Tiberiu/energyMarket/";

    public static void main(String[] args) throws Exception {
        // Rută internă pentru procesele izolate de tip Worker
        if (args.length > 0 && "RUN_WORKER".equals(args[0])) {
            runSimulationWorker();
            return;
        }

        System.out.println("=========================================================");
        System.out.println("   LAUNCHING FULL MAS PERFORMANCE AND SCALABILITY SUITE");
        System.out.println("=========================================================");

        // Pregătirea tuturor topologiilor pe disc înainte de execuție
        generateDeploymentXML("deployment_all_live.xml", "MarketAgent", "ProsumerAgent", "ProsumerAgent", "ProsumerAgent");
        generateDeploymentXML("deployment_mostly_mocked.xml", "MarketAgent", "ProsumerAgent", "MockPlaybackShell", "MockPlaybackShell");
        generateDeploymentXML("deployment_runtime_swap.xml", "MarketAgent", "ProsumerAgent", "DynamicBridgeAgent", "DynamicBridgeAgent");
        generateDeploymentXML("deployment_fully_mocked.xml", "MarketAgent", "MockPlaybackShell", "MockPlaybackShell", "MockPlaybackShell");

        // Execuția celor 5 profile experimentale din teză
        BenchmarkResult test1 = executeIsolatedTest("deployment_all_live.xml", "false", "Test 1: Logging DISABLED (All Live)");
        Thread.sleep(2000);

        BenchmarkResult test2 = executeIsolatedTest("deployment_all_live.xml", "true", "Test 2: Logging ENABLED (All Live)");
        Thread.sleep(2000);

        BenchmarkResult test3 = executeIsolatedTest("deployment_mostly_mocked.xml", "false", "Test 3: Mostly Mocked (Zone A Live)");
        Thread.sleep(2000);

        BenchmarkResult test4 = executeIsolatedTest("deployment_runtime_swap.xml", "false", "Test 4: Mid-Run Hot-Swap (B & C Live Promoted)");
        Thread.sleep(2000);

        BenchmarkResult test5 = executeIsolatedTest("deployment_fully_mocked.xml", "false", "Test 5: 100% Fully Mocked (Baseline)");

        // RAPORTUL CENTRALIZATOR PENTRU COMISIE / ARTICOL
        System.out.println("\n=========================================================================");
        System.out.println("                       CONSOLIDATED BENCHMARK REPORT                     ");
        System.out.println("=========================================================================");
        System.out.printf("%-45s | %-15s | %-12s\n", "Configuration Profile (10,000 Trades)", "Duration (ms)", "Avg CPU Load");
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("%-45s | %-15d | %-12s\n", "Test 1: Logging DISABLED (All Live)", test1.durationMs, String.format("%.2f%%", test1.avgCpu));
        System.out.printf("%-45s | %-15d | %-12s\n", "Test 2: Logging ENABLED (All Live)", test2.durationMs, String.format("%.2f%%", test2.avgCpu));
        System.out.printf("%-45s | %-15d | %-12s\n", "Test 3: Mostly Mocked (Zone A Only Live)", test3.durationMs, String.format("%.2f%%", test3.avgCpu));
        System.out.printf("%-45s | %-15d | %-12s\n", "Test 4: Mid-Run Hot-Swap (B & C promoted)", test4.durationMs, String.format("%.2f%%", test4.avgCpu));
        System.out.printf("%-45s | %-15d | %-12s\n", "Test 5: 100% Fully Mocked (Passive Replay)", test5.durationMs, String.format("%.2f%%", test5.avgCpu));
        System.out.println("=========================================================================");
    }

    private static BenchmarkResult executeIsolatedTest(String xmlName, String recordingProperty, String title) throws Exception {
        System.out.println("\n[START] " + title + "\n---------------------------------------------------------");
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "-Dflash.recorder.enabled=" + recordingProperty,
                FinalMASPerformanceSuite.class.getName(),
                "RUN_WORKER",
                xmlName
        );

        // Redirecționăm și stream-ul de erori în cel standard pentru a nu pierde eventualele excepții
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        double calculatedCpu = 0.0;

        // BUCLE DE CITIRE FULL OUTPUT: Trimite în consolă absolut tot ce produce FLASH-MAS în timp real
        while ((line = reader.readLine()) != null) {
            if (line.contains("WORKER_CPU_METRIC|")) {
                calculatedCpu = Double.parseDouble(line.split("\\|")[1]);
            } else {
                // Afișăm linia exact așa cum a fost generată de procesul de simulare
                System.out.println(line);
            }
        }

        process.waitFor();
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("---------------------------------------------------------");
        System.out.println("[FINISH] " + title + " executed in " + duration + " ms.\n");
        return new BenchmarkResult(duration, calculatedCpu);
    }

    private static void runSimulationWorker() throws Exception {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long startCpuTime = osBean.getProcessCpuTime();
        long startTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long elapsedCpuNano = osBean.getProcessCpuTime() - startCpuTime;
            long elapsedWallMs = System.currentTimeMillis() - startTime;
            int cores = osBean.getAvailableProcessors();

            double cpuUsage = 0.0;
            if (elapsedWallMs > 0 && startCpuTime >= 0) {
                cpuUsage = ((elapsedCpuNano / 1_000_000.0) / (elapsedWallMs * cores)) * 100.0;
            }
            System.out.println("WORKER_CPU_METRIC|" + cpuUsage);
        }));

        String cmd = System.getProperty("sun.java.command");
        String[] tokens = cmd.split(" ");
        String xmlTarget = tokens[tokens.length - 1];

        FlashBoot.main(new String[] { EXP_DIR + xmlTarget });
    }

    private static void generateDeploymentXML(String filename, String marketClass, String zoneAClass, String zoneBClass, String zoneCClass) throws IOException {
        File dir = new File(EXP_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(EXP_DIR + filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<deployment xmlns=\"http://flash.xqhs.net/deployment-schema\"\n");
            writer.write("            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
            writer.write("    <package>Tiberiu.energyMarket</package>\n");
            writer.write("    <pylon kind=\"local\">\n\n");

            writer.write("        <agent name=\"MarketAgent_A\" classpath=\"" + marketClass + "\" />\n");
            for (int i = 1; i <= 33; i++) {
                writer.write("        <agent name=\"Prosumer_A" + i + "\" classpath=\"" + zoneAClass + "\" />\n");
            }

            writer.write("\n        <agent name=\"MarketAgent_B\" classpath=\"" + marketClass + "\" />\n");
            for (int i = 1; i <= 33; i++) {
                writer.write("        <agent name=\"Prosumer_B" + i + "\" classpath=\"" + zoneBClass + "\" />\n");
            }

            writer.write("\n        <agent name=\"MarketAgent_C\" classpath=\"" + marketClass + "\" />\n");
            for (int i = 1; i <= 33; i++) {
                writer.write("        <agent name=\"Prosumer_C" + i + "\" classpath=\"" + zoneCClass + "\" />\n");
            }

            writer.write("    </pylon>\n");
            writer.write("</deployment>\n");
        }
    }

    private static class BenchmarkResult {
        long durationMs;
        double avgCpu;
        BenchmarkResult(long d, double c) { this.durationMs = d; this.avgCpu = c; }
    }
}