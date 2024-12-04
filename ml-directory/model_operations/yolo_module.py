
from util import import_functionality, log
import model_store

ultralytics = import_functionality("ultralytics")

class YoloOps(model_store.ModelOperations):
    def load(self, model_config):
        model_path = model_config['path']
        cuda = model_config['cuda'] and model_store.cuda_available()
        try:
            self.model = ultralytics.YOLO(model_path)
        except Exception as e:
            log(f"Could not instantiate model because {e}")
            return None
        try:
            if cuda:
                self.model = self.model.cuda()
            else:
                self.model = self.model.cpu()
        except Exception as e:
            log(f"Could not call model.{'cuda' if cuda else 'cpu'}() because {e}")
        return self.model