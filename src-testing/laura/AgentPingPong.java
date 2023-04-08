package laura;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ros.RosPylon;
import net.xqhs.util.logging.Unit;

/**
 * The implementation of the agents.
 */
public class AgentPingPong extends Unit implements Agent {
	/**
	 * The name of the component parameter that contains the id of the other agent.
	 */
	protected static final String	OTHER_AGENT_PARAMETER_NAME	= "sendTo";
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	SHARD_ENDPOINT				= "ping";
	/**
	 * Initial delay before the first ping message.
	 */
	protected static final long		PING_INITIAL_DELAY			= 0;
	/**
	 * Time between ping messages.
	 */
	protected static final long		PING_PERIOD					= 2000;
	/**
	 * Timer for pinging.
	 */
	Timer							pingTimer					= null;
	/**
	 * Cache for the name of the other agent.
	 */
	List<String>					otherAgents					= null;
	/**
	 * The messaging shard.
	 */
	MessagingShard					msgShard					= null;
	/**
	 * The name of this agent.
	 */
	String							agentName					= null;
	
	/**
	 * @param configuration
	 */
	public AgentPingPong(MultiTreeMap configuration) {
		agentName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		setUnitName(agentName);// .setLogLevel(Level.ALL);
		if(configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
		msgShard = new RosPylon.SimpleLocalMessaging();
		msgShard.addContext(new ShardContainer() {
			@Override
			public String getEntityName() {
				return getName();
			}
			
			@Override
			public void postAgentEvent(AgentEvent event) {
				AgentPingPong.this.postAgentEvent(event);
			}
			
			@Override
			public AgentShard getAgentShard(AgentShardDesignation designation) {
				return null;
			}
		});
		lf("Agent started.");
	}
	
	@Override
	public boolean start() {
		if(otherAgents != null) {
			pingTimer = new Timer();
			pingTimer.schedule(new TimerTask() {
				/**
				 * The index of the message sent.
				 */
				int tick = 0;
				long startTime = System.nanoTime();
				long stopTime;
				
				@Override
				public void run() {
					if (tick < 1000) {
						tick++;
						for (String otherAgent : otherAgents) {
							System.out.println("Sending the message to " + otherAgent);
							if (!msgShard.sendMessage(AgentWave.makePath(getName(), "ping"),
									AgentWave.makePath(otherAgent, "pong"), "ping-no " + tick))
								le("Message sending failed");
						}
						stopTime = System.nanoTime();
						long totalTime = stopTime - startTime;
						System.out.println("Time since first message: " + totalTime);
					}
					else {
						return;
					}
				}
			}, PING_INITIAL_DELAY, PING_PERIOD);
		}
		return true;
	}
	
	/**
	 * @param event
	 *            - the event received.
	 */
	protected void postAgentEvent(AgentEvent event) {
		li("received: " + event.toString());
		if(event.getType().equals(AgentEventType.AGENT_WAVE) && otherAgents == null) {
			String replyContent = ((AgentWave) event).getContent() + " reply";
			msgShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT), ((AgentWave) event).getCompleteSource(),
					replyContent);
		}
	}
	
	@Override
	public boolean stop() {
		return false;
	}
	
	@Override
	public boolean isRunning() {
		return true;
	}
	
	@Override
	public String getName() {
		return agentName;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		lf("Context added: ", context.getEntityName());
		return msgShard.addGeneralContext(context);
	}
	
	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return addContext((MessagingPylonProxy) context);
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return null;
	}
	
	@Override
	protected void le(String message, Object... arguments) {
		super.le(message, arguments);
	}
	
	@Override
	protected void lf(String message, Object... arguments) {
		super.lf(message, arguments);
	}
}