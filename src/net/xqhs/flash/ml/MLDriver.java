package net.xqhs.flash.ml;

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

public class MLDriver extends EntityCore<Node> implements EntityProxy<MLDriver> {
	
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
	
	{ // use the same block of constants from server.py
		SERVER_URL = "http://localhost:5000/";
		ML_SRC_PATH = "src/net/xqhs/flash/ml/";
		ML_DIRECTORY_PATH = "ml-directory/";
		OP_MODULE_PACKAGE = "operations-modules";
		SERVER_FILE = "python_module/server.py";
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
	}
	
	/**
	 * Use this to store the server process, to stop iit when needed.
	 */
	private Process serverProcess;
	
	/**
	 * Map of available models, and their config
	 */
	private Map<String, Map<String, Object>> modelsList = new HashMap<>();

	/**
	 * Map of available datasets, and their config
	 */
	private Map<String, Map<String, Object>> datasetsList = new HashMap<>();
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		// start the python server, capture the server's stdin, stdout, stderr
		li("starting Python ML server...");
		try {
			ProcessBuilder pb = new ProcessBuilder("python",
					DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN] + "/"
					+ MLDriver.class.getPackage().getName().replace('.', '/') + "/" + SERVER_FILE);
			// pb.directory(new File(<directory from where you want to run the command>));
			// pb.inheritIO();
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			serverProcess = pb.start();
			int initialtries = 5, tries = initialtries;
			int spaceBetweenTries = 1000;
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
				// lf("try []", tries);
				try { // wait for the process to start.
					Thread.sleep(spaceBetweenTries);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				if(!serverProcess.isAlive())
					break;
				try {
					if(checkResponse(setupConnection(GET_MODELS_SERVICE, "GET", null)) == null)
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
			syncServerConfig();
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
	 * Method to set up the connection to the python server, and send the request. At the moment, the request property
	 * is always the same, but it could change in the future In such case, the method would take the request property as
	 * parameter
	 *
	 * @param route_endpoint
	 *            The endpoint of the route to connect to
	 * @param request_method
	 *            The request method to use
	 * @param params
	 *            The parameters to send to the server. If null, no parameters are sent
	 * 			
	 * @return The connection to the server
	 */
	protected HttpURLConnection setupConnection(String route_endpoint, String request_method,
			Map<String, String> params) {
		if(serverProcess == null || !serverProcess.isAlive()) {
			le("Server process not active.");
			return null;
		}
		try {
			String location = SERVER_URL + route_endpoint;
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(request_method);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);
			
			if(params != null) {
				String PostData = "";
				for(Map.Entry<String, String> param : params.entrySet()) {
					PostData += param.getKey() + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&";
				}
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(PostData);
				wr.flush();
			}
			return connection;
			
		} catch(IOException e) {
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
					li("Response: []", response);
				return !iserror ? response : null;
			}
			
		} catch(IOException e) {
			le("Error: [] []", e.getMessage(), e.getStackTrace());
			return null;
		}
	}
	
	/**
	 * Method to parse the json response from the server. It takes the response, and the key of the data to parse. It
	 * returns the parsed data as an object, which can be casted to the appropriate type
	 *
	 * @param key
	 *            The key of the data to parse in the returned json
	 * @param repsonse
	 *            The json response from the server
	 * 			
	 * @return The parsed data as an object
	 */
	public Object parseResponse(String key, String repsonse) {
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = jsonParser.parse(repsonse).getAsJsonObject();
		JsonElement jsonElement = jsonObject.get(key);
		Gson gson = new Gson();
		Object obj = gson.fromJson(jsonElement, Object.class);
		return obj;
	}
	
	/**
	 * Method to sync the models and their configurations available on the server and the client. It sends a request to
	 * the server, and update the modelsList attribute with the response. The response is a map of maps, each map
	 * containing the data of a model associated with its ID.
	 */
	public void syncServerConfig() {
		// Set up the connection
		HttpURLConnection connection = setupConnection(GET_MODELS_SERVICE, "GET", null);
		
		// Check the response
		String response = checkResponse(connection);
		if(response != null) {
			this.modelsList = (Map<String, Map<String, Object>>) parseResponse("models", response);
			li("available models: ", this.modelsList.keySet());
		}
	}
	
	/**
	 * Method to get the list of the available models.
	 *
	 * @return The list of the available models
	 */
	public Map<String, Map<String, Object>> getModels() {
		return modelsList;
	}
	
	/**
	 * Method to encode the data to send to the server. It takes the path of the data to encode, and returns the encoded
	 * data as a string
	 *
	 * @param dataPath
	 *            The path of the data to encode
	 * 			
	 * @return The encoded data
	 */
	private String encodeImage(String dataPath) {
		String encodedData;
		try {
			// Read and encode the image data
			BufferedImage image = ImageIO.read(new File(dataPath));
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpeg", baos);
			byte[] imageData = baos.toByteArray();
			encodedData = Base64.getEncoder().encodeToString(imageData);
		} catch(IOException e) {
			e.printStackTrace();
			le("Error: could not read the image file");
			return null;
		}
		return encodedData;
	}
	
	/**
	 * Methode to add a model to the python server. It takes strings of the model path and its name as parameter, and
	 * return its ID if it exists. The server send a success message if the model is properly added, and an error
	 * message if it is not. Adding a model also add it to the modelsList attribute, with its configuration.
	 *
	 * @param model_id
	 *            The Id of the model to add
	 * @param model_path
	 *            The path for the model to load
	 * @param model_config
	 *            a dictionary containing the model configuration if needed. Some information are required, such as: -
	 *            "cuda": true if the model is to be run on GPU, false otherwise - "transform": true if the model
	 *            requires a transformation operation on the input data, false otherwise - "operation_module": the name
	 *            of the module containing the operation specific to the model (e.g. "imageInput" for the models that
	 *            require an image as input) Other information can be added to the configuration, depending on the
	 *            model. Be carefull to give information that are relevant to the model, and to the operation module
	 * 			
	 * @return The Id of the model if it is properly added, null if it is not
	 */
	public String addModel(String model_id, String model_path, Map<String, Object> model_config) {
		// Convert the model_config to a JSON string
		Gson gson = new Gson();
		String jsonConfig = gson.toJson(model_config);
		
		// Set up the form data
		Map<String, String> postData = new HashMap<>();
		postData.put(MODEL_NAME_PARAM, model_id);
		postData.put(MODEL_FILE_PARAM, model_path);
		postData.put(MODEL_CONFIG_PARAM, jsonConfig);

		// Set up the connection
		HttpURLConnection connection = setupConnection(ADD_MODEL_SERVICE, "POST", postData);
		// Check the response
		String response = checkResponse(connection);
		if(response != null) {
			lf("Model " + model_id + " added successfully");
			Map<String, Object> values = (Map<String, Object>) parseResponse("model", response);
			this.modelsList.put(model_id, values);
			
			li("available models: " + this.modelsList.keySet());
			return model_id;
		}
		return null;
	}

	public String addDataset(String dataset_name, String classes) {
		// Set up the form data
		Map<String, String> postData = new HashMap<>();
		postData.put(DATASET_NAME_PARAM, dataset_name);
		postData.put(DATASET_CLASSES_PARAM, classes);

		// Set up the connection
		HttpURLConnection connection = setupConnection(ADD_DATASET_SERVICE, "POST", postData);
		// Check the response
		String response = checkResponse(connection);
		if(response != null) {
			lf("Dataset " + dataset_name + " added successfully");
			Map<String, Object> values = (Map<String, Object>) parseResponse("dataset", response);
			this.datasetsList.put(dataset_name, values);

			li("Available datasets: " + this.datasetsList.keySet());
			return dataset_name;
		}
		return null;
	}
	
	/**
	 * Methode to predict a result from a model. It gives to the server the model ID and the data to predict. Then it
	 * shows the result of the prediction, or an error message if the prediction failed. Currently, the data to predict
	 * is an image, encoded in base64.
	 *
	 * @param model
	 *            The ID of the model to use
	 * @param data_path
	 *            The path for the file we use to predict
	 * 			
	 * @return The prediction result, as a list of double
	 */
	public ArrayList<Object> predict(String model, String data_path, boolean needEncode) {
		String toPredict = data_path;
		if(needEncode) {
			// Encode the data
			toPredict = encodeImage(data_path);
		}
		
		// Create the request data
		Map<String, String> postData = new HashMap<>();
		postData.put(MODEL_NAME_PARAM, model);
		postData.put(INPUT_DATA_PARAM, toPredict);
		
		// Set up the connection
		HttpURLConnection connection = setupConnection(PREDICT_SERVICE, "POST", postData);
		// Check the response
		String response = checkResponse(connection);
		if(response != null) {
			ArrayList<Object> prediction_list = (ArrayList<Object>) parseResponse("prediction", response);
			
			li("Prediction: " + prediction_list);
			return prediction_list;
		}
		return null;
	}
	
	/**
	 * Method to export a model from the server. When exporting a model, it creates a config file with the model's
	 * configuration, and a .pth file with the model's weights. It takes the model ID and the export directory as
	 * parameter, and return the path to the exported model.
	 *
	 * @param model_id
	 *            The ID of the model to export
	 * @param export_directory
	 *            The directory where to export the model
	 * 			
	 * @return The path to the exported model
	 */
	public String exportModel(String model_id, String export_directory) {
		// Set up the form data
		Map<String, String> postData = new HashMap<>();
		postData.put(MODEL_NAME_PARAM, model_id);
		postData.put(EXPORT_PATH_PARAM, export_directory);
		
		// Set up the connection
		HttpURLConnection connection = setupConnection(EXPORT_MODEL_SERVICE, "POST", postData);
		// Check the response
		String response = checkResponse(connection);
		if(response != null) {
			lf("Model [] exported successfully", model_id);
			return export_directory + "/" + model_id + MODEL_ENDPOINT;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<MLDriver> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
}
