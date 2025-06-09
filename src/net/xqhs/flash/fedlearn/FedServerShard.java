package net.xqhs.flash.fedlearn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Shard for Federated Learning server functionality.
 */
public class FedServerShard extends AgentShardGeneral {
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID		= 1L;
	/**
	 * Shard designation,
	 */
	public static final String	DESIGNATION				= "Fed:Server";
	/**
	 * Parameter name for the number of clients.
	 */
	private static final String	NCLIENTS_PARAMETER_NAME	= "nclients";
	
	/**
	 * The node-local {@link FedDriver} instance.
	 */
	FedDriver	fedDriver;
	/**
	 * The number of clients.
	 */
	int			nclients;

	List<String> clientUpdates = new ArrayList<>();
	
	/**
	 * No-arg constructor.
	 */
	public FedServerShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		nclients = Integer.parseInt(configuration.get(NCLIENTS_PARAMETER_NAME));
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
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		
		switch(event.getType()) {
		case AGENT_START:
			// do start procedures
			li("FedServerShard starting...");
			broadcastGlobalModel("InitialModelData");
			
			// message sending example, sent to all agents:
			// IntStream.range(1, nclients + 1).forEachOrdered(n -> {
			// 	AgentWave wave = new AgentWave("test content").appendDestination(Constants.CLIENT_AGENT_PREFIX + n,
			// 			FedClientShard.DESIGNATION);
			// 	sendMessageFromShard(wave);
			// });
			break;
		case AGENT_WAVE:
			if(DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
				AgentWave wave = (AgentWave) event;
				lf("Processing wave: ", wave);
				// TODO process message
				String content = wave.getContent();
				if (content != null && content.startsWith("CLIENT_UPDATE")) {
					String clientUpdate = content.substring("CLIENT_UPDATE".length()).trim();
					li("Received client update.");
					clientUpdates.add(clientUpdate);

					if (clientUpdates.size() >= nclients) {
						li("All cliet updates received. Aggregating...");
						// Aggregate using Flask server
						// startFit implemented by Marius and Dragos
						String aggregatedModel = fedDriver.startFit(1, 60f);

						broadcastGlobalModel(aggregatedModel);
						clientUpdates.clear();
					}
				} else {
					lw("Unknown wave content received: " + content);
				}
			}
			break;
		
		default:
			break;
		}
	}

	private void broadcastGlobalModel(String modelData) {
		String globalModelMessage = "GLOBAL_MODEL_UPDATE " + modelData;
		IntStream.range(1, nclients + 1).forEachOrdered(n -> {
			AgentWave wave = new AgentWave(globalModelMessage)
					.appendDestination(Constants.CLIENT_AGENT_PREFIX + n, FedClientShard.DESIGNATION);
			sendMessageFromShard(wave);
		});
	}
}
