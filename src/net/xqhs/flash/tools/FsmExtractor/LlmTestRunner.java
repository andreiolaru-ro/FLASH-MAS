package net.xqhs.flash.tools.FsmExtractor;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Standalone demo environment for the LLM Formal Verification Pipeline.
 * Natively integrated with the technical requirements from the "easyLog" documentation.
 */
public class LlmTestRunner {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("Initializing LLM Formal Verification Pipeline for easyLog...");
        System.out.println("=========================================================");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[GUI Warning] Could not set native look and feel. Using default.");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Flash-MAS JSON Log File");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Log Files (*.json)", "json");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showOpenDialog(null);

        if (userSelection != JFileChooser.APPROVE_OPTION) {
            System.out.println("[Process Terminated] No file was selected. Exiting pipeline.");
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String jsonFilePath = selectedFile.getAbsolutePath();

        try {
            System.out.println("Reading log file from disk: " + jsonFilePath);
            String rawJsonData = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

            String flatRawJsonData = rawJsonData.replace("\r", "").replace("\n", " ").replace("\t", " ");

            // ACTOR ISOLATION: Extragem doar traseul unui singur agent pentru a preveni buclele concurentiale
            System.out.println("[Data Prep] Distilling logs using Actor-Isolation filtering...");
            String agentTraceJson = isolateSingleAgentTrace(flatRawJsonData);

            // PROMPT INJECTION: Force the LLM to respect the  EasyLog structure (Regex and final nodes)
            String easyLogEnforcedPayload = "CRITICAL INSTRUCTIONS FOR EASYLOG:\\n" +
                    "1. Create a strictly linear FSM for the isolated agent below (no loops / self-loops).\\n" +
                    "2. MANDATORY DECLARATION: FINAL [shape=doublecircle, color=green, fontcolor=green];\\n" +
                    "3. MANDATORY DECLARATION: ERROR [shape=doublecircle, color=red, fontcolor=red];\\n" +
                    "4. Labels must contain the keywords from the JSON (e.g., label=\"WAVE_SENT\" or label=\"TRADE_SUCCESS\").\\n" +
                    "5. Direct the flow to the FINAL node in case of TRADE_SUCCESS.\\n" +
                    "ISOLATED LOG TO ANALYZE:\\n" + agentTraceJson;

            System.out.println("\n[LLM Inference] Triggering LLM extraction on isolated dataset...");
            String fsmCode = LlmFsmExtractor.extractFsmFromJson(easyLogEnforcedPayload);

            if (fsmCode != null) {
                System.out.println("\n--- FINAL GENERATED FSM CODE (Graphviz DOT) ---\n");
                System.out.println(fsmCode);
                System.out.println("\n-----------------------------------------\n");

                LlmFsmExtractor.saveToFile(fsmCode, "generated_protocol.dot");

            } else {
                System.err.println("[Pipeline Failure] Extraction returned null. Please verify Ollama is running.");
            }

        } catch (IOException e) {
            System.err.println("[File Error] Could not read the selected JSON file: " + jsonFilePath);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[Pipeline Error] An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Data Normalization Engine (Actor-Isolation Version).
     * Finds the first agent that performs an action and extracts their personal chronology exclusively.
     * This prevents the LLM from confusing the actions of 5 simultaneous agents as "loops" in the protocol.
     */
    private static String isolateSingleAgentTrace(String flatJson) {
        String content = flatJson.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);

        String[] objects = content.split("\\},\\s*\\{");
        String targetAgent = null;

        // 1. Find the first prosumer agent that sent an offer
        for (String obj : objects) {
            if (obj.contains("\"type\": \"WAVE_SENT\"") && !obj.contains("MarketAgent")) {
                targetAgent = extractJsonValue(obj, "entityName");
                break;
            }
        }

        if (targetAgent == null) return "[]";

        System.out.println("[Actor-Isolation] Modeling lifecycle strictly for agent: " + targetAgent);

        // 2. 2. Extract ONLY this agent's events
        StringBuilder trace = new StringBuilder("[");
        boolean first = true;

        for (String obj : objects) {
            if (!obj.startsWith("{")) obj = "{" + obj;
            if (!obj.endsWith("}")) obj = obj + "}";

            if (obj.contains("\"entityName\": \"" + targetAgent + "\"")) {
                if (obj.contains("\"type\": \"WAVE_SENT\"") || obj.contains("\"type\": \"WAVE_RECEIVED\"")) {
                    if (!first) trace.append(", ");
                    trace.append(obj);
                    first = false;
                }
            }
        }
        trace.append("]");
        return trace.toString();
    }

    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\": \"";
        int start = json.indexOf(searchKey);
        if (start == -1) return "UNKNOWN";

        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "UNKNOWN";

        return json.substring(start, end);
    }
}