import multiprocessing
import time
import requests
import fed
from util import log

# --- Configuration  ---
NUM_CLIENTS = 10
SERVER_PORT = 6001
CLIENT_PORT_START = 7001
SERVER_URL = f"http://localhost:{SERVER_PORT}"

# --- Client Configurations  ---
clients = []
for i in range(NUM_CLIENTS):
    client_id = f"client_{i+1}"
    port = CLIENT_PORT_START + i
    clients.append({
        "id": client_id,
        "port": port,
        "url": f"http://localhost:{port}",
        "partition_id": i
    })

CLIENT_URL_MAP = {client['id']: client['url'] for client in clients}


def post_request(url, endpoint, json_data, description):
    """Handles POST requests and logging."""
    full_url = f"{url}/{endpoint}"
    try:
        log(f"--> Sending POST to {full_url} for: {description}")
        response = requests.post(full_url, json=json_data)
        response.raise_for_status()

        json_response = response.json()

        if 'history' in json_response:
            log(f"<-- SUCCESS from {full_url}: Fit process completed. History received.")
            log(f"    History: {json_response['history']}")
        else:
            log(f"<-- SUCCESS from {full_url}: [...]")

        return json_response

    except requests.exceptions.RequestException as e:
        log(f"<-- FAILED from {full_url}: {e}")
        if e.response:
            log(f"    Response content: {e.response.text}")
        return None

def get_request(url, endpoint, description):
    """Handles GET requests and logging."""
    full_url = f"{url}/{endpoint}"
    try:
        log(f"--> Sending GET to {full_url} for: {description}")
        response = requests.get(full_url)
        response.raise_for_status()
        log(f"<-- SUCCESS from {full_url}")
        return response.json()
    except requests.exceptions.RequestException as e:
        log(f"<-- FAILED from {full_url}: {e}")
        return None


def start_flask_in_process(target, port):
    """Starts a Flask app in a new daemon process."""
    process = multiprocessing.Process(target=target, args=(port,), daemon=True)
    process.start()
    log(f"Flask app started for {target.__name__} on port {port} in a background process (PID: {process.pid}).")
    return process

def top_level_fit_task(server_url, start_fit_endpoint, num_rounds, timeout):
    """A top-level function to start the fit process, suitable for multiprocessing."""
    post_request(server_url, start_fit_endpoint, {'num_rounds': num_rounds, 'timeout': timeout}, "Start Fit Process")

def start_fit_in_process(num_rounds, timeout):
    """Starts the server's fit process in a separate process."""
    args = (SERVER_URL, fed.START_FIT, num_rounds, timeout)
    process = multiprocessing.Process(target=top_level_fit_task, args=args, daemon=True)
    process.start()
    log(f"Fit process started in a background process (PID: {process.pid}).")
    return process



if __name__ == '__main__':
    multiprocessing.set_start_method("spawn", force=True)

    log("--- Starting Federated Learning Scenario for {} clients ---".format(NUM_CLIENTS))

    all_processes = []
    try:
        # 1. Start Server and all Client Flask applications
        log("--- Starting Server process ---")
        server_process = start_flask_in_process(fed.start, SERVER_PORT)
        all_processes.append(server_process)

        log("--- Starting Client processes ---")
        for client in clients:
            client_process = start_flask_in_process(fed.start, client['port'])
            all_processes.append(client_process)

        log("Waiting for processes to initialize...")
        time.sleep(8)
        log("Initialization complete.")

        # 2. Initialize each Client
        for client in clients:
            log(f"\n--- Initializing {client['id']} ---")
            client_init_data = {
                'server_agent_id': 'server-001',
                'dataset': 'cifar10',
                'partition_id': client['partition_id'],
                'num_partitions': NUM_CLIENTS,
                'device': 'cpu'
            }
            post_request(client['url'], fed.CLIENT_INIT, client_init_data, f"Initialize FedClient {client['id']}")

        # 3. Register each Client with the Server
        log("\n--- Registering all clients with the server ---")
        for client in clients:
            register_data = {fed.CLIENT_ID: client['id']}
            post_request(SERVER_URL, fed.REGISTER_CLIENT_PROXY, register_data, f"Register Client Proxy for {client['id']}")

        # 4. Initialize the Federated Server
        log("\n--- Initializing the Federated Server ---")
        server_init_data = {
            fed.NUM_CLIENTS: NUM_CLIENTS,
            fed.FRACTION_FIT: 1.0,
            fed.FRACTION_EVALUATE: 1.0,
            fed.MIN_FIT_CLIENTS: NUM_CLIENTS,
            fed.MIN_EVALUATE_CLIENTS: NUM_CLIENTS,
            fed.MIN_AVAILABLE_CLIENTS: NUM_CLIENTS
        }
        post_request(SERVER_URL, fed.INITIALIZE_FED_SERVICE, server_init_data, "Initialize Fed Server")

        # 5. Start the Fit Process
        fit_process = start_fit_in_process(num_rounds=3, timeout=240)
        time.sleep(2)

        # 6. Intermediary Loop
        log("\n--- Starting Task-Result Communication Loop ---")
        while fit_process.is_alive():
            task_response = get_request(SERVER_URL, fed.GET_TASK, "Get tasks from server")

            if task_response and task_response.get('tasks'):
                tasks = task_response['tasks']
                log(f"Received {len(tasks)} task(s) from server.")

                for task in tasks:
                    proxy_id = task['proxy_id']
                    target_client_url = CLIENT_URL_MAP.get(proxy_id)

                    if not target_client_url:
                        log(f"ERROR: No client URL found for proxy_id '{proxy_id}'. Skipping task.")
                        continue


                    client_data_payload = {
                        'proxy_id': proxy_id,
                        'instruction': task['instruction']
                    }
                    client_res = post_request(target_client_url, fed.CLIENT_DATA, client_data_payload, f"Process task for {proxy_id}")

                    if client_res and 'results' in client_res:
                        server_res_payload = {
                            'proxy_id': client_res['proxy_id'],
                            'results': client_res['results']
                        }
                        post_request(SERVER_URL, fed.RES, server_res_payload, f"Submit result for {client_res['proxy_id']}")
            else:
                log("No tasks available from server. Waiting...")

            time.sleep(5)

        log("--- Fit process finished. Scenario complete. ---")

    finally:
        log("\n--- Terminating all background processes. ---")
        for p in all_processes:
            if p.is_alive():
                p.terminate()
                p.join()
        log("--- All processes terminated. ---")