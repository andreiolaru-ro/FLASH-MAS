package net.xqhs.flash.ml;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;
import org.yaml.snakeyaml.Yaml;


public class MLDriver extends Unit implements ConfigurableEntity<Node>, EntityProxy<MLDriver> {

	/**
	 * Use this to store the server process, to stop iit when needed.
	 */
	private Process serverProcess;

	/**
	 * list of available models and their paths
	 */
	private ArrayList<String> models = new ArrayList<String>();

	/**
	 * path to the .yaml config file
	 */
	protected static final String MODEL_CONFIG_FILE = "src/net/xqhs/flash/ml/python_module/config.yaml";

	/**
	 * url of the python server
	 */
	protected static final String SERVER_URL = "http://localhost:5000/";

	@Override
	public boolean configure(MultiTreeMap configuration) {
		setUnitName(configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME));
		return true;
	}
	
	@Override
	public boolean start() {
		// TODO Auto-generated method stub

		// start the python server, capture the server's stdin, stdout, stderr
		li("starting Python ML server...");
		try {
			ProcessBuilder pb = new ProcessBuilder("python", DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[0] + "/"
					+ MLDriver.class.getPackage().getName().replace('.', '/') + "/python_module/server.py");
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
			//setModelsFromYAML();   //choose whether to load the models from the .yaml file or from the server
			setModelsFromServer();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
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
		return false;
	}
	
	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean serverIsRunning() {
		return serverProcess!=null;
	}

	/**
	 * Method to set up the connection to the python server
	 * At the moment, the request property is always the same, but it could change in the future
	 * In such case, the method would take the request property as parameter
	 *
	 * @param route_endpoint
	 * 			The endpoint of the route to connect to
	 * @param request_method
	 * 			The request method to use
	 *
	 * @return
	 * 			The connection to the server
	 */
	protected HttpURLConnection setupConnection(String route_endpoint, String request_method) throws IOException {
		String location = SERVER_URL + route_endpoint;
		URL url = new URL(location);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(request_method);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setDoOutput(true);
		return connection;
	}

	protected String checkResponse(HttpURLConnection connection) throws IOException {
		String response = "";
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			// The model was successfully loaded
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					response += line;
				}
			}
			lf("Response: " + response);
			return response;
		} else {
			// Other error occurred, handle it accordingly
			le("Error: " + responseCode);
			le("Error: " + connection.getResponseMessage());
		}
		return null;
	}

	/**
	 * Methode to add a model to the python server.
	 * It takes A string of the model path as parameter, and return its ID if it exists.
	 * The ID is created by splitting the model path, to keep only the name of the model
	 * The server send a success message if the model is properly added, and an error message if it is not.
	 *
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
	public String addModel(String model_path, Map<String, Object> model_config) {

		//here we split both with "/" and "\\" in case the path is given with the wrong slash
		String model_name = model_path.split("/")[model_path.split("/").length - 1];
		model_name = model_name.split("\\\\")[model_path.split("\\\\").length - 1];
		model_name = model_name.split("\\.")[0].toLowerCase();

		try {
			// Convert the model_config to a JSON string
			Gson gson = new Gson();
			String jsonConfig = gson.toJson(model_config);

			// Set up the form data
			String postData = "model_name=" + URLEncoder.encode(model_name, "UTF-8");
			postData += "&model_file=" + URLEncoder.encode(model_path, "UTF-8");
			postData += "&model_config=" + URLEncoder.encode(jsonConfig, "UTF-8");

			// Set up the connection
			HttpURLConnection connection = setupConnection("add_model", "POST");

			// Send the form data to the server
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.writeBytes(postData);
				wr.flush();
			}

			// Check the response
			if (checkResponse(connection) != null) {
				//setModelsFromYAML();   //choose whether to load the models from the .yaml file or from the server
				setModelsFromServer();
				return model_name;
			}

		} catch(IOException e) {
			e.printStackTrace();
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
		try {
			// Read and encode the image data
			BufferedImage image = ImageIO.read(new File(data_path));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpeg", baos);
			byte[] imageData = baos.toByteArray();
			String imageBase64 = Base64.getEncoder().encodeToString(imageData);

			// Create the request data
			String postData = "model_name=" + URLEncoder.encode(model, "UTF-8");
			postData += "&input_data=" + URLEncoder.encode(imageBase64, "UTF-8");

			// Set up the connection
			HttpURLConnection connection = setupConnection("predict", "POST");

			// Write the data to the connection
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.write(postData.getBytes());
				wr.flush();
			}

			// Check the response
			String response = checkResponse(connection);
			if (response != null) {
				JsonParser jsonParser = new JsonParser();
				JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
				String prediction = jsonObject.get("prediction").toString();
				prediction = prediction.substring(2, prediction.length() - 2);

				ArrayList<Double> prediction_list = new ArrayList();
				String[] split_pred = prediction.split(",");
				for (String s : split_pred) {
					prediction_list.add(Double.parseDouble(s));
				}

				li("Prediction: " + prediction_list);
				return prediction_list;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Methode to get all the models' data from the config file.
	 * This takes the form of a list of maps, each map containing the data of a model.
	 *
	 * @return
	 * 			The list of the models' data
	 */
	protected ArrayList<Map<String, Object>> getYamlData(){
		try (InputStream inputStream = new FileInputStream(MODEL_CONFIG_FILE)) {
			Yaml yaml = new Yaml();
			Map<String, Object> yamlData = yaml.load(inputStream);
			return (ArrayList<Map<String, Object>>) yamlData.get("MODELS");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Methode to set the list of the available models from the yaml config file.
	 * it uses models' data from the config file to add the name of the models to the list.
	 */
	public void setModelsFromYAML() {
		ArrayList<Map<String, Object>> models_data = getYamlData();

		for (Map<String, Object> model : models_data) {
			String name = (String) model.get("name");
			if (!this.models.contains(name))
				this.models.add(name);
		}
		li("available models: ", this.models.toString());
	}

	/**
	 * Alternative method to get the models list directly from the server.
	 */
	public void setModelsFromServer() {
		try {
			// Set up the connection
			HttpURLConnection connection = setupConnection("get_models", "GET");

			// Check the response
			String response = checkResponse(connection);
			if (response != null) {
				JsonParser jsonParser = new JsonParser();
				JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
				String models = jsonObject.get("models").toString();
				models = models.substring(1, models.length() - 1);

				ArrayList<String> models_list = new ArrayList<>();
				String[] split_models = models.split(",");
				for (String s : split_models)
					//remove the \" around each name
					models_list.add(s.substring(1, s.length() - 1));

				this.models = models_list;
				li("available models: ", this.models);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getModels() {
		return models;
	}

	/**
	 * Methode to get the data of a model from its name.
	 * It returns a map containing the configuration datas of the model
	 *
	 * @param model_name
	 * 			The name of the model we want to get the data
	 *
	 * @return
	 * 			The map containing the configuration datas of the model
	 * 			for now it has the following structure:
	 * 			-name: the name of the model
	 * 			-path: the path of the model
	 * 		    -cuda: true if the model use cuda, false otherwise
	 * 		    -input_size: the size of the input data
	 * 		    -input_space: the space of the input data
	 * 		    -normalization: the normalization of the input data
	 * 		    -class_names: the classes of the model
	 *
	 */
	public Map<String,Object> getConfigFromYAML(String model_name){
		ArrayList<Map<String, Object>> models_data = getYamlData();

		for (Map<String, Object> model : models_data) {
			String name = (String) model.get("name");
			if (name.equals(model_name)) {
				return model;
			}
		}
		le("model not found: " + model_name);
		return null;
	}

	/**
	 * Alternative method to get the model's data directly from the server.
	 */
	public Map<String,Object> getConfigFromServer(String model_name){
		try {
			// Set up the connection
			HttpURLConnection connection = setupConnection("get_model_config", "POST");

			// Write the data to the connection
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.write(("model_name=" + URLEncoder.encode(model_name, "UTF-8")).getBytes());
				wr.flush();
			}

			// Check the response
			String response = checkResponse(connection);
			if (response != null) {
				JsonParser jsonParser = new JsonParser();
				JsonObject jsonObject = jsonParser.parse(response).getAsJsonObject();
				String config = jsonObject.get("model_config").toString();
				config = config.substring(1, config.length() - 1);
				li("model config: ", config);
/**
				Map<String, Object> config_map = new HashMap<>();
				String[] split_config = config.split(",");
				for (String s : split_config) {
					String[] split_s = s.split(":");
					String key = split_s[0].substring(1, split_s[0].length() - 1);
					String value = split_s[1].substring(1, split_s[1].length() - 1);
					config_map.put(key, value);
				}

				li("model config: ", config_map.toString());
				return config_map;*/
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		le("model not found: " + model_name);
		le("available models: " + this.models);
		return null;
	}
	
	// TODO other methods
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
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
