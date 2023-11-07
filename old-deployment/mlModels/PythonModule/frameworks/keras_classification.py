from interfaces.classification import Classification
from tensorflow import keras
import numpy as np
import rdflib

class KerasClassification(Classification):
    def __init__(self, model_extension):
        super().__init__(model_extension)

    def load(self, path, description):
        for x in description.query("SELECT ?pred ?obj WHERE { <http://example.org#Loss> ?pred ?obj }"):
            loss = x[1].split('#')[-1]

        for x in description.query("SELECT ?pred ?obj WHERE { <http://example.org#Optimizer> ?pred ?obj }"):
            optimizer = x[1].split('#')[-1]

        metrics = []
        for x in description.query("SELECT ?sub ?pred WHERE { ?sub ?pred <http://www.w3.org/ns/mls#EvaluationMeasure> }"):
            metrics.append(x[0].split('#')[-1].lower())

        try:
            model = keras.models.load_model(path)
            model.compile(loss=loss, optimizer=optimizer, metrics=metrics)
        except:
            return None, "Could not load model"
        finally:
            stringlist = []
            model.summary(print_fn=lambda x: stringlist.append(x))
            model_summary = "\n".join(stringlist)

            return model, model_summary

    def predict(self, model, inputs):
        predictions = np.argmax(model.predict(inputs), axis=1)

        return predictions.tolist()

    def train(self, model, inputs, labels, batch_size, epochs):
        try:
            model.fit(inputs, labels, batch_size=batch_size, epochs=epochs, validation_split=0.1)
        except Exception as e:
            return False, str(e)
        finally:
            return True, "Model has been trained"

    def score(self, model, inputs, labels):
        try:
            predictions = np.argmax(models[model].predict(data), axis=1)
            accuracy = np.count_nonzero(predictions == labels) / len(labels) * 100
        except Exception as e:
            return None, str(e)
        finally:
            return accuracy, "Prediction complere"

    def save(self, model, path):
        try:
            model.save(path)
        except:
            return False, "Could not save model"
        finally:
            stringlist = []
            model.summary(print_fn=lambda x: stringlist.append(x))
            model_summary = "\n".join(stringlist)

            return True, model_summary