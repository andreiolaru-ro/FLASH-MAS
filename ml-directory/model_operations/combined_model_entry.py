from net.xqhs.flash.ml.models.combined_model import CombinedModel

class CombinedModelWrapper:
    def __init__(self, config=None, root_path=None, original_root_path=None):
        self.model = CombinedModel(config=config, root_path=root_path, original_root_path=original_root_path)

    def predict(self, *args, **kwargs):
        return self.model.predict(*args, **kwargs)

# This is what model_store expects to find and call
cls = CombinedModelWrapper

