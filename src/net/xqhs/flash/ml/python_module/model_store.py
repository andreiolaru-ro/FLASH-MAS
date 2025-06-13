from util import log
import yaml  # ✅ Properly imported here
import io
import base64
from PIL import Image  # Required for image loading
import torch  # Required for tensor conversion

models = {}
datasets = {}

class ModelOperations:
    def load(self, config):
        self.model = None
        return self.model

    def process_input(self, input):
        return input

    def predict(self, input):
        return self.model(input)

    def process_output(self, output):
        return output

    def load_image(self, image_path):
        buffered = io.BytesIO()
        image = Image.open(image_path)
        image.save(buffered, format="JPEG")
        input_data = base64.b64encode(buffered.getvalue()).decode('utf-8')
        return input_data

    def image_to_tensor(self, input_data, model_name, cuda=False):
        input_bytes = base64.b64decode(input_data)
        input_tensor = Image.open(io.BytesIO(input_bytes))
        input_tensor = models[model_name]['transform'].transform(input_tensor)
        input_batch = input_tensor.unsqueeze(0)
        if cuda:
            input_batch = input_batch.cuda()
        return input_batch


def cuda_available():
    try:
        return torch.cuda.is_available()
    except Exception:
        return False


def import_functionality(name, pippackage=None, critical=False, autoinstall=False):
    from importlib import import_module
    from sys import stderr
    head = "<ML server> "

    def logE(*args): print(f"{head}", *args, file=stderr, flush=True)
    components = name.split('.')
    package = components[0]

    try:
        mod = import_module(package)
        for comp in components[1:]:
            mod = getattr(mod, comp)

        # If it's a module, return the 'cls' if defined
        if hasattr(mod, "__file__"):
            if hasattr(mod, "cls"):
                return getattr(mod, "cls")
            else:
                raise ImportError(f"Module '{name}' does not define 'cls'")
        else:
            return mod
    except Exception as e:
        pippackage = pippackage or name.split(".")[0]
        logE(f"{package} unavailable (use pip install {pippackage}): {e}")
        if critical:
            exit(1)
        return None


def load_models_from_config(config_file, storage_directory="", storage_prefix=""):
    global models
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    for model_config in config['MODELS']:
        if 'code' not in model_config:
            log("Code entry not found for", model_config['name'], "; model will not be available")
            continue

        log("Loading", model_config['name'])

        cls = import_functionality(model_config['code'])
        if not cls:
            log("Failed to import", model_config['code'], "- skipping model", model_config['name'])
            continue

        modelCode = cls()

        if model_config['path'].startswith(storage_prefix):
            model_config['path'] = storage_directory + model_config['path'][len(storage_prefix):]

        modelCode.load(model_config)
        models[model_config['name']] = modelCode

    return models


def load_datasets_from_config(config_file):
    global datasets
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    for dataset in config['DATASETS']:
        datasets[dataset['name']] = {'class_names': dataset['class_names']}

    return datasets
