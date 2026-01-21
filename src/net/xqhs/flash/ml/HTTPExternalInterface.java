package net.xqhs.flash.ml;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HTTPExternalInterface implements ExternalInterface {
	private String serverUrl;
	private int serverPort;
	private ExecutorService executor;
	private volatile boolean ready = false;
	
	@Override
	public boolean initialize(Map<String, Object> config) {
		this.serverUrl = (String) config.getOrDefault("host", "http://localhost");
		this.serverPort = (Integer) config.getOrDefault("port", 5023);
		this.executor = Executors.newFixedThreadPool(10);
		this.ready = true;
		return true;
	}
	
	@Override
	public void sendAsync(String request, ResponseCallback callback) {
		executor.submit(() -> {
			try {
				String response = sendHttpRequest(request);
				callback.onResponse(response);
			} catch (Exception e) {
				callback.onError(e);
			}
		});
	}
	
	@Override
	public boolean isReady() {
		return ready;
	}
	
	@Override
	public void shutdown() {
		ready = false;
		if (executor != null) {
			executor.shutdown();
		}
	}
	
	private String sendHttpRequest(String request) throws Exception {
        URL url = new URL(serverUrl + ":" + serverPort + "/predict");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        String postData = "model_name=" + URLEncoder.encode("YOLOv8-pedestrians", "UTF-8") + "&";
        postData += "input_data=" + URLEncoder.encode(request, "UTF-8") + "&";

        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(postData);
        wr.flush();

        BufferedReader in;
        if (connection.getResponseCode() == 200) {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        if (connection.getResponseCode() != 200) {
            throw new Exception("HTTP " + connection.getResponseCode() + ": " + response);
        }

        return response.toString();
	}
}
