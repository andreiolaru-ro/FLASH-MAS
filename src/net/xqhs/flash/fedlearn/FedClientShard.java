package net.xqhs.flash.fedlearn;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Shard for Federated Learning client functionality.
 */
public class FedClientShard extends AgentShardGeneral {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Shard designation,
	 */
	public static final String	DESIGNATION			= "Fed:Client";
	
	/**
	 * The node-local {@link FedDriver} instance.
	 */
	FedDriver					fedDriver;

	private String agentName;
	private String serverAgentId;
	private String dataset;
	private int partitionId;
	private int numPartitions;
	
	/**
	 * No-arg constructor.
	 */
	public FedClientShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}

	@Override
	public boolean configure(MultiTreeMap configuration) {
		// Read client configuration
		serverAgentId = configuration.get("server_agent_id");
		dataset = configuration.get("dataset");
		partitionId = Integer.parseInt(configuration.get("partition_id"));
		numPartitions = Integer.parseInt(configuration.get("num_partitions"));

		return super.configure(configuration);
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		if(context instanceof FedDriver) {
			fedDriver = (FedDriver) context;
			li("Fed Driver detected");
			return true;
		}
		agentName = getAgent().getEntityName();
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		
		switch(event.getType()) {
		case AGENT_START:
			li("FedClientShard starting...");

			// Initialize federated client
			boolean initSuccess = fedDriver.initFedClient(
				serverAgentId,
				dataset,
				partitionId,
				numPartitions
			) != null;

			if (!initSuccess) {
				le("Failed to initialize federated client");
			}

			// Register with server
			AgentWave registerWave = new AgentWave("REGISTER_CLIENT " + agentName)
					.appendDestination(serverAgentId, FedServerShard.DESIGNATION);
			sendMessageFromShard(registerWave);
			break;
		case AGENT_WAVE:
			if(DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
				AgentWave wave = (AgentWave) event;
				lf("Processing wave: ", wave);
				String content = wave.getContent();
				if (content != null && content.startsWith("TASK:")) {
					String instruction = content.substring("TASK:".length());
					li("Received task from server");

					// Process task
					String result = fedDriver.clientData(
						agentName,
						instruction
					);

					if (result == null) {
						lw("Task processing failed for client: " + agentName);
						return;
					}

					// Send result back
					AgentWave resultWave = new AgentWave("TASK_RESULT " + agentName + ":" + result)
							.appendDestination(wave.getFirstSource(), FedServerShard.DESIGNATION);
					sendMessageFromShard(resultWave);
				}
			}
			break;
		
		default:
			break;
		}
	}
}
