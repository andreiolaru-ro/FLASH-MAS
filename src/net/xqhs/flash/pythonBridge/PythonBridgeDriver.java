package net.xqhs.flash.pythonBridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.ml.AsyncDriver;
import net.xqhs.util.logging.Unit;

/**
 * Base for all Java-Python bridges in this package: everything that is independent of <i>how</i> Java actually
 * talks to Python lives here, so that each concrete transport only has to implement its own calling mechanism
 * and can be used interchangeably through {@link AsyncDriver}.
 */
public abstract class PythonBridgeDriver extends Unit implements AsyncDriver {

    // ==================== defaults & configuration constants ====================

    /**
     * Default venv location: a folder at the project root (sibling to {@code src/}), shared by all transports, so
     * packages installed for one are already there for the others.
     */
    protected static final String	DEFAULT_VENV_PATH						= "pythonBridge-venv";

    protected static final String	WINDOWS_VENV_BIN_DIR					= "Scripts";
    protected static final String	UNIX_VENV_BIN_DIR						= "bin";
    protected static final String	WINDOWS_PYTHON_EXECUTABLE				= "python.exe";
    protected static final String	UNIX_PYTHON_EXECUTABLE					= "python";
    protected static final String	WINDOWS_PIP_EXECUTABLE					= "pip.exe";
    protected static final String	UNIX_PIP_EXECUTABLE						= "pip";
    protected static final String	WINDOWS_SYSTEM_PYTHON_COMMAND			= "python";
    protected static final String	UNIX_SYSTEM_PYTHON_COMMAND_FALLBACK		= "python3";

    /**
     Has to match python_module/util.py's import_functionality() on a failed import.
     */
    protected static final Pattern	MISSING_PACKAGE_PATTERN					= Pattern
            .compile("unavailable \\(use pip install ([\\w\\-.]+)");


    protected static final int		MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE	= 5;
    protected static final int		MAX_TOTAL_ATTEMPTS						= 100;
    /** How long to wait, per start attempt, for either a failure or a successful connection. */
    protected static final int		START_TIMEOUT_MS						= 15000;
    protected static final int		START_POLL_MS							= 300;
    /** How long to wait for `python -m venv` / `pip install` / a script run to completion to finish. */
    protected static final int		VENV_CREATE_TIMEOUT_S					= 60;
    protected static final int		PIP_INSTALL_TIMEOUT_S					= 300;
    protected static final int		SCRIPT_RUN_TIMEOUT_S					= 60;

    // ==================== instance state ====================

    protected final Path			venvPath;
    protected final ExecutorService	executor								= Executors.newFixedThreadPool(4);
    protected volatile Process		serverProcess;
    protected volatile boolean		ready									= false;
    protected volatile String		systemPythonCommandCache;

    protected PythonBridgeDriver(String venvRelativePath) {
        setUnitName(getClass().getSimpleName());
        this.venvPath = Paths.get(venvRelativePath);
    }

    protected PythonBridgeDriver() {
        this(DEFAULT_VENV_PATH);
    }

    // ==================== lifecycle ====================

    /**
     * Creates the venv if needed, then brings the Python side up via {@link #attemptStart()}, installing any
     * package reported missing and retrying (see {@link #MAX_CONSECUTIVE_ATTEMPTS_PER_PACKAGE}).
     *
     * @return <code>true</code> if the Python side ended up usable.
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
                le("Python side failed to start and reported no missing package; giving up.");
                return false;
            }

            String currentProblem = String.join(",", result.missingPackages);
            if(currentProblem.equals(lastProblem))
                attemptsOnCurrentProblem++;
            else { // different package than last time -> we made progress; the budget resets
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

    /** Shuts down the executor and hands over to {@link #stopPython()} for transport-specific cleanup. */
    public void stop() {
        ready = false;
        executor.shutdown();
        try {
            if(!executor.awaitTermination(5, TimeUnit.SECONDS))
                executor.shutdownNow();
        } catch(InterruptedException e) {
            executor.shutdownNow();
        }
        stopPython();
        if(serverProcess != null) {
            serverProcess.destroy();
            serverProcess = null;
        }
    }

    public boolean isReady() {
        return ready;
    }

    /** One attempt at bringing the Python side up. Reports missing packages so the caller can install them. */
    protected abstract AttemptResult attemptStart();

    /** Transport-specific cleanup. The base class already stops {@link #serverProcess} and the executor. */
    protected abstract void stopPython();

    /** The actual call into Python. Everything else in this class exists to make this method possible. */
    protected abstract AgentWave doProcess(AgentWave wave) throws Exception;

    // ==================== AsyncDriver ====================

    @Override
    public AgentWave process(AgentWave wave) {
        try {
            return doProcess(wave);
        } catch(Exception e) {
            return wave.createReply(describeError(e));
        }
    }

    @Override
    public void processAsync(AgentWave wave, WaveReceiver callback) {
        executor.submit(() -> {
            try {
                callback.receive(doProcess(wave));
            } catch(Exception e) {
                callback.receive(wave.createReply(describeError(e)));
            }
        });
    }


    protected String describeError(Throwable error) {
        Throwable cause = (error instanceof java.util.concurrent.ExecutionException && error.getCause() != null)
                ? error.getCause() : error;
        String message = cause.getMessage();
        return "ERROR: " + (message != null && !message.isEmpty() ? message : cause.getClass().getSimpleName());
    }


    protected void checkReady() {
        if(!ready)
            throw new IllegalStateException("Python bridge is not ready.");
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

    /** Absolute path of a file in this package's python_module directory. */
    protected Path pythonModuleFile(String fileName) {
        return Paths.get(DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN],
                PythonBridgeDriver.class.getPackage().getName().replace('.', '/'), "python_module", fileName);
    }

    /**
     * The system Python command to use for creating the venv (not the venv's own python -- that doesn't exist
     * yet at this point). On Windows this is always "python". On Linux/Mac, some setups don't have a "python3"
     * binary at all and only have "python" already pointing to Python 3, while others have both. So: only fall
     * back to "python3" if plain "python" isn't already Python 3 -- don't assume "python3" exists.
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
            return false; // command not found, or some other problem running it -- treat as "not usable"
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
            if(!p.waitFor(VENV_CREATE_TIMEOUT_S, TimeUnit.SECONDS)) {
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

    /**
     * Runs `<venv>/pip install <pkg>` directly -- no shell, no activation. JAVA_HOME is passed through from the
     * running JVM because some packages (notably jep, used by {@link PythonJepBridgeDriver}) compile against the
     * JDK and won't build without it.
     */
    protected boolean pipInstall(String pkg) {
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPipExecutable(), "install", pkg);
            pb.environment().put("JAVA_HOME", System.getProperty("java.home", ""));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            if(!p.waitFor(PIP_INSTALL_TIMEOUT_S, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch(IOException | InterruptedException e) {
            le("Pip install failed for [] with exception: []", pkg, e);
            return false;
        }
    }

    /** Asks the venv's python for a value, e.g. a sysconfig path. Returns <code>null</code> on any failure. */
    protected String queryVenvPython(String pythonExpression) {
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPythonExecutable(), "-c", pythonExpression);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out;
            try(BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = in.readLine()) != null)
                    sb.append(line);
                out = sb.toString().trim();
            }
            if(!p.waitFor(30, TimeUnit.SECONDS) || p.exitValue() != 0)
                return null;
            return out.isEmpty() ? null : out;
        } catch(IOException | InterruptedException e) {
            return null;
        }
    }

    // ==================== running python scripts with missing-package detection ====================

    /** Outcome of one start attempt: either it worked, or these packages need installing first. */
    protected static class AttemptResult {
        protected boolean		started			= false;
        protected List<String>	missingPackages	= new ArrayList<>();
    }

    /**
     * Starts a long-running Python script (a server) with the venv's python and watches its output. If the
     * script exits early reporting a missing package, that's recorded so the caller can install it and retry.
     * While the process stays alive, {@code readinessCheck} is polled until it passes.
     */
    protected AttemptResult runServerScript(Path script, BooleanSupplier readinessCheck) {
        AttemptResult result = new AttemptResult();
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPythonExecutable(), script.toString());
            pb.redirectErrorStream(true);
            serverProcess = pb.start();
            Thread readerThread = watchOutput(serverProcess, result);

            int waited = 0;
            while(waited < START_TIMEOUT_MS) {
                try {
                    Thread.sleep(START_POLL_MS);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                waited += START_POLL_MS;

                if(!serverProcess.isAlive()) { // exited -- let the reader flush the last lines
                    try {
                        readerThread.join(1000);
                    } catch(InterruptedException ignored) {
                        // nothing to do
                    }
                    return result;
                }
                if(!result.missingPackages.isEmpty()) {
                    serverProcess.destroyForcibly();
                    return result;
                }
                if(readinessCheck.getAsBoolean()) {
                    result.started = true;
                    return result;
                }
            }
            le("Python server did not become ready within []ms.", Integer.valueOf(START_TIMEOUT_MS));
            serverProcess.destroyForcibly();
            return result;
        } catch(IOException e) {
            le("Failed to start python script []: []", script, e);
            return result;
        }
    }

    /**
     * Runs a Python script that is expected to terminate (used as an import probe: it exits 0 when every
     * required package is importable, and exits 1 after printing the parseable line when one is missing).
     */
    protected AttemptResult runScriptToCompletion(Path script) {
        AttemptResult result = new AttemptResult();
        try {
            ProcessBuilder pb = new ProcessBuilder(venvPythonExecutable(), script.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            Thread readerThread = watchOutput(p, result);
            if(!p.waitFor(SCRIPT_RUN_TIMEOUT_S, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                le("Python script [] timed out.", script);
                return result;
            }
            try {
                readerThread.join(1000);
            } catch(InterruptedException ignored) {
                // nothing to do
            }
            result.started = p.exitValue() == 0 && result.missingPackages.isEmpty();
            return result;
        } catch(IOException | InterruptedException e) {
            le("Failed to run python script []: []", script, e);
            return result;
        }
    }

    /** Pumps a process's merged output into the log, recording any missing package it reports. */
    protected Thread watchOutput(Process process, AttemptResult result) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread readerThread = new Thread(() -> {
            String line;
            try {
                while((line = reader.readLine()) != null) {
                    lf("[python] []", line);
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
        return readerThread;
    }
}