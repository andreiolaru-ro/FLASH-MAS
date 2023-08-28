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
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

import net.xqhs.flash.core.ConfigurableEntity;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Unit;


public class MLDriver extends Unit implements ConfigurableEntity<Node>, EntityProxy<MLDriver> {

	/**
	 * Use this to store the server process, to stop iit when needed.
	 */
	private Process serverProcess;

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
	 * @return The ID of the model
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
			String location = "http://localhost:5000/add_model";
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			// Send the form data to the server
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.writeBytes(postData);
				wr.flush();
			}

			// Check the response code
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// The model was successfully loaded
				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(connection.getInputStream()))) {
					String line;
					while ((line = in.readLine()) != null) {
						System.out.println(line);
					}
				}
			} else {
				// Other error occurred, handle it accordingly
				System.err.println("Error: " + responseCode);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}

		return model_name;
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
	 */
	public void predict(String model, String data_path) {
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
			String location = "http://localhost:5000/predict";
			URL url = new URL(location);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);

			// Write the data to the connection
			try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
				wr.write(postData.getBytes());
				wr.flush();
			}

			// Check the response code
			String response = "";
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(connection.getInputStream()))) {
					String line;
					while ((line = in.readLine()) != null) {
						response += line;
					}
				}
			} else {
				System.err.println("Error: " + responseCode);
			}

			//show the result of the prediction
			System.out.println(response);

		} catch (IOException e) {
			e.printStackTrace();
		}
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
