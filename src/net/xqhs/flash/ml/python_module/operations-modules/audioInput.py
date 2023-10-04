import os
import warnings
from typing import List
from itertools import groupby
import torch
import torchaudio
import os
from glob import glob

workdir = os.getcwd()
device = torch.device('cpu')
noUse, decoder, irrelevant = torch.hub.load(repo_or_dir='snakers4/silero-models',
                                       model='silero_stt',
                                       language='en', # also available 'de', 'es'
                                       device=device)


def read_batch(audio_path: str):
    return read_audio(audio_path)


def split_into_batches(lst: List[str],
                       batch_size: int = 10):
    return [lst[i:i + batch_size]
            for i in
            range(0, len(lst), batch_size)]


def read_audio(path: str,
               target_sr: int = 16000):
    wav, sr = torchaudio.load(path)

    if wav.size(0) > 1:
        wav = wav.mean(dim=0, keepdim=True)

    if sr != target_sr:
        transform = torchaudio.transforms.Resample(orig_freq=sr,
                                                   new_freq=target_sr)
        wav = transform(wav)
        sr = target_sr

    assert sr == target_sr
    return wav.squeeze(0)


def prepare_model_input(wav: torch.Tensor,
                        device=torch.device('cpu')):
    max_seqlength = max(len(wav), 12800)
    input_tensor = torch.zeros(1, max_seqlength)
    input_tensor[0, :len(wav)].copy_(wav)
    input_tensor = input_tensor.to(device)
    return input_tensor

def predict_op(model_name, input_data, models):
    model = models[model_name]['model']
    model.eval()
    input = prepare_model_input(read_batch(input_data))
    output = model(input)
    returnList = []
    for data in output:
        returnList.append(decoder(data.cpu()))
    return returnList
