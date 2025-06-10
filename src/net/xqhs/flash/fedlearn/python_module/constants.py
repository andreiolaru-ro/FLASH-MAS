


# constants (use the same block of constants from MLDriver.java):   -- from here --
SERVER_URL = "http://localhost";
SERVER_PORT = 5023;
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
# -- until here --


# ---------------------------- CLIENT ROUTES CONSTANTS ----------------------------
CLIENT_INIT = "init_client"
CLIENT_DATA = "client_data"


# ---------------------------- SERVER ROUTES CONSTANTS ----------------------------
INITIALIZE_FED_SERVICE = "initialize_fed";
REGISTER_CLIENT_PROXY = "register_client_proxy";
STRATEGY = "strategy";
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
