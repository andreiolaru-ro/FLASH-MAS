from flask import Flask, request
from tensorflow import keras
import numpy as np
import json
from utils import *
from frameworks.keras_classification import KerasClassification
from frameworks.torch_classification import TorchClassification

app = Flask(__name__)

frameworks = {'keras': KerasClassification(model_extension='h5'), 'torch': TorchClassification(model_extension='pt', input_size=(64, 1, 28, 28))}
models = {}
framework = frameworks['keras']

@app.route('/load', methods=['GET'])
def load():
    data = request.data

    g = rdflib.Graph().parse(format="turtle", data=data)

    if check_parameters(['model'], request.args):
        model_name = request.args.get('model')
        model_path = 'models/' + model_name + '.' + framework.model_extension
        model, log = framework.load(model_path, "categorical_crossentropy", "adam", ["accuracy"])
        
        if model is not None:
            models[model_name] = {}
            models[model_name]['model'] = model
            models[model_name]['framework'] = 'keras' #g.query(query_object("Software"))[0]
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
    if check_parameters(['model', 'name'], request.args):
        model = request.args.get('model')

        if model in models:
            name  = request.args.get('name')
            path  = 'models/' + name + '.' + framework.model_extension

            success, log = frameworks[models[model]['framework']].save(models[model]['model'], path)

            return log, 200 if success else 500

    return "Invalid arguments", 400

if __name__ == '__main__':
    app.run(port=5000)