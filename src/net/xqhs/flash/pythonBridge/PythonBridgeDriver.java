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
import net.xqhs.util.logging.Unit;

/**
 * Java-Python bridge, built on the client - flask server model:
 * <ul>
 * <li>it creates and manages its own Python virtual environment (never touches the system Python); </li>
 * <li>it detects missing packages reported by the server script and installs them into that venv automatically,
 * retrying until the server starts or a give-up limit is reached.</li>
 * </ul>
 */

public class PythonBridgeDriver extends Unit implements AsyncDriver {

    // ==================== defaults & configuration constants ====================

    /** Default base URL of the Flask server. */
    protected static final String	DEFAULT_SERVER_URL						= "http://localhost";
    /** Default port the Flask server listens on. Must match {@code SERVER_PORT} in bridge_server.py. */
    protected static final int		DEFAULT_SERVER_PORT						= 5099;
    /** Default server script location, relative to this class's package under the main source root. */
    protected static final String	DEFAULT_SERVER_SCRIPT_RELATIVE_PATH		= "python_module/bridge_server.py";
    /** Default venv location: at the project root. */
    protected static final String	DEFAULT_VENV_PATH						= "pythonBridge-venv";
    /** Endpoint used when a wave carries no destination element at all. */
    protected static final String	DEFAULT_ENDPOINT						= "call";
    /** Endpoint used to poll whether the server has finished starting up. */
    protected static final String	PING_ENDPOINT							= "ping";

    protected static final String	WINDOWS_VENV_BIN_DIR					= "Scripts";
    protected static final String	UNIX_VENV_BIN_DIR						= "bin";
    protected static final String	WINDOWS_PYTHON_EXECUTABLE				= "python.exe";
    protected static final String	UNIX_PYTHON_EXECUTABLE					= "python";
    protected static final String	WINDOWS_PIP_EXECUTABLE					= "pip.exe";
    protected static final String	UNIX_PIP_EXECUTABLE					    = "pip";
    protected static final String	WINDOWS_SYSTEM_PYTHON_COMMAND			= "python";
    protected static final String	UNIX_SYSTEM_PYTHON_COMMAND_FALLBACK	    = "python3";

    /**
     * Has to match python_module/util.py's import_functionality() on a failed import.
     */
    protected static final Pattern	MISSING_PACKAGE_PATTERN				= Pattern
            .compile("unavailable \\(use pip install ([\\w\\-.]+)");

    protected static final int		MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE	= 5;
    protected static final int		MAX_TOTAL_ATTEMPTS						= 100;
    /** How long to wait, per start attempt, for either a failure or a successful connection. */
    protected static final int		START_TIMEOUT_MS						= 10000;
    protected static final int		START_POLL_MS							= 300;
    /** How long to wait for `python -m venv` / `pip install` to finish. */
    protected static final int		VENV_CREATE_TIMEOUT_S					= 60;
    protected static final int		PIP_INSTALL_TIMEOUT_S					= 120;

    // ==================== instance state ====================

    protected final String			serverUrl;
    protected final int				serverPort;
    protected final Path			serverScriptPath;
    protected final Path			venvPath;

    protected final ExecutorService	executor								= Executors.newFixedThreadPool(4);
    protected volatile Process		serverProcess;
    protected volatile boolean		ready									= false;
    protected volatile String		systemPythonCommandCache;

    /**
     * @param serverUrl
     *            e.g. {@link #DEFAULT_SERVER_URL}
     * @param serverPort
     *            port the Flask server will listen on
     * @param serverScriptRelativePath
     *            path to the server script, relative to this class's package (e.g.
     *            {@link #DEFAULT_SERVER_SCRIPT_RELATIVE_PATH})
     * @param venvRelativePath
     *            where to create/reuse the venv, relative to the working directory the JVM was started from (e.g.
     *            {@link #DEFAULT_VENV_PATH})
     */
    public PythonBridgeDriver(String serverUrl, int serverPort, String serverScriptRelativePath,
                              String venvRelativePath) {
        setUnitName(getClass().getSimpleName());
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.serverScriptPath = Paths.get(
                DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN],
                getClass().getPackage().getName().replace('.', '/'), serverScriptRelativePath);
        this.venvPath = Paths.get(venvRelativePath);
    }

    /** Convenience constructor using all the defaults above. */
    public PythonBridgeDriver() {
        this(DEFAULT_SERVER_URL, DEFAULT_SERVER_PORT, DEFAULT_SERVER_SCRIPT_RELATIVE_PATH, DEFAULT_VENV_PATH);
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
                le("Server failed to start and reported no missing package; giving up.");
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
            li("Missing: [] (attempt []/[] on this package)", currentProblem,
                    Integer.valueOf(attemptsOnCurrentProblem), Integer.valueOf(MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE));

            if(attemptsOnCurrentProblem > MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE) {
                le("Giving up: '[]' still missing after [] install attempts.", currentProblem,
                        Integer.valueOf(MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE));
                return false;
            }

            for(String pkg : result.missingPackages) {
                li("Installing missing package into venv: []", pkg);
                pipInstall(pkg);
            }
        }
        le("Giving up after [] total attempts (safety net).", Integer.valueOf(MAX_TOTAL_ATTEMPTS));
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

    protected boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    protected Path venvBinDir() {
        return venvPath.resolve(isWindows() ? WINDOWS_VENV_BIN_DIR : UNIX_VENV_BIN_DIR);
    }

    protected String venvPythonExecutable() {
        return venvBinDir().resolve(isWindows() ? WINDOWS_PYTHON_EXECUTABLE : UNIX_PYTHON_EXECUTABLE).toString();
    }

    protected String venvPipExecutable() {
        return venvBinDir().resolve(isWindows() ? WINDOWS_PIP_EXECUTABLE : UNIX_PIP_EXECUTABLE).toString();
    }

    /**
     * The system Python command to use for creating the venv (not the venv's own python -- that doesn't exist
     * yet at this point). On Windows this is always "python". On Linux/Mac, some setups don't have a "python3"
     * binary at all and only have "python" already pointing to Python 3 (e.g. some distros, some conda setups),
     * while others have both. So: only fall back to "python3" if plain "python" isn't already Python 3 -- don't
     * assume "python3" exists just because this isn't Windows.
     */
    protected synchronized String systemPythonCommand() {
        if(systemPythonCommandCache != null)
            return systemPythonCommandCache;
        if(isWindows())
            return systemPythonCommandCache = WINDOWS_SYSTEM_PYTHON_COMMAND;
        systemPythonCommandCache = isPython3(UNIX_PYTHON_EXECUTABLE) ? UNIX_PYTHON_EXECUTABLE
                : UNIX_SYSTEM_PYTHON_COMMAND_FALLBACK;
        return systemPythonCommandCache;
    }

    /** Runs `<command> -c "..."` and checks whether it reports itself as Python 3. */
    protected boolean isPython3(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "-c",
                    "import sys; sys.exit(0 if sys.version_info[0] == 3 else 1)");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch(IOException | InterruptedException e) {
            return false;
        }
    }

    /** Creates the venv at {@link #venvPath} if it isn't already there. Never activates it through a shell. */
    protected boolean ensureVenv() {
        if(Files.isDirectory(venvBinDir())) {
            li("Reusing existing venv at []", venvPath);
            return true;
        }
        li("Creating venv at [] ...", venvPath);
        try {
            if(venvPath.getParent() != null)
                Files.createDirectories(venvPath.getParent());
            String pythonCmd = systemPythonCommand();
            li("Using system python command: []", pythonCmd);
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-m", "venv", venvPath.toString());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            boolean finished = p.waitFor(VENV_CREATE_TIMEOUT_S, TimeUnit.SECONDS);
            if(!finished) {
                p.destroyForcibly();
                le("Venv creation timed out.");
                return false;
            }
            if(p.exitValue() != 0) {
                le("Venv creation failed, exit code []", Integer.valueOf(p.exitValue()));
                return false;
            }
            return true;
        } catch(IOException | InterruptedException e) {
            le("Venv creation failed with exception: []", e);
            return false;
        }
    }

    /** Runs `<venv>/pip install <pkg>` directly -- no shell, no activation. */
    protected boolean pipInstall(String pkg) {
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
            le("Pip install failed for [] with exception: []", pkg, e);
            return false;
        }
    }

    // ==================== server startup ====================

    protected static class AttemptResult {
        protected boolean			started			= false;
        protected List<String>	    missingPackages	= new ArrayList<>();
    }

    /**
     * Starts the server script with the venv's python and watches its output. If the script exits quickly
     * reporting a missing package (see {@link #MISSING_PACKAGE_PATTERN}), that's recorded in the result so the
     * caller can install it and retry. If the process stays alive, polls the HTTP endpoint until it answers.
     */
    protected AttemptResult attemptStart() {
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
                        lf("[bridge_server] []", line);
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
            le("Server did not answer within []ms.", Integer.valueOf(START_TIMEOUT_MS));
            serverProcess.destroyForcibly();
            return result;
        } catch(IOException e) {
            le("Failed to start server process: []", e);
            return result;
        }
    }

    protected boolean testConnection() {
        try {
            URL url = new URL(serverUrl + ":" + serverPort + "/" + PING_ENDPOINT);
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
     * Sends every payload key/value pair from the wave as its own form field, to the endpoint named by the wave's first
     * destination element (defaulting to {@link #DEFAULT_ENDPOINT}), and wraps the response as the reply's
     * content. A key can carry more than one value, so each
     * value is sent as a separate "key=value" pair -- the Python side reads repeated keys with
     * {@code request.form.getlist(key)}.
     */
    protected AgentWave doProcess(AgentWave wave) throws Exception {
        if(!ready)
            throw new IllegalStateException("Python bridge server is not ready.");

        String[] destinations = wave.getDestinationElements();
        String endpoint = (destinations != null && destinations.length > 0) ? destinations[0] : DEFAULT_ENDPOINT;

        StringBuilder data = new StringBuilder();
        for(String key : wave.getContentElements()) {
            for(String value : wave.getValues(key)) {
                if(data.length() > 0)
                    data.append("&");
                data.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
            }
        }

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