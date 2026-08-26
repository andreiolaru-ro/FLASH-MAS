"""
Python side of PythonJepBridgeDriver
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from util import import_functionality, require_package, log

# jep is needed by the Java side (jar + native library live in the pip package),
# so probing for it here is what triggers its automatic installation. It has to be
# checked without importing it -- jep refuses to import outside a JVM.
require_package("jep", critical=True)
np = import_functionality("numpy", critical=True)


def run_inference(payload):
    """Default entry point: echo the payload back with a 'prediction', like the reference repo's demo."""
    log("run_inference called with:", payload)
    return {"prediction": 42, "echo": str(payload)}


def uses_numpy(payload):
    """Shows that an auto-installed third-party package is usable from the embedded interpreter."""
    log("uses_numpy called with:", payload)
    return float(np.sum([1, 2, 3]))


if __name__ == '__main__':
    log("all required packages available")