import os
import shutil
import importlib
import sys
import base64
from PIL import Image
import io
import json

# from ruamel import yaml

# constants:
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

print("<ML server> loading prerequisites...")

try:
    import torch
except Exception as e:
    print("PyTorch unavailable (use pip install torch ):", e)
    print("If there is a problem with MobileNetV2, try to run the Regenerate.py script in "
          "src-experiments\aifolk\ml_driver")
    exit(1)
try:
    from torchvision import transforms
except Exception as e:
    print("Torchvision unavailable (use pip install torchvision ):", e)
    exit(1)
try:
    import torchaudio
except Exception as e:
    print("Torchaudio unavailable (use pip install torchaudio ):", e)
    exit(1)
try:
    from omegaconf import OmegaConf
except Exception as e:
    print("OmegaConf unavailable (use pip install omegaconf ):", e)
    # exit(1)
try:
    import soundfile
except Exception as e:
    print("Soundfile unavailable (use pip install soundfile ):", e)
    # exit(1)
try:
    from flask import Flask, request, jsonify, json
except Exception as e:
    print("Flask unavailable (use pip install flask ): ", e)
    exit(1)
try:
    import yaml
except Exception as e:
    print("Yaml unavailable (use pip install pyyaml ):", e)
    exit(1)


def import_transform(name):
    components = name.split('.')
    mod = __import__(components[0])
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod

def get_model(model_config):
    model_name = model_config['name']
    model_path = model_config['path']
    cuda = model_config['cuda'] and torch.cuda.is_available()
    device = 'cuda:0' if cuda else 'cpu'

    model = torch.load(model_path,  map_location = device)
    if cuda:
        model = model.cuda()
    else:
        model = model.cpu()
    if 'transform' in model_config:
        transform_class = import_transform(model_config['transform'])
        transform = transform_class()
        input_size = transform.input_size
    else:
        transform = None
        input_size = None
    model.eval()
    return model, transform, input_size

def load_models_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    models = {}
    for model_config in config['MODELS']:
        model_name = model_config['name']
        model_path = model_config['path']
        cuda = model_config['cuda'] and torch.cuda.is_available()
        model, transform, input_size = get_model(model_config)
        task = model_config['task']
        dataset = model_config['dataset']
        models[model_name] = {
            'model': model,
            'input_size': input_size,
            'cuda': cuda,
            'transform': transform,
            'task': task,
            'dataset': dataset
        }

    return models

def load_datasets_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)
    datasets = {}
    for dataset in config['DATASETS']:
        datasets[dataset['name']] = {'class_names': dataset['class_names']}
    return datasets

print("<ML server> prerequisites loaded; starting server... ")
app = Flask(__name__)
print("<ML server> working directory: " + ML_DIRECTORY_PATH)
models = load_models_from_config((ML_DIRECTORY_PATH + MODEL_CONFIG_FILE))
datasets = load_datasets_from_config(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE)

@app.route('/' + ADD_MODEL_SERVICE, methods=['POST'])
def add_model():
    global models
    model_name = request.form.get(MODEL_NAME_PARAM)
    model_path = request.form.get(MODEL_FILE_PARAM)
    model_properties = request.form.get(MODEL_CONFIG_PARAM)
    if model_name and model_path:
        model_properties = json.loads(model_properties)
        model_properties['name'] = model_name
        model_properties['path'] = model_path
        model, transform, input_size = get_model(model_properties)
        cuda = model_properties['cuda'] and torch.cuda.is_available()
        task = model_properties['task']
        dataset = model_properties['dataset']
        models[model_name] = {
            'model': model,
            'input_size': input_size,
            'cuda': cuda,
            'transform': transform,
            'task': task,
            'dataset': dataset
        }
        return jsonify({'message': f'Model "{model_name}" has been successfully added.'})
    else:
        return jsonify({'error': 'Model name and/or model file are missing.'}), 400


@app.route('/' + ADD_DATASET_SERVICE, methods=['POST'])
def add_dataset():
    global datasets
    dataset_name = request.form.get(DATASET_NAME_PARAM)
    dataset_classes = request.form.get(DATASET_CLASSES_PARAM)
    classes = json.loads(dataset_classes)
    if dataset_name and classes:
        if dataset_name in datasets:
            return jsonify({'error': f'Dataset "{dataset_name}" already exists.'}), 404
        datasets[dataset_name] = {'class_names': classes}
        return jsonify({'message': f'Dataset "{dataset_name}" has been successfully added.'})
    else:
        return jsonify({'error': 'Dataset name and/or class names are missing.'}), 400


@app.route('/' + PREDICT_SERVICE, methods=['POST'])
def predict():
    global models
    global datasets
    model_name = request.form.get(MODEL_NAME_PARAM)
    input_data = request.form.get(INPUT_DATA_PARAM)

    if model_name in models:
        model = models[model_name]['model']
        input_bytes = base64.b64decode(input_data)
        input_tensor = Image.open(io.BytesIO(input_bytes))
        if models[model_name]['transform']:
            input_tensor = models[model_name]['transform'].transform(input_tensor)
        input_batch = input_tensor.unsqueeze(0)
        if models[model_name]['cuda']:
            input_batch = input_batch.cuda()
        output = model(input_batch)
        dataset = models[model_name]['dataset']
        response = {'prediction': output.tolist()}
        if dataset in datasets:
            response['class_names'] = datasets[dataset]['class_names']
        response['task'] = models[model_name]['task']
        response['dataset'] = models[model_name]['dataset']
        return jsonify(response)
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404


@app.route('/' + GET_MODELS_SERVICE, methods=['GET'])
def get_models():
    returned_models = {}
    with open(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE, 'r') as config_file:
        config_data = yaml.safe_load(config_file)
    for model in config_data['MODELS']:
        returned_models[model['name']] = {
            #for each key in the model_config, add it to the new model
            key: model[key] for key in model
        }
    return jsonify({'models': returned_models})


@app.route('/' + EXPORT_MODEL_SERVICE, methods=['POST'])
def export_model():
    model_name = request.form.get(MODEL_NAME_PARAM)
    export_directory_path = request.form.get(EXPORT_PATH_PARAM)

    model_config = {}

    if model_name in models:
        config_path = ML_DIRECTORY_PATH + MODEL_CONFIG_FILE
        with open(config_path, 'r') as config_file:
            config_data = yaml.safe_load(config_file)
        for model in config_data['MODELS']:
            if model['name'] == model_name:
                model_config = dict(model)
                break
        #now create a new config file and fill it with the model's data to export
        config_data = {'MODELS': [model_config]}
        if not os.path.exists(export_directory_path):
            os.makedirs(export_directory_path)
        config_path = export_directory_path + '/' + model_name + '_' + MODEL_CONFIG_FILE
        with open(config_path, 'w') as config_file:
            yaml.dump(config_data, config_file)
        #now copy the model file
        export_file = export_directory_path + '/' + model_name + MODEL_ENDPOINT
        torch.save(models[model_name]['model'], export_file)

        return jsonify({'message': f'Model "{model_name}" has been successfully exported.', 'destination': export_directory_path})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404


if __name__ == '__main__':
    print("<ML server> starting...")
    app.run()
