# Copyright 2020 Flower Labs GmbH. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
"""Flower ClientManager."""
import random
import threading
import queue
from enum import IntEnum, Enum
from logging import *
from queue import Queue
from typing import Optional, List, Tuple

from flwr.common import GetPropertiesIns, GetParametersIns, FitRes, EvaluateRes, GetParametersRes, GetPropertiesRes, \
    FitIns, EvaluateIns
from flwr.common.logger import log
from flwr.server.client_manager import ClientManager

from flwr.server.client_proxy import ClientProxy


class ResponseListType(IntEnum):
    """Type for the response list."""
    CV = 0
    RES = 1

class TaskType(Enum):
    """Type for the task type."""
    GET_PROPERTIES = "get_properties"
    GET_PARAMETERS = "get_parameters"
    FIT = "fit"
    EVALUATE = "evaluate"


class SimpleClientManager(ClientManager):
    """Provides a pool of available clients."""
    _instance = None
    _lock = threading.Lock()

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(SimpleClientManager, cls).__new__(cls)
        return cls._instance

    def __init__(self) -> None:
        if not hasattr(self, 'clients'):
            self.clients: dict[str, 'ClientProxy'] = {}
            self._cv = threading.Condition()

            self.instruction_queue: Queue[Tuple[str,GetPropertiesIns | GetParametersIns | FitIns | EvaluateIns]] = queue.Queue()
            self.response_dict: dict[str,dict[TaskType,List[threading.Condition | GetPropertiesRes | GetParametersRes | FitRes | EvaluateRes]]] = {}

    def __len__(self) -> int:
        """Return the number of available clients.

        Returns
        -------
        num_available : int
            The number of currently available clients.
        """
        return len(self.clients)

    def num_available(self) -> int:
        """Return the number of available clients.

        Returns
        -------
        num_available : int
            The number of currently available clients.
        """
        return len(self)

    def wait_for(self, num_clients: int, timeout: int = 86400) -> bool:
        """Wait until at least `num_clients` are available.

        Blocks until the requested number of clients is available or until a
        timeout is reached. Current timeout default: 1 day.

        Parameters
        ----------
        num_clients : int
            The number of clients to wait for.
        timeout : int
            The time in seconds to wait for, defaults to 86400 (24h).

        Returns
        -------
        success : bool
        """
        with self._cv:
            return self._cv.wait_for(
                lambda: len(self.clients) >= num_clients, timeout=timeout
            )

    def register(self, client: ClientProxy) -> bool:
        """Register Flower ClientProxy instance.

        Parameters
        ----------
        client : flwr.server.client_proxy.ClientProxy

        Returns
        -------
        success : bool
            Indicating if registration was successful. False if ClientProxy is
            already registered or can not be registered for any reason.
        """
        if client.cid in self.clients:
            return False

        self.clients[client.cid] = client
        self.response_dict[client.cid] = {}

        with self._cv:
            self._cv.notify_all()

        return True

    def unregister(self, client: ClientProxy) -> None:
        """Unregister Flower ClientProxy instance.

        This method is idempotent.

        Parameters
        ----------
        client : flwr.server.client_proxy.ClientProxy
        """
        if client.cid in self.clients:
            del self.clients[client.cid]
            del self.response_dict[client.cid]

            with self._cv:
                self._cv.notify_all()

    def all(self) -> dict[str, ClientProxy]:
        """Return all available clients."""
        return self.clients


    @staticmethod
    def _exec_client_operation(
            proxy_id: str,
            instruction_queue: Queue[Tuple[str,GetPropertiesIns | GetParametersIns | FitIns |
                                               EvaluateIns]],
            response_dict: dict[str,dict[TaskType,List[threading.Condition | GetPropertiesRes | GetParametersRes | FitRes | EvaluateRes]]],
            ins: GetPropertiesIns |
                 GetParametersIns |
                 FitIns |
                 EvaluateIns,
            timeout: Optional[float]
    ) -> GetPropertiesRes | GetParametersRes | FitRes | EvaluateRes:
        """ Executes a client operation by delegating it to the client."""
        task_type = None
        if isinstance(ins, GetPropertiesIns):
            task_type = TaskType.GET_PROPERTIES
        elif isinstance(ins, GetParametersIns):
            task_type = TaskType.GET_PARAMETERS
        elif isinstance(ins, FitIns):
            task_type = TaskType.FIT
        elif isinstance(ins, EvaluateIns):
            task_type = TaskType.EVALUATE
        else:
            raise ValueError(f"Unknown instruction type: {type(ins).__name__}")


        instruction_queue.put((proxy_id, ins))
        response_dict[proxy_id][task_type] = [threading.Condition(), None]

        with response_dict[proxy_id][task_type][ResponseListType.CV]:
            response_dict[proxy_id][task_type][ResponseListType.CV].wait(timeout)

        res = None
        if proxy_id in response_dict and task_type in response_dict[proxy_id]:
            res = response_dict[proxy_id][task_type][ResponseListType.RES]
            del response_dict[proxy_id][task_type]
        else:
            log(ERROR, f"Proxy ID {proxy_id} not found in response dictionary for task type {task_type}.")

        return res


    def set_result(
            self,
            proxy_id: str,
            res: GetPropertiesRes |
                 GetParametersRes |
                 FitRes |
                 EvaluateRes
    ) -> None:
        """Set the result for a specific proxy ID."""
        if proxy_id in self.response_dict and res is not None:
            task_type = None
            if isinstance(res, GetPropertiesRes):
                task_type = TaskType.GET_PROPERTIES
            elif isinstance(res, GetParametersRes):
                task_type = TaskType.GET_PARAMETERS
            elif isinstance(res, FitRes):
                task_type = TaskType.FIT
            elif isinstance(res, EvaluateRes):
                task_type = TaskType.EVALUATE

            if task_type is not None and task_type in self.response_dict[proxy_id]:
                self.response_dict[proxy_id][task_type][ResponseListType.RES] = res
                with self.response_dict[proxy_id][task_type][ResponseListType.CV]:
                    self.response_dict[proxy_id][task_type][ResponseListType.CV].notify_all()
            else:
                log(ERROR, f"Task type {task_type} not found in response dictionary for proxy ID {proxy_id}.")

    def get_tasks(self) -> List[Tuple[str, GetPropertiesIns | GetParametersIns | FitIns | EvaluateIns]]:
        """Get all available tasks from all clients."""
        tasks = []
        while not self.instruction_queue.empty():
            proxy_id, ins = self.instruction_queue.get()
            tasks.append((proxy_id, ins))

        return tasks


    def get_properties(self, ins: GetPropertiesIns, timeout: Optional[float], group_id: int | None, proxy_id: str) -> GetPropertiesRes:
        """Get properties from a specific client proxy."""
        return SimpleClientManager._exec_client_operation(proxy_id, self.instruction_queue, self.response_dict, ins, timeout)

    def get_parameters(self, ins: GetParametersIns, timeout: Optional[float], group_id: int | None, proxy_id: str) -> GetParametersRes:
        """Get parameters from a specific client proxy."""
        log(INFO, f"[ClientManager] Delegating get_parameters to Client {proxy_id} -> group_id: {group_id}")
        return SimpleClientManager._exec_client_operation(proxy_id, self.instruction_queue, self.response_dict, ins, timeout)

    def fit(self, ins: FitIns, timeout: Optional[float], group_id: int | None, proxy_id: str) -> FitRes:
        """Fit a specific client proxy with the provided instructions."""
        log(INFO, f"[ClientManager] Delegating fit to Client {proxy_id} -> group_id: {group_id}")
        return SimpleClientManager._exec_client_operation(proxy_id, self.instruction_queue, self.response_dict, ins, timeout)

    def evaluate(self, ins: EvaluateIns, timeout: Optional[float], group_id, proxy_id) -> EvaluateRes:
        """Evaluate a specific client proxy with the provided instructions."""
        log(INFO, f"[ClientManager] Delegating evaluate to Client {proxy_id} -> group_id: {group_id}")
        return SimpleClientManager._exec_client_operation(proxy_id, self.instruction_queue, self.response_dict, ins, timeout)


    def sample(
            self,
            num_clients: int,
            min_num_clients: Optional[int] = None,
            criterion = None,
    ) -> list[ClientProxy]:
        """Sample a number of Flower ClientProxy instances."""
        # Block until at least num_clients are connected.
        if min_num_clients is None:
            min_num_clients = num_clients
        self.wait_for(min_num_clients)
        # Sample clients which meet the criterion
        available_cids = list(self.clients)
        if criterion is not None:
            available_cids = [
                cid for cid in available_cids if criterion.select(self.clients[cid])
            ]

        if num_clients > len(available_cids):
            log(
                INFO,
                "Sampling failed: number of available clients"
                " (%s) is less than number of requested clients (%s).",
                len(available_cids),
                num_clients,
            )
            return []

        sampled_cids = random.sample(available_cids, num_clients)
        return [self.clients[cid] for cid in sampled_cids]
