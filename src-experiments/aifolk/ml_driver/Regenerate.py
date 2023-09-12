import requests
import torch as torch

model = torch.hub.load('pytorch/vision:v0.10.0', 'mobilenet_v2', pretrained=True)
model.eval()
torch.save(model, 'src/net/xqhs/flash/ml/python_module/models/mobilenetv2.pth')
exit(0)