import cv2
import numpy as np

class BrightnessModel:
    def __init__(self):
        pass

    def predict(self, image_path):
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Could not load image: {image_path}")
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        brightness_score = np.mean(gray)
        return "unet" if brightness_score < 120 else "water"
