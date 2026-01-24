package net.xqhs.flash.ml;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PythonHTTPInterface implements AsyncDriver, PythonInterface {

    private String serverUrl;
    private int serverPort;
    private String serverFile;
    private volatile boolean ready = false;
    private ExecutorService executorService;
    private Process serverProcess;

    public PythonHTTPInterface() {
        this.executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * Initialize and start the Python server.
     * 
     * @param config Configuration map with keys: "url", "port", "serverFile"
     * @return true if initialization successful, false otherwise
     */
    @Override
    public boolean initialize(Map<String, Object> config) {
        this.serverUrl = (String) config.get("url");
        this.serverPort = (Integer) config.get("port");
        this.serverFile = (String) config.get("serverFile");
        
        return startPythonServer();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Start the Python Flask server process.
     * Starts the python server, captures the server's stdin, stdout, stderr.
     */
    private boolean startPythonServer() {
        System.out.println("Starting Python ML server on port " + serverPort + "...");
        try {
            ProcessBuilder pb = new ProcessBuilder("python3",
                    DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[DeploymentConfiguration.SOURCE_INDEX_MAIN] + "/"
                    + MLDriver.class.getPackage().getName().replace('.', '/') + "/" + serverFile);
            // pb.directory(new File(<directory from where you want to run the command>));
            // pb.inheritIO();
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            serverProcess = pb.start();
            
            int initialTries = 5, tries = initialTries;
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
                System.err.println("Python server could not start in the given time: " 
                        + (initialTries * spaceBetweenTries) + "ms");
                return false;
            }
            
            System.out.println("Attempt connection because server process is " 
                    + (serverProcess.isAlive() ? "alive" : "dead"));
            tries = initialTries;
            
            while(!connected && tries-- >= 0) {
                // System.out.println("try " + tries);
                try { // wait for the process to start.
                    Thread.sleep(spaceBetweenTries);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                if(!serverProcess.isAlive()) {
                    System.err.println("Server process is alive no more");
                    break;
                }
                try {
                    if (testConnection()) {
                        System.out.println("connected");
                        connected = true;
                    }
                } catch(Exception e) {
                    // just wait
                }
            }
            
            if(!serverProcess.isAlive()) {
                System.err.println("Python server failed to start, error: " + serverProcess.exitValue());
                return false;
            }
            if(!connected) {
                System.err.println("Python server connection failed; no server available.");
                return false;
            }
            
            System.out.println("Python server is up");
            this.ready = true;
            return true;
            
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Test connection to Python server by calling get_models endpoint.
     */
    private boolean testConnection() {
        try {
            String location = serverUrl + ":" + serverPort + "/get_models";
            URL url = new URL(location);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            
            BufferedReader in;
            if (connection.getResponseCode() == 200) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                return false;
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            return connection.getResponseCode() == 200;
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    public void processAsync(AgentWave wave, WaveReceiver callback) {
        executorService.submit(() -> {
            try {
                AgentWave reply = doProcess(wave);
                callback.receive(reply);
            } catch (Exception e) {
                callback.receive(wave.createReply("ERROR: " + e.getMessage()));
            }
        });
    }

    @Override
    public AgentWave process(AgentWave wave) {
        throw new UnsupportedOperationException("Only async processing supported. Use processAsync()");
    }

    private AgentWave doProcess(AgentWave wave) throws Exception {
        String endpoint = extractEndpoint(wave);
        String requestData = buildRequestData(wave, endpoint);

        String response = sendHttpRequest(endpoint, requestData);

        return wave.createReply(response);
    }

    private String extractEndpoint(AgentWave wave) {
        String[] destinations = wave.getDestinationElements();
        if (destinations != null && destinations.length > 0) {
            return destinations[0];
        }
        return "predict";
    }

    private String buildRequestData(AgentWave wave, String endpoint) throws UnsupportedEncodingException {
        StringBuilder data = new StringBuilder();
        switch(endpoint) {
            case "predict":
                appendParam(data, "input_data", wave.getContent());
                appendParam(data, "model_name", wave.get("model"));
                break;

            case "add_model":
                appendParam(data, "model_name", wave.get("model_name"));
                appendParam(data, "model_file", wave.get("model_file"));
                appendParam(data, "model_config", wave.get("model_config"));
                break;

            case "add_dataset":
                appendParam(data, "dataset_name", wave.get("dataset_name"));
                appendParam(data, "dataset_classes", wave.get("dataset_classes"));
                break;

            case "export_model":
                appendParam(data, "model_name", wave.get("model_name"));
                appendParam(data, "export_directory_path", wave.get("export_path"));
                break;

            case "get_models":
                break;

            default:
                throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }

        return data.toString();
    }

    private void appendParam(StringBuilder data, String key, String value) throws UnsupportedEncodingException {
        if (value != null) {
            if (!data.isEmpty()) {
                data.append("&");
            }
            data.append(key).append("=").append(URLEncoder.encode(value, "UTF-8"));
        }
    }

    private String sendHttpRequest(String endpoint, String postData) throws Exception {
        URL url = new URL(serverUrl + ":" + serverPort + "/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

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
            throw new Exception("HTTP " + connection.getResponseCode() + ": " + response.toString());
        }

        return response.toString();
    }

    // PythonInterface Implementation

    @Override
    public String getModels() {
        try {
            return sendGetRequest("get_models");
        } catch (Exception e) {
            System.err.println("Error getting models: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String addModel(String modelName, String modelPath, String modelConfig) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("model_name", modelName);
            params.put("model_file", modelPath);
            params.put("model_config", modelConfig);
            return sendPostRequest("add_model", params);
        } catch (Exception e) {
            System.err.println("Error adding model: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String addDataset(String datasetName, String classes) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("dataset_name", datasetName);
            params.put("dataset_classes", classes);
            return sendPostRequest("add_dataset", params);
        } catch (Exception e) {
            System.err.println("Error adding dataset: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String predict(String modelName, String inputData) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("model_name", modelName);
            params.put("input_data", inputData);
            return sendPostRequest("predict", params);
        } catch (Exception e) {
            System.err.println("Error making prediction: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String exportModel(String modelName, String exportPath) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("model_name", modelName);
            params.put("export_directory_path", exportPath);
            return sendPostRequest("export_model", params);
        } catch (Exception e) {
            System.err.println("Error exporting model: " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to set up the connection to the python server, and send the GET request.
     * 
     * @param endpoint
     *            The endpoint of the route to connect to
     * 			
     * @return The response from the server, if the response code is OK. Throws exception otherwise.
     */
    private String sendGetRequest(String endpoint) throws Exception {
        if(serverProcess == null || !serverProcess.isAlive()) {
            throw new Exception("Server process not active.");
        }
        
        URL url = new URL(serverUrl + ":" + serverPort + "/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        
        return checkResponse(connection);
    }

    /**
     * Method to set up the connection to the python server, and send the POST request. At the moment, the request property
     * is always the same, but it could change in the future. In such case, the method would take the request property as
     * parameter.
     *
     * @param endpoint
     *            The endpoint of the route to connect to
     * @param params
     *            The parameters to send to the server. If null, no parameters are sent
     * 			
     * @return The response from the server, if the response code is OK. Throws exception otherwise.
     */
    private String sendPostRequest(String endpoint, Map<String, String> params) throws Exception {
        if(serverProcess == null || !serverProcess.isAlive()) {
            throw new Exception("Server process not active.");
        }
        
        URL url = new URL(serverUrl + ":" + serverPort + "/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
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
        
        return checkResponse(connection);
    }
    
    /**
     * Method to check the response from the server. If the response code returned by the server is OK, a string
     * containing the response is returned
     *
     * @param connection
     *            The connection to the server
     * 			
     * @return The response from the server, if the response code is OK. Throws exception otherwise.
     */
    private String checkResponse(HttpURLConnection connection) throws Exception {
        if(connection == null)
            throw new Exception("Connection is null");
        
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
                System.err.println("Error: " + responseCode + " " + connection.getResponseMessage() + ". Response: " + response);
            else
                System.out.println("Response: " + response);
            
            if(iserror)
                throw new Exception("HTTP " + responseCode + ": " + response);
            
            return response;
        }
    }

    @Override
    public void shutdown() {
        ready = false;
        
        // Shutdown thread pool
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        
        // Destroy Python server process
        if (serverProcess != null) {
            try {
                serverProcess.destroy();
                serverProcess = null;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}