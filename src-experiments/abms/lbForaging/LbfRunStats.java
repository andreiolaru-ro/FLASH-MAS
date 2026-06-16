package abms.lbForaging;

import abms.common.BatchRunner.RunObserver;
import abms.common.RunStatistics;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.core.Entity;

/**
 * Per-scenario observer that walks the simulation after each LBF run to read
 * the {@link ForagingContext} totals and the per-agent cumulative reward, then
 * prints a literature-style summary (mean ± sd, min, max) at the end.
 */
public class LbfRunStats implements RunObserver {
    private final RunStatistics stats = new RunStatistics();
    private final String scenarioName;

    public LbfRunStats(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    @Override
    public void onRunCompleted(int runIndex, Simulation sim) {
        ForagingContext foraging = null;
        for (SimulationContext ctx : sim.getSimulationContexts())
            if (ctx instanceof ForagingContext) foraging = (ForagingContext) ctx;
        if (foraging == null) return;

        int total = Math.max(1, foraging.getTotalFoodCount());
        int collected = foraging.getCollectedFoodCount();
        stats.record("food_collected", collected);
        stats.record("food_total", foraging.getTotalFoodCount());
        stats.record("collection_ratio", (double) collected / total);
        stats.record("cooperative_collections", foraging.getCooperativeCollections());
        stats.record("solo_collections", foraging.getSoloCollections());
        if (foraging.getFirstCollectionStep() >= 0)
            stats.record("first_collection_step", foraging.getFirstCollectionStep());
        if (foraging.getLastCollectionStep() >= 0)
            stats.record("last_collection_step", foraging.getLastCollectionStep());

        double teamReturn = 0;
        int foragerCount = 0;
        for (Entity<?> entity : sim.getSimulationObjects())
            if (entity instanceof ForagerAgent) {
                teamReturn += ((ForagerAgent) entity).getCumulativeReward();
                foragerCount++;
            }
        stats.record("team_return", teamReturn);
        if (foragerCount > 0)
            stats.record("mean_per_agent_return", teamReturn / foragerCount);

        System.out.printf("Run %d: collected %d/%d, team return %.2f%n",
                runIndex, collected, foraging.getTotalFoodCount(), teamReturn);
    }

    @Override
    public void onAllRunsCompleted(int runs) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("LBF aggregate statistics over " + runs + " runs  (" + scenarioName + ")");
        System.out.println("==========================================================");
        System.out.println(stats.formatSummary("food_collected", "foods collected per episode"));
        System.out.println(stats.formatSummary("collection_ratio", "collection ratio"));
        System.out.println(stats.formatSummary("cooperative_collections", "cooperative loads per episode"));
        System.out.println(stats.formatSummary("solo_collections", "solo loads per episode"));
        System.out.println(stats.formatSummary("first_collection_step", "step of first collection"));
        System.out.println(stats.formatSummary("last_collection_step", "step of last collection"));
        System.out.println(stats.formatSummary("team_return", "team return"));
        System.out.println(stats.formatSummary("mean_per_agent_return", "mean per-agent return"));
        System.out.println("==========================================================");
    }
}
