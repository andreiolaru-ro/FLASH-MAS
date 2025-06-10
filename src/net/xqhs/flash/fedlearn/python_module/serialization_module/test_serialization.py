import unittest

from flwr.common import (FitIns, EvaluateIns, GetPropertiesIns, GetParametersIns, EvaluateRes,
                         GetPropertiesRes, GetParametersRes, FitRes, Parameters, Status, Code, Scalar)

from serialization import my_serialize, my_deserialize, MessageDirection


try:
    import numpy as np
    dummy_parameters = Parameters(tensors=[np.array([1, 2, 3]).tobytes()], tensor_type="numpy.ndarray")
    dummy_parameters_empty = Parameters(tensors=[], tensor_type="numpy.ndarray")
except ImportError:
    print("NumPy not found for testing, using dummy bytes for parameters. For real usage, install NumPy.")
    dummy_parameters = Parameters(tensors=[b"\x01\x02\x03"], tensor_type="bytes")
    dummy_parameters_empty = Parameters(tensors=[], tensor_type="bytes")


class TestFlowerSerDe(unittest.TestCase):

    def _test_message_cycle(self, original_message, message_type_name):
        """Helper function to test the serialize-deserialize cycle for a message."""
        print(f"\nTesting {message_type_name}...")
        try:
            # 1. Serialize
            serialized_data = my_serialize(original_message)
            self.assertIsInstance(serialized_data, bytes, f"{message_type_name}: Serialization did not return bytes.")
            print(f"  Serialized {message_type_name} (first 60 bytes): {serialized_data[:60]}...")

            # 2. Deserialize
            if message_type_name == 'GetParametersIns' or message_type_name == 'GetPropertiesIns' or message_type_name == 'FitIns' or message_type_name == 'EvaluateIns':
                deserialized_message = my_deserialize(serialized_data, MessageDirection.SERVER_TO_CLIENT)
            else:
                deserialized_message = my_deserialize(serialized_data, MessageDirection.CLIENT_TO_SERVER)

            print(f"  Deserialized {message_type_name}: {deserialized_message}")

            # 3. Validate
            self.assertIsInstance(deserialized_message, type(original_message),
                                  f"{message_type_name}: Deserialized type mismatch.")

            # Detailed content checks (add more as needed per message type)
            if hasattr(original_message, 'config') and hasattr(deserialized_message, 'config'):
                self.assertEqual(original_message.config, deserialized_message.config,
                                 f"{message_type_name}: Config mismatch.")
            if hasattr(original_message, 'parameters') and hasattr(deserialized_message, 'parameters'):
                # Direct comparison of Parameters objects can be complex.
                # Check tensor_type and number of tensors for basic validation.
                self.assertEqual(original_message.parameters.tensor_type, deserialized_message.parameters.tensor_type,
                                 f"{message_type_name}: Parameters tensor_type mismatch.")
                self.assertEqual(len(original_message.parameters.tensors), len(deserialized_message.parameters.tensors),
                                 f"{message_type_name}: Parameters tensor count mismatch.")
                # For a more robust check, you might iterate and compare tensor bytes.
                if original_message.parameters.tensors: # only compare if tensors exist
                    self.assertEqual(original_message.parameters.tensors[0], deserialized_message.parameters.tensors[0],
                                     f"{message_type_name}: Parameters first tensor content mismatch.")

            if hasattr(original_message, 'status') and hasattr(deserialized_message, 'status'):
                self.assertEqual(original_message.status.code, deserialized_message.status.code,
                                 f"{message_type_name}: Status code mismatch.")
                self.assertEqual(original_message.status.message, deserialized_message.status.message,
                                 f"{message_type_name}: Status message mismatch.")

            if hasattr(original_message, 'metrics') and hasattr(deserialized_message, 'metrics'):
                self.assertEqual(original_message.metrics, deserialized_message.metrics,
                                 f"{message_type_name}: Metrics mismatch.")

            if hasattr(original_message, 'loss') and hasattr(deserialized_message, 'loss'):
                self.assertAlmostEqual(original_message.loss, deserialized_message.loss, places=7,
                                       msg=f"{message_type_name}: Loss mismatch.")

            if hasattr(original_message, 'num_examples') and hasattr(deserialized_message, 'num_examples'):
                self.assertEqual(original_message.num_examples, deserialized_message.num_examples,
                                 f"{message_type_name}: Num_examples mismatch.")

            if hasattr(original_message, 'properties') and hasattr(deserialized_message, 'properties'):
                self.assertEqual(original_message.properties, deserialized_message.properties,
                                 f"{message_type_name}: Properties mismatch.")

            print(f"  ✅ {message_type_name} cycle test passed.")

        except Exception as e:
            self.fail(f"Error during {message_type_name} cycle test: {e}")


    def test_get_parameters_ins(self):
        msg = GetParametersIns(config={"test_key": "test_val_ins_params"})
        self._test_message_cycle(msg, "GetParametersIns")

    def test_fit_ins(self):
        msg = FitIns(parameters=dummy_parameters, config={"epochs": 10})
        self._test_message_cycle(msg, "FitIns")

    def test_evaluate_ins(self):
        msg = EvaluateIns(parameters=dummy_parameters, config={"batch_size": 64})
        self._test_message_cycle(msg, "EvaluateIns")

    def test_get_properties_ins(self):
        msg = GetPropertiesIns(config={"request_prop": "client_id"})
        self._test_message_cycle(msg, "GetPropertiesIns")

    def test_get_parameters_res(self):
        status = Status(code=Code.OK, message="Params delivered")
        msg = GetParametersRes(status=status, parameters=dummy_parameters)
        self._test_message_cycle(msg, "GetParametersRes")

    def test_fit_res(self):
        status = Status(code=Code.OK, message="Fit successful")
        metrics: dict[str, Scalar] = {"accuracy": 0.98, "loss": 0.123}
        msg = FitRes(status=status, parameters=dummy_parameters_empty, num_examples=100, metrics=metrics)
        self._test_message_cycle(msg, "FitRes")

    def test_evaluate_res(self):
        status = Status(code=Code.OK, message="Eval complete")
        metrics: dict[str, Scalar] = {"final_accuracy": 0.975}
        msg = EvaluateRes(status=status, loss=0.25, num_examples=50, metrics=metrics)
        self._test_message_cycle(msg, "EvaluateRes")

    def test_get_properties_res(self):
        status = Status(code=Code.OK, message="Props delivered")
        props: dict[str, Scalar] = {"client_id": "client_123", "gpu_present": False}
        msg = GetPropertiesRes(status=status, properties=props)
        self._test_message_cycle(msg, "GetPropertiesRes")


if __name__ == '__main__':
    print("Running Flower Serialize/Deserialize Tests...")
    unittest.main(verbosity=2)
