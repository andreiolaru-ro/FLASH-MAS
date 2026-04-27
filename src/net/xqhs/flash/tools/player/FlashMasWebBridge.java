package net.xqhs.flash.tools.player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.*;
import net.xqhs.flash.core.recorder.SimulationEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * FlashMasWebBridge - The HTTP API and Static File Server for the Hybrid Control Center.
 * <p>
 * This class acts as the bridge between the React/VanillaJS frontend and the Java Multi-Agent backend.
 * It forces the global recording system into 'Web Mode' and manages the simulation lifecycle.
 * </p>
 */
public class FlashMasWebBridge {

    /** Global flag indicating if the simulation is currently paused. Live agents should respect this. */
    public static volatile boolean IS_PAUSED = false;

    private static final Gson gson = new Gson();
    private static MultiAgentReplayer activeReplayer = null;
    private static List<SimulationEvent> globalEventsCache = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Force the Flash-MAS RecorderService to stream data via WebRecorder instead of saving to disk
        System.setProperty("flash.recorder.enabled", "true");
        System.setProperty("flash.recorder.file_mode", "false");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/launch", new LaunchHandler());
        server.createContext("/status", new StatusHandler());
        server.createContext("/control", new ControlHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        System.out.println("=================================================");
        System.out.println("[WebBridge] Hybrid Control Center Online");
        System.out.println("[WebBridge] Listening on: http://localhost:8080");
        System.out.println("=================================================");

        server.start();
    }

    /**
     * Handles serving the Single Page Application (index.html).
     * Employs robust classpath and filesystem scanning to locate the file.
     */
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream is = null;
            byte[] content = null;

            // 1. Try loading from the same package as this class (Classpath)
            is = FlashMasWebBridge.class.getResourceAsStream("index.html");

            // 2. Try loading from the root of the resources folder (Classpath)
            if (is == null) {
                is = FlashMasWebBridge.class.getResourceAsStream("/index.html");
            }

            // Read from InputStream if found
            if (is != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) bos.write(buffer, 0, len);
                content = bos.toByteArray();
                is.close();
            } else {
                // 3. Fallback: Try loading directly from the working directory on disk
                File f = new File("index.html");
                if (f.exists()) {
                    content = Files.readAllBytes(f.toPath());
                }
            }

            if (content == null) {
                String error = "ERROR: 'index.html' could not be found in the classpath or root directory.";
                System.err.println("[WebBridge] " + error);
                t.sendResponseHeaders(404, error.length());
                t.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                t.close();
                return;
            }

            t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            t.sendResponseHeaders(200, content.length);
            t.getResponseBody().write(content);
            t.close();
        }
    }

    /**
     * Handles POST requests containing the historical JSON log and launch configurations.
     */
    static class LaunchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                Map<String, Object> payload = gson.fromJson(new BufferedReader(isr), new TypeToken<Map<String, Object>>(){}.getType());

                List<Map<String, Object>> rawEvents = (List<Map<String, Object>>) payload.get("logData");
                Map<String, String> modes = (Map<String, String>) payload.get("agentModes");
                Map<String, String> registry = (Map<String, String>) payload.get("agentRegistry");
                int startIdx = ((Double) payload.getOrDefault("startIndex", 0.0)).intValue();

                globalEventsCache.clear();
                for (Map<String, Object> r : rawEvents) {
                    globalEventsCache.add(gson.fromJson(gson.toJson(r), SimulationEvent.class));
                }

                // Cleanly destroy any previous simulation run before starting a new one
                if (activeReplayer != null) {
                    System.out.println("[WebBridge] Terminating previous simulation instance...");
                    activeReplayer.stop();
                    activeReplayer = null;
                    IS_PAUSED = false;
                    Thread.sleep(500); // Allow JVM to free resources
                }

                activeReplayer = new MultiAgentReplayer(globalEventsCache, startIdx, modes, registry);
                new Thread(activeReplayer, "HybridReplayer-Thread").start();

                exchange.sendResponseHeaders(200, 0);
            } catch (Exception e) {
                System.err.println("[WebBridge] Failed to launch simulation: " + e.getMessage());
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Exposes current playback status and streams live events back to the UI.
     */
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> status = new HashMap<>();
            if (activeReplayer != null) {
                status.put("currentIndex", activeReplayer.getCurrentIndex());
                status.put("isPlaying", activeReplayer.isPlaying());
                status.put("liveEvents", activeReplayer.consumeLiveEvents());
            }
            String resp = gson.toJson(status);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Handles playback controls (Play, Pause, Speed) and Dynamic Agent Hot-Swapping.
     */
    static class ControlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (activeReplayer != null && query != null) {
                if (query.contains("command=play")) {
                    IS_PAUSED = false;
                    activeReplayer.resume();
                }
                else if (query.contains("command=pause")) {
                    IS_PAUSED = true;
                    activeReplayer.pause();
                }
                else if (query.contains("command=speed")) {
                    String val = query.split("value=")[1].split("&")[0];
                    activeReplayer.setTimeScale(Double.parseDouble(val));
                }
                else if (query.contains("command=promote") || query.contains("command=demote")) {
                    String name = query.split("agentName=")[1].split("&")[0];
                    String newMode = query.contains("promote") ? "live" : "mocked";

                    int currentIdx = activeReplayer.getCurrentIndex();
                    Map<String, String> currentModes = new HashMap<>(activeReplayer.getAgentModes());
                    Map<String, String> registry = activeReplayer.getAgentRegistry();

                    currentModes.put(name, newMode);

                    // Stop current engine to prepare for hot-swap
                    IS_PAUSED = false;
                    activeReplayer.stop();

                    System.out.println("[WebBridge] Hot-Swapping " + name + " to " + newMode.toUpperCase());
                    activeReplayer = new MultiAgentReplayer(globalEventsCache, currentIdx, currentModes, registry);
                    new Thread(activeReplayer, "HybridReplayer-Thread").start();
                }
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }
}