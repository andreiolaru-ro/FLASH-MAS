

from util import import_functionality, log
from constants import *

flask = import_functionality("flask")
log("creating server... ")
app = flask.Flask(__name__)

def start():
    app.run()

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
def predict():
    global models
    global datasets
    model_name = request.form.get(MODEL_NAME_PARAM)
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
            output = model(input_batch)
        else:
            output = model(input_data)
        dataset = models[model_name]['dataset']
        response = {'prediction': output if isinstance(output, list) else output.tolist()}
        # log("Response:", response)
        if models[model_name]["output"]:
            response['prediction'] = models[model_name]['output'].process_output(response['prediction'])
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

