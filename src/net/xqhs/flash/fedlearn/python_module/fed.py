
from typing import Optional

from flask import request, jsonify
from flwr.server.server import Server as FlowerServer
from flwr.server.strategy import FedAvg

from constants import *
from fed_server_module.CustomClientProxy import CustomClientProxy
from fed_server_module.SimpleClientManager import SimpleClientManager
from serialization_module.serialization import MessageDirection
from serialization_module.serialization import serialize_to_b64_str, deserialize_from_b64_str
from util import import_functionality, log, weighted_average, weighted_average_fit_metrics

server : FlowerServer | None = None

flask = import_functionality("flask")
log("creating server... ")
app = flask.Flask(__name__)

def start(port: int = SERVER_PORT):
    app.run(port=port)


#### HEALTHCHECK ENDPOINT ####
@app.route('/healthcheck', methods=['GET'])
def healthcheck():
    return jsonify({'message': 'Flask server is alive and well'}), 200

# ---------------------------- CLIENT ROUTES ----------------------------
from fed_client_module.fed_client import FedClient
fedclient: Optional[FedClient] = None

@app.route('/' + CLIENT_INIT, methods=['POST'])
def init_client_route():
    """
    Initializes the federated learning client.
    Expects JSON:
    {
        'server_agent_id': str,
        'dataset': [Optional] str,
        'partition_id': int,
        'num_partitions': int
        'device': [Optional] str = 'cpu' | 'cuda' | 'mps'
        'model_state_dict': Optional[Dict[str, List[float]]]  # JSON can't directly send torch.Tensor
    }
    """
    global fedclient
    data = request.get_json()

    if not data:
        return jsonify({'error': 'Invalid JSON payload'}), 400

    server_agent_id = data.get('server_agent_id')
    dataset = data.get('dataset', 'cifar10')
    partition_id = data.get('partition_id')
    num_partitions = data.get('num_partitions')
    device = data.get('device', 'cpu')
    model_state_dict = data.get('model_state_dict')

    # Check if params are correct
    if server_agent_id is None:
        return jsonify({'error': 'Missing "server_agent_id" in request'}), 400
    if partition_id is None:
        return jsonify({'error': 'Missing "partition_id" in request'}), 400
    if num_partitions is None:
        return jsonify({'error': 'Missing "num_partitions" in request'}), 400

    # try casting partition_id and num_partitions to int
    partition_id = int(partition_id)
    num_partitions = int(num_partitions)

    # Type Checking
    if not isinstance(server_agent_id, str):
        return jsonify({'error': 'Invalid type for "server_agent_id". Expected string.'}), 400
    if not isinstance(dataset, str):
        return jsonify({'error': 'Invalid type for "dataset". Expected string.'}), 400
    if not isinstance(device, str):
        return jsonify({'error': 'Invalid type for "device". Expected string.'}), 400
    allowed_devices = ['cuda', 'cpu', 'mps']
    if device not in allowed_devices:
        return jsonify({'error': f'Invalid value for "device". Allowed values are: {", ".join(allowed_devices)}.'}), 400

    # Value validation
    if not server_agent_id.strip():
        return jsonify({'error': '"server_agent_id" cannot be empty.'}), 400
    if partition_id < 0:
        return jsonify({'error': '"partition_id" cannot be negative.'}), 400
    if num_partitions <= 0:
        return jsonify({'error': '"num_partitions" must be a positive integer.'}), 400
    if partition_id >= num_partitions:
        return jsonify({'error': '"partition_id" must be less than "num_partitions".'}), 400

    try:
        fedclient = FedClient(
            server_agent_id=server_agent_id,
            partition_id=partition_id,
            num_partitions=num_partitions,
            dataset=dataset,
            device=device,
            model_state_dict=model_state_dict
        )

        return jsonify({'message': f'FedClient initialized for server: {server_agent_id}'}), 200
    except Exception as e:
        return jsonify({'error': f'Failed to initialize FedClient: {str(e)}'}), 500


@app.route('/' + CLIENT_DATA, methods=['POST'])
def client_data():
    """
    Receives a client instruction and executes it.
    Expects JSON:
    {
        'proxy_id': str,  # ID of the client proxy
        'instruction': str  # Base64 encoded serialized instruction
    }
    """
    data = flask.request.get_json()
    client_id = data.get('proxy_id')
    raw_ins = data.get('instruction')

    ins = deserialize_from_b64_str(raw_ins, MessageDirection.SERVER_TO_CLIENT)
    client_res = fedclient.exec_client_operation(ins)
    serialized_res = serialize_to_b64_str(client_res)

    if client_res:
        return flask.jsonify({
            'proxy_id': client_id,
            'results': serialized_res
        }), 200
    else:
        return flask.jsonify({
            'proxy_id': client_id,
            'message': 'Something went wrong while executing client operation...'
        }), 500


# -------------------------- CLIENT ROUTES END --------------------------


# ---------------------------- SERVER ROUTES ----------------------------

@app.route('/' + REGISTER_CLIENT_PROXY, methods=['POST'])
def register():
    """
    Receives : client_id
    Creates a client proxy and registers it with the server."""
    data = flask.request.get_json()
    client_id = data.get(CLIENT_ID)
    if not client_id:
        return flask.jsonify({'error': 'Client ID is required.'}), 400
    client_proxy = CustomClientProxy(client_id)
    if not client_proxy:
        return flask.jsonify({'error': 'Failed to create client proxy.'}), 500
    client_manager = SimpleClientManager()
    client_manager.register(client_proxy)
    client_proxy.set_client_manager(client_manager)
    # print(client_manager.all())
    return flask.jsonify({'message': f'Client proxy with ID "{client_id}" has been successfully registered.'}), 200



@app.route('/' + INITIALIZE_FED_SERVICE, methods=['POST'])
def init():
    """ Initialize the server with necessary configurations.
    Receives :
    - num_clients - Number of clients used for the federated learning process.
    - strategy args:
        - fraction_fit - Sampling fraction of clients to use for training.
        - fraction_evaluate - Sampling fraction of clients to use for evaluation.
        - min_fit_clients - Minimum number of clients to use for training.
        - min_evaluate_clients - Minimum number of clients to use for evaluation.
        - min_available_clients - Minimum number of clients that must be available for training.
    Waits for all clients to register with a proxy, creates a FedAvg strategy
    with the provided parameters, and finally
    creates a Flower server with the client manager and strategy.
    """
    data = flask.request.get_json()
    if not data:
        return flask.jsonify({'error': 'No data provided.'}), 400
    client_manager = SimpleClientManager()
    num_clients = int(data.get(NUM_CLIENTS))

    log("Waiting for clients to register... ")
    client_manager.wait_for(num_clients,timeout=240)

    fraction_fit = data.get(FRACTION_FIT)
    fraction_evaluate = data.get(FRACTION_EVALUATE)
    min_fit_clients = data.get(MIN_FIT_CLIENTS)
    min_evaluate_clients = data.get(MIN_EVALUATE_CLIENTS)
    min_available_clients = data.get(MIN_AVAILABLE_CLIENTS)

    strategy = FedAvg(
        fraction_fit=fraction_fit,
        fraction_evaluate=fraction_evaluate,
        min_fit_clients=min_fit_clients,
        min_evaluate_clients=min_evaluate_clients,
        min_available_clients=min_available_clients,
        fit_metrics_aggregation_fn=weighted_average_fit_metrics,
        evaluate_metrics_aggregation_fn=weighted_average,
    )
    global server
    server = FlowerServer(client_manager=client_manager, strategy=strategy)
    if not server:
        return flask.jsonify({'error': 'Failed to create server.'}), 500
    log("Server initialized with strategy:", strategy)
    return flask.jsonify({'message': 'Server has been successfully initialized.'}), 200


@app.route('/' + START_FIT, methods=['POST'])
def start_fit():
    """ Starts the fit process on the server, given the number of rounds and timeout. Then waits
    for all clients to complete their tasks and returns the history of the fit process.
    Receives:
        - num_rounds - Number of rounds to run the fit process.
        - timeout - Timeout for the fit process in seconds.
    """

    data = flask.request.get_json()
    num_rounds = data.get('num_rounds')
    timeout = float(data.get('timeout'))
    if not num_rounds or not timeout:
        return flask.jsonify({'error': 'num_rounds and timeout are required.'}), 400
    global server
    history, elapsed_time = server.fit(num_rounds=num_rounds, timeout=timeout)
    if not history:
        return flask.jsonify({'error': 'Failed to start fit process.'}), 500
    log("Fit process completed in", elapsed_time, "seconds.")
    return flask.jsonify({'message': 'Fit process completed successfully.', 'history': repr(history), 'elapsed_time': elapsed_time}), 200

@app.route('/' + GET_TASK, methods=['GET'])
def get_tasks():
    """
    Returns all instructions and operations for all clients.
    """
    client_manager = SimpleClientManager()
    if not client_manager:
        return flask.jsonify({'error': 'No client manager available.'}), 500
    tasks = client_manager.get_tasks()
    res = []
    for proxy_id, ins in tasks:
        serialized_ins = serialize_to_b64_str(ins)

        res.append({
            'proxy_id': proxy_id,
            'instruction': serialized_ins
        })
    if not res:
        log("No tasks available for clients.")
        return flask.jsonify({'message': 'No tasks available for clients.'}), 404
    log("Returning tasks for", len(res), "clients.")
    return flask.jsonify({'tasks': res}), 200

@app.route('/' + RES, methods=['POST'])
def get_res():
    """
    Gets the results of the tasks from the client.
    Expects JSON:
    {
        'results': str,  # Base64 encoded serialized results
        'proxy_id': str  # ID of the client proxy
    }
    """
    data = flask.request.get_json()

    res_ins = data.get('results')
    results = deserialize_from_b64_str(res_ins, MessageDirection.CLIENT_TO_SERVER)

    if not results:
        return flask.jsonify({'error': 'Results not obtained.'}), 400
    proxy_id = data.get('proxy_id')
    client_manager = SimpleClientManager()
    client_manager.set_result(proxy_id, results)
    log("Results received from client", proxy_id, "...")
    return flask.jsonify({'message': 'Results received successfully.'}), 200


# -------------------------- SERVER ROUTES END --------------------------