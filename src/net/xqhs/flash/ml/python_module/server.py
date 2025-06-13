import os, pathlib
import sys
import yaml
import flask

from util import import_functionality, log
from constants import *

PACKAGE_DIRECTORIES = [
    "pythonlib/lib/site-packages/",
    "model_operations/"
]

PYTHONLIB_PATH = [ML_DIRECTORY_PATH + dir for dir in PACKAGE_DIRECTORIES]
ORIGINAL_ML_DIRECTORY_PATH = ML_DIRECTORY_PATH

project_root = pathlib.Path(__file__)
project_root_str = str(project_root).replace("\\", "/")
fixpaths = str(os.getcwd()) == str(project_root.parent)
log("fix paths:", fixpaths)

first_branch = ML_SRC_PATH.split("/")[0]
while project_root_str.split("/")[-1] != first_branch:
    project_root = project_root.parent
    project_root_str = str(project_root).replace("\\", "/")
    if fixpaths:
        ML_DIRECTORY_PATH = "../" + ML_DIRECTORY_PATH

project_root = project_root.parent
log("project root:", str(project_root))
log("working directory for ML:", ML_DIRECTORY_PATH)

for one_path in PYTHONLIB_PATH:
    sys.path.insert(0, str(project_root.absolute()) + "/" + one_path)

log("System path:", sys.path)

# Load Python dependencies
log("loading prerequisites...")
log("prerequisites loaded")

import MLServer
import model_store

models = model_store.load_models_from_config(
    ML_DIRECTORY_PATH + MODEL_CONFIG_FILE,
    ML_DIRECTORY_PATH,
    ORIGINAL_ML_DIRECTORY_PATH
)
datasets = model_store.load_datasets_from_config(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE)

if __name__ == '__main__':
    log("starting...")
    MLServer.start()
else:
    log("nothing to do for now.")
