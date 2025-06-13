import torch
from torchvision import transforms
from PIL import Image
import segmentation_models_pytorch as smp

class UNetModel:
    def __init__(self, model_path):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

     
        globals_dict = {
            "segmentation_models_pytorch.decoders.unet.model.Unet": smp.Unet
        }

       
        with torch.serialization.safe_globals(globals_dict):
            self.model = torch.load(model_path, map_location=self.device, weights_only=False)

        self.model.to(self.device)
        self.model.eval()

        self.transform = transforms.Compose([
            transforms.Resize((256, 256)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406],
                                 std=[0.229, 0.224, 0.225])
        ])

    def predict(self, image_path):
        image = Image.open(image_path).convert("RGB")
        input_tensor = self.transform(image).unsqueeze(0).to(self.device)

        with torch.no_grad():
            output = self.model(input_tensor)

            if output.shape[1] > 1:
                output = output[:, 0, :, :]

            output = output.squeeze(0)
            probs = torch.sigmoid(output)
            avg_prob = probs.mean().item()

        return avg_prob
