package net.xqhs.flash.pythonBridge;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * Bridge that talks to a local Flask server over HTTP, started as a child process from the managed venv.
 */
public class PythonHttpBridgeDriver extends PythonBridgeDriver {

    protected static final String	DEFAULT_SERVER_URL				= "http://localhost";
    protected static final int		DEFAULT_SERVER_PORT				= 5099;
    /** Must match {@code SERVER_PORT} in bridge_server.py. */
    protected static final String	DEFAULT_SERVER_SCRIPT			= "bridge_server.py";
    /** Endpoint used when a wave carries no destination element at all. */
    protected static final String	DEFAULT_ENDPOINT				= "call";
    /** Endpoint used to poll whether the server has finished starting up. */
    protected static final String	PING_ENDPOINT					= "ping";
    /** GET endpoint exposed by bridge_server.py, used to exercise {@link #get(String)}. */
    protected static final String	LIST_ENDPOINT					= "list";
    protected static final int		CONNECTION_TEST_TIMEOUT_MS		= 500;

    protected final String			serverUrl;
    protected final int				serverPort;
    protected final String			serverScript;

    public PythonHttpBridgeDriver(String serverUrl, int serverPort, String serverScript, String venvRelativePath) {
        super(venvRelativePath);
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.serverScript = serverScript;
    }

    public PythonHttpBridgeDriver() {
        this(DEFAULT_SERVER_URL, DEFAULT_SERVER_PORT, DEFAULT_SERVER_SCRIPT, DEFAULT_VENV_PATH);
    }

    @Override
    protected AttemptResult attemptStart() {
        return runServerScript(pythonModuleFile(serverScript), this::testConnection);
    }

    @Override
    protected void stopPython() {
        // the base class destroys serverProcess; nothing else to release
    }

    protected boolean testConnection() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    serverUrl + ":" + serverPort + "/" + PING_ENDPOINT).openConnection();
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
     * Sends every payload key/value pair from the wave as its own form field, to the endpoint named by the wave's first
     * destination element (defaulting to {@link #DEFAULT_ENDPOINT}), and wraps the response as the reply's
     * content. A key can carry more than one value, so each
     * value is sent as a separate "key=value" pair -- the Python side reads repeated keys with
     * {@code request.form.getlist(key)}.
     */
    @Override
    protected AgentWave doProcess(AgentWave wave) throws Exception {
        checkReady();
        String[] destinations = wave.getDestinationElements();
        String endpoint = (destinations != null && destinations.length > 0) ? destinations[0] : DEFAULT_ENDPOINT;

        StringBuilder data = new StringBuilder();
        for(String key : wave.getContentElements())
            for(String value : wave.getValues(key))
                appendParam(data, key, value);

        return wave.createReply(sendPost(endpoint, data.toString()));
    }

    /**
     * Plain HTTP GET call to a named endpoint. Modeled
     * after {@code PythonHTTPInterface.getModels()}, but made generic.
     *
     * @param endpoint
     *            the Python-side route to call, e.g. {@link #LIST_ENDPOINT}
     * @return the raw response body
     */
    public String get(String endpoint) throws Exception {
        checkReady();
        HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl + ":" + serverPort + "/" + endpoint)
                .openConnection();
        connection.setRequestMethod("GET");
        return readResponse(connection);
    }

    /**
     * Plain HTTP POST call to a named endpoint with an explicit, single-valued parameter map, bypassing
     * {@link AgentWave} entirely. Modeled after {@code PythonHTTPInterface.predict(...)} / its private
     * {@code sendPostRequest()} helper from the python-bridge branch, generalized here to any endpoint/params
     * instead of a fixed "model_name"/"input_data" pair. Unlike {@link #process(AgentWave)}, a key can only
     * carry one value here -- use the wave-based methods if a key needs several.
     *
     * @param endpoint
     *            the Python-side route to call, e.g. {@link #DEFAULT_ENDPOINT}
     * @param params
     *            parameters to send as form fields; may be <code>null</code> for none
     * @return the raw response body
     */

    public String post(String endpoint, Map<String, String> params) throws Exception {
        checkReady();
        StringBuilder data = new StringBuilder();
        if(params != null)
            for(Map.Entry<String, String> entry : params.entrySet())
                appendParam(data, entry.getKey(), entry.getValue());
        return sendPost(endpoint, data.toString());
    }

    protected void appendParam(StringBuilder data, String key, String value) throws Exception {
        if(data.length() > 0)
            data.append("&");
        data.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
    }

    protected String sendPost(String endpoint, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl + ":" + serverPort + "/" + endpoint)
                .openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        try(DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            out.writeBytes(body);
        }
        return readResponse(connection);
    }

    /** Reads the response body and throws if the HTTP status indicates an error. */
    protected String readResponse(HttpURLConnection connection) throws Exception {
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
        return response;
    }
}