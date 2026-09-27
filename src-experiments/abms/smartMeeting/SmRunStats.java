package abms.smartMeeting;

import abms.common.BatchRunner.RunObserver;
import abms.common.RunStatistics;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.core.Entity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the common structured trace after each Smart Meeting run and prints
 * aggregate numbers across runs.
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
        Map<String, Long> auctionStartSteps = new LinkedHashMap<>();
        Map<String, Integer> respondedPersons = new LinkedHashMap<>();

        for (Entity<?> entity : sim.getSimulationObjects())
            if (entity instanceof PersonAgent)
                respondedPersons.put(entity.getName(), Integer.valueOf(0));

        JSONArray trace = ScenarioTrace.snapshot();
        for (Object value : trace) {
            JSONObject event = (JSONObject) value;
            String type = String.valueOf(event.get("event"));
            String requestId = event.get("requestId") == null ? null : String.valueOf(event.get("requestId"));
            if ("auction-started".equals(type) && requestId != null) {
                totalAuctions++;
                auctionStartSteps.put(requestId, longValue(event.get("step")));
            } else if ("bid-received".equals(type)) {
                sumBids++;
                if (Boolean.TRUE.equals(event.get("success")))
                    sumFeasibleBids++;
            } else if ("auction-resolved".equals(type) && requestId != null) {
                boolean won = Boolean.TRUE.equals(event.get("success"));
                if (won) {
                    totalWon++;
                    String room = event.get("room") == null ? null : String.valueOf(event.get("room"));
                    if (room != null)
                        winsPerRoom.merge(room, Integer.valueOf(1), Integer::sum);
                }
                Long startStep = auctionStartSteps.get(requestId);
                if (startStep != null)
                    sumLatency += longValue(event.get("step")) - startStep.longValue();
            } else if ("booking-response-received".equals(type)) {
                if (Boolean.TRUE.equals(event.get("success")))
                    personsAccepted++;
                else
                    personsRejected++;
                String person = event.get("agent") == null ? null : String.valueOf(event.get("agent"));
                if (person != null)
                    respondedPersons.put(person, Integer.valueOf(1));
            }
        }
        for (Integer responded : respondedPersons.values())
            if (responded.intValue() == 0)
                personsNoResponse++;

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

    private static long longValue(Object value) {
        if (value instanceof Number)
            return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
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
