from util import import_functionality, log, logE
from constants import *
from model_store import *
import sys
import os
import yaml
import torch
import numpy as np
import traceback
import flask


current_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.abspath(os.path.join(current_dir, ".."))
sys.path.append(parent_dir)

from models.combined_model import CombinedModel


log("creating server... ")
app = flask.Flask(__name__)

@app.before_request
def log_request_info():
    print(f"[FLASK] Incoming request: {flask.request.method} {flask.request.path}")

@app.after_request
def log_response_info(response):
    print(f"[FLASK] Outgoing response: {response.status} {response.get_data(as_text=True)[:300]}")
    return response

@app.route("/ping", methods=["GET"])
def ping():
    return flask.jsonify({"status": "ok"})


class CombinedModelAdapter:
    def __init__(self, model):
        self.model = model

    def process_input(self, input_data):
        print("[DEBUG] Processing input:", input_data)
        return input_data

    def predict(self, processed_input):
        print("[DEBUG] Running CombinedModel.predict with:", processed_input)
        result = self.model.predict(processed_input)
        print("[DEBUG] Raw prediction result:", result)
        if isinstance(result, dict) and "result" in result:
            return result["result"]
        return result

    def process_output(self, result):
        if isinstance(result, (np.ndarray, torch.Tensor)):
            return result.tolist()
        return result


print("[DEBUG] CombinedModel.__init__ called")
combined_model = CombinedModel(
    r"C:\Users\teote\Downloads\Unet-resnet50.pt",
    r"C:\Users\teote\Downloads\Water_detection_15epochs.h5"
)
combined_model_adapter = CombinedModelAdapter(combined_model)

def start():
    app.run(port=SERVER_PORT)


@app.route('/' + ADD_MODEL_SERVICE, methods=['POST'])
def add_model():
    global models
    model_name = flask.request.form.get(MODEL_NAME_PARAM)
    model_path = flask.request.form.get(MODEL_FILE_PARAM)
    model_properties = flask.request.form.get(MODEL_CONFIG_PARAM)

    if model_name and model_path:
        model_properties = flask.json.loads(model_properties)
        model_properties['path'] = model_path
        models[model_name] = load_model(model_properties)
        return flask.jsonify({'message': f'Model "{model_name}" has been successfully added.'})
    else:
        return flask.jsonify({'error': 'Model name and/or model file are missing.'}), 400

@app.route('/' + ADD_DATASET_SERVICE, methods=['POST'])
def add_dataset():
    global datasets
    dataset_name = flask.request.form.get(DATASET_NAME_PARAM)
    dataset_classes = flask.request.form.get(DATASET_CLASSES_PARAM)
    classes = flask.json.loads(dataset_classes)

    if dataset_name and classes:
        if dataset_name in datasets:
            return flask.jsonify({'error': f'Dataset "{dataset_name}" already exists.'}), 404
        datasets[dataset_name] = {'class_names': classes}
        return flask.jsonify({'message': f'Dataset "{dataset_name}" has been successfully added.'})
    else:
        return flask.jsonify({'error': 'Dataset name and/or class names are missing.'}), 400

@app.route('/' + PREDICT_SERVICE, methods=['POST'])
def predict():
    model_name = flask.request.form.get(MODEL_NAME_PARAM)
    input_data = flask.request.form.get(INPUT_DATA_PARAM)

    print("[DEBUG] /predict endpoint called")
    print("[DEBUG] model_name:", model_name)
    print("[DEBUG] input_data:", input_data)

    try:
        if model_name == "CombinedModel":
            processed_input = combined_model_adapter.process_input(input_data)
            output = combined_model_adapter.predict(processed_input)
            result = combined_model_adapter.process_output(output)
        else:
            return flask.jsonify({'error': f'Model \"{model_name}\" is not available.'}), 404

        response = {'prediction': result if isinstance(result, list) else [result]}
        print("[DEBUG] response:", response)
        return flask.jsonify(response)

    except Exception as e:
        print("[ERROR] Exception in /predict:", str(e))
        traceback.print_exc()  
        return flask.jsonify({
            'error': f'Model \"{model_name}\" failed to run with exception: {e}'
        }), 500

@app.route('/' + GET_MODELS_SERVICE, methods=['GET'])
def get_models():
    print("[DEBUG] /get_models called")
    returned_models = {
        "CombinedModel": {
            "name": "CombinedModel",
            "code": "combined_model.CombinedModel",
            "path": "ml-directory/models/combined_placeholder.pt",
            "description": "Combines UNet and Water Detection based on brightness"
        },
        "DummyModel": {
            "name": "DummyModel",
            "code": "dummy_model.Dummy",
            "path": "ml-directory/models/dummy.pt",
            "description": "Dummy placeholder"
        }
    }
    return flask.jsonify({'models': returned_models})

@app.route('/' + EXPORT_MODEL_SERVICE, methods=['POST'])
def export_model():
    model_name = flask.request.form.get(MODEL_NAME_PARAM)
    export_directory_path = flask.request.form.get(EXPORT_PATH_PARAM)

    if model_name == "CombinedModel":
        return flask.jsonify({'message': 'CombinedModel is static and not exportable.'}), 400

    return flask.jsonify({'error': f'Model \"{model_name}\" does not exist.'}), 404
