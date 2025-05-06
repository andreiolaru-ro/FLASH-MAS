package testing;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * A generic AgentPingPong using WaveMessagingPylonProxy.
 */
public class AgentPingPongPlain extends BaseAgent {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;

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

	// For WebSocket we need to wait until all agents are started and registered to the server.
	protected static final long		PING_INITIAL_DELAY			= 2000;
	/**
	 * Time between ping messages.
	 */
	protected static final long		PING_PERIOD					= 2000;
	/**
	 * The name of the component parameter that contains the number of pings that should be sent.
	 */
	protected static final String	PING_NUMBER_PARAMETER_NAME	= "ping-number";
	/**
	 * Default number of pings that should be sent in case PING_NUMBER_PARAMETER_NAME is not present in the configuration.
	 */
	protected static final int		DEFAULT_PING_NUMBER			= 5;
	/**
	 * Limit number of pings sent.
	 */
	protected int pingLimit;
	/**
	 * Timer for pinging.
	 */
	protected Timer			pingTimer	= null;
	/**
	 * Cache for the name of the other agent.
	 */
	protected List<String>	otherAgents	= null;
	/**
	 * The index of the message sent.
	 */
	protected int				tick		= 0;

	WaveMessagingPylonProxy pylonProxy = null;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
		if(configuration.isSet(PING_NUMBER_PARAMETER_NAME)) {
			pingLimit = Integer.parseInt(configuration.getFirstValue(PING_NUMBER_PARAMETER_NAME));
		}
		else {
			pingLimit = DEFAULT_PING_NUMBER;
		}
		return true;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		pylonProxy = (WaveMessagingPylonProxy) getContext();
		if(pylonProxy == null)
			throw new IllegalStateException("No PylonProxy present");
		pylonProxy.register(getName(), new WaveReceiver() {
			@Override
			public void receive(AgentWave wave) {
				processEvent(wave);
			}
		});
		if(otherAgents != null) {
			pingTimer = new Timer();
			pingTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					sendPing();
				}
			}, PING_INITIAL_DELAY, PING_PERIOD);
		}
		li("Agent started");
		return true;
	}
	
	protected boolean processEvent(AgentWave wave) {
		li("received: " + wave.toString());
		if(otherAgents == null) { // only replies it is a pong agent
			String replyContent = wave.getContent() + " reply";
			li("sending reply ", wave.createReply(replyContent));
			return pylonProxy.send((wave).createReply(replyContent));
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		pingTimer.cancel();
		li("Agent stopped");
		return true;
	}
	
	protected void sendPing() {
		tick++;
		for(String otherAgent : otherAgents) {
			AgentWave wave = new AgentWave("ping-no " + tick, otherAgent, "pong").addSourceElementFirst("ping");
			lf("Sending the message [] to ", wave, otherAgent);
			if(!pylonProxy.send(wave.addSourceElementFirst(getName())))
				le("Message sending failed");
		}

		if(pingLimit >= 0 && tick >= pingLimit) {
			li("Ping limit reached, stopping agent.");
			stop();
			return;
		}
	}
}