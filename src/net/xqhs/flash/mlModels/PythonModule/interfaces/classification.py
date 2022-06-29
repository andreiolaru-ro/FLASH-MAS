from .task import Task
from typing import *

class Classification(Task):
    def load(self, path, description) -> (Any, str):
        pass

    def predict(self, model, inputs) -> list:
        pass

    def train(self, model, inputs, labels, batch_size, epochs) -> (bool, str):
        pass

    def score(self, model, inputs, labels) -> (float, str):
        pass

    def save(self, model, path) -> (bool, str):
        pass