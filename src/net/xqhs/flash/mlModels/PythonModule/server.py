import base64
import json
# from utils import *


try: from flask import Flask, request 
except Exception as e:
	print("Flask unavailable (use pip install flask ): ", e)
	exit(1)

try: import numpy as np
except Exception as e: print("Numpy unavailable (use pip install numpy ): ", e)

try:
	from tensorflow import keras
	from frameworks.keras_classification import KerasClassification
except Exception as e:
	print("Keras/Tensorflow unavailable (use pip install tensorflow rdflib ):", e)

try: from frameworks.torch_classification import TorchClassification
except Exception as e: print("Torch unavailable (use pip install torch torchvision torchaudio torchsummary ):", e)

app = Flask(__name__)

frameworks = {'Keras': KerasClassification(model_extension='h5'), 'PyTorch': TorchClassification(model_extension='pt', input_size=(64, 1, 28, 28))}
models = {}
framework = frameworks['Keras']

@app.route('/')
def hello():
    return 'Hello, World!'

@app.route('/load', methods=['GET'])
def load():
    data = json.loads(request.data)

    g = rdflib.Graph().parse(format="turtle", data=data['description'])

    model_bytes = base64.b64decode(data['model'])

    for x in g.query(query_subject(DATA['Name'])):
        model_name = x[1].split('#')[-1]

    for x in g.query(query_object(MLS["Software"])):
        software = x[0].split('#')[-1]

    model_path  = 'models/' + model_name + '_tmp.' + frameworks[software].model_extension
    with open(model_path, 'wb') as file:
        file.write(model_bytes)
    model, log  = frameworks[software].load(model_path, g)
    
    if model is not None:
        models[model_name] = {}
        models[model_name]['model'] = model
        models[model_name]['framework'] = software
        return log, 200
    else:
        return log, 500

    return "Invalid arguments", 400

@app.route('/predict', methods=['POST'])
def predict():
    req = json.loads(request.data)

    if check_parameters(['model', 'data'], req):
        model = req['model']

        if model in models:
            data  = req['data']

            predictions = frameworks[models[model]['framework']].predict(models[model]['model'], np.array(data))

            return json.dumps(predictions), 200

    return "Invalid arguments", 400

@app.route('/train', methods=['POST'])
def train_model():
    req = json.loads(request.data)

    if check_parameters(['model', 'data', 'epochs', 'labels', 'batch_size'], req):
        model  = req['model']

        if model in models:
            batch_size = np.array(req['batch_size'])
            epochs = np.array(req['epochs'])
            labels = np.array(req['labels'])
            data   = np.array(req['data'])

            success, log = frameworks[models[model]['framework']].train(models[model]['model'], data, labels, batch_size, epochs)

            return log, 200 if success else 500

    return "Invalid arguments", 400

@app.route('/score', methods=['POST'])
def score():
    req = json.loads(request.data)

    if check_parameters(['model', 'data', 'labels'], req):
        model  = req['model']

        if model in models:
            labels = np.array(req['labels'])
            data   = np.array(req['data'])

            accuracy, log = frameworks[models[model]['framework']].score(models[model]['model'], data, labels)

            if accuracy is not None:
                return str(accuracy) + '%', 200
            else:
                return log, 500

    return "Invalid arguments", 400

@app.route('/save', methods=['GET'])
def save_model():
    if check_parameters(['model'], request.args):
        model = request.args.get('model')

        if model in models:
            path  = 'models/' + model + '_tmp.' + framework.model_extension

            success, log = frameworks[models[model]['framework']].save(models[model]['model'], path)

            with open(path, 'rb') as file:
                model_byte = file.read()
                model = base64.b64encode(model_byte).decode("utf-8")

            return model, 200 if success else 500

    return "Invalid arguments", 400

if __name__ == '__main__':
    print("Starting Flask server")
    app.run(port=5000)