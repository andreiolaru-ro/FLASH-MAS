import os, pathlib
import sys

from util import import_functionality, log
from constants import *

# getting the libraries
# Linux:
# create directory ml-directory/pythonlib
# cd ml-directory/pythonlib
#  python -m venv .
#  source bin/activate
#  pip install <package>

# Windows:
# create directory ml-directory/pythonlib
# cd ml-directory/pythonlib
# python -m venv <c:\path\to\myenv>
# .\Scripts\activate.bat
# .\Scripts\pip.exe install <package>

# these are relative to ML_DIRECTORY
PACKAGE_DIRECTORIES = ["pythonlib/lib/site-packages/", "pythonlib/lib/python3.11/site-packages/", "model_operations/"]


# construct system path, relative to how this file is run
# can be run from FLASH-MAS, or can be run directly from the directory of this file

PYTHONLIB_PATH = [ML_DIRECTORY_PATH + dir for dir in PACKAGE_DIRECTORIES]
ORIGINAL_ML_DIRECTORY_PATH = ML_DIRECTORY_PATH

project_root = pathlib.Path(__file__)
project_root_str = str(project_root).replace("\\", "/")
# print(str(os.getcwd()), "\n", str(project_root.parent))
# fixpaths is true if the file is run from its directory
fixpaths = str(os.getcwd()) == str(project_root.parent)
log("fix paths:", fixpaths)
first_branch = ML_SRC_PATH.split("/")[0]
while project_root_str.split("/")[-1] != first_branch:
    project_root = project_root.parent
    project_root_str = str(project_root).replace("\\", "/")
    if fixpaths: ML_DIRECTORY_PATH = "../" + ML_DIRECTORY_PATH
project_root = project_root.parent
log("project root: " + str(project_root))
log("working directory for ML: " + ML_DIRECTORY_PATH)
for one_path in PYTHONLIB_PATH:
    pylib_path = project_root.absolute()
    pylib_path = str(pylib_path) + "/" + one_path
    # pylib_path = pylib_path.replace("/", "\\")
    sys.path.insert(0, pylib_path) # TODO regexpreplace path in ML_SRC_PATH
log("System path: ", sys.path)

# imports

log("loading prerequisites...")

import MLServer
import model_store

log("prerequisites loaded")


models = model_store.load_models_from_config(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE, ML_DIRECTORY_PATH, ORIGINAL_ML_DIRECTORY_PATH)
datasets = model_store.load_datasets_from_config(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE)

# MLServer.predict()
# log("Test exit.")
# exit(0)

if __name__ == '__main__':
    log("starting...")
    MLServer.start()
else:
    log("nothing to do for now.")

