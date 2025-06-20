package net.xqhs.flash.fedlearn;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shard for Federated Learning server functionality.
 */
public class FedServerShard extends AgentShardGeneral {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Shard designation,
	 */
	public static final String DESIGNATION = "Fed:Server";
	/**
	 * Parameter name for the number of clients.
	 */
	private static final String NCLIENTS_PARAMETER_NAME = "nclients";

	/**
	 * The node-local {@link FedDriver} instance.
	 */
	FedDriver fedDriver;
	/**
	 * The number of clients.
	 */
	int nclients;

	private List<String> registeredClients = new ArrayList<>();
	private Timer taskTimer;
	private final AtomicBoolean fitInProgress = new AtomicBoolean(false);
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	// FL config parameters
	private float fractionFit;
	private float fractionEvaluate;
	private int minFitClients;
	private int minEvaluateClients;
	private int minAvailableClients;
	private int numRounds;
	private float timeout;

	/**
	 * No-arg constructor.
	 */
	public FedServerShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}

	@Override
	public boolean configure(MultiTreeMap configuration) {
		nclients = Integer.parseInt(configuration.get(NCLIENTS_PARAMETER_NAME));

		// Read FL parameters
		fractionFit = Float.parseFloat(configuration.get("fraction_fit"));
		fractionEvaluate = Float.parseFloat(configuration.get("fraction_evaluate"));
		minFitClients = Integer.parseInt(configuration.get("min_fit_clients"));
		minEvaluateClients = Integer.parseInt(configuration.get("min_evaluate_clients"));
		minAvailableClients = Integer.parseInt(configuration.get("min_available_clients"));
		numRounds = Integer.parseInt(configuration.get("num_rounds"));
		timeout = Float.parseFloat(configuration.get("timeout"));

		return super.configure(configuration);
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (!super.addGeneralContext(context))
			return false;
		if (context instanceof FedDriver) {
			fedDriver = (FedDriver) context;
			li("Fed Driver detected");
			return true;
		}
		return true;
	}

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);

		switch (event.getType()) {
			case AGENT_START:
				li("FedServerShard is online and waiting for " + nclients + " clients to register.");
				break;

			case AGENT_WAVE:
				if (DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
					AgentWave wave = (AgentWave) event;
					lf("Processing wave: ", wave);
					String content = wave.getContent();
					if (content != null) {
						if (content.startsWith("REGISTER_CLIENT")) {
							String clientId = content.substring("REGISTER_CLIENT".length()).trim();
							if (!registeredClients.contains(clientId)) {
								li("Received registration from client: " + clientId);
								registeredClients.add(clientId);

								// Register with the Python server's ClientManager
								boolean regSuccess = fedDriver.register(clientId) != null;
								if (!regSuccess) {
									lw("Failed to register client with Python server: " + clientId);
								}

								// Check if all clients have now registered
								if (registeredClients.size() >= nclients) {
									li("All " + nclients + " clients registered. Initializing server...");
									initializeAndStartFederatedLearning();
								}
							}
						} else if (content.startsWith("TASK_RESULT")) {
							// This logic remains the same and is correct
							String[] parts = content.substring("TASK_RESULT".length()).trim().split(":", 2);
							if (parts.length == 2) {
								String clientId = parts[0];
								String result = parts[1];
								li("Received result from client: " + clientId);

								boolean resSuccess = fedDriver.getRes(clientId, result) != null;
								if (!resSuccess) {
									lw("Failed to submit result for client: " + clientId);
								}
							}
						}
					}
				}
				break;

			default:
				break;
		}
	}


	private void initializeAndStartFederatedLearning() {
		new Thread(() -> {
			// 1. Initialize the Server
			li("Calling initFedServer...");
			boolean initSuccess = fedDriver.initFedServer(
					fractionFit, fractionEvaluate, minFitClients,
					minEvaluateClients, minAvailableClients, nclients) != null;

			if (!initSuccess) {
				le("Failed to initialize server. Aborting fit process.");
				return;
			}

			li("Federated server initialized successfully.");

			// The entire process is now managed sequentially here.
			manageFitAndTaskLoop();

		}).start();
	}

	private void manageFitAndTaskLoop() {
		// 1. Set the flag and start the background task poller
		li("Starting task polling loop...");
		fitInProgress.set(true);
		startTaskLoop();

		// 2. Make the BLOCKING call to start the fit process.
		//    Execution will pause on this line until the fit is complete.
		li("Starting fit process for " + numRounds + " rounds. This will block until finished...");
		String fitResponse = fedDriver.startFit(numRounds, timeout);

		// 3. Once startFit returns, the process is over. Stop the loop.
		li("Fit process has returned. Stopping task polling loop...");
		stopTaskLoop();

		if (fitResponse != null) {
			li("Fit process completed successfully. Response: " + fitResponse);
		} else {
			le("Fit process failed or returned no response.");
		}
	}


	private void startTaskLoop() {
		taskTimer = new Timer(true);
		taskTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// This check is now the gatekeeper. When fitInProgress becomes false,
				// the timer will cancel itself on its next run.
				if (!fitInProgress.get()) {
					li("Fit no longer in progress. Cancelling timer task.");
					this.cancel();
					return;
				}

				String taskResponse = fedDriver.getTask();
				if (taskResponse == null || taskResponse.isEmpty()) {
					// No tasks available right now, just wait for the next interval
					return;
				}
				// Check for the "no tasks" message from the Flask server
				if (taskResponse.contains("No tasks available for clients.")){
					return;
				}

				li("++++++++++++++++++++ FedServerShard got task: " + taskResponse);

				try {
					// Parse JSON response
					Gson gson = new Gson();
					JsonObject responseObj = gson.fromJson(taskResponse, JsonObject.class);
					if (responseObj.has("tasks")) {
						JsonArray tasks = responseObj.getAsJsonArray("tasks");
						for (JsonElement taskElem : tasks) {
							JsonObject task = taskElem.getAsJsonObject();
							String proxyId = task.get("proxy_id").getAsString();
							String instruction = task.get("instruction").getAsString();

							// Send task to client
							AgentWave taskWave = new AgentWave("TASK:" + instruction)
									.appendDestination(proxyId, FedClientShard.DESIGNATION);
							sendMessageFromShard(taskWave);
						}
					}
				} catch (Exception e) {
					le("Error parsing task response: " + e.getMessage());
				}
			}
		}, 0, 3000); // check every 3 seconds
	}

	private void stopTaskLoop() {
		// Setting the flag to false is the primary signal for the loop to stop.
		fitInProgress.set(false);

		if (taskTimer != null) {
			taskTimer.cancel();
			taskTimer = null;
		}
		li("Fit process and task loop have been stopped.");
	}

	@Override
	public boolean stop() {
		stopTaskLoop();
		return super.stop();
	}
}