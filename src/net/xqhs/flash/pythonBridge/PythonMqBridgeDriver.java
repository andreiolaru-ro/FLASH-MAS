package net.xqhs.flash.pythonBridge;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;

/**
 * Bridge that submits work to a Python service and receives results <i>out of band</i>, over a Redis stream.
 * <p>
 * This is the transport whose shape genuinely differs from the other two. Submitting a job returns as soon as
 * the service has accepted it; the Python worker runs on its own thread pool and publishes the result to
 * {@code results:<agentId>} whenever it finishes. A listener thread here reads that stream and matches each
 * result back to its job. So:
 * <ul>
 * <li>{@link #processAsync(AgentWave, WaveReceiver)} does not park a pool thread waiting for a reply -- it
 * registers the callback under the job id and returns immediately, and the listener fires the callback when
 * the result actually arrives. Several jobs can be in flight at once with the same callback.</li>
 * <li>{@link #process(AgentWave)} keeps the synchronous contract by submitting and then waiting for that one
 * job's result, so callers that want a blocking call still get one.</li>
 * </ul>
 * Redis is handled automatically: if one is already reachable it is reused untouched, otherwise the driver
 * brings one up with docker compose (see {@code python_module/docker-compose.yml}) and takes it down again on
 * {@link #stop()}. Only a Redis this driver started is ever stopped. Pass {@code false} to
 * {@link #PythonMqBridgeDriver(boolean)} to opt out and manage Redis yourself.
 */
public class PythonMqBridgeDriver extends PythonBridgeDriver {

    protected static final String	DEFAULT_SERVICE_URL		= "http://localhost";
    protected static final int		DEFAULT_SERVICE_PORT	= 8000;
    protected static final String	DEFAULT_SERVICE_SCRIPT	= "mq_service.py";
    protected static final String	DEFAULT_REDIS_HOST		= "localhost";
    protected static final int		DEFAULT_REDIS_PORT		= 6379;
    /** Stream keys are per agent, so several agents can share one Python service without crosstalk. */
    protected static final String	DEFAULT_AGENT_ID		= "flash-agent";
    protected static final String	RESULT_STREAM_PREFIX	= "results:";
    protected static final String	SUBMIT_ENDPOINT			= "infer";
    protected static final String	HEALTH_ENDPOINT			= "health";
    protected static final int		XREAD_BLOCK_MS			= 5000;
    protected static final int		SYNC_WAIT_TIMEOUT_S		= 120;
    protected static final int		CONNECTION_TEST_TIMEOUT_MS = 500;

    /** Compose file shipped next to the Python sources, used to bring up Redis when none is running. */
    protected static final String	DEFAULT_COMPOSE_FILE	= "docker-compose.yml";
    /**
     * Explicit project name, so that {@code down} takes away exactly what {@code up} brought -- otherwise the
     * project name is derived from the compose file's directory and is easy to get wrong from another cwd.
     */
    protected static final String	COMPOSE_PROJECT_NAME	= "flash-pythonbridge";
    protected static final int		DOCKER_COMMAND_TIMEOUT_S	= 120;
    /** How long to wait for Redis to accept connections after `compose up` returns. */
    protected static final int		REDIS_WAIT_TIMEOUT_MS	= 30000;
    protected static final int		REDIS_WAIT_POLL_MS		= 500;

    protected final String			serviceUrl;
    protected final int				servicePort;
    protected final String			serviceScript;
    protected final String			redisHost;
    protected final int				redisPort;
    protected final String			agentId;
    /** When false, the driver never touches docker and expects Redis to be running already. */
    protected final boolean			manageRedisWithDocker;
    /** Set only when this driver actually started Redis. We don't stop Redis unless we started it. */
    protected volatile boolean		startedRedisOurselves;
    /** Resolved once: "docker compose" (v2 plugin) or "docker-compose" (v1 standalone), whichever exists. */
    protected volatile List<String>	composeCommand;

    /** Jobs submitted and not yet answered, keyed by job id. */
    protected final Map<String, CompletableFuture<String>>	pending	= new ConcurrentHashMap<>();
    protected volatile Thread								listenerThread;
    protected volatile boolean								listening;

    public PythonMqBridgeDriver(String serviceUrl, int servicePort, String serviceScript, String redisHost,
                                int redisPort, String agentId, boolean manageRedisWithDocker, String venvRelativePath) {
        super(venvRelativePath);
        this.serviceUrl = serviceUrl;
        this.servicePort = servicePort;
        this.serviceScript = serviceScript;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.agentId = agentId;
        this.manageRedisWithDocker = manageRedisWithDocker;
    }

    /**
     * @param manageRedisWithDocker
     *            when false, docker is never invoked and a Redis must already be reachable.
     */
    public PythonMqBridgeDriver(boolean manageRedisWithDocker) {
        this(DEFAULT_SERVICE_URL, DEFAULT_SERVICE_PORT, DEFAULT_SERVICE_SCRIPT, DEFAULT_REDIS_HOST, DEFAULT_REDIS_PORT,
                DEFAULT_AGENT_ID, manageRedisWithDocker, DEFAULT_VENV_PATH);
    }

    /** Defaults, with Redis started through docker compose if none is already running. */
    public PythonMqBridgeDriver() {
        this(true);
    }


    protected String resultStream() {
        return RESULT_STREAM_PREFIX + agentId;
    }

    @Override
    protected AttemptResult attemptStart() {
        AttemptResult result = runServerScript(pythonModuleFile(serviceScript), this::testConnection);
        if(!result.started)
            return result;
        if(!ensureRedis()) {
            result.started = false;
            return result;
        }
        startListener();
        return result;
    }

    /**
     * Makes sure a Redis is reachable, starting one with docker compose if not. An already-running Redis is
     * reused as is and is never taken down later -- only a Redis this driver started is stopped by
     * {@link #stopPython()}.
     */
    protected boolean ensureRedis() {
        if(redisReachable()) {
            li("Redis already running at []:[]; reusing it (it will be left running).", redisHost,
                    Integer.valueOf(redisPort));
            return true;
        }
        if(!manageRedisWithDocker) {
            le("Redis is not reachable at []:[] and docker management is off. Start one and try again.", redisHost,
                    Integer.valueOf(redisPort));
            return false;
        }
        List<String> compose = composeCommand();
        if(compose == null) {
            le("Redis is not reachable at []:[], and neither `docker compose` nor `docker-compose` is available. "
                            + "Start Redis yourself (docker, WSL, or a local redis-server) and try again.", redisHost,
                    Integer.valueOf(redisPort));
            return false;
        }
        li("No Redis running; bringing one up with []", String.join(" ", compose));
        if(!runComposeCommand(compose, "up", "-d"))
            return false;
        if(!waitForRedis()) {
            le("Redis container started but did not accept connections within []ms.",
                    Integer.valueOf(REDIS_WAIT_TIMEOUT_MS));
            return false;
        }
        startedRedisOurselves = true;
        li("Redis is up (started by this driver; it will be stopped on shutdown).");
        return true;
    }

    /** Detects which compose command exists: v2 plugin (`docker compose`) or v1 standalone (`docker-compose`). */
    protected synchronized List<String> composeCommand() {
        if(composeCommand != null)
            return composeCommand.isEmpty() ? null : composeCommand;
        if(commandWorks("docker", "compose", "version"))
            composeCommand = Arrays.asList("docker", "compose");
        else if(commandWorks("docker-compose", "version"))
            composeCommand = Arrays.asList("docker-compose");
        else
            composeCommand = Collections.emptyList();
        return composeCommand.isEmpty() ? null : composeCommand;
    }

    protected boolean commandWorks(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            if(!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch(IOException | InterruptedException e) {
            return false; // not installed, or the docker daemon isn't running
        }
    }

    protected boolean runComposeCommand(List<String> compose, String... arguments) {
        List<String> command = new ArrayList<>(compose);
        command.add("-p");
        command.add(COMPOSE_PROJECT_NAME);
        command.add("-f");
        command.add(pythonModuleFile(DEFAULT_COMPOSE_FILE).toAbsolutePath().toString());
        command.addAll(Arrays.asList(arguments));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            if(!p.waitFor(DOCKER_COMMAND_TIMEOUT_S, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                le("Compose command timed out: []", String.join(" ", command));
                return false;
            }
            if(p.exitValue() != 0) {
                le("Compose command failed (exit []): []", Integer.valueOf(p.exitValue()), String.join(" ", command));
                return false;
            }
            return true;
        } catch(IOException | InterruptedException e) {
            le("Compose command failed: []", e);
            return false;
        }
    }

    protected boolean waitForRedis() {
        int waited = 0;
        while(waited < REDIS_WAIT_TIMEOUT_MS) {
            if(redisReachable())
                return true;
            try {
                Thread.sleep(REDIS_WAIT_POLL_MS);
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            waited += REDIS_WAIT_POLL_MS;
        }
        return false;
    }

    protected boolean redisReachable() {
        try(RedisStreamClient probe = new RedisStreamClient(redisHost, redisPort, 1000)) {
            return probe.ping();
        } catch(Exception e) {
            return false;
        }
    }

    protected boolean testConnection() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    serviceUrl + ":" + servicePort + "/" + HEALTH_ENDPOINT).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TEST_TIMEOUT_MS);
            connection.setReadTimeout(CONNECTION_TEST_TIMEOUT_MS);
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 200;
        } catch(Exception e) {
            return false;
        }
    }

    /**
     * Reads the result stream in a loop, completing whichever job each entry belongs to. Starting from
     * {@code $} means only results published from now on are seen, so leftovers from an earlier run are
     * ignored.
     */
    protected void startListener() {
        listening = true;
        listenerThread = new Thread(() -> {
            String lastId = "$";
            try(RedisStreamClient client = new RedisStreamClient(redisHost, redisPort, XREAD_BLOCK_MS)) {
                li("Listening on stream []", resultStream());
                while(listening && !Thread.currentThread().isInterrupted()) {
                    List<RedisStreamClient.StreamEntry> entries = client.xread(resultStream(), lastId,
                            XREAD_BLOCK_MS);
                    for(RedisStreamClient.StreamEntry entry : entries) {
                        lastId = entry.id;
                        String jobId = entry.fields.get("job_id");
                        String value = entry.fields.get("result");
                        lf("Result for job [] : []", jobId, value);
                        CompletableFuture<String> future = pending.remove(jobId);
                        if(future != null)
                            future.complete(value);
                        else
                            lf("No pending job for id [] (ignored).", jobId);
                    }
                }
            } catch(Exception e) {
                if(listening)
                    le("Result listener stopped: []", e);
            }
        }, getClass().getSimpleName() + "-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    protected void stopPython() {
        listening = false;
        if(listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        for(CompletableFuture<String> future : pending.values())
            future.completeExceptionally(new IllegalStateException("Bridge stopped before the result arrived."));
        pending.clear();
        if(startedRedisOurselves) {
            List<String> compose = composeCommand();
            if(compose != null) {
                li("Stopping the Redis container this driver started.");
                runComposeCommand(compose, "down");
            }
            startedRedisOurselves = false;
        }
    }

    protected String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Submits and then waits for this job's result to come back on the stream. */
    @Override
    protected AgentWave doProcess(AgentWave wave) throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        String jobId = registerAndSubmit(wave, future);
        try {
            return wave.createReply(future.get(SYNC_WAIT_TIMEOUT_S, TimeUnit.SECONDS));
        } catch(Exception e) {
            pending.remove(jobId);
            throw e;
        }
    }

    /**
     * The message-queue version of async: submit, register the callback under the job id, return at once. No
     * thread is parked waiting -- the listener fires the callback when the result is published, so many jobs
     * can be outstanding at the same time, sharing one callback if the caller wants.
     */
    @Override
    public void processAsync(AgentWave wave, WaveReceiver callback) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.whenComplete((value, error) -> callback
                .receive(wave.createReply(error == null ? value : describeError(error))));
        try {
            registerAndSubmit(wave, future);
        } catch(Exception e) {
            future.completeExceptionally(e);
        }
    }

    protected String registerAndSubmit(AgentWave wave, CompletableFuture<String> future) throws Exception {
        checkReady();
        String jobId = UUID.randomUUID().toString();
        pending.put(jobId, future);
        try {
            submitWithId(wave, jobId);
        } catch(Exception e) {
            pending.remove(jobId);
            throw e;
        }
        return jobId;
    }


    protected void submitWithId(AgentWave wave, String jobId) throws Exception {
        StringBuilder payload = new StringBuilder();
        for(String key : wave.getContentElements())
            for(String value : wave.getValues(key)) {
                if(payload.length() > 0)
                    payload.append(",");
                payload.append(quote(key)).append(":").append(quote(value));
            }
        String body = "{" + quote("agent_id") + ":" + quote(agentId) + "," + quote("job_id") + ":" + quote(jobId)
                + "," + quote("payload") + ":{" + payload + "}}";

        HttpURLConnection connection = (HttpURLConnection) new URL(
                serviceUrl + ":" + servicePort + "/" + SUBMIT_ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.write(body.getBytes("UTF-8"));
        }
        if(connection.getResponseCode() != 200)
            throw new Exception("Failed to submit job: HTTP " + connection.getResponseCode());
        connection.disconnect();
    }
}