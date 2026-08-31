package benchmarking;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Utility class for benchmarking execution times across different categories.
 * It allows starting and stopping timers, accumulating the total duration
 * for each category over multiple runs.
 */
public class Benchmark {

    private static final Map<String, Long> timers = new LinkedHashMap<>();
    private static final Map<String, Long> activeStarts = new HashMap<>();

    private Benchmark() {}

    /**
     * Starts or restarts the timer for the specified category
     *
     * @param category the name of the category to start measuring
     */
    public static void start(String category) {
        activeStarts.put(category, System.currentTimeMillis());
    }

    /**
     * Stops the timer for the specified category. Can be restarted by calling the .start() method.
     * @param category the name of the category to stop measuring.
     */
    public static void stop(String category) {
        Long startTime = activeStarts.remove(category);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            timers.put(category, timers.getOrDefault(category, 0L) + duration);
        } else {
            System.err.println("stop() called for category '" + category + "' without matching start()");
        }
    }

    public static void clear() {
        timers.clear();
        activeStarts.clear();
    }

    public static void printResults() {
        System.out.println("=========================================");
        System.out.println("           BENCHMARK RESULTS             ");
        System.out.println("=========================================");
        for (Map.Entry<String, Long> entry : timers.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " ms");
        }
        System.out.println("=========================================\n");
    }
}