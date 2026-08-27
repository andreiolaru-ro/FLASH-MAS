import os
import sys
import json
import pickle
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from util import import_functionality, log

import_functionality("flask", critical=True)
from flask import Flask, request, jsonify

DEMO_PACKAGES = ["cowsay", "pyfiglet", "emoji"]
demo_modules = {name: import_functionality(name, critical=True) for name in DEMO_PACKAGES}
cowsay = demo_modules["cowsay"]
pyfiglet = demo_modules["pyfiglet"]
emoji = demo_modules["emoji"]

import_functionality("scikit-learn", pip_name="scikit-learn", critical=True)
import_functionality("numpy", critical=True)

import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.neural_network import MLPClassifier
from sklearn.datasets import load_iris, make_classification
from sklearn.preprocessing import StandardScaler

SERVER_PORT = 5099

app = Flask(__name__)

trained_models = {}
trained_scalers = {}

@app.route('/ping', methods=['GET'])
def ping():
    return jsonify({"status": "ok"})


@app.route('/call', methods=['POST'])
def call():
    text = request.form.get('content', 'hello from FLASH-MAS')
    result = {
        "cowsay": cowsay.get_output_string('cow', text),
        "figlet": pyfiglet.figlet_format(text),
        "emoji": emoji.emojize(text + " :rocket:"),
    }
    log("returned result for input:", text)
    return jsonify(result)



@app.route('/train', methods=['POST'])
def train():
    """
    Train a model identified by the 'model_id' form parameter.
    Supported model IDs:
      - 'logistic_iris'  : Fast Logistic Regression on Iris dataset (~miliseconds)
      - 'mlp_large'      : Heavy MLP on large synthetic dataset (~1 minute, no artificial delay)
    """
    model_id = request.form.get('model_id', '')

    if not model_id:
        return jsonify({"status": "error", "message": "Missing model_id parameter"}), 400

    log(f"Starting training for model: {model_id}")
    start_time = time.time()

    try:
        if model_id == 'logistic_iris':
            # Model 1: Fast Logistic Regression on Iris dataset
            iris = load_iris()
            X, y = iris.data, iris.target

            scaler = StandardScaler()
            X_scaled = scaler.fit_transform(X)

            model = LogisticRegression(max_iter=1000, random_state=42)
            model.fit(X_scaled, y)

            trained_models[model_id] = model
            trained_scalers[model_id] = scaler

            elapsed = time.time() - start_time
            classes = iris.target_names.tolist()
            log(f"Model '{model_id}' trained in {elapsed:.3f}s. Classes: {classes}")
            return jsonify({
                "status": "success",
                "model_id": model_id,
                "description": "Logistic Regression on Iris dataset",
                "training_time_s": round(elapsed, 3),
                "classes": classes,
                "n_samples": len(X),
                "n_features": X.shape[1]
            })

        elif model_id == 'mlp_large':
            # Model 2: Heavy MLP on large synthetic dataset
            log("Generating large synthetic dataset (500k samples, 100 features)...")
            X, y = make_classification(
                n_samples=500000,
                n_features=100,
                n_informative=50,
                n_redundant=25,
                n_classes=5,
                random_state=42
            )

            scaler = StandardScaler()
            X_scaled = scaler.fit_transform(X)

            model = MLPClassifier(
                hidden_layer_sizes=(512, 512, 256, 128),
                max_iter=500,
                random_state=42,
                verbose=False,
                early_stopping=False
            )
            log("Training MLP... this will take approximately 1 minute.")
            model.fit(X_scaled, y)

            trained_models[model_id] = model
            trained_scalers[model_id] = scaler

            elapsed = time.time() - start_time
            log(f"Model '{model_id}' trained in {elapsed:.1f}s after {model.n_iter_} iterations.")
            return jsonify({
                "status": "success",
                "model_id": model_id,
                "description": "MLP on large synthetic dataset (500k samples, 100 features)",
                "training_time_s": round(elapsed, 1),
                "n_iterations": model.n_iter_,
                "n_samples": len(X),
                "n_features": X.shape[1],
                "n_classes": 5
            })

        else:
            return jsonify({"status": "error", "message": f"Unknown model_id: '{model_id}'"}), 400

    except Exception as e:
        log(f"Error training model '{model_id}': {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


@app.route('/predict', methods=['POST'])
def predict():
    """
    Run inference using a trained model.
    Parameters (form):
      - 'model_id'  : ID of the trained model
      - 'features'  : JSON array of feature values (e.g. "[5.1, 3.5, 1.4, 0.2]")
    """
    model_id = request.form.get('model_id', '')
    features_raw = request.form.get('features', '')

    if not model_id:
        return jsonify({"status": "error", "message": "Missing model_id parameter"}), 400
    if not features_raw:
        return jsonify({"status": "error", "message": "Missing features parameter"}), 400
    if model_id not in trained_models:
        return jsonify({"status": "error", "message": f"Model '{model_id}' has not been trained yet"}), 400

    try:
        features = json.loads(features_raw)
        X = np.array(features).reshape(1, -1)

        scaler = trained_scalers[model_id]
        model = trained_models[model_id]

        X_scaled = scaler.transform(X)

        start_time = time.time()
        prediction = model.predict(X_scaled)
        probabilities = model.predict_proba(X_scaled)
        elapsed_ms = (time.time() - start_time) * 1000

        predicted_class = int(prediction[0])
        confidence = float(np.max(probabilities))

        log(f"Prediction for '{model_id}': class={predicted_class}, confidence={confidence:.3f}, time={elapsed_ms:.2f}ms")
        return jsonify({
            "status": "success",
            "model_id": model_id,
            "predicted_class": predicted_class,
            "confidence": round(confidence, 4),
            "inference_time_ms": round(elapsed_ms, 2)
        })

    except Exception as e:
        log(f"Error during inference for model '{model_id}': {e}")
        return jsonify({"status": "error", "message": str(e)}), 500


if __name__ == '__main__':
    log("all required packages available, starting server on port", SERVER_PORT)
    app.run(host='0.0.0.0', port=SERVER_PORT)