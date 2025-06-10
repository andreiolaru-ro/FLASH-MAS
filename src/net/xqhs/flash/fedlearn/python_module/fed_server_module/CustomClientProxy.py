# Copyright 2022 Flower Labs GmbH. All Rights Reserved.
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
"""Tests for Flower ClientProxy."""
from logging import INFO
from typing import Optional

import flwr
from flwr.common import (
    EvaluateIns,
    EvaluateRes,
    FitIns,
    FitRes,
    GetParametersIns,
    GetParametersRes,
    GetPropertiesIns,
    GetPropertiesRes, log,
)
from flwr.server.client_proxy import ClientProxy

from . import SimpleClientManager

global server

class CustomClientProxy(ClientProxy):
    def __init__(self, cid: str):
        super().__init__(cid),
        self.client_manager : SimpleClientManager | None = None

    def set_client_manager(self, client_manager: SimpleClientManager) -> None:
        """Set the client manager for this proxy."""
        self.client_manager = client_manager
        log(INFO, f"Client %s set client manager: %s", self.cid, self.client_manager)

    def get_properties(self, ins: GetPropertiesIns, timeout: Optional[float], group_id) -> GetPropertiesRes:
        return self.client_manager.get_properties(ins, timeout, group_id, self.cid)

    def get_parameters(self, ins: GetParametersIns, timeout: Optional[float], group_id) -> GetParametersRes:
        return self.client_manager.get_parameters(ins, timeout, group_id, self.cid)

    def fit(self, ins: FitIns, timeout: Optional[float], group_id) -> FitRes:
        return self.client_manager.fit(ins, timeout, group_id, self.cid)

    def evaluate(self, ins: EvaluateIns, timeout: Optional[float], group_id) -> EvaluateRes:
        return self.client_manager.evaluate(ins, timeout, group_id, self.cid)

    def reconnect(self, ins: flwr.common.ReconnectIns, timeout: Optional[float], group_id) -> flwr.common.DisconnectRes:
        # In this simulation, reconnect doesn't do much
        return flwr.common.DisconnectRes(reason="Simulation reconnect (no-op)")

def test_cid() -> None:
    """Tests if the register method works correctly."""
    # Prepare
    cid_expected = "1"
    client_proxy = CustomClientProxy(cid=cid_expected)

    # Execute
    cid_actual = client_proxy.cid

    # Assert
    assert cid_actual == cid_expected


def test_properties_are_empty() -> None:
    """Tests if the register method works correctly."""
    # Prepare
    client_proxy = CustomClientProxy(cid="1")

    # Execute
    actual_properties = client_proxy.properties

    # Assert
    assert not actual_properties
