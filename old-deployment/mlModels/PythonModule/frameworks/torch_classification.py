from interfaces.classification import Classification
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torchvision import datasets, transforms
from torch.optim.lr_scheduler import StepLR
from torchsummary import summary

class TorchClassification(Classification):
    def __init__(self, model_extension, input_size):
        super().__init__(model_extension)
        self.input_size = input_size

    def load(self, path, description):
        try:
            model = torch.jit.load(path)
        except:
            return None, "Could not load model"

        return model, ""#str(summary(model, self.input_size))

    def predict(self, model, inputs):
        model.eval()
        inputs = torch.from_numpy(inputs).float()

        with torch.no_grad():
            output = model(inputs)
            predictions = output.argmax(dim=1, keepdim=False)

        return predictions.tolist()

    def train(self, model, inputs, labels, batch_size, epochs):
        model.train()
        inputs = torch.from_numpy(inputs).float()
        optimizer = optim.Adadelta(model.parameters(), lr=1.0)

        try:
            optimizer.zero_grad()
            output = model(inputs)
            optimizer.step()
        except Exception as e:
            return False, str(e)


        return True, "Model has been trained"

    def score(self, model, inputs, labels):
        try:
            model.eval()
            inputs = torch.from_numpy(inputs).float()

            with torch.no_grad():
                output = model(inputs)
                test_loss += F.nll_loss(inputs, labels, reduction='sum').item()
                pred = output.argmax(dim=1, keepdim=True)
                correct += pred.eq(target.view_as(pred)).sum().item()

            accuracy = 100. * correct / inputs.size()[0]
        except Exception as e:
            return None, str(e)
       
        return accuracy, "Prediction complere"

    def save(self, model, path):
        try:
            torch.jit.save(torch.jit.trace(model, torch.zeros(self.input_size).float()), path)
        except:
            return False, "Could not save model"
        
        return True, ""#str(summary(model, self.input_size))