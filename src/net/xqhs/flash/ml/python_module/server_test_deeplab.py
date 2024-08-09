import os, pathlib
import shutil
import importlib
import sys
import base64
from PIL import Image
import io
import json
from builtins import isinstance
# import torch

head = "<ML server> "

# getting the libraries
# in ml-directory/pythonlib:
#  python -m venv .

# Windows:
# .\Scripts\activate.bat
# .\Scripts\pip.exe install ultralytics
# .\Scripts\pip.exe install shapely



# from ruamel import yaml

# constants:
SERVER_URL = "http://localhost:5000/";
ML_SRC_PATH = "src/net/xqhs/flash/ml/";
ML_DIRECTORY_PATH = "ml-directory/";
PYTHONLIB_PATH = []
PYTHONLIB_PATH.append(ML_DIRECTORY_PATH + "pythonlib/lib/site-packages/")
PYTHONLIB_PATH.append(ML_DIRECTORY_PATH + "pythonlib/lib/python3.11/site-packages/")
OP_MODULE_PACKAGE = "operations-modules";
SERVER_FILE = "python_module/server.py";
MODEL_CONFIG_FILE = "config-deeplab.yaml";
MODELS_DIRECTORY = "models/";
MODEL_ENDPOINT = ".pth";
ADD_MODEL_SERVICE = "add_model";
ADD_DATASET_SERVICE = "add_dataset";
PREDICT_SERVICE = "predict";
GET_MODELS_SERVICE = "get_models";
EXPORT_MODEL_SERVICE = "export_model";
MODEL_NAME_PARAM = "model_name";
MODEL_FILE_PARAM = "model_file";
MODEL_CONFIG_PARAM = "model_config";2
INPUT_DATA_PARAM = "input_data";
EXPORT_PATH_PARAM = "export_directory_path";
OPERATION_MODULE_PARAM = "operation_module";
TRANSFORM_OP_PARAM = "transform_op";
PREDICT_OP_PARAM = "predict_op";
DATASET_NAME_PARAM = "dataset_name";
DATASET_CLASSES_PARAM = "dataset_classes";


print("loading prerequisites...")
project_root = pathlib.Path(__file__)
first_branch = ML_SRC_PATH.split("/")[0]
while str(project_root).split("/")[-1] != first_branch:
    project_root = project_root.parent
    ML_DIRECTORY_PATH = "../" + ML_DIRECTORY_PATH
project_root = project_root.parent
print(project_root)
print(ML_DIRECTORY_PATH)
for one_path in PYTHONLIB_PATH:
    pylib_path = project_root.absolute()
    pylib_path = str(pylib_path) + "/" + one_path
    print(pylib_path)
    # pylib_path = pylib_path.replace("/", "\\")
    sys.path.insert(0, pylib_path) # TODO regexpreplace path in ML_SRC_PATH
print(sys.path)

try:
    import torch
except Exception as e:
    print(head + "PyTorch unavailable (use pip install torch ):", e)
    print("head + If there is a problem with MobileNetV2, try to run the Regenerate.py script in "
          "src-experiments\aifolk\ml_driver")
    exit(1)
try:
    from torchvision import transforms
except Exception as e:
    print(head + "Torchvision unavailable (use pip install torchvision ):", e)
    exit(1)
try:
    import torchaudio
except Exception as e:
    print(head + "Torchaudio unavailable (use pip install torchaudio ):", e)
    # exit(1)
try:
    from omegaconf import OmegaConf
except Exception as e:
    print(head + "OmegaConf unavailable (use pip install omegaconf ):", e)
    # exit(1)
try:
    import soundfile
except Exception as e:
    print(head + "Soundfile unavailable (use pip install soundfile ):", e)
    # exit(1)
try:
    from ultralytics import YOLO
except Exception as e:
    print(head + "YOLO unavailable (use pip install ultralytics ):", e)
    # exit(1)
try:
    from flask import Flask, request, jsonify, json
except Exception as e:
    print(head + "Flask unavailable (use pip install flask ): ", e)
    exit(1)
try:
    import yaml
except Exception as e:
    print(head + "Yaml unavailable (use pip install pyyaml ):", e)
    exit(1)


def import_functionality(name):
    components = name.split('.')
    mod = __import__(components[0])
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod

def get_model(model_config):
    global model_map
    model_path = model_config['path']
    cuda = model_config['cuda'] and torch.cuda.is_available()
    device = 'cuda:0' if cuda else 'cpu'
    print(model_path)
    
    if model_path in model_map: # reuse existing loaded model?
        model = model_map[model_path]
    else:

        print("../../../../../../"+model_path)
        model = {
            "torch": torch.load("../../../../../../"+model_path,  map_location = device),
            "yolo": None, #YOLO("../../../../../../"+model_path)
            }[model_config.get("type", "torch")]
        model_map[model_path] = model
    try:
        if cuda:
            model = model.cuda()
        else:
            model = model.cpu()
    except Exception as e:
        print(f"{head} Could not call model.{'cuda' if cuda else 'cpu'}() because {e}")
    if 'transform' in model_config:
        transform_class = import_functionality(model_config['transform'])
        transform = transform_class()
        input_size = transform.input_size
    else:
        transform = None
        input_size = None
    if 'output' in model_config:
        output_class = import_functionality(model_config['output'])
        output = output_class()
    else:
        output = None
    # try:
    #     model.eval() # what was the point of this?
    # except Exception as e:
    #     print(f"{head} Could not call model.eval() because {e}")
    return model, transform, input_size, output

def load_model(config):
    model, transform, input_size, output = get_model(config)
    entry = {
        'model': model,
        'transform': transform,
        'input_size': input_size,
        'output': output,
        'cuda': config.get('cuda', None) and torch.cuda.is_available(),
        'task': config.get('task', None),
        'dataset': config.get('dataset', None),
    }
    return entry

def load_models_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    models = {}
    for model_config in config['MODELS']:
        print(model_config)
        print("!!")
        models[model_config['name']] = load_model(model_config)

    return models

def load_datasets_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)
    datasets = {}
    for dataset in config['DATASETS']:
        datasets[dataset['name']] = {'class_names': dataset['class_names']}
    return datasets

print(f"{head} prerequisites loaded; starting server... ")
app = Flask(__name__)
print(f"{head} working directory: " + ML_DIRECTORY_PATH)
model_map = {}
print(os.getcwd())
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
        model_properties['path'] = model_path
        models[model_name] = load_model(model_properties)
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
def predict(model_name=None, input_data=None):
    global models
    global datasets
    if not model_name:
        model_name = request.form.get(MODEL_NAME_PARAM)
    if not input_data:
        input_data = request.form.get(INPUT_DATA_PARAM)

    if model_name in models:
        model = models[model_name]['model']
        if len(input_data) < 1024 and os.path.isfile(input_data) and models[model_name]['transform']:
            buffered = io.BytesIO()
            image = Image.open(input_data)
            image.save(buffered, format="JPEG")
            input_data = base64.b64encode(buffered.getvalue()).decode('utf-8')
        if models[model_name]['transform']:
            input_bytes = base64.b64decode(input_data)
            input_tensor = Image.open(io.BytesIO(input_bytes))
            input_tensor = models[model_name]['transform'].transform(input_tensor)
            input_batch = input_tensor.unsqueeze(0)
            if models[model_name]['cuda']:
                input_batch = input_batch.cuda()
            model.eval()
            output = model(input_batch)
        else:
            output = model(input_data)
        print(output)
        dataset = models[model_name]['dataset']
        response = {'prediction': output} # if isinstance(output, list) else output.tolist()
        print("Response:", response)
        if models[model_name]["output"]:
            response['prediction'] = models[model_name]['output'].process_output(response['prediction'])
        if dataset in datasets:
            response['class_names'] = datasets[dataset]['class_names']
        response['task'] = models[model_name]['task']
        response['dataset'] = models[model_name]['dataset']
        print("HERE")
        print("response:", response)
        print("HERE")
        return
        
        ret = jsonify(response)
        return ret
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
    print(f"{head} starting...")
    # app.run()
    import numpy
    print(numpy.__version__)
    predict("deeplabv3plus_cityscapes", ML_DIRECTORY_PATH + "input/ADE_val_00000793.jpg")
    """  prediction = torch.tensor(response['prediction'])
    
        prediction = prediction.max(1)[1].cpu().numpy()[0]
        dict1 = {}
        for x in prediction:
        	for y in x:
        		if y > 0:
        			if y in dict1:
        				dict1[y] += 1
	        		else:
	        			dict1[y] = 1
	        		
	        		
        print(dict1)        	
        if 15 in dict1:
        	suma +=	dict1[15]
        if 7 in dict1:
        	suma2 += dict1[7]"""
    
