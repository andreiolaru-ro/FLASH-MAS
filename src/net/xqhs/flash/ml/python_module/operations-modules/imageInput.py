# transformation operations for all the image based models
from PIL import Image
import io
import base64


try:
    import torch
except Exception as e:
    print("PyTorch unavailable (use pip install torch ):", e)
    print("If there is a problem with MobileNetV2, try tu run the Regenerate.py script in "
          "src-experiments\aifolk\ml_driver")
    exit(1)
try:
    from torchvision import transforms
except Exception as e:
    print("Torchvision unavailable (use pip install torchvision ):", e)
    exit(1)


def transform_op(model_config):
    if model_config['input_space'] == "RGB":
        list_of_transforms = []
        if 'input_size' in model_config:
            input_size = model_config['input_size']
            list_of_transforms.append(transforms.Resize(size=input_size))
        list_of_transforms.append(transforms.ToTensor())
        if 'normalization' in model_config:
            mean = model_config['normalization']['mean']
            std = model_config['normalization']['std']
            list_of_transforms.append(transforms.Normalize(mean, std))
        transform = transforms.Compose(list_of_transforms)
        return transform

def predict_op(model_name, input_data, models):
    model = models[model_name]['model']
    model.eval()
    input_bytes = base64.b64decode(input_data)
    input_tensor = Image.open(io.BytesIO(input_bytes))
    if models[model_name]['transform']:
        input_tensor = models[model_name]['transform'](input_tensor)
    input_batch = input_tensor.unsqueeze(0)
    if models[model_name]['cuda']:
        input_batch = input_batch.cuda()
    output = model(input_batch)
    class_names = models[model_name]['class_names']
    return output, class_names