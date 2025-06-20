package net.xqhs.flash.fedlearn;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.node.Node;

public class FedDriver extends EntityCore<Node> implements EntityProxy<FedDriver> {
	
	/**
	 * port of the python server
	 */
	private int				SERVER_PORT;
	/**
	 * url of the python server
	 */
	public static String	SERVER_URL;
	/**
	 * path to the md driver source code
	 */
	public static String	ML_SRC_PATH;
	/**
	 * path to the ml directory where we store the models and the config file
	 */
	public static String	ML_DIRECTORY_PATH;
	/**
	 * name of the python server file
	 */
	public static String	SERVER_FILE;
	/**
	 * name of the yaml config file
	 */
	public static String	MODEL_CONFIG_FILE;
	/**
	 * path to the models directory
	 */
	public static String	MODELS_DIRECTORY;
	/**
	 * endpoint for the models names
	 */
	public static String	MODEL_ENDPOINT;
	/**
	 * endpoint for the server route
	 */
	public static String	ADD_MODEL_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String	PREDICT_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String	GET_MODELS_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String	EXPORT_MODEL_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String	ADD_DATASET_SERVICE;
	/**
	 * parameter for the model name
	 */
	public static String	MODEL_NAME_PARAM;
	/**
	 * parameter for the model file
	 */
	public static String	MODEL_FILE_PARAM;
	/**
	 * parameter for the model config
	 */
	public static String	MODEL_CONFIG_PARAM;
	/**
	 * parameter for the input data
	 */
	public static String	INPUT_DATA_PARAM;
	/**
	 * parameter for the export path
	 */
	public static String	EXPORT_PATH_PARAM;
	/**
	 * package name for the operations modules
	 */
	public static String	OP_MODULE_PACKAGE;
	/**
	 * parameter for the operation module
	 */
	public static String	OPERATION_MODULE_PARAM;
	/**
	 * parameter for the transform operation
	 */
	public static String	TRANSFORM_OP_PARAM;
	/**
	 * parameter for the predict operation
	 */
	public static String	PREDICT_OP_PARAM;

	/**
	 * parameter for the dataset name
	 */
	public static String	DATASET_NAME_PARAM;

	/**
	 * parameter for the dataset classes
	 */
	public static String	DATASET_CLASSES_PARAM;

	private static String	CLIENT_INIT;

	private static String	CLIENT_DATA;

	private static String	INITIALIZE_FED_SERVICE;

	private static String	REGISTER_CLIENT_PROXY;

	private static String	CLIENT_ID;

	private static String	FRACTION_FIT;

	private static String	FRACTION_EVALUATE;

	private static String	MIN_FIT_CLIENTS;

	private static String	MIN_EVALUATE_CLIENTS;

	private static String	MIN_AVAILABLE_CLIENTS;

	private static String	NUM_CLIENTS;

	private static String	START_FIT;

	private static String	GET_TASK;

	private static String	RES;

	private static String	HEALTHCHECK_ENDPOINT;

	private static String	SERVER_PORT_PARAM = "port";

	{ // use the same block of constants from fed.py
		SERVER_URL = "http://localhost";
		ML_SRC_PATH = "src/net/xqhs/flash/fedlearn/";
		ML_DIRECTORY_PATH = "ml-directory/";
		OP_MODULE_PACKAGE = "operations-modules";
		SERVER_FILE = "python_module/fed_init.py";
		MODEL_CONFIG_FILE = "config.yaml";
		MODELS_DIRECTORY = "models/";
		MODEL_ENDPOINT = ".pth";
		ADD_MODEL_SERVICE = "add_model";
		ADD_DATASET_SERVICE = "add_dataset";
		PREDICT_SERVICE = "predict";
		GET_MODELS_SERVICE = "get_models";
		EXPORT_MODEL_SERVICE = "export_model";
		MODEL_NAME_PARAM = "model_name";
		MODEL_FILE_PARAM = "model_file";
		MODEL_CONFIG_PARAM = "model_config";
		INPUT_DATA_PARAM = "input_data";
		EXPORT_PATH_PARAM = "export_directory_path";
		OPERATION_MODULE_PARAM = "operation_module";
		TRANSFORM_OP_PARAM = "transform_op";
		PREDICT_OP_PARAM = "predict_op";
		DATASET_NAME_PARAM = "dataset_name";
		DATASET_CLASSES_PARAM = "dataset_classes";
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
	
	/**
	 * Use this to store the server process, to stop iit when needed.
	 */
	private Process serverProcess;

	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		// start the python server, capture the server's stdin, stdout, stderr
		li("starting Python ML server...");
		String port = getConfiguration().get(SERVER_PORT_PARAM);
		SERVER_PORT = Integer.parseInt(port);
		try {
			ProcessBuilder pb = new ProcessBuilder("python",
					DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN] + "/"
					+ FedDriver.class.getPackage().getName().replace('.', '/') + "/" + SERVER_FILE, port);
			// pb.directory(new File(<directory from where you want to run the command>));
			// pb.inheritIO();

			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			serverProcess = pb.start();
			int initialtries = 5, tries = initialtries;
			int spaceBetweenTries = 2000;
			boolean started = false, connected = false;
			while(!started && tries-- >= 0) {
				try { // wait for the process to start.
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
						Integer.valueOf(initialtries * spaceBetweenTries));
				return false;
			}
			lf("Attempt connection because server process is []", serverProcess.isAlive() ? "alive" : "dead");
			tries = initialtries;
			while(!connected && tries-- >= 0) {
				// lf("try []", Integer.valueOf(tries));
				try { // wait for the process to start.
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
					// just wait
				}
			}
			if(!serverProcess.isAlive()) {
				le("Python server failed to start, error [].", Integer.valueOf(serverProcess.exitValue()));
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
	 * Method to set up the connection to the python server, and send the request.
	 * This version serializes the parameters as a JSON payload.
	 *
	 * @param route_endpoint
	 * The endpoint of the route to connect to
	 * @param request_method
	 * The request method to use
	 * @param params
	 * The parameters to send to the server as a JSON object. If null, no body is sent.
	 * * @return The connection to the server
	 */
	protected HttpURLConnection setupConnection(String route_endpoint, String request_method,
												Map<String, String> params) {
		if (serverProcess == null || !serverProcess.isAlive()) {
			le("Server process not active.");
			return null;
		}
		try {
			String location = SERVER_URL + ":" + SERVER_PORT + "/" + route_endpoint;
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(request_method);

			// Set the content type to JSON, as expected by the Flask server
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);

			if (params != null) {
				// Use Gson to convert the parameter map to a JSON string
				Gson gson = new Gson();
				String jsonPayload = gson.toJson(params);

				// Print the message that is sent
				lf("Sending JSON payload to" + location);

				// Write the JSON string to the output stream
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
	 * Method to check the response from the server. If the response code returned by the server is OK, a string
	 * containing the response is returned
	 *
	 * @param connection
	 *            The connection to the server
	 * 			
	 * @return The response from the server, if the response code is OK. Null otherwise
	 */
	protected String checkResponse(HttpURLConnection connection) {
		if(connection == null)
			return null;
		try {
			String response = "";
			int responseCode = connection.getResponseCode();
			boolean iserror = responseCode >= 400;
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(!iserror ? connection.getInputStream() : connection.getErrorStream()))) {
				String line;
				while((line = in.readLine()) != null) {
					response += line;
				}
				if(iserror)
					le("Error: [][]. Response: ", responseCode, connection.getResponseMessage(), response);
				else
					li("Got response from " + connection.getURL().toString() + " : " + connection.getResponseCode());
				return !iserror ? response : null;
			}
			
		} catch(IOException e) {
			le("Error: [] []", e.getMessage(), e.getStackTrace());
			return null;
		}
	}

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
	 * Initializes a federated learning client instance on the Python server.
	 * This method sends a request to configure a fed client with a specific dataset partition.
	 *
	 * @param server_agent_id The unique identifier of the server agent managing the federated process.
	 * @param dataset_name The name of the dataset the client will use for training/evaluation.
	 * @param partition_id The specific partition id of the dataset assigned to this client.
	 * @param num_partitions The total number of partitions the dataset is divided into.
	 * @return The server's response as a String upon successful initialization, or null on failure.
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
	 * Sends task instructions from a client proxy to the federated learning client to be executed
	 * This is used for communication during federated tasks, like sending model updates or receiving tasks.
	 *
	 * @param proxy_id The unique identifier for the client proxy sending the data.
	 * @param instruction The serialized instructions being sent to the client.
	 * @return The client's response (that contains the operation result) as a String, or null if the communication fails.
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

	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<FedDriver> asContext() {
		return this;
	}

	@Override
	public String getEntityName() {
		return getName();
	}

}
