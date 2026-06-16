package abms.smartMeeting;

import java.util.LinkedHashMap;
import java.util.Map;

import abms.common.BatchRunner.RunObserver;
import abms.common.RunStatistics;
import abms.smartMeeting.AuctionAgent.AuctionOutcome;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.core.Entity;

/**
 * Walks the simulation after each Smart Meeting run, harvests per-auction
 * outcomes from each {@link AuctionAgent}, and prints aggregate numbers across runs.
 */
public class SmRunStats implements RunObserver {
    private final RunStatistics stats = new RunStatistics();
    private final Map<String, Integer> winsPerRoom = new LinkedHashMap<>();
    private final String scenarioName;

    public SmRunStats(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    @Override
    public void onRunCompleted(int runIndex, Simulation sim) {
        int totalAuctions = 0, totalWon = 0;
        int personsAccepted = 0, personsRejected = 0, personsNoResponse = 0;
        double sumLatency = 0, sumBids = 0, sumFeasibleBids = 0;

        for (Entity<?> entity : sim.getSimulationObjects()) {
            if (entity instanceof AuctionAgent) {
                AuctionAgent auction = (AuctionAgent) entity;
                for (AuctionOutcome outcome : auction.getOutcomes()) {
                    totalAuctions++;
                    sumLatency += outcome.latencySteps();
                    sumBids += outcome.bidsReceived;
                    sumFeasibleBids += outcome.feasibleBids;
                    if (outcome.won) totalWon++;
                }
                for (Map.Entry<String, Integer> w : auction.getWinsPerRoom().entrySet())
                    winsPerRoom.merge(w.getKey(), w.getValue(), Integer::sum);
            }
            if (entity instanceof PersonAgent) {
                PersonAgent person = (PersonAgent) entity;
                if (!person.isResponseReceived()) personsNoResponse++;
                else if (person.isResponseAccepted()) personsAccepted++;
                else personsRejected++;
            }
        }

        stats.record("auctions_started", totalAuctions);
        stats.record("auctions_won", totalWon);
        stats.record("auctions_failed", Math.max(0, totalAuctions - totalWon));
        stats.record("auction_success_rate", totalAuctions == 0 ? 0 : (double) totalWon / totalAuctions);
        if (totalAuctions > 0) {
            stats.record("mean_latency_steps", sumLatency / totalAuctions);
            stats.record("mean_bids_per_auction", sumBids / totalAuctions);
            stats.record("mean_feasible_bids_per_auction", sumFeasibleBids / totalAuctions);
        }
        stats.record("persons_accepted", personsAccepted);
        stats.record("persons_rejected", personsRejected);
        stats.record("persons_no_response", personsNoResponse);

        System.out.printf("Run %d: %d auctions, %d won, accept=%d reject=%d nopath=%d%n",
                runIndex, totalAuctions, totalWon, personsAccepted, personsRejected, personsNoResponse);
    }

    @Override
    public void onAllRunsCompleted(int runs) {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("SmartMeeting aggregate statistics over " + runs + " runs  (" + scenarioName + ")");
        System.out.println("==========================================================");
        System.out.println(stats.formatSummary("auctions_started", "auctions started per run"));
        System.out.println(stats.formatSummary("auctions_won", "auctions won per run"));
        System.out.println(stats.formatSummary("auctions_failed", "auctions failed per run"));
        System.out.println(stats.formatSummary("auction_success_rate", "auction success rate"));
        System.out.println(stats.formatSummary("mean_latency_steps", "auction latency (steps)"));
        System.out.println(stats.formatSummary("mean_bids_per_auction", "bids received per auction"));
        System.out.println(stats.formatSummary("mean_feasible_bids_per_auction", "feasible bids per auction"));
        System.out.println(stats.formatSummary("persons_accepted", "persons accepted per run"));
        System.out.println(stats.formatSummary("persons_rejected", "persons rejected per run"));
        System.out.println(stats.formatSummary("persons_no_response", "persons not responded per run"));
        System.out.println();
        System.out.println("  Winner distribution across all runs:");
        int totalWins = 0;
        for (Integer w : winsPerRoom.values()) totalWins += w;
        if (totalWins == 0) {
            System.out.println("    (no auctions won)");
        } else {
            for (Map.Entry<String, Integer> entry : winsPerRoom.entrySet()) {
                int w = entry.getValue();
                System.out.printf("    %-10s %5d wins (%.1f%%)%n",
                        entry.getKey(), w, 100.0 * w / totalWins);
            }
        }
        System.out.println("==========================================================");
    }
}
