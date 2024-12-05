from util import import_functionality, log

yaml = import_functionality("yaml", pippackage = "pyyaml")

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
    def image_to_tensor(self, input_data, cuda = False):
        input_bytes = base64.b64decode(input_data)
        input_tensor = Image.open(io.BytesIO(input_bytes))
        input_tensor = models[model_name]['transform'].transform(input_tensor)
        input_batch = input_tensor.unsqueeze(0)
        if cuda:
            input_batch = input_batch.cuda()
        return input_batch


def cuda_available():
    torch = import_functionality("torch")
    try:
        return torch.cuda.is_available()
    except Exception as e:
        return False

def load_models_from_config(config_file, storage_directory = "", storage_prefix = ""):
    global models
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    for model_config in config['MODELS']:
        if 'code' not in model_config: 
            log("Code entry not found for", model_config['name'], "; model will not be available")
            continue
        log("Loading", model_config['name'])
        # get model-specific processing code
        cls = import_functionality(model_config['code'])
        modelCode = cls()
        # fix path
        if model_config['path'].startswith(storage_prefix):
            model_config['path'] = storage_directory + model_config['path'][len(storage_prefix):]
        # load model
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
