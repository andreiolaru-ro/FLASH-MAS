
from util import import_functionality, log
import model_store

ultralytics = import_functionality("ultralytics")

class YoloOps(model_store.ModelOperations):
    def load(self, model_config):
        model_path = model_config['path']
        cuda = model_config['cuda'] and model_store.cuda_available()
        try:
            log("Model at ", model_path, "; cuda is", "on" if cuda else "off")
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
    
    def process_output2(self, output, classes):
        from shapely.geometry import Polygon
        i = -1
        nr = {}
        area = {}
        for output in output:
            if output.masks == None:
                continue
            for mask in output.masks:
                lst = []
                i=i+1
                cls = output.boxes[i].cls[0].item()
                if classes != "All" and cls not in classes:
                    continue
                for x in mask.xy:
                    if cls not in nr: nr[cls] = 0
                    nr[cls] += 1
                    for y in x:
                        lst.append(y)
                    polygon = Polygon(lst)
                    if cls not in area: area[cls] = 0
                    area[cls] += polygon.area            
        return (nr, area)
    
    def process_output(self, output):
        # default: pedestrians
        numbers, areas = self.process_output2(output, [0])
        return (numbers.get(0, 0), areas.get(0, 0))