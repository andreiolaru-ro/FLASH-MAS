package net.xqhs.flash.pythonBridge;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.ml.AsyncDriver;

/**
 * Java-Python bridge, built on the client - flask server model:
 * <ul>
 * <li>it creates and manages its own Python virtual environment (never touches the system Python); </li>
 * <li>it detects missing packages reported by the server script and installs them into that venv automatically,
 * retrying until the server starts or a give-up limit is reached.</li>
 * </ul>
 */
public class PythonBridgeDriver implements AsyncDriver {

    /**
     * Has to match python_module/util.py's import_functionality() on a failed import.
     */
    private static final Pattern	MISSING_PACKAGE_PATTERN	= Pattern
            .compile("unavailable \\(use pip install ([\\w\\-.]+)");


    private static final int		MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE	= 5;
    private static final int		MAX_TOTAL_ATTEMPTS						= 100;
    private static final int		START_TIMEOUT_MS		= 10000;
    private static final int		START_POLL_MS			= 300;

    /** How long to wait for `python -m venv` / `pip install` to finish. */
    private static final int		VENV_CREATE_TIMEOUT_S	= 60;
    private static final int		PIP_INSTALL_TIMEOUT_S	= 120;

    private final String			serverUrl;
    private final int				serverPort;
    private final Path				serverScriptPath;
    private final Path				venvPath;

    private final ExecutorService	executor				= Executors.newFixedThreadPool(4);
    private volatile Process		serverProcess;
    private volatile boolean		ready					= false;

    /**
     * @param serverUrl
     *            e.g. "http://localhost"
     * @param serverPort
     *            port the Flask server will listen on
     * @param serverScriptRelativePath
     *            path to the server script, relative to this class's package (e.g. "python_module/bridge_server.py")
     * @param venvRelativePath
     *            where to create/reuse the venv, relative to the working directory the JVM was started from (e.g.
     *            "src/net/xqhs/flash/pythonBridge/python_module/venv")
     */
    public PythonBridgeDriver(String serverUrl, int serverPort, String serverScriptRelativePath,
                              String venvRelativePath) {
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.serverScriptPath = Paths.get(
                DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN],
                PythonBridgeDriver.class.getPackage().getName().replace('.', '/'), serverScriptRelativePath);
        this.venvPath = Paths.get(venvRelativePath);
    }

    /** Convenience constructor using the bundled bridge_server.py, a local venv next to it, and port 5099. */
    public PythonBridgeDriver() {
        this("http://localhost", 5099, "python_module/bridge_server.py",
                "src/net/xqhs/flash/pythonBridge/python_module/venv");
    }

    /**
     * Creates the venv if it doesn't exist yet, then starts the Python server, automatically installing any
     * package it reports as missing and retrying. The retry budget is per distinct missing package (see
     * {@link #MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE}): as long as each attempt reports a *different* missing
     * package than the previous one, it keeps going -- it only gives up if the same package fails to become
     * available several times in a row.
     *
     * @return <code>true</code> if the server ended up running and reachable.
     */
    public boolean start() {
        if(!ensureVenv())
            return false;

        String lastProblem = null;
        int attemptsOnCurrentProblem = 0;

        for(int totalAttempts = 1; totalAttempts <= MAX_TOTAL_ATTEMPTS; totalAttempts++) {
            AttemptResult result = attemptStart();
            if(result.started) {
                ready = true;
                return true;
            }
            if(result.missingPackages.isEmpty()) {
                System.err.println(
                        "[PythonBridgeDriver] server failed to start and reported no missing package; giving up.");
                return false;
            }

            String currentProblem = String.join(",", result.missingPackages);
            if(currentProblem.equals(lastProblem)) {
                attemptsOnCurrentProblem++;
            } else {
                // different package than last time -> we made progress; the budget resets
                lastProblem = currentProblem;
                attemptsOnCurrentProblem = 1;
            }
            System.out.println("[PythonBridgeDriver] missing: " + currentProblem + " (attempt "
                    + attemptsOnCurrentProblem + "/" + MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE + " on this package)");

            if(attemptsOnCurrentProblem > MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE) {
                System.err.println("[PythonBridgeDriver] giving up: '" + currentProblem + "' still missing after "
                        + MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE + " install attempts.");
                return false;
            }

            for(String pkg : result.missingPackages) {
                System.out.println("[PythonBridgeDriver] installing missing package into venv: " + pkg);
                pipInstall(pkg);
            }
        }
        System.err.println("[PythonBridgeDriver] giving up after " + MAX_TOTAL_ATTEMPTS
                + " total attempts (safety net).");
        return false;
    }

    /** Stops the server process and shuts down the executor. Safe to call even if start() was never called. */
    public void stop() {
        ready = false;
        executor.shutdown();
        try {
            if(!executor.awaitTermination(5, TimeUnit.SECONDS))
                executor.shutdownNow();
        } catch(InterruptedException e) {
            executor.shutdownNow();
        }
        if(serverProcess != null) {
            serverProcess.destroy();
            serverProcess = null;
        }
    }

    public boolean isReady() {
        return ready;
    }

    // ==================== venv management ====================

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Path venvBinDir() {
        return venvPath.resolve(isWindows() ? "Scripts" : "bin");
    }

    private String venvPythonExecutable() {
        return venvBinDir().resolve(isWindows() ? "python.exe" : "python").toString();
    }

    private String venvPipExecutable() {
        return venvBinDir().resolve(isWindows() ? "pip.exe" : "pip").toString();
    }

    /** Creates the venv at {@link #venvPath} if it isn't already there. Never activates it through a shell. */
    private boolean ensureVenv() {
        if(Files.isDirectory(venvBinDir())) {
            System.out.println("[PythonBridgeDriver] reusing existing venv at " + venvPath);
            return true;
        }
        System.out.println("[PythonBridgeDriver] creating venv at " + venvPath + " ...");
        try {
            if(venvPath.getParent() != null)
                Files.createDirectories(venvPath.getParent());
            String pythonCmd = isWindows() ? "python" : "python3";
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-m", "venv", venvPath.toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            boolean finished = p.waitFor(VENV_CREATE_TIMEOUT_S, TimeUnit.SECONDS);
            if(!finished) {
                p.destroyForcibly();
                System.err.println("[PythonBridgeDriver] venv creation timed out.");
                return false;
            }
            if(p.exitValue() != 0) {
                System.err.println("[PythonBridgeDriver] venv creation failed, exit code " + p.exitValue());
                return false;
            }
            return true;
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Runs `<venv>/pip install <pkg>` directly -- no shell, no activation. */
    private boolean pipInstall(String pkg) {
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPipExecutable(), "install", pkg);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            boolean finished = p.waitFor(PIP_INSTALL_TIMEOUT_S, TimeUnit.SECONDS);
            if(!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== server startup ====================

    private static class AttemptResult {
        boolean			started			= false;
        List<String>	missingPackages	= new ArrayList<>();
    }

    /**
     * Starts the server script with the venv's python and watches its output. If the script exits quickly
     * reporting a missing package (see {@link #MISSING_PACKAGE_PATTERN}), that's recorded in the result so the
     * caller can install it and retry. If the process stays alive, polls the HTTP endpoint until it answers.
     */
    private AttemptResult attemptStart() {
        AttemptResult result = new AttemptResult();
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPythonExecutable(), serverScriptPath.toString());
            pb.redirectErrorStream(true);
            serverProcess = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
            Thread readerThread = new Thread(() -> {
                String line;
                try {
                    while((line = reader.readLine()) != null) {
                        System.out.println("[bridge_server] " + line);
                        Matcher m = MISSING_PACKAGE_PATTERN.matcher(line);
                        if(m.find())
                            result.missingPackages.add(m.group(1).trim());
                    }
                } catch(IOException ignored) {
                    // stream closes when the process ends; nothing to do
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            int waited = 0;
            while(waited < START_TIMEOUT_MS) {
                try {
                    Thread.sleep(START_POLL_MS);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                waited += START_POLL_MS;

                if(!serverProcess.isAlive()) {
                    // exited - give the reader thread a moment to flush the last lines it's reading
                    try {
                        readerThread.join(1000);
                    } catch(InterruptedException ignored) {
                        // continue
                    }
                    return result;
                }
                if(!result.missingPackages.isEmpty()) {
                    serverProcess.destroyForcibly();
                    return result;
                }
                if(testConnection()) {
                    result.started = true;
                    return result;
                }
            }
            System.err.println("[PythonBridgeDriver] server did not answer within " + START_TIMEOUT_MS + "ms.");
            serverProcess.destroyForcibly();
            return result;
        } catch(IOException e) {
            e.printStackTrace();
            return result;
        }
    }

    private boolean testConnection() {
        try {
            URL url = new URL(serverUrl + ":" + serverPort + "/ping");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 200;
        } catch(Exception e) {
            return false;
        }
    }

    // ==================== AsyncDriver ====================

    @Override
    public void processAsync(AgentWave wave, WaveReceiver callback) {
        executor.submit(() -> {
            try {
                callback.receive(doProcess(wave));
            } catch(Exception e) {
                callback.receive(wave.createReply("ERROR: " + e.getMessage()));
            }
        });
    }

    @Override
    public AgentWave process(AgentWave wave) {
        try {
            return doProcess(wave);
        } catch(Exception e) {
            return wave.createReply("ERROR: " + e.getMessage());
        }
    }

    /**
     * Sends the wave's content as the "input" form field to the endpoint named by the wave's first destination
     * element (defaulting to "call"), and wraps the response as the reply's content.
     */
    private AgentWave doProcess(AgentWave wave) throws Exception {
        if(!ready)
            throw new IllegalStateException("Python bridge server is not ready.");

        String[] destinations = wave.getDestinationElements();
        String endpoint = (destinations != null && destinations.length > 0) ? destinations[0] : "call";

        StringBuilder data = new StringBuilder();
        String content = wave.getContent();
        if(content != null)
            data.append("input=").append(URLEncoder.encode(content, "UTF-8"));

        URL url = new URL(serverUrl + ":" + serverPort + "/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.writeBytes(data.toString());
        }

        boolean isError = connection.getResponseCode() >= 400;
        String response;
        try(BufferedReader in = new BufferedReader(
                new InputStreamReader(isError ? connection.getErrorStream() : connection.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = in.readLine()) != null)
                sb.append(line);
            response = sb.toString();
        }
        if(isError)
            throw new Exception("HTTP " + connection.getResponseCode() + ": " + response);

        return wave.createReply(response);
    }
}