import base64
from enum import IntEnum
from typing import Union

from flwr.common import (
    FitIns, EvaluateIns, GetPropertiesIns, GetParametersIns,
    EvaluateRes, GetPropertiesRes, GetParametersRes, FitRes
)

from flwr.common.serde import (
    get_parameters_ins_to_proto, fit_ins_to_proto, evaluate_ins_to_proto,
    get_properties_ins_to_proto, get_parameters_res_to_proto, fit_res_to_proto,
    evaluate_res_to_proto, get_properties_res_to_proto,
    get_properties_ins_from_proto, get_parameters_ins_from_proto,
    fit_ins_from_proto, evaluate_ins_from_proto, get_properties_res_from_proto,
    get_parameters_res_from_proto, fit_res_from_proto, evaluate_res_from_proto
)
from flwr.proto.transport_pb2 import ClientMessage, ServerMessage
from google.protobuf.message import DecodeError


class MessageDirection(IntEnum):
    """Defines the direction of a message for clear deserialization."""
    SERVER_TO_CLIENT = 1  # Instructions (Ins)
    CLIENT_TO_SERVER = 2  # Results (Res)


def serialize_to_b64_str(message_obj: Union[
    GetParametersIns, FitIns, EvaluateIns, GetPropertiesIns,
    GetParametersRes, FitRes, EvaluateRes, GetPropertiesRes
]) -> str:
    """Serializes a Flower object to a base64 encoded string for JSON transport."""
    proto_bytes = my_serialize(message_obj)
    b64_bytes = base64.b64encode(proto_bytes)
    return b64_bytes.decode('utf-8')


def deserialize_from_b64_str(
        b64_str: str,
        direction: MessageDirection
) -> Union[
    GetParametersIns, FitIns, EvaluateIns, GetPropertiesIns,
    GetParametersRes, FitRes, EvaluateRes, GetPropertiesRes
]:
    """Deserializes a base64 encoded string into a Flower object."""
    proto_bytes = base64.b64decode(b64_str)
    return my_deserialize(proto_bytes, direction)

def my_serialize(
        message_obj: Union[
            GetParametersIns, FitIns, EvaluateIns, GetPropertiesIns,
            GetParametersRes, FitRes, EvaluateRes, GetPropertiesRes
        ]
) -> bytes:
    """Serializes a Flower instruction or result dataclass into bytes."""
    # FedServer to FedClient instructions
    if isinstance(message_obj, GetParametersIns):
        proto = get_parameters_ins_to_proto(message_obj)
        return ServerMessage(get_parameters_ins=proto).SerializeToString()
    if isinstance(message_obj, FitIns):
        proto = fit_ins_to_proto(message_obj)
        return ServerMessage(fit_ins=proto).SerializeToString()
    if isinstance(message_obj, EvaluateIns):
        proto = evaluate_ins_to_proto(message_obj)
        return ServerMessage(evaluate_ins=proto).SerializeToString()
    if isinstance(message_obj, GetPropertiesIns):
        proto = get_properties_ins_to_proto(message_obj)
        return ServerMessage(get_properties_ins=proto).SerializeToString()

    # FedClient to FedServer results
    if isinstance(message_obj, GetParametersRes):
        proto = get_parameters_res_to_proto(message_obj)
        return ClientMessage(get_parameters_res=proto).SerializeToString()
    if isinstance(message_obj, FitRes):
        proto = fit_res_to_proto(message_obj)
        return ClientMessage(fit_res=proto).SerializeToString()
    if isinstance(message_obj, EvaluateRes):
        proto = evaluate_res_to_proto(message_obj)
        return ClientMessage(evaluate_res=proto).SerializeToString()
    if isinstance(message_obj, GetPropertiesRes):
        proto = get_properties_res_to_proto(message_obj)
        return ClientMessage(get_properties_res=proto).SerializeToString()

    raise TypeError(
        f"Serialization for type {type(message_obj).__name__} not implemented or "
        "type is incorrect."
    )


def my_deserialize(
        data: bytes,
        direction: int
) -> Union[
    GetParametersIns, FitIns, EvaluateIns, GetPropertiesIns,
    GetParametersRes, FitRes, EvaluateRes, GetPropertiesRes
]:
    """
    Deserializes bytes into a Flower instruction or result dataclass
    based on the message direction.
    """
    if not isinstance(direction, MessageDirection):
        raise TypeError(
            f"direction must be an instance of MessageDirection, not {type(direction)}"
        )

    try:
        if direction == MessageDirection.SERVER_TO_CLIENT:
            # Server to Client are instructions (Ins) wrapped in ServerMessage
            server_msg = ServerMessage()
            server_msg.ParseFromString(data)
            msg_type = server_msg.WhichOneof("msg")

            if msg_type == "get_properties_ins":
                return get_properties_ins_from_proto(server_msg.get_properties_ins)
            if msg_type == "get_parameters_ins":
                return get_parameters_ins_from_proto(server_msg.get_parameters_ins)
            if msg_type == "fit_ins":
                return fit_ins_from_proto(server_msg.fit_ins)
            if msg_type == "evaluate_ins":
                return evaluate_ins_from_proto(server_msg.evaluate_ins)
            raise ValueError(
                f"Unknown or empty message type '{msg_type}' in ServerMessage"
            )

        if direction == MessageDirection.CLIENT_TO_SERVER:
            # Client to Server are results (Res) wrapped in ClientMessage
            client_msg = ClientMessage()
            client_msg.ParseFromString(data)
            msg_type = client_msg.WhichOneof("msg")

            if msg_type == "get_properties_res":
                return get_properties_res_from_proto(client_msg.get_properties_res)
            if msg_type == "get_parameters_res":
                return get_parameters_res_from_proto(client_msg.get_parameters_res)
            if msg_type == "fit_res":
                return fit_res_from_proto(client_msg.fit_res)
            if msg_type == "evaluate_res":
                return evaluate_res_from_proto(client_msg.evaluate_res)
            raise ValueError(
                f"Unknown or empty message type '{msg_type}' in ClientMessage"
            )

    except DecodeError as e:
        raise ValueError("Could not deserialize data: Invalid protobuf structure.") from e
    except Exception as e:
        raise ValueError(f"Unexpected error during deserialization: {e}") from e

    raise ValueError("Invalid message direction or logic error.")