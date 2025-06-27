package net.xqhs.flash.fedlearn;

import com.google.gson.Gson;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Acts as the primary bridge between the Java-based FLASH-MAS framework and the Python-based federated learning server.
 * This entity is responsible for managing the lifecycle of the Python server process (starting and stopping it)
 * and for providing a Java API to communicate with the Flask server's REST endpoints. Each instance of this driver
 * manages a single Python server process on a specific port.
 */
public class FedDriver extends EntityCore<Node> implements EntityProxy<FedDriver> {

	//<editor-fold desc="Static constants for API communication">

	/**
	 * The port for the Python server instance managed by this driver. This is an instance variable to prevent race
	 * conditions in multi-node environments where multiple drivers might be instantiated in the same JVM.
	 */
	private int             SERVER_PORT;
	/**
	 * The base URL for the Python server, typically localhost.
	 */
	public static String   SERVER_URL;
	/**
	 * The relative path from the project root to the federated learning source code.
	 */
	public static String   ML_SRC_PATH;
	/**
	 * The relative path to the directory for storing ML models and configurations.
	 */
	public static String   ML_DIRECTORY_PATH;
	/**
	 * The name of the main Python script that initializes the Flask server.
	 */
	public static String   SERVER_FILE;

	// --- API Endpoint and Parameter Constants ---

	/** The API endpoint for initializing a federated client. */
	private static String  CLIENT_INIT;
	/** The API endpoint for a client to execute a task. */
	private static String  CLIENT_DATA;
	/** The API endpoint to initialize the federated server strategy. */
	private static String  INITIALIZE_FED_SERVICE;
	/** The API endpoint to register a client proxy with the server's client manager. */
	private static String  REGISTER_CLIENT_PROXY;
	/** The request parameter key for the client's unique ID. */
	private static String  CLIENT_ID;
	/** The request parameter key for the fraction of clients to use for training. */
	private static String  FRACTION_FIT;
	/** The request parameter key for the fraction of clients to use for evaluation. */
	private static String  FRACTION_EVALUATE;
	/** The request parameter key for the minimum number of clients for training. */
	private static String  MIN_FIT_CLIENTS;
	/** The request parameter key for the minimum number of clients for evaluation. */
	private static String  MIN_EVALUATE_CLIENTS;
	/** The request parameter key for the minimum number of available clients to start a round. */
	private static String  MIN_AVAILABLE_CLIENTS;
	/** The request parameter key for the total number of clients participating. */
	private static String  NUM_CLIENTS;
	/** The API endpoint to start the main federated training loop. */
	private static String  START_FIT;
	/** The API endpoint to get pending tasks from the server. */
	private static String  GET_TASK;
	/** The API endpoint for a client to submit its results back to the server. */
	private static String  RES;
	/** The API endpoint for checking if the Python server is alive and ready. */
	private static String  HEALTHCHECK_ENDPOINT;
	/** The configuration parameter key for the server's port. */
	private static String  SERVER_PORT_PARAM = "port";

	{ // Initializer block for defining the static constants.
		SERVER_URL = "http://localhost";
		ML_SRC_PATH = "src/net/xqhs/flash/fedlearn/";
		ML_DIRECTORY_PATH = "ml-directory/";
		SERVER_FILE = "python_module/fed_init.py";

		// fed_learn constants
		HEALTHCHECK_ENDPOINT = "healthcheck";
		CLIENT_INIT = "init_client";
		CLIENT_DATA = "client_data";
		INITIALIZE_FED_SERVICE = "initialize_fed";
		REGISTER_CLIENT_PROXY = "register_client_proxy";
		CLIENT_ID = "client_id";
		FRACTION_FIT = "fraction_fit";
		FRACTION_EVALUATE = "fraction_evaluate";
		MIN_FIT_CLIENTS = "min_fit_clients";
		MIN_EVALUATE_CLIENTS = "min_evaluate_clients";
		MIN_AVAILABLE_CLIENTS = "min_available_clients";
		NUM_CLIENTS = "num_clients";
		START_FIT = "start_fit";
		GET_TASK = "get_task";
		RES = "res";
	}
	//</editor-fold>

	/**
	 * Stores the running Python server process, allowing it to be managed and terminated.
	 */
	private Process serverProcess;

	/**
	 * Starts the FedDriver entity. This method launches the associated Python Flask server as a separate process,
	 * waits for it to initialize, and performs a health check to ensure it's ready for communication before
	 * allowing the framework to proceed.
	 *
	 * @return {@code true} if the server process starts and responds successfully, {@code false} otherwise.
	 */
	@Override
	public boolean start() {
		if(!super.start())
			return false;

		li("starting Python ML server...");
		String port = getConfiguration().get(SERVER_PORT_PARAM);
		this.SERVER_PORT = Integer.parseInt(port);

		try {
			ProcessBuilder pb = new ProcessBuilder("python",
					DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN] + "/"
							+ FedDriver.class.getPackage().getName().replace('.', '/') + "/" + SERVER_FILE, port);

			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			serverProcess = pb.start();

			int initialtries = 5, tries = initialtries;
			int spaceBetweenTries = 2000;
			boolean started = false, connected = false;

			// Wait for the process to become alive
			while(!started && tries-- >= 0) {
				try {
					Thread.sleep(spaceBetweenTries);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				if(serverProcess.isAlive())
					started = true;
			}

			if(!started) {
				serverProcess.destroyForcibly();
				le("Python server could not start in the given time [].",
						initialtries * spaceBetweenTries);
				return false;
			}

			lf("Attempt connection because server process is []", serverProcess.isAlive() ? "alive" : "dead");

			// Ping the healthcheck endpoint until it responds
			tries = initialtries;
			while(!connected && tries-- >= 0) {
				try {
					Thread.sleep(spaceBetweenTries);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				if(!serverProcess.isAlive()) {
					lf("Server process is alive no more");
					break;
				}
				try {
					if(checkResponse(setupConnection(HEALTHCHECK_ENDPOINT, "GET", null)) == null)
						continue;
					lf("connected");
					connected = true;
				} catch(Exception e) {
					// Ignore connection exceptions during probing
				}
			}

			if(!serverProcess.isAlive()) {
				le("Python server failed to start, error [].", serverProcess.exitValue());
				return false;
			}
			if(!connected) {
				le("Python server connection failed; no server available.");
				return false;
			}

			li("Python server is up");
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Stops the FedDriver entity, ensuring the managed Python server subprocess is terminated.
	 *
	 * @return {@code true} if the process is stopped successfully or was not running, {@code false} on error.
	 */
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		if(this.serverProcess != null) {
			try {
				this.serverProcess.destroy();
				this.serverProcess = null;
				return true;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets up an HTTP connection to a specific endpoint on the Python server.
	 * This core utility method serializes the provided parameters into a JSON payload,
	 * sets the appropriate headers, and prepares the connection for sending data.
	 *
	 * @param route_endpoint The target API endpoint (e.g., "initialize_fed").
	 * @param request_method The HTTP method to use (e.g., "POST", "GET").
	 * @param params         A map of key-value pairs to be sent as the JSON request body. Can be {@code null} for requests with no body.
	 * @return An {@link HttpURLConnection} object ready to be sent, or {@code null} if an error occurs.
	 */
	protected HttpURLConnection setupConnection(String route_endpoint, String request_method, Map<String, String> params) {
		if (serverProcess == null || !serverProcess.isAlive()) {
			le("Server process not active.");
			return null;
		}
		try {
			String location = SERVER_URL + ":" + this.SERVER_PORT + "/" + route_endpoint;
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(request_method);

			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);

			if (params != null) {
				Gson gson = new Gson();
				String jsonPayload = gson.toJson(params);
				lf("Sending JSON payload to " + location);

				try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
					wr.write(jsonPayload.getBytes("UTF-8"));
					wr.flush();
				}
			}
			return connection;

		} catch (IOException e) {
			le("Error: [] []", e.getMessage(), e.getStackTrace());
			return null;
		}
	}

	/**
	 * Reads and validates the response from an {@link HttpURLConnection}.
	 * If the HTTP response code indicates success (2xx), it returns the response body as a string.
	 * If it indicates an error (4xx or 5xx), it logs the error and returns {@code null}.
	 *
	 * @param connection The active HTTP connection from which to read the response.
	 * @return The response body as a String on success, or {@code null} on failure or error.
	 */
	protected String checkResponse(HttpURLConnection connection) {
		if(connection == null)
			return null;
		try {
			StringBuilder response = new StringBuilder();
			int responseCode = connection.getResponseCode();
			boolean isError = responseCode >= 400;

			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(isError ? connection.getErrorStream() : connection.getInputStream()))) {
				String line;
				while((line = in.readLine()) != null) {
					response.append(line);
				}
				if(isError) {
					le("Error: [][]. Response: {}", responseCode, connection.getResponseMessage(), response.toString());
				} else {
					li("Got response from " + connection.getURL().toString() + " : " + responseCode);
				}
				return !isError ? response.toString() : null;
			}
		} catch(IOException e) {
			le("Error: [] []", e.getMessage(), e.getStackTrace());
			return null;
		}
	}

	/**
	 * Registers a client with the Python server's client manager.
	 *
	 * @param client_id The unique identifier for the client being registered.
	 * @return The server's response on success, or {@code null} on failure.
	 */
	public String register(String client_id) {
		Map<String, String> postData = new HashMap<>();
		postData.put(CLIENT_ID, client_id);
		HttpURLConnection connection = setupConnection(REGISTER_CLIENT_PROXY, "POST", postData);

		String response = checkResponse(connection);
		if(response != null) {
			lf("Client [] registered successfully", client_id);
			return response;
		}
		le("Error: could not register client []", client_id);
		return null;
	}

	/**
	 * Initializes the federated learning server with a given strategy and configuration.
	 *
	 * @param fraction_fit          Fraction of clients for training.
	 * @param fraction_evaluate     Fraction of clients for evaluation.
	 * @param min_fit_clients       Minimum clients for training.
	 * @param min_evaluate_clients  Minimum clients for evaluation.
	 * @param min_available_clients Minimum available clients to begin a round.
	 * @param num_clients           The total number of clients expected.
	 * @return The server's response on success, or {@code null} on failure.
	 */
	public String initFedServer( float fraction_fit, float fraction_evaluate,
								 int min_fit_clients, int min_evaluate_clients, int min_available_clients, int num_clients) {
		Map<String, String> postData = new HashMap<>();
		postData.put(FRACTION_FIT, Float.toString(fraction_fit));
		postData.put(FRACTION_EVALUATE, Float.toString(fraction_evaluate));
		postData.put(MIN_FIT_CLIENTS, Integer.toString(min_fit_clients));
		postData.put(MIN_EVALUATE_CLIENTS, Integer.toString(min_evaluate_clients));
		postData.put(MIN_AVAILABLE_CLIENTS, Integer.toString(min_available_clients));
		postData.put(NUM_CLIENTS, Integer.toString(num_clients));

		HttpURLConnection connection = setupConnection(INITIALIZE_FED_SERVICE, "POST", postData);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed initialized successfully");
			return response;
		}
		le("Error: could not initialize fed");
		return null;
	}

	/**
	 * Starts the main federated training process on the server.
	 *
	 * @param num_rounds The number of training rounds to perform.
	 * @param timeout    A timeout in seconds for the fit process.
	 * @return The server's response on success, or {@code null} on failure.
	 */
	public String startFit(Integer num_rounds, float timeout) {
		Map<String, String> postData = new HashMap<>();
		postData.put("num_rounds", Integer.toString(num_rounds));
		postData.put("timeout", Float.toString(timeout));

		HttpURLConnection connection = setupConnection(START_FIT, "POST", postData);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed fit started successfully");
			return response;
		}
		le("Error: could not start fit");
		return null;
	}

	/**
	 * Polls the server for any available tasks for the clients.
	 *
	 * @return A JSON string containing a list of tasks, or {@code null} on failure.
	 */
	public String getTask(){
		HttpURLConnection connection = setupConnection(GET_TASK, "GET", null);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed tasks retrieved successfully");
			return response;
		}
		le("Error: could not get task");
		return null;
	}

	/**
	 * Submits the results from a client's completed task back to the server.
	 *
	 * @param proxy_id The unique ID of the client proxy submitting the result.
	 * @param results  The serialized task results.
	 * @return The server's response on success, or {@code null} on failure.
	 */
	public String getRes(String proxy_id, String results){
		Map<String, String> postData = new HashMap<>();
		postData.put("proxy_id", proxy_id);
		postData.put("results", results);

		HttpURLConnection connection = setupConnection(RES, "POST", postData);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed results sent successfully");
			return response;
		}
		le("Error: could not send results");
		return null;
	}

	/**
	 * Initializes a federated learning client instance on its corresponding Python server.
	 * This configures the client with its specific dataset partition.
	 *
	 * @param server_agent_id The unique ID of the central server agent.
	 * @param dataset_name    The name of the dataset to use.
	 * @param partition_id    The specific partition ID assigned to this client.
	 * @param num_partitions  The total number of partitions the dataset is divided into.
	 * @return The server's response on success, or {@code null} on failure.
	 */
	public String initFedClient(String server_agent_id, String dataset_name, Integer partition_id, Integer num_partitions) {
		Map<String, String> postData = new HashMap<>();
		postData.put("server_agent_id", server_agent_id);
		postData.put("dataset", dataset_name);
		postData.put("partition_id", Integer.toString(partition_id));
		postData.put("num_partitions", Integer.toString(num_partitions));

		HttpURLConnection connection = setupConnection(CLIENT_INIT, "POST", postData);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed client initialized successfully -> CLIENT " + partition_id);
			return response;
		}
		le("Error: could not initialize fed client");
		return null;
	}

	/**
	 * Sends a task instruction from the framework to the client's Python server for execution.
	 *
	 * @param proxy_id    The unique ID of the client proxy.
	 * @param instruction The serialized task instruction from the main federated server.
	 * @return The client's response containing the task result, or {@code null} on failure.
	 */
	public String clientData(String proxy_id, String instruction){
		Map<String, String> postData = new HashMap<>();
		postData.put("proxy_id", proxy_id);
		postData.put("instruction", instruction);

		HttpURLConnection connection = setupConnection(CLIENT_DATA, "POST", postData);
		String response = checkResponse(connection);
		if(response != null) {
			lf("Fed client data sent successfully -> CLIENT " + proxy_id);
			return response;
		}
		le("Error: could not send fed client data");
		return null;
	}

	/**
	 * Returns this instance of FedDriver to be used as a context object within the framework.
	 * @return This {@code FedDriver} instance.
	 */
	@Override
	public EntityProxy<FedDriver> asContext() {
		return this;
	}

	/**
	 * Returns the name of this entity instance.
	 * @return The name of the entity.
	 */
	@Override
	public String getEntityName() {
		return getName();
	}

}
