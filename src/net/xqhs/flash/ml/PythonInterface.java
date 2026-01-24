package net.xqhs.flash.ml;

import java.util.Map;

/**
 * Abstract interface for communication with Python ML backend.
 * Implementations can use different protocols (HTTP, gRPC, MQ, etc.)
 */
public interface PythonInterface {
    
    /**
     * Initialize the Python backend connection.
     * 
     * @param config Configuration parameters (url, port, file path, etc.)
     * @return true if initialization successful, false otherwise
     */
    boolean initialize(Map<String, Object> config);
    
    /**
     * Check if the Python backend is ready to accept requests.
     * 
     * @return true if ready, false otherwise
     */
    boolean isReady();
    
    /**
     * Get available models from the Python backend.
     * 
     * @return JSON string with models information, or null if error
     */
    String getModels();
    
    /**
     * Add a new model to the Python backend.
     * 
     * @param modelName The name/ID of the model
     * @param modelPath The path to the model file
     * @param modelConfig The model configuration as JSON string
     * @return JSON string with model information, or null if error
     */
    String addModel(String modelName, String modelPath, String modelConfig);
    
    /**
     * Add a dataset to the Python backend.
     * 
     * @param datasetName The name of the dataset
     * @param classes The dataset classes
     * @return JSON string with dataset information, or null if error
     */
    String addDataset(String datasetName, String classes);
    
    /**
     * Make a prediction using a model.
     * 
     * @param modelName The model to use
     * @param inputData The input data (could be file path, base64, etc.)
     * @return JSON string with prediction result, or null if error
     */
    String predict(String modelName, String inputData);
    
    /**
     * Export a model from the Python backend.
     * 
     * @param modelName The model to export
     * @param exportPath The directory path to export to
     * @return The path to the exported model, or null if error
     */
    String exportModel(String modelName, String exportPath);
    
    /**
     * Shutdown the Python backend and clean up resources.
     */
    void shutdown();
}
