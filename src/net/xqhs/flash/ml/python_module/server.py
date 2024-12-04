import os, pathlib
import shutil
import sys
import base64
from PIL import Image
import io
import json
from builtins import isinstance



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

PACKAGE_DIRECTORIES = ["pythonlib/lib/site-packages/", "pythonlib/lib/python3.11/site-packages/", "model_operations/"]


# construct system path, relative to how this file is run
# can be run from FLASH-MAS, or can be run directly from its directory

PYTHONLIB_PATH = [ML_DIRECTORY_PATH + dir for dir in PACKAGE_DIRECTORIES]

project_root = pathlib.Path(__file__)
project_root_str = str(project_root).replace("\\", "/")
# print(str(os.getcwd()), "\n", str(project_root.parent))
if(str(os.getcwd()) == str(project_root.parent)):
    log("fixing paths")
    # the file is run from its directory
    first_branch = ML_SRC_PATH.split("/")[0]
    while project_root_str.split("/")[-1] != first_branch:
        project_root = project_root.parent
        project_root_str = str(project_root).replace("\\", "/")
        ML_DIRECTORY_PATH = "../" + ML_DIRECTORY_PATH
project_root = project_root.parent
for one_path in PYTHONLIB_PATH:
    pylib_path = project_root.absolute()
    pylib_path = str(pylib_path) + "/" + one_path
    # pylib_path = pylib_path.replace("/", "\\")
    sys.path.insert(0, pylib_path) # TODO regexpreplace path in ML_SRC_PATH
log("System path: ", sys.path)

# imports

log("loading prerequisites...")

import server_operations
import model_store

log("prerequisites loaded")

log("working directory: " + ML_DIRECTORY_PATH)

models = model_store.load_models_from_config((ML_DIRECTORY_PATH + MODEL_CONFIG_FILE))
datasets = model_store.load_datasets_from_config(ML_DIRECTORY_PATH + MODEL_CONFIG_FILE)

log("Test exit.")
exit(0)

if __name__ == '__main__':
    # run from application
    log("starting...")
    server_operations.start()
else:
    # direct run from IDE
    log("nothing to do for now.")

