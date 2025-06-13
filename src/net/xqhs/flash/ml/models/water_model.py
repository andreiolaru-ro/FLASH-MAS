import tensorflow as tf
from PIL import Image
import numpy as np
import os


class WaterDetectionModel:
    def __init__(self, model_path):
        self.model = tf.keras.models.load_model(model_path)
        self.input_size = (250, 250) 

    def predict(self, image_path):
        image = Image.open(image_path).resize(self.input_size).convert("RGB")
        img_array = np.array(image) / 255.0
        input_tensor = np.expand_dims(img_array, axis=0)
        output = self.model.predict(input_tensor)

      
        return float(output.squeeze().mean())

