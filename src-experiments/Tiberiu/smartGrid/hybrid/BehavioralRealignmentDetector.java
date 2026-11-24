package Tiberiu.smartGrid.hybrid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates whether a promoted LIVE agent has realigned with its historical baseline
 * trajectory using sliding-window behavioral checkpointing and dense vector embeddings
 * (semantic token hashing + normalized numerical payload features).
 */
public class BehavioralRealignmentDetector {

    private static final int		EMBEDDING_DIM		= 64;
    private static final Pattern	NUMBER_PATTERN		= Pattern.compile("-?\\d+(\\.\\d+)?");

    private final int				stabilityWindow;
    private final double			similarityThreshold;

    /** Sliding window of per-tick cosine similarity checkpoints for each promoted agent. */
    private final Map<String, Deque<Double>> checkpointHistory = new HashMap<>();

    public BehavioralRealignmentDetector(int stabilityWindow, double similarityThreshold) {
        this.stabilityWindow = stabilityWindow;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Records a behavioral checkpoint for {@code agentName} at {@code tick} by embedding
     * its actual outbound messages and comparing them against the historical baseline trace.
     *
     * @return {@code true} if the agent remained above {@code similarityThreshold} for the entire
     *         {@code stabilityWindow} and can be safely demoted back to MOCKED.
     */
    public boolean recordAndCheckRealignment(String agentName, long tick,
                                             List<String> actualOutboundPayloads, TickTraceStore traceStore) {

        List<TickTraceStore.RecordedMessage> historicalOutgoing =
                traceStore.getOutgoingMessagesForAgent(agentName, tick);

        double similarity = computeTickBehavioralSimilarity(actualOutboundPayloads, historicalOutgoing);

        Deque<Double> window = checkpointHistory.computeIfAbsent(agentName, k -> new ArrayDeque<>());
        window.addLast(similarity);
        if (window.size() > stabilityWindow) {
            window.removeFirst();
        }

        if (window.size() < stabilityWindow) {
            return false;
        }

        for (double score : window) {
            if (score < similarityThreshold) {
                return false;
            }
        }

        // Realignment confirmed across the entire checkpoint window
        checkpointHistory.remove(agentName);
        return true;
    }

    public void resetAgent(String agentName) {
        checkpointHistory.remove(agentName);
    }

    private double computeTickBehavioralSimilarity(List<String> actualPayloads,
                                                   List<TickTraceStore.RecordedMessage> historicalMessages) {
        if (actualPayloads.isEmpty() && historicalMessages.isEmpty()) {
            return 1.0; // Both silent at this tick => perfectly aligned
        }
        if (actualPayloads.isEmpty() || historicalMessages.isEmpty()) {
            return 0.0; // Activity mismatch (silent vs. active)
        }

        double[] actualVec = new double[EMBEDDING_DIM];
        for (String payload : actualPayloads) {
            accumulateMessageEmbedding(actualVec, payload);
        }

        double[] histVec = new double[EMBEDDING_DIM];
        for (TickTraceStore.RecordedMessage msg : historicalMessages) {
            accumulateMessageEmbedding(histVec, msg.content);
        }

        return cosineSimilarity(actualVec, histVec);
    }

    /**
     * Projects a structured or unstructured message payload into a fixed-dimension dense vector:
     * - Indices [0..47]: signed feature hashing over symbolic/semantic tokens (e.g., BID, ASK, CLEAR).
     * - Indices [48..63]: normalized numerical quantities extracted from the payload.
     */
    private void accumulateMessageEmbedding(double[] target, String content) {
        if (content == null || content.isEmpty())
            return;

        String[] tokens = content.split("[|:,;\\s]+");
        int numSlot = 48;

        for (String token : tokens) {
            if (token.isEmpty())
                continue;

            Matcher m = NUMBER_PATTERN.matcher(token);
            if (m.matches()) {
                double val = Double.parseDouble(token);
                // Soft-normalized numerical feature in [-1, 1]
                double normalized = val / (1.0 + Math.abs(val));
                target[numSlot % EMBEDDING_DIM] += normalized;
                numSlot++;
            } else {
                int hash = token.toLowerCase().hashCode();
                int idx = Math.floorMod(hash, 48);
                double sign = ((hash & 1) == 0) ? 1.0 : -1.0;
                target[idx] += sign;
            }
        }
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0)
            return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}