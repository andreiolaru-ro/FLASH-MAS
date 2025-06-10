from flwr.common import (
    Code, EvaluateIns, EvaluateRes, FitIns, FitRes, GetParametersIns,
    GetParametersRes, Status, ndarrays_to_parameters, parameters_to_ndarrays,
)
from typing import List
import numpy as np

from flwr.client import Client
from logging import *
from flwr.common.logger import log

from net.xqhs.flash.fedlearn.python_module.fed_client_module.client_utils import (
    get_parameters, set_parameters, train, test
)


class FlowerClient(Client):
    """An example FlowerClient for the fedlearn scenario."""

    def __init__(self, partition_id, net, trainloader, valloader):
        self.partition_id = partition_id
        self.net = net
        self.trainloader = trainloader
        self.valloader = valloader

    def get_parameters(self, ins: GetParametersIns) -> GetParametersRes:
        log(INFO, f"[Client {self.partition_id}] get_parameters")

        # Get parameters as a list of NumPy ndarray's
        ndarrays: List[np.ndarray] = get_parameters(self.net)

        # Serialize ndarray's into a Parameters object
        parameters = ndarrays_to_parameters(ndarrays)

        # Build and return response
        status = Status(code=Code.OK, message="Success")
        return GetParametersRes(
            status=status,
            parameters=parameters,
        )

    def fit(self, ins: FitIns) -> FitRes:
        log(INFO, f"[Client {self.partition_id}] fit, config: {ins.config}")

        # Deserialize parameters to NumPy ndarray's
        ndarrays_original = parameters_to_ndarrays(ins.parameters)

        # Update local model, train, get updated parameters
        set_parameters(self.net, ndarrays_original)
        train(self.net, self.trainloader, epochs=1)
        ndarrays_updated = get_parameters(self.net)

        # Serialize ndarray's into a Parameters object
        parameters_updated = ndarrays_to_parameters(ndarrays_updated)

        # Build and return response
        status = Status(code=Code.OK, message="Success")
        return FitRes(
            status=status,
            parameters=parameters_updated,
            num_examples=len(self.trainloader),
            metrics={},
        )

    def evaluate(self, ins: EvaluateIns) -> EvaluateRes:
        log(INFO, f"[Client {self.partition_id}] evaluate, config: {ins.config}")

        # Deserialize parameters to NumPy ndarray's
        ndarrays_original = parameters_to_ndarrays(ins.parameters)

        set_parameters(self.net, ndarrays_original)
        loss, accuracy = test(self.net, self.valloader)

        # Build and return response
        status = Status(code=Code.OK, message="Success")
        return EvaluateRes(
            status=status,
            loss=float(loss),
            num_examples=len(self.valloader),
            metrics={"accuracy": float(accuracy)},
        )
