from util import import_functionality, log

yaml = import_functionality("yaml", pippackage = "pyyaml")

model_map = {}

class ModelOperations:
    def load(self, config):
        return None
    def process_input(self, input):
        return input
    def predict(self, model, input):
        return model(input)
    def process_output(self, output):
        return output


def cuda_available():
    try:
        return torch.cuda.is_available()
    except Exception as e:
        return False

def get_model(model_config):
    global model_map
    model_path = model_config['path']
    cuda = model_config['cuda'] and cuda_available()
    device = 'cuda:0' if cuda else 'cpu'

    if model_path in model_map: # reuse existing loaded model?
        model = model_map[model_path]
    else:
        model = {
            "torch": torch.load(model_path,  map_location = device),
            "yolo": YOLO(model_path),
            }[model_config.get("type", "torch")]
        model_map[model_path] = model
    try:
        if cuda:
            model = model.cuda()
        else:
            model = model.cpu()
    except Exception as e:
        log("Could not call model.{'cuda' if cuda else 'cpu'}() because {e}")
    if 'transform' in model_config:
        transform_class = import_functionality(model_config['transform'])
        transform = transform_class()
        input_size = transform.input_size
    else:
        transform = None
        input_size = None
    if 'output' in model_config:
        output_class = import_functionality(model_config['output'])
        output = output_class()
    else:
        output = None
    # try:
    #     model.eval() # what was the point of this?
    # except Exception as e:
    #     log("Could not call model.eval() because {e}")
    return model, transform, input_size, output

def load_models_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)

    models = {}
    for model_config in config['MODELS']:
        if 'code' not in model_config: continue
        log("Loading", model_config['name'])
        cls = import_functionality(model_config['code'])
        modelCode = cls()
        modelCode.load(model_config)
        models[model_config['name']] = modelCode        

    return models

def load_datasets_from_config(config_file):
    with open(config_file, 'r') as f:
        config = yaml.safe_load(f)
    datasets = {}
    for dataset in config['DATASETS']:
        datasets[dataset['name']] = {'class_names': dataset['class_names']}
    return datasets
