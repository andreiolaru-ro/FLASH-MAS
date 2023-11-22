import requests
import torch as torch

# model = torch.hub.load('pytorch/vision:v0.10.0', 'mobilenet_v2', pretrained=True)
# model.eval()
# torch.save(model, 'src/net/xqhs/flash/ml/python_module/models/mobilenetv2.pth')
# exit(0)

device = torch.device('cpu')  # gpu also works, but our models are fast enough for CPU
model, decoder, utils = torch.hub.load(repo_or_dir='snakers4/silero-models',
                                         model='silero_stt',
                                         language='en', # also available 'de', 'es'
                                         device=device)
torch.jit.save(model, 'silero_stt.pth')
