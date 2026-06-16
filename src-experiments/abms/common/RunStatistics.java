package abms.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Numeric accumulator used for per-scenario per-metric aggregation across runs.
 * Reports mean, std-dev, min, max
 */
public final class RunStatistics {
    private final Map<String, List<Double>> samples = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> categoricalCounts = new LinkedHashMap<>();

    public void record(String key, double value) {
        samples.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    public void recordCategorical(String key, String category) {
        categoricalCounts.computeIfAbsent(key, k -> new LinkedHashMap<>())
                .merge(category, 1, Integer::sum);
    }

    public int sampleCount(String key) {
        List<Double> list = samples.get(key);
        return list == null ? 0 : list.size();
    }

    public double mean(String key) {
        List<Double> list = samples.get(key);
        if (list == null || list.isEmpty()) return 0;
        double sum = 0;
        for (Double d : list) sum += d;
        return sum / list.size();
    }

    public double stddev(String key) {
        List<Double> list = samples.get(key);
        if (list == null || list.size() < 2) return 0;
        double m = mean(key);
        double s = 0;
        for (Double d : list) {
            double diff = d - m;
            s += diff * diff;
        }
        return Math.sqrt(s / (list.size() - 1));
    }

    public double min(String key) {
        List<Double> list = samples.get(key);
        if (list == null || list.isEmpty()) return 0;
        return Collections.min(list);
    }

    public double max(String key) {
        List<Double> list = samples.get(key);
        if (list == null || list.isEmpty()) return 0;
        return Collections.max(list);
    }

    public Map<String, Integer> categories(String key) {
        Map<String, Integer> map = categoricalCounts.get(key);
        return map == null ? Collections.emptyMap() : map;
    }

    public String formatSummary(String key, String label) {
        return String.format("  %-35s n=%d  mean=%.4f  sd=%.4f  min=%.4f  max=%.4f",
                label, sampleCount(key), mean(key), stddev(key), min(key), max(key));
    }
}
