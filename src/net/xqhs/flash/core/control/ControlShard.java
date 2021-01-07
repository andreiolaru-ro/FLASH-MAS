package net.xqhs.flash.core.control;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils;
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
		setUnitName("control-shard").setLoggerType(PlatformUtils.platformLogType());
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
			if(!(((AgentWave) event).getFirstDestinationElement()).equals(SHARD_ENDPOINT))
				break;
			parseAgentWave(event);
			break;
		case AGENT_START:
			li("Shard []/[] started.", thisAgent, SHARD_ENDPOINT);
			break;
		case AGENT_STOP:
			li("Shard []/[] stopped.", thisAgent, SHARD_ENDPOINT);
			break;
		case SIMULATION_START:
			li("Shard []/[] started simulation.", thisAgent, SHARD_ENDPOINT);
			break;
		case SIMULATION_PAUSE:
			li("Shard []/[] paused simulation.", thisAgent, SHARD_ENDPOINT);
			break;
		default:
			break;
		}
	}
	
	protected void parseAgentWave(AgentEvent event) {
		if(!((AgentWave) event).getFirstDestinationElement().equals(SHARD_ENDPOINT))
			return;
		Object obj = JSONValue.parse(((AgentWave) event).getContent());
		if(obj == null)
			return;
		if(obj instanceof JSONObject) {
			JSONObject jo = (JSONObject) obj;
			if(jo.get(OperationUtils.NAME) != null) {
				String operation = (String) jo.get(OperationUtils.NAME);
				switch(operation) {
				case "stop":
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
					break;
				case "start_simulation":
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.SIMULATION_START));
					break;
				default:
					break;
				}
			}
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
