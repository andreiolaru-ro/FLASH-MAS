import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from util import import_functionality, log

import_functionality("flask", critical=True)
from flask import Flask, request, jsonify

DEMO_PACKAGES = ["cowsay", "pyfiglet", "emoji"]
demo_modules = {name: import_functionality(name, critical=True) for name in DEMO_PACKAGES}
cowsay = demo_modules["cowsay"]
pyfiglet = demo_modules["pyfiglet"]
emoji = demo_modules["emoji"]

SERVER_PORT = 5099

app = Flask(__name__)


@app.route('/ping', methods=['GET'])
def ping():
    return jsonify({"status": "ok"})

@app.route('/list', methods=['GET'])
def list_demo_packages():
    return jsonify({"available_demo_packages": DEMO_PACKAGES})

@app.route('/call', methods=['POST'])
def call():
    text = request.form.get('content', 'hello from FLASH-MAS')
    result = {
        "cowsay": cowsay.get_output_string('cow', text),
        "figlet": pyfiglet.figlet_format(text),
        "emoji": emoji.emojize(text + " :rocket:"),
    }
    log("returned result for input:", text)
    return jsonify(result)


if __name__ == '__main__':
    log("all required packages available, starting server on port", SERVER_PORT)
    app.run(host='0.0.0.0', port=SERVER_PORT)