

from util import import_functionality, log, logE
from constants import *
from model_store import *

flask = import_functionality("flask")
log("creating server... ")
app = flask.Flask(__name__)

def start():
    app.run(port = SERVER_PORT)

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
    global models
    global datasets
    model_name = flask.request.form.get(MODEL_NAME_PARAM)
    input_data = flask.request.form.get(INPUT_DATA_PARAM)

    if model_name in models:
        model = models[model_name]
        
        try:
            processed_input = model.process_input(input_data)
            output = model.predict(processed_input)
            result = model.process_output(output)
            response = {'prediction': (result if isinstance(result, list) else [result])}
            ret = flask.jsonify(response)
            log("returned", ret)
            return ret
        except Exception as e:
             flask.jsonify({'error': f'Model "{model_name}" failed to run with exception {e}.'}), 500
    else:
        return flask.jsonify({'error': f'Model "{model_name}" does not exist.'}), 404


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
    return flask.jsonify({'models': returned_models})


@app.route('/' + EXPORT_MODEL_SERVICE, methods=['POST'])
def export_model():
    global models
    model_name = flask.request.form.get(MODEL_NAME_PARAM)
    export_directory_path = flask.request.form.get(EXPORT_PATH_PARAM)

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

        return flask.jsonify({'message': f'Model "{model_name}" has been successfully exported.', 'destination': export_directory_path})
    else:
        return flask.jsonify({'error': f'Model "{model_name}" does not exist.'}), 404

