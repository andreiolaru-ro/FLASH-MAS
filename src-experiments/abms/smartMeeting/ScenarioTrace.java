package abms.smartMeeting;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Structured, in-process trace shared by local and distributed Smart Meeting runs.
 */
public final class ScenarioTrace {
    private static final List<JSONObject> events = new ArrayList<>();
    private static int run;
    private static long step = -1;
    private static long sequence;
    private static String mode = "simulation";
    private static String scenario = "sm-unnamed";
    private static String scenarioVersion = "1.0";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ScenarioTrace() {
    }

    public static synchronized void reset(int runIndex) {
        events.clear();
        run = runIndex;
        step = -1;
        sequence = 0;
        mode = System.getProperty("smartmeeting.mode", "simulation");
        scenario = System.getProperty("smartmeeting.scenario", "sm-unnamed");
        scenarioVersion = System.getProperty("smartmeeting.scenario.version", "1.0");
    }

    public static synchronized void exportRun() {
        String modeDirectory = mode.replaceAll("[^A-Za-z0-9._-]", "_");
        Path directory = Paths.get(System.getProperty("smartmeeting.results.dir",
                "results/smartmeeting"), modeDirectory);
        try {
            Files.createDirectories(directory);
            Path traceFile = directory.resolve(String.format("run-%03d.jsonl", run));
            List<String> lines = new ArrayList<>();
            for (JSONObject event : events)
                lines.add(event.toJSONString());
            Files.write(traceFile, lines, StandardCharsets.UTF_8);
            Files.write(directory.resolve(String.format("run-%03d-summary.json", run)),
                    summary().toJSONString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Could not export Smart Meeting trace for run " + run + ": " + e.getMessage());
        }
    }

    private static boolean isDeployment() {
        return !"simulation".equals(mode);
    }

    @SuppressWarnings("unchecked")
    public static synchronized JSONObject summary() {
        String timeKey = isDeployment() ? "timeMs" : "step";
        String unit = isDeployment() ? "Ms" : "Steps";
        int auctionsStarted = 0;
        int auctionsWon = 0;
        int bidsReceived = 0;
        int feasibleBids = 0;
        int personsAccepted = 0;
        int personsRejected = 0;
        Map<String, Long> auctionStartSteps = new java.util.LinkedHashMap<>();
        long latencyTotal = 0;
        Map<String, Integer> winnerDistribution = new java.util.LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> bookingsPerRoom = new java.util.TreeMap<>();
        Map<String, Long> requestStartSteps = new java.util.LinkedHashMap<>();
        List<Long> responseLatencies = new ArrayList<>();
        for (JSONObject event : events) {
            String type = String.valueOf(event.get("event"));
            String requestId = event.get("requestId") == null ? null : String.valueOf(event.get("requestId"));
            if ("booking-request-sent".equals(type) || "request-created".equals(type)) {
                if (requestId != null)
                    requestStartSteps.put(requestId, longValue(event.get(timeKey)));
            } else if ("auction-started".equals(type)) {
                auctionsStarted++;
                if (requestId != null)
                    auctionStartSteps.put(requestId, longValue(event.get(timeKey)));
            } else if ("auction-resolved".equals(type)) {
                if (Boolean.TRUE.equals(event.get("success"))) {
                    auctionsWon++;
                    String room = event.get("room") == null ? null : String.valueOf(event.get("room"));
                    if (room != null) {
                        winnerDistribution.merge(room, Integer.valueOf(1), Integer::sum);
                        Map<String, Object> booking = new java.util.LinkedHashMap<>();
                        if (event.get("slot") != null)
                            booking.put("time", formatSlot(TimeSlot.parse(String.valueOf(event.get("slot")))));
                        if (event.get("person") != null)
                            booking.put("person", event.get("person"));
                        bookingsPerRoom.computeIfAbsent(room, r -> new ArrayList<>()).add(booking);
                    }
                }
                Long start = requestId == null ? null : auctionStartSteps.get(requestId);
                if (start != null)
                    latencyTotal += longValue(event.get(timeKey)) - start.longValue();
            } else if ("bid-received".equals(type)) {
                bidsReceived++;
                if (Boolean.TRUE.equals(event.get("success"))) feasibleBids++;
            } else if ("booking-response-received".equals(type)) {
                if (Boolean.TRUE.equals(event.get("success"))) personsAccepted++;
                else personsRejected++;
                Long start = requestId == null ? null : requestStartSteps.get(requestId);
                if (start != null)
                    responseLatencies.add(Long.valueOf(longValue(event.get(timeKey)) - start.longValue()));
            }
        }
        JSONObject result = new JSONObject();
        result.put("scenario", scenario);
        result.put("scenarioVersion", scenarioVersion);
        result.put("run", run);
        result.put("mode", mode);
        result.put("events", events.size());
        result.put("auctionsStarted", auctionsStarted);
        result.put("auctionsWon", auctionsWon);
        result.put("auctionSuccessRate", auctionsStarted == 0 ? 0.0
                : (double) auctionsWon / auctionsStarted);
        result.put("meanLatency" + unit, auctionsStarted == 0 ? 0.0
                : (double) latencyTotal / auctionsStarted);
        result.put("bidsReceived", bidsReceived);
        result.put("meanBidsPerAuction", auctionsStarted == 0 ? 0.0
                : (double) bidsReceived / auctionsStarted);
        result.put("feasibleBids", feasibleBids);
        result.put("meanFeasibleBidsPerAuction", auctionsStarted == 0 ? 0.0
                : (double) feasibleBids / auctionsStarted);
        result.put("personsAccepted", personsAccepted);
        result.put("personsRejected", personsRejected);
        int responses = personsAccepted + personsRejected;
        result.put("bookingAcceptanceRate", responses == 0 ? 0.0
                : (double) personsAccepted / responses);
        result.put("meanResponseLatency" + unit, mean(responseLatencies));
        result.put("p95ResponseLatency" + unit, percentile(responseLatencies, 0.95));
        result.put("winnerDistribution", roomReservations(bookingsPerRoom));
        result.put("winnerEntropy", entropy(winnerDistribution, auctionsWon));
        result.put("winnerGini", gini(winnerDistribution));
        return result;
    }

    private static Map<String, Object> roomReservations(Map<String, List<Map<String, Object>>> bookingsPerRoom) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : bookingsPerRoom.entrySet()) {
            List<Map<String, Object>> bookings = new ArrayList<>(entry.getValue());
            bookings.sort(java.util.Comparator.comparing(b -> String.valueOf(b.get("time"))));
            Map<String, Object> room = new java.util.LinkedHashMap<>();
            room.put("reservations", Integer.valueOf(bookings.size()));
            room.put("bookings", bookings);
            result.put(entry.getKey(), room);
        }
        return result;
    }

    /** Formats a slot as hours of the day, e.g. 09:00-10:00. */
    private static String formatSlot(TimeSlot slot) {
        return String.format("%02d:%02d-%02d:%02d", slot.getStartMinute() / 60, slot.getStartMinute() % 60,
                slot.getEndMinute() / 60, slot.getEndMinute() % 60);
    }

    private static double mean(List<Long> values) {
        if (values.isEmpty())
            return 0.0;
        long total = 0;
        for (Long value : values)
            total += value.longValue();
        return (double) total / values.size();
    }

    private static double percentile(List<Long> values, double fraction) {
        if (values.isEmpty())
            return 0.0;
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(fraction * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1))).doubleValue();
    }

    private static double entropy(Map<String, Integer> distribution, int total) {
        if (total == 0)
            return 0.0;
        double result = 0.0;
        for (Integer count : distribution.values()) {
            double probability = (double) count.intValue() / total;
            result -= probability * Math.log(probability);
        }
        return result;
    }

    private static double gini(Map<String, Integer> distribution) {
        if (distribution.isEmpty())
            return 0.0;
        List<Integer> values = new ArrayList<>(distribution.values());
        Collections.sort(values);
        long total = 0;
        for (Integer value : values)
            total += value.intValue();
        if (total == 0)
            return 0.0;
        long weighted = 0;
        for (int i = 0; i < values.size(); i++)
            weighted += (long) (i + 1) * values.get(i).intValue();
        return (2.0 * weighted) / (values.size() * (double) total)
                - ((double) values.size() + 1.0) / values.size();
    }

    private static long longValue(Object value) {
        if (value instanceof Number)
            return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    public static synchronized void setStep(long currentStep) {
        step = currentStep;
    }

    @SuppressWarnings("unchecked")
    public static synchronized void record(String agent, String agentType, String event,
                                           String requestId, String room, Boolean success, String reason) {
        record(agent, agentType, event, requestId, room, success, reason,
                System.getProperty("smartmeeting.node", "unknown"));
    }

    public static synchronized void record(String agent, String agentType, String event,
                                           String requestId, String room, Boolean success, String reason, String nodeId) {
        record(agent, agentType, event, requestId, room, success, reason, nodeId, null, null);
    }

    @SuppressWarnings("unchecked")
    public static synchronized void record(String agent, String agentType, String event, String requestId,
                                           String room, Boolean success, String reason, String nodeId, String person,
                                           String slot) {
        JSONObject value = new JSONObject();
        value.put("scenario", scenario);
        value.put("scenarioVersion", scenarioVersion);
        value.put("run", run);
        value.put("step", step);
        value.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        if (isDeployment())
            value.put("timeMs", System.currentTimeMillis());
        value.put("sequence", sequence++);
        value.put("mode", mode);
        value.put("agent", agent);
        value.put("agentType", agentType);
        value.put("nodeId", nodeId);
        value.put("event", event);
        if (requestId != null) value.put("requestId", requestId);
        if (room != null) value.put("room", room);
        if (success != null) value.put("success", success);
        if (reason != null) value.put("reason", reason);
        if (person != null) value.put("person", person);
        if (slot != null) value.put("slot", slot);
        events.add(value);
    }

    public static synchronized JSONArray snapshot() {
        JSONArray result = new JSONArray();
        result.addAll(events);
        return result;
    }
}
