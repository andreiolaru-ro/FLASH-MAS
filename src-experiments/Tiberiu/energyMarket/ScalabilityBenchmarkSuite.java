package Tiberiu.energyMarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import net.xqhs.flash.FlashBoot;

public class ScalabilityBenchmarkSuite {

    private static final String EXP_DIR = "src-experiments/Tiberiu/energyMarket/";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "RUN_WORKER".equals(args[0])) {
            runSimulationWorker();
            return;
        }

        System.out.println("=========================================================");
        System.out.println("   LAUNCHING AUTOMATED HYBRID SCALABILITY BENCHMARK");
        System.out.println("=========================================================");

        // TESTUL 3: Majoritatea agenților sunt înghețați în Mocked (Doar Zona A e Live, Zonele B și C sunt Mocked)
        generateHybridDeployment("deployment_mostly_mocked.xml", false);
        BenchmarkResult test3 = executeIsolatedTest("deployment_mostly_mocked.xml");

        Thread.sleep(4000);

        // TESTUL 4: Începe cu Zonele B și C în mod Mocked, dar ele devin LIVE în timpul execuției (Hot-Swap)
        generateHybridDeployment("deployment_runtime_swap.xml", true);
        BenchmarkResult test4 = executeIsolatedTest("deployment_runtime_swap.xml");

        System.out.println("--> Launching Test 5: 100% Fully Mocked Environment...");
        generateHybridDeployment("deployment_fully_mocked.xml", false);

        // AFISAREA RAPORTULUI COMPARATIV
        System.out.println("\n=========================================================");
        System.out.println("             SCALABILITY BENCHMARK REPORT");
        System.out.println("=========================================================");
        System.out.printf("%-30s | %-15s | %-12s\n", "Configuration", "Duration (ms)", "Avg CPU Load");
        System.out.println("---------------------------------------------------------");
        System.out.printf("%-30s | %-15d | %-12s\n", "Test 3: Mostly Mocked (Zone A)", test3.durationMs, String.format("%.2f%%", test3.avgCpu));
        System.out.printf("%-30s | %-15d | %-12s\n", "Test 4: Mid-Run Hot-Swap (B & C)", test4.durationMs, String.format("%.2f%%", test4.avgCpu));
        System.out.println("=========================================================");


    }

    private static BenchmarkResult executeIsolatedTest(String xmlName) throws Exception {
        System.out.println("\n--> Executing Profile: " + xmlName + "...");
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-cp", classpath, "-Dflash.recorder.enabled=false",
                ScalabilityBenchmarkSuite.class.getName(), "RUN_WORKER", xmlName
        );

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        double avgCpu = 0.0;

        while ((line = reader.readLine()) != null) {
            if (line.contains("WORKER_CPU_METRIC|")) {
                avgCpu = Double.parseDouble(line.split("\\|")[1]);
            } else if (line.contains("TRADE #") || line.contains("MUTATED") || line.contains("SUCCESS")) {
                System.out.println("  [Simulation] " + line);
            }
        }

        process.waitFor();
        long duration = System.currentTimeMillis() - startTime;
        return new BenchmarkResult(duration, avgCpu);
    }

    private static void runSimulationWorker() throws Exception {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long startCpuTime = osBean.getProcessCpuTime();
        long startTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long elapsedCpuNano = osBean.getProcessCpuTime() - startCpuTime;
            long elapsedWallMs = System.currentTimeMillis() - startTime;
            int cores = osBean.getAvailableProcessors();
            double cpuUsage = (elapsedWallMs > 0) ? ((elapsedCpuNano / 1_000_000.0) / (elapsedWallMs * cores)) * 100.0 : 0.0;
            System.out.println("WORKER_CPU_METRIC|" + cpuUsage);
        }));

        // Preluăm numele XML-ului primit ca argument de proces
        String xmlTarget = System.getProperty("sun.java.command").split(" ")[2];
        FlashBoot.main(new String[] { EXP_DIR + xmlTarget });
    }

    private static void generateHybridDeployment(String filename, boolean useDynamicSwap) throws IOException {
        File file = new File(EXP_DIR + filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<deployment xmlns=\"http://flash.xqhs.net/deployment-schema\"\n");
            writer.write("            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
            writer.write("    <package>Tiberiu.energyMarket</package>\n");
            writer.write("    <pylon kind=\"local\">\n\n");

            // ZONE A: Intotdeauna complet LIVE
            writer.write("        <agent name=\"MarketAgent_A\" classpath=\"MarketAgent\" />\n");
            for (int i = 1; i <= 33; i++) {
                writer.write("        <agent name=\"Prosumer_A" + i + "\" classpath=\"ProsumerAgent\" />\n");
            }

            // ZONE B & C: Hibride (Fie Mocked permanent, fie DynamicBridge pentru Hot-Swap)
            String targetClass = useDynamicSwap ? "DynamicBridgeAgent" : "MockPlaybackShell";
            String[] proxyZones = {"B", "C"};

            for (String zone : proxyZones) {
                writer.write("\n        <agent name=\"MarketAgent_" + zone + "\" classpath=\"MarketAgent\" />\n");
                for (int i = 1; i <= 33; i++) {
                    writer.write("        <agent name=\"Prosumer_" + zone + i + "\" classpath=\"" + targetClass + "\" />\n");
                }
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