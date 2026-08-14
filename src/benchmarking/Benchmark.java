package benchmarking;

public class Benchmark {
    private static long totalTime = 0;
    private Benchmark() {}
    public static void addTime(long time) {
        totalTime += time;
    }
    public static long getTime() {
        return totalTime;
    }
    public static void printResults() {
        System.out.println("=========================================");
        System.out.println("           BENCHMARK RESULTS             ");
        System.out.println("=========================================");
        System.out.println("Total Time: " + totalTime + " ms" + "\n");
    }
}
