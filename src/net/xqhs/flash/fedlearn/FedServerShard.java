package net.xqhs.flash.fedlearn;

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
			
			// message sending example, sent to all agents:
			IntStream.range(1, nclients + 1).forEachOrdered(n -> {
				AgentWave wave = new AgentWave("test content").appendDestination(Constants.CLIENT_AGENT_PREFIX + n,
						FedClientShard.DESIGNATION);
				sendMessageFromShard(wave);
			});
			break;
		case AGENT_WAVE:
			if(DESIGNATION.equals(event.getValue(AgentWave.DESTINATION_ELEMENT))) {
				AgentWave wave = (AgentWave) event;
				lf("processing wave ", wave);
				// TODO process message
			}
			break;
		
		default:
			break;
		}
	}
}
