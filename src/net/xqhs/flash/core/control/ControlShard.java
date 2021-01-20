package net.xqhs.flash.core.control;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils.ControlOperation;
import net.xqhs.flash.core.util.PlatformUtils;

public class ControlShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = 5214882018809437402L;
	
	/**
	 * Endpoint element for this shard.
	 */
	public static final String SHARD_ENDPOINT = StandardAgentShard.CONTROL.shardName();
	
	/**
	 * Cache for the name of this agent.
	 */
	String thisAgent = null;
	
	{
		setUnitName("control-shard");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * Default constructor
	 */
	public ControlShard() {
		super(StandardAgentShard.CONTROL.toAgentShardDesignation());
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		return super.configure(configuration);
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			if(!SHARD_ENDPOINT.equals(((AgentWave) event).getFirstDestinationElement()))
				break;
			AgentWave wave = ((AgentWave) event).removeFirstDestinationElement();
			String operation = wave.getFirstDestinationElement();
			if(operation == null)
				li("Unknown control operation [].", operation);
			else
				switch(ControlOperation.fromOperation(operation)) {
				case START_SIMULATION:
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.SIMULATION_START));
					break;
				case STOP:
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
					break;
				default:
					li("Unhandled control operation [].", operation);
					break;
				}
			break;
		default:
			// nothing to do.
			break;
		}
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null)
			thisAgent = getAgent().getEntityName();
	}
	
	protected boolean sendMessage(String content) {
		// TODO: messages are sent via messaging shard
		// return sendMessage(content, SHARD_ENDPOINT, otherAgent, PingBackTestComponent.SHARD_ENDPOINT);
		return true;
	}
	
	@Override
	protected MultiTreeMap getShardData() {
		return super.getShardData();
	}
	
}
