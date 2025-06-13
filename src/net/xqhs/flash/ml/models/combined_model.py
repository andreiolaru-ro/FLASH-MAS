from .unet_model import UNetModel
from .water_model import WaterDetectionModel
from .brightness_model import BrightnessModel

class CombinedModel:
    def __init__(self, unet_path, water_path):
        print("[DEBUG] CombinedModel.__init__ called")
        self.unet = UNetModel(unet_path)
        self.water = WaterDetectionModel(water_path)
        self.brightness_model = BrightnessModel()

    def predict(self, image_path):
        print("[DEBUG] CombinedModel.predict called with:", image_path)
        model_choice = self.brightness_model.predict(image_path)
        print("[DEBUG] Brightness model selected:", model_choice)

        if model_choice == "unet":
            output = self.unet.predict(image_path)
        else:
            output = self.water.predict(image_path)

        print("[DEBUG] CombinedModel prediction output:", output)
        return {
            "model": model_choice,
            "result": output
        }

