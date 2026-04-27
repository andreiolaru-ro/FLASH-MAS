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
 * Integrat nativ cu cerintele tehnice din documentatia "easyLog".
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

            // PROMPT INJECTION: Fortam LLM-ul sa respecte structura EasyLog (Regex si Noduri finale)
            String easyLogEnforcedPayload = "INSTRUCTIUNI CRITICE PENTRU EASYLOG:\\n" +
                    "1. Creeaza un FSM strict liniar pentru agentul izolat mai jos (fara bucle / self-loops).\\n" +
                    "2. DECLARA OBLIGATORIU: FINAL [shape=doublecircle, color=green, fontcolor=green];\\n" +
                    "3. DECLARA OBLIGATORIU: ERROR [shape=doublecircle, color=red, fontcolor=red];\\n" +
                    "4. Etichetele (label) trebuie sa contina cuvintele cheie din JSON (ex: label=\"WAVE_SENT\" sau label=\"TRADE_SUCCESS\").\\n" +
                    "5. Condu fluxul catre nodul FINAL in caz de TRADE_SUCCESS.\\n" +
                    "LOG IZOLAT DE ANALIZAT:\\n" + agentTraceJson;

            System.out.println("\n[LLM Inference] Triggering LLM extraction on isolated dataset...");
            String fsmCode = LlmFsmExtractor.extractFsmFromJson(easyLogEnforcedPayload);

            if (fsmCode != null) {
                System.out.println("\n--- FINAL GENERATED FSM CODE (Graphviz DOT) ---\n");
                System.out.println(fsmCode);
                System.out.println("\n-----------------------------------------\n");

                LlmFsmExtractor.saveToFile(fsmCode, "protocol_generat.dot");

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
     * Gaseste primul agent care face o actiune si ii extrage exclusiv cronologia personala.
     * Acest lucru previne ca LLM-ul sa confunde actiunile a 5 agenti simultani drept "bucle" in protocol.
     */
    private static String isolateSingleAgentTrace(String flatJson) {
        String content = flatJson.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);

        String[] objects = content.split("\\},\\s*\\{");
        String targetAgent = null;

        // 1. Gasim primul agent prosumator care a trimis o oferta
        for (String obj : objects) {
            if (obj.contains("\"type\": \"WAVE_SENT\"") && !obj.contains("MarketAgent")) {
                targetAgent = extractJsonValue(obj, "entityName");
                break;
            }
        }

        if (targetAgent == null) return "[]";

        System.out.println("[Actor-Isolation] Modeling lifecycle strictly for agent: " + targetAgent);

        // 2. Extragem DOAR evenimentele acestui agent
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