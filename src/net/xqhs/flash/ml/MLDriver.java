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
	public static String SERVER_URL;
	/**
	 * path to the md driver source code
	 */
	public static String ML_SRC_PATH;
	/**
	 * path to the ml directory where we store the models and the config file
	 */
	public static String ML_DIRECTORY_PATH;
	/**
	 * name of the python server file
	 */
	public static String SERVER_FILE;
	/**
	 * name of the yaml config file
	 */
	public static String MODEL_CONFIG_FILE;
	/**
	 * path to the models directory
	 */
	public static String MODELS_DIRECTORY;
	/**
	 * endpoint for the models names
	 */
	public static String MODEL_ENDPOINT;
	/**
	 * endpoint for the server route
	 */
	public static String ADD_MODEL_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String PREDICT_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String GET_MODELS_SERVICE;
	/**
	 * endpoint for the server route
	 */
	public static String EXPORT_MODEL_SERVICE;
	/**
	 * parameter for the model name
	 */
	public static String MODEL_NAME_PARAM;
	/**
	 * parameter for the model file
	 */
	public static String MODEL_FILE_PARAM;
	/**
	 * parameter for the model config
	 */
	public static String MODEL_CONFIG_PARAM;
	/**
	 * parameter for the input data
	 */
	public static String INPUT_DATA_PARAM;
	/**
	 * parameter for the export path
	 */
	public static String EXPORT_PATH_PARAM;

	{
		SERVER_URL = "http://localhost:5000/";
		ML_SRC_PATH = "src/net/xqhs/flash/ml/";
		ML_DIRECTORY_PATH = "ml-directory/";
		SERVER_FILE = "python_module/server.py";
		MODEL_CONFIG_FILE = "config.yaml";
		MODELS_DIRECTORY = "models/";
		MODEL_ENDPOINT = ".pth";
		ADD_MODEL_SERVICE = "add_model";
		PREDICT_SERVICE = "predict";
		GET_MODELS_SERVICE = "get_models";
		EXPORT_MODEL_SERVICE = "export_model";
		MODEL_NAME_PARAM = "model_name";
		MODEL_FILE_PARAM = "model_file";
		MODEL_CONFIG_PARAM = "model_config";
		INPUT_DATA_PARAM = "input_data";
		EXPORT_PATH_PARAM = "export_directory_path";

	}

	/**
	 * Use this to store the server process, to stop iit when needed.
	 */
	private Process serverProcess;

	/**
	 * Map of available models, and their config
	 */
	private Map<String, Map<String, Object>> modelsList = new HashMap<>();


	@Override
	public boolean start() {
		if(!super.start())
			return false;
		// start the python server, capture the server's stdin, stdout, stderr
		li("starting Python ML server...");
		try {
			ProcessBuilder pb = new ProcessBuilder("python", DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[0] + "/"
					+ MLDriver.class.getPackage().getName().replace('.', '/') + "/" + SERVER_FILE);
			// pb.directory(new File(<directory from where you want to run the command>));
			// pb.inheritIO();
			pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			this.serverProcess = pb.start();

			// wait for the server to start
			// TODO: find a better way to do this
			try {
				Thread.sleep(10000);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			syncServerConfig();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		if (this.serverProcess != null) {
			try {
				this.serverProcess.destroy();
				this.serverProcess = null;
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * Method to set up the connection to the python server, and send the request.
	 * At the moment, the request property is always the same, but it could change in the future
	 * In such case, the method would take the request property as parameter
	 *
	 * @param route_endpoint
	 * 			The endpoint of the route to connect to
	 * @param request_method
	 * 			The request method to use
	 * @param params
	 * 			The parameters to send to the server. If null, no parameters are sent
	 *
	 * @return
	 * 			The connection to the server
	 */
	protected HttpURLConnection setupConnection(String route_endpoint, String request_method, Map<String, String> params) {
		try {
			String location = SERVER_URL + route_endpoint;
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(request_method);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			if (params != null) {
				String PostData = "";
				for (Map.Entry<String, String> param : params.entrySet()) {
					PostData += param.getKey() + "=" + URLEncoder.encode(param.getValue(), "UTF-8") + "&";
				}
				DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(PostData);
				wr.flush();
			}
			return connection;

		} catch (IOException e) {
			e.printStackTrace();
			le("Error: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Method to check the response from the server.
	 * If the response code returned by the server is OK, a string containing the response is returned
	 *
	 * @param connection
	 * 			The connection to the server
	 *
	 * @return
	 * 			The response from the server, if the response code is OK. Null otherwise
	 */
	protected String checkResponse(HttpURLConnection connection) {
		try {
			String response = "";
			int responseCode = connection.getResponseCode();
			boolean iserror = responseCode >= 400;
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(!iserror ? connection.getInputStream() : connection.getErrorStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					response += line;
				}
				if(iserror)
					le("Error: [][]. Response: ",responseCode, connection.getResponseMessage(), response);
				else
					li("Response: " + response);
				return !iserror ? response : null;
			}

		} catch (IOException e) {
			e.printStackTrace();
			le("Error: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Method to sync the models and their configurations available on the server and the client.
	 * It sends a request to the server, and update the modelsList attribute with the response.
	 * The response is a map of maps, each map containing the data of a model associated with its ID.
	 */
	public void syncServerConfig() {
		// Set up the connection
		HttpURLConnection connection = setupConnection(GET_MODELS_SERVICE, "GET", null);

		// Check the response
		String response = checkResponse(connection);
		if (response != null) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
			JsonElement models = jsonObject.get("models");
			Gson gson = new Gson();
			this.modelsList = gson.fromJson(models, Map.class);

			li("available models: ", this.modelsList.keySet());
		}
	}

	/**
	 * Method to get the list of the available models.
	 *
	 * @return
	 * 			The list of the available models
	 */
	public Map<String, Map<String, Object>> getModels() {
		return modelsList;
	}

	/**
	 * Methode to add a model to the python server.
	 * It takes strings of the model path and its name as parameter, and return its ID if it exists.
	 * The server send a success message if the model is properly added, and an error message if it is not.
	 * Adding a model also add it to the modelsList attribute, with its configuration.
	 *
	 * @param model_id
	 * 			The Id of the model to add
	 * @param model_path
	 * 			The path for the model to load
	 * @param model_config
	 * 			a dictionary containing the model configuration if needed. For now, it has to contain:
	 * 			- input_space: the input space of the model
	 * 			- input_size: the input size of the model
	 * 			- norm_mean: the mean of the normalization
	 * 			- norm_std: the standard deviation of the normalization
	 * 			- cuda: if the model is on cuda or not
	 * 		    - class_names: the name of the classes
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
		if (connection == null) {
			le("Error: connection is null");
			return null;
		}

		// Check the response
		String response = checkResponse(connection);
		if (response != null) {
			lf("Model " + model_id + " added successfully");
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
			JsonElement model_info = jsonObject.get("model");
			Map<String, Object> values = gson.fromJson(model_info, Map.class);
			values.remove("name");
			this.modelsList.put(model_id, values);

			li("available models: " + this.modelsList.keySet());
			return model_id;
		}
		return null;
	}

	/**
	 * Methode to predict a result from a model. It gives to the server the model ID and the data to predict.
	 * Then it shows the result of the prediction, or an error message if the prediction failed.
	 * Currently, the data to predict is an image, encoded in base64.
	 *
	 * @param model
	 * 			The ID of the model to use
	 * @param data_path
	 * 			The path for the file we use to predict
	 *
	 * @return
	 * 			The prediction result, as a list of double
	 */
	public ArrayList<Double> predict(String model, String data_path) {

		String imageBase64 = null;
		try {
			// Read and encode the image data
			BufferedImage image = ImageIO.read(new File(data_path));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpeg", baos);
			byte[] imageData = baos.toByteArray();
			imageBase64 = Base64.getEncoder().encodeToString(imageData);
		}
		catch (IOException e) {
			e.printStackTrace();
			le("Error: could not read the image file");
			return null;
		}

		// Create the request data
		Map<String, String> postData = new HashMap<>();
		postData.put(MODEL_NAME_PARAM, model);
		postData.put(INPUT_DATA_PARAM, imageBase64);

		// Set up the connection
		HttpURLConnection connection = setupConnection(PREDICT_SERVICE, "POST", postData);
		if (connection == null) {
			le("Error: connection is null");
			return null;
		}

		// Check the response
		String response = checkResponse(connection);
		if (response != null) {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
			JsonElement prediction_json = jsonObject.get("prediction");
			Gson gson = new Gson();
			ArrayList<ArrayList<Double>> prediction_gson = gson.fromJson(prediction_json, ArrayList.class);
			ArrayList<Double> prediction_list = prediction_gson.get(0);

			li("Prediction: " + prediction_list);
			return prediction_list;
		}
		return null;
	}

	/**
	 * Method to export a model from the server.
	 * When exporting a model, it creates a config file with the model's configuration, and a .pth file with the model's weights.
	 * It takes the model ID and the export directory as parameter, and return the path to the exported model.
	 *
	 * @param model_id
	 * 			The ID of the model to export
	 * @param export_directory
	 * 			The directory where to export the model
	 *
	 * @return
	 * 			The path to the exported model
	 */
	public String exportModel(String model_id, String export_directory) {

		// Set up the form data
		Map<String, String> postData = new HashMap<>();
		postData.put(MODEL_NAME_PARAM, model_id);
		postData.put(EXPORT_PATH_PARAM, export_directory);

		// Set up the connection
		HttpURLConnection connection = setupConnection(EXPORT_MODEL_SERVICE, "POST", postData);
		if (connection == null) {
			le("Error: connection is null");
			return null;
		}

		// Check the response
		String response = checkResponse(connection);
		if (response != null) {
			lf("Model " + model_id + " exported successfully");
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
