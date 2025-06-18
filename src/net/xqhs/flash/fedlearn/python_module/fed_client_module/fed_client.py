from typing import Dict, Optional, Union

import torch
from flwr.common import (
    FitIns, EvaluateIns, GetPropertiesIns, GetParametersIns,
    EvaluateRes, GetPropertiesRes, GetParametersRes, FitRes
)
from flwr.common.logger import log
from logging import *

from net.xqhs.flash.fedlearn.python_module.fed_client_module.client_utils import (
    load_datasets
)
from net.xqhs.flash.fedlearn.python_module.fed_client_module.flower_client import (
    FlowerClient
)
from net.xqhs.flash.fedlearn.python_module.fed_client_module.convnet import Net


class FedClient:
    """Wrapper over FlowerClient, managing client-side operations and dataset."""

    _instance: Optional["FedClient"] = None

    def __new__(
            cls,
            server_agent_id: str,
            partition_id: int,
            num_partitions: int,
            dataset: str = "cifar10",
            device: str = "cpu",
            model_state_dict: Optional[Dict[str, torch.Tensor]] = None,
    ) -> "FedClient":
        """Ensures only one FedClient instance (singleton)."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        else:
            log(
                WARNING,
                f"Warning: Attempted to create another FedClient instance. "
                f"Using existing instance (server_agent_id={cls._instance.server}, "
                f"partition_id={cls._instance.partition_id}, "
                f"dataset='{cls._instance.dataset}'). New params ignored."
                )

        return cls._instance

    def __init__(
            self,
            server_agent_id: str,
            partition_id: int,
            num_partitions: int,
            dataset: str = "cifar10",
            device: str = "cpu",
            model_state_dict: Optional[Dict[str, torch.Tensor]] = None,
    ):
        """
        Initializes FedClient (dataset loading, model setup).
        Only truly initializes on first call due to singleton nature and flag.
        """
        if not hasattr(self, "_initialized") or not self._initialized:
            self.server = server_agent_id
            self.device = device
            self.dataset = dataset
            self.partition_id = partition_id
            self.num_partitions = num_partitions
            self.trainloader, self.valloader, self.testloader = load_datasets(
                self.dataset, self.partition_id, self.num_partitions
            )

            if self.trainloader is None or self.valloader is None:
                raise RuntimeError("Dataset loaders were not properly initialized.")

            self.net = Net()
            if model_state_dict:
                self.net.load_state_dict(model_state_dict)
            self.net.to(self.device)

            self.flower_client = FlowerClient(
                self.partition_id, self.net, self.trainloader, self.valloader
            )
            self._initialized = True
            log(INFO, f"[Client {self.partition_id}] Loaded model and dataset {self.dataset}")

    def _check_flower_client(self):
        if self.flower_client is None:
            raise RuntimeError(
                "FlowerClient not initialized. FedClient not instantiated correctly."
            )

    def get_properties(self, ins: GetPropertiesIns) -> GetPropertiesRes:
        """Delegates get_properties to FlowerClient."""
        self._check_flower_client()
        return self.flower_client.get_properties(ins)

    def get_parameters(self, ins: GetParametersIns) -> GetParametersRes:
        """Delegates get_parameters to FlowerClient."""
        self._check_flower_client()
        return self.flower_client.get_parameters(ins)

    def fit(self, ins: FitIns) -> FitRes:
        """Delegates fit to FlowerClient."""
        self._check_flower_client()
        return self.flower_client.fit(ins)

    def evaluate(self, ins: EvaluateIns) -> EvaluateRes:
        """Delegates evaluate to FlowerClient."""
        self._check_flower_client()
        return self.flower_client.evaluate(ins)

    def exec_client_operation(
            self, ins: Union[GetPropertiesIns, GetParametersIns, FitIns, EvaluateIns]
    ) -> Union[GetPropertiesRes, GetParametersRes, FitRes, EvaluateRes, None]:
        """Executes flower client operation based on instructions."""
        self._check_flower_client()
        if isinstance(ins, GetPropertiesIns):
            return self.get_properties(ins)
        elif isinstance(ins, GetParametersIns):
            return self.get_parameters(ins)
        elif isinstance(ins, FitIns):
            return self.fit(ins)
        elif isinstance(ins, EvaluateIns):
            return self.evaluate(ins)
        else:
            return None