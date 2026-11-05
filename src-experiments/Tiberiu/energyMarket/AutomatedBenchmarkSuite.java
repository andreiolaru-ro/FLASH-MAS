package Tiberiu.energyMarket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import net.xqhs.flash.FlashBoot;

public class AutomatedBenchmarkSuite {

    private static final String EXP_DIR = "src-experiments/Tiberiu/energyMarket/";
    private static final String XML_FILENAME = "deployment_3microgrids.xml";

    public static void main(String[] args) throws Exception {
        // Dacă argumentele conțin un flag specific, înseamnă că acest proces este un "Worker" de simulare
        if (args.length > 0 && "RUN_WORKER".equals(args[0])) {
            runSimulationWorker();
            return;
        }

        System.out.println("=========================================================");
        System.out.println("   LAUNCHING AUTOMATED MAS RECORDER BENCHMARK");
        System.out.println("=========================================================");

        // 1. Asigurăm existența topologiei simetrice pe 3 microgriduri (102 agenți)
        generateMultiZoneDeployment(XML_FILENAME, 33);

        // 2. RULĂM TESTUL 1: Fără logging (flash.recorder.enabled = false)
        BenchmarkResult test1Results = executeIsolatedTest(false);

        // Un mic buffer de repaus între teste pentru răcirea CPU-lui și curățarea memoriei
        Thread.sleep(3000);

        // 3. RULĂM TESTUL 2: Cu logging activ (flash.recorder.enabled = true)
        BenchmarkResult test2Results = executeIsolatedTest(true);

        // 4. AFIȘAREA RAPORTULUI COMPARATIV FINAL
        System.out.println("\n=========================================================");
        System.out.println("                FINAL BENCHMARK REPORT");
        System.out.println("=========================================================");
        System.out.printf("%-25s | %-15s | %-12s\n", "Configuration", "Duration (ms)", "Avg CPU Load");
        System.out.println("---------------------------------------------------------");
        System.out.printf("%-25s | %-15d | %-12s\n",
                "Test 1: Logging DISABLED", test1Results.durationMs, String.format("%.2f%%", test1Results.avgCpu));
        System.out.printf("%-25s | %-15d | %-12s\n",
                "Test 2: Logging ENABLED", test2Results.durationMs, String.format("%.2f%%", test2Results.avgCpu));
        System.out.println("=========================================================");
    }

    /**
     * Lansează o instanță separată de JVM pentru a izola execuția și a măsura corect resursele.
     */
    private static BenchmarkResult executeIsolatedTest(boolean isLoggingEnabled) throws Exception {
        String status = isLoggingEnabled ? "ENABLED" : "DISABLED";
        System.out.println("\n--> Starting Test Case: Logging is " + status + "...");

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = AutomatedBenchmarkSuite.class.getName();

        // Construim comanda de rulare trecând proprietatea flash.recorder.enabled și argumentul de worker
        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "-Dflash.recorder.enabled=" + isLoggingEnabled,
                className,
                "RUN_WORKER"
        );

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        // Thread separat pentru monitorizarea CPU-ului procesului subsidiar din exterior,
        // sau citirea metricilor transmise de worker prin stream-ul standard de output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        double finalAvgCpu = 0.0;

        while ((line = reader.readLine()) != null) {
            // Monitorizăm consola workerului pentru a captura metrica de CPU calculată la final de el însuși
            if (line.contains("WORKER_CPU_METRIC|")) {
                finalAvgCpu = Double.parseDouble(line.split("\\|")[1]);
            } else {
                // Redirecționăm logurile normale în consola principală pentru vizibilitate
                System.out.println("  [Worker] " + line);
            }
        }

        int exitCode = process.waitFor();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("--> Test Case Finished with exit code: " + exitCode);
        return new BenchmarkResult(duration, finalAvgCpu);
    }

    /**
     * Metoda rulată în interiorul procesului izolat (Worker).
     * Măsoară consumul intern de CPU și pornește efectiv platforma FLASH-MAS.
     */
    /**
     * Metoda rulată în interiorul procesului izolat (Worker).
     * Măsoară precis diferența de timp CPU utilizat pe parcursul simulării.
     */
    private static void runSimulationWorker() throws Exception {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // Luăm timpul CPU inițial alocat acestui proces (în nanosecunde)
        long startCpuTime = osBean.getProcessCpuTime();
        long startTime = System.currentTimeMillis();

        // Înregistrăm un shutdown hook pentru a calcula metrica exact înainte ca JVM-ul să moară
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long endCpuTime = osBean.getProcessCpuTime();
            long endTime = System.currentTimeMillis();

            long elapsedCpuNano = endCpuTime - startCpuTime;
            long elapsedWallMs = endTime - startTime;

            // Numărul de nuclee disponibile (pentru a scala corect procentul total)
            int availableProcessors = osBean.getAvailableProcessors();

            // Formula de calcul: (Timp CPU în ms / (Timp Real în ms * Nr Nuclee)) * 100
            double cpuUsagePercentage = 0.0;
            if (elapsedWallMs > 0 && startCpuTime >= 0 && endCpuTime >= 0) {
                double cpuTimeMs = elapsedCpuNano / 1_000_000.0;
                cpuUsagePercentage = (cpuTimeMs / (elapsedWallMs * availableProcessors)) * 100.0;
            }

            // Trimitem metrica precisă către orchestrator
            System.out.println("WORKER_CPU_METRIC|" + cpuUsagePercentage);
        }));

        // Pornim simularea nativă FLASH-MAS
        FlashBoot.main(new String[] { EXP_DIR + XML_FILENAME });

        // Buffer de siguranță în caz de blocaj
        Thread.sleep(60000);
    }

    // Structură de date pentru stocarea rezultatelor intermediate
    private static class BenchmarkResult {
        long durationMs;
        double avgCpu;

        BenchmarkResult(long duration, double cpu) {
            this.durationMs = duration;
            this.avgCpu = cpu;
        }
    }

    /**
     * Eșantionează sarcina procesorului la fiecare 100ms.
     */
    private static class CpuSampler implements Runnable {
        private final OperatingSystemMXBean osBean;
        private double cpuSum = 0;
        private int samples = 0;

        CpuSampler(OperatingSystemMXBean bean) {
            this.osBean = bean;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(100);
                    double load = osBean.getProcessCpuLoad() * 100.0;
                    if (load >= 0) {
                        cpuSum += load;
                        samples++;
                    }
                }
            } catch (InterruptedException e) {
                // Thread oprit normal la final
            } finally {
                double avg = samples > 0 ? (cpuSum / samples) : 0.0;
                // Transmitem rezultatul către procesul părinte prin stream-ul standard de ieșire
                System.out.println("WORKER_CPU_METRIC|" + avg);
            }
        }
    }

    private static void generateMultiZoneDeployment(String filename, int prosumersPerZone) throws IOException {
        File dir = new File(EXP_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(EXP_DIR + filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<deployment xmlns=\"http://flash.xqhs.net/deployment-schema\"\n");
            writer.write("            xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.write("            xsi:schemaLocation=\"http://flash.xqhs.net/deployment-schema ../../src-schema/deployment-schema.xsd\">\n\n");
            writer.write("    <package>Tiberiu.energyMarket</package>\n");
            writer.write("    <pylon kind=\"local\">\n\n");

            String[] zones = {"A", "B", "C"};
            for (String zone : zones) {
                writer.write("        <agent name=\"MarketAgent_" + zone + "\" classpath=\"MarketAgent\" />\n");
                for (int i = 1; i <= prosumersPerZone; i++) {
                    writer.write("        <agent name=\"Prosumer_" + zone + i + "\" classpath=\"ProsumerAgent\" />\n");
                }
                writer.write("\n");
            }
            writer.write("    </pylon>\n");
            writer.write("</deployment>\n"); // Corectat: adaugat slash-ul de inchidere formal
        }
    }
}