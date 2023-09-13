import os
import shutil
import sys

# from ruamel import yaml

# constants:
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

print("<ML server> loading prerequisites...")

try:
    import torch
except Exception as e:
    print("PyTorch unavailable (use pip install torch ):", e)
    print("If there is a problem with MobileNetV2, try tu run the Regenerate.py script in "
          "src-experiments\aifolk\ml_driver")
    exit(1)
try:
    from torchvision import transforms
except Exception as e:
    print("Torchvision unavailable (use pip install torchvision ):", e)
    exit(1)
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
from PIL import Image
import io
import base64


def load_models_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    models = {}
    for model_config in config['MODELS']:
        model_name = model_config['name']
        model_path = model_config['path']
        cuda = model_config['cuda'] and torch.cuda.is_available()
        device = 'cuda:0' if cuda else 'cpu'

        model = torch.load(model_path, map_location=device)
        if cuda:
            model = model.cuda()
        else:
            model = model.cpu()

        transform = None
        if model_config['input_space'] == "RGB":
            list_of_transforms = []
            if 'input_size' in model_config:
                input_size = model_config['input_size']
                list_of_transforms.append(transforms.Resize(size=input_size))
            list_of_transforms.append(transforms.ToTensor())
            if 'normalization' in model_config:
                mean = model_config['normalization']['mean']
                std = model_config['normalization']['std']
                list_of_transforms.append(transforms.Normalize(mean, std))
            transform = transforms.Compose(list_of_transforms)

        models[model_name] = {
            'model': model,
            'input_size': input_size,
            'cuda': cuda,
            'transform': transform
        }
        if 'class_names' in model_config:
            models[model_name]['class_names'] = model_config['class_names']

    return models


app = Flask(__name__)
print("<ML server> working directory: " + ML_DIRECTORY_PATH)
models = load_models_from_config((ML_DIRECTORY_PATH + MODEL_CONFIG_FILE))


@app.route('/' + ADD_MODEL_SERVICE, methods=['POST'])
def add_model():
    global models
    model_name = request.form.get(MODEL_NAME_PARAM)
    model_file = request.form.get(MODEL_FILE_PARAM)

    # check if it already exist
    new_model_path = ML_DIRECTORY_PATH + MODELS_DIRECTORY + model_name + MODEL_ENDPOINT
    if os.path.exists(new_model_path):
        return jsonify({'error': f'Model "{model_name}" already exists.'}), 409

    # configure the details for the model with the client's information
    model_config = request.form.get(MODEL_CONFIG_PARAM)
    model_config = json.loads(model_config)

    if model_name and model_file:

        config_path = ML_DIRECTORY_PATH + MODEL_CONFIG_FILE
        with open(config_path, 'r') as config_file:
            config_data = yaml.safe_load(config_file)

        # Define the new model to add
        new_model = {
            "name": model_name,
            "path": new_model_path,
            "cuda": model_config['cuda'],
            "input_space": model_config['input_space'],
            "input_size": model_config['input_size'],
            "normalization": {
                "mean": model_config['norm_mean'],
                "std": model_config['norm_std']
            },
            "class_names": model_config['class_names']
        }

        # Append the new model to the existing models list
        config_data["MODELS"].append(new_model)

        # Write the updated data back to the YAML file
        with open(config_path, 'w') as config_file:
            yaml.dump(config_data, config_file)

        # save the file in the models directory
        shutil.copyfile(model_file, new_model_path)

        # reload the models
        models = load_models_from_config(config_path)

        return jsonify({'message': f'Model "{model_name}" has been successfully added.', 'model': new_model})
    else:
        return jsonify({'error': 'Model name and/or model file are missing.'}), 400


@app.route('/load_model', methods=['POST'])
def load_model():
    model_name = request.form.get(MODEL_NAME_PARAM)

    if model_name in models:
        model = models[model_name]
        # model.eval()
        return jsonify({'message': f'Model "{model_name}" has been successfully loaded.'})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404


@app.route('/' + PREDICT_SERVICE, methods=['POST'])
def predict():
    global models
    model_name = request.form.get(MODEL_NAME_PARAM)
    input_data = request.form.get(INPUT_DATA_PARAM)

    if model_name in models:
        model = models[model_name]['model']
        input_bytes = base64.b64decode(input_data)
        input_tensor = Image.open(io.BytesIO(input_bytes))
        if models[model_name]['transform']:
            input_tensor = models[model_name]['transform'](input_tensor)
        input_batch = input_tensor.unsqueeze(0)
        if models[model_name]['cuda']:
            input_batch = input_batch.cuda()
        output = model(input_batch)
        class_names = models[model_name]['class_names']
        return jsonify({'prediction': output.tolist(), 'class_names': class_names})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404


@app.route('/' + GET_MODELS_SERVICE, methods=['GET'])
def get_models():
    returned_models = {}
    with open(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE, 'r') as config_file:
        config_data = yaml.safe_load(config_file)
    for model in config_data['MODELS']:
        returned_models[model['name']] = {
            'path': model['path'],
            'cuda': model['cuda'],
            'input_space': model['input_space'],
            'input_size': model['input_size'],
            'normalization': {
                'mean': model['normalization']['mean'],
                'std': model['normalization']['std']
            },
            'class_names': model['class_names']
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
