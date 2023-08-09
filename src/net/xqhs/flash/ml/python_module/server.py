import torch
from flask import Flask, request, jsonify
import yaml
from torchvision import transforms
from PIL import Image
import io
import base64


REGENERATE = False
# REGENERATE = True # normally comment this

if REGENERATE:
	import requests
	model = torch.hub.load('pytorch/vision:v0.10.0', 'mobilenet_v2', pretrained=True)
	model.eval()
	torch.save(model, 'models/mobilenetv2.pth')
	exit(0)




def load_models_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    models = {}
    for model_config in config['MODELS']:
        model_name = model_config['name']
        model_path = model_config['path']
        cuda = model_config['cuda'] and torch.cuda.is_available()
        device = 'cuda:0' if cuda else 'cpu'

        model = torch.load(model_path,  map_location = device)
        if cuda:
            model = model.cuda()
        else:
            model = model.cpu()

        transform = None
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

        models[model_name] = {
            'model': model,
            'input_size': input_size,
            'cuda': cuda,
            'transform': transform
        }
        if 'class_names' in model_config:
            models[model_name]['class_names'] = model_config['class_names']

    return models

app = Flask(__name__)
models = load_models_from_config("config.yaml")

@app.route('/add_model', methods=['POST'])
def add_model():
    model_name = request.form.get('model_name')
    model_file = request.files.get('model_file')

    if model_name and model_file:
        model = torch.load(model_file)
        models[model_name] = model
        return jsonify({'message': f'Model "{model_name}" has been successfully added.'})
    else:
        return jsonify({'error': 'Model name and/or model file are missing.'}), 400

@app.route('/load_model', methods=['POST'])
def load_model():
    model_name = request.form.get('model_name')

    if model_name in models:
        model = models[model_name]
        model.eval()
        return jsonify({'message': f'Model "{model_name}" has been successfully loaded.'})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404

@app.route('/predict', methods=['POST'])
def predict():
    model_name = request.form.get('model_name')
    input_data = request.form.get('input_data')

    if model_name in models:
        model = models[model_name]['model']
        input_bytes = base64.b64decode(input_data)
        input_tensor = Image.open(io.BytesIO(input_bytes))
        if models[model_name]['transform']:
            input_tensor = models[model_name]['transform'](input_tensor)
        input_batch = input_tensor.unsqueeze(0)
        if models[model_name]['cuda']:
            input_batch = input_batch.cuda()
        output = model(input_batch)
        class_names = models[model_name]['class_names']
        return jsonify({'prediction': output.tolist(), 'class_names': class_names})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404

@app.route('/export_model', methods=['POST'])
def export_model():
    model_name = request.form.get('model_name')
    export_file = request.form.get('export_file')

    if model_name in models:
        model = models[model_name]
        torch.save(model, export_file)
        return jsonify({'message': f'Model "{model_name}" has been successfully exported.'})
    else:
        return jsonify({'error': f'Model "{model_name}" does not exist.'}), 404

if __name__ == '__main__':
    app.run()
