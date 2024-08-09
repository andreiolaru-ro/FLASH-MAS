import os, pathlib, sys

class BaseTransform:
    def transform(self, x):
        pass
    def process_output(self, output):
        return output
class YOLOTransform(BaseTransform):
    def process_output(self, output, classes = "All"):
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
class YOLOGetPedestrians(YOLOTransform):
    def process_output(self, output):
        numbers, areas = YOLOTransform.process_output(self, output, classes = [0])
        return (numbers.get(0, 0), areas.get(0, 0))
    

ML_DIRECTORY_PATH = "ml-directory/";
ML_SRC_PATH = "src/net/xqhs/flash/ml/";
PYTHONLIB_PATH = []
PYTHONLIB_PATH.append(ML_DIRECTORY_PATH + "pythonlib/lib/site-packages/")
PYTHONLIB_PATH.append(ML_DIRECTORY_PATH + "pythonlib/lib/python3.11/site-packages/")

print("loading prerequisites...")
project_root = pathlib.Path(__file__)
first_branch = ML_SRC_PATH.split("/")[0]
while str(project_root).split("/")[-1] != first_branch:
    project_root = project_root.parent
    ML_DIRECTORY_PATH = "../" + ML_DIRECTORY_PATH
project_root = project_root.parent
print(project_root)
print(ML_DIRECTORY_PATH)
for one_path in PYTHONLIB_PATH:
    pylib_path = project_root.absolute()
    pylib_path = str(pylib_path) + "/" + one_path
    print(pylib_path)
    # pylib_path = pylib_path.replace("/", "\\")
    sys.path.insert(0, pylib_path) # TODO regexpreplace path in ML_SRC_PATH
print(sys.path)


from ultralytics import YOLO
import numpy

model= YOLO(ML_DIRECTORY_PATH + "/models/yolov8n-seg.pt")
model.cpu()
input = ML_DIRECTORY_PATH + "/input/ADE_val_00000794.jpg"
results = model(input)
res = YOLOGetPedestrians().process_output(results)
print(res)

