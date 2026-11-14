package testing;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.WaveMessagingPylonProxy;
import net.xqhs.flash.core.support.WaveReceiver;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * A generic AgentPingPong communicating by calling directly the WaveMessagingPylonProxy in the context of which it is
 * placed.
 * <p>
 * For flexibility, all comms-related methods are placed in separate methods.
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
	 * Initial delay before the first ping message, to make sure that all agents started.
	 */
	protected static final long		PING_INITIAL_DELAY			= 1000;
	/**
	 * Time between ping messages.
	 */
	protected static final long		PING_PERIOD					= 1000;
	/**
	 * The name of the component parameter that contains the number of pings that should be sent.
	 */
	protected static final String	PING_NUMBER_PARAMETER_NAME	= "ping-number";
	/**
	 * Default number of pings that should be sent in case PING_NUMBER_PARAMETER_NAME is not present in the
	 * configuration.
	 */
	protected static final int		DEFAULT_PING_NUMBER			= 5;
	/**
	 * Limit number of pings sent.
	 */
	protected int					pingLimit;
	/**
	 * Timer for pinging.
	 */
	protected Timer					pingTimer					= null;
	/**
	 * Cache for the name of the other agent.
	 */
	protected List<String>			otherAgents					= null;
	/**
	 * The index of the message sent.
	 */
	protected int					tick						= 0;
	/**
	 * The pylon that will be used for sending and receiving messages.
	 */
	WaveMessagingPylonProxy			pylonProxy					= null;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.isSet(OTHER_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(OTHER_AGENT_PARAMETER_NAME);
		if(configuration.isSet(PING_NUMBER_PARAMETER_NAME))
			pingLimit = Integer.parseInt(configuration.getFirstValue(PING_NUMBER_PARAMETER_NAME));
		else
			pingLimit = DEFAULT_PING_NUMBER;
		return true;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		if(!super.addContext(context))
			return false;
		lf("Context added: ", context.getEntityName());
		return register(context);
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
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
	
	/**
	 * Prints the received {@link AgentWave} and, if it is the case, replies to it.
	 * 
	 * @param wave
	 *            - the wave to process.
	 * @return an indication of success.
	 */
	protected boolean processEvent(AgentWave wave) {
		li("received: " + wave.toString());
		if(otherAgents == null) { // only replies it is a pong agent
			String replyContent = wave.getContent() + " reply";
			li("sending reply ", wave.createReply(replyContent));
			boolean mustStop = false;
			if(wave.getContent().substring(wave.getContent().length() - 4).equals("last"))
				mustStop = true;
			boolean ret = send((wave).createReply(replyContent));
			if(mustStop)
				return stop() && ret;
			return ret;
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		if(pingTimer != null)
			pingTimer.cancel();
		li("Agent stopped");
		return true;
	}
	
	/**
	 * Sends a "ping" message to the "other" agents.
	 */
	protected void sendPing() {
		tick++;
		for(String otherAgent : otherAgents) {
			AgentWave wave = new AgentWave("ping-no " + tick + (tick == pingLimit ? " last" : ""), otherAgent, "pong")
					.addSourceElementFirst("ping");
			lf("Sending the message [] to ", wave, otherAgent);
			if(!send(wave.addSourceElementFirst(getName())))
				le("Message sending failed");
		}
		
		if(pingLimit >= 0 && tick >= pingLimit) {
			li("Ping limit reached, stopping agent.");
			stop();
			return;
		}
	}
	
	/**
	 * Registers with the communications infrastructure.
	 * 
	 * @param context
	 *            - the context, if necessary, <code>null</code> otherwise.
	 * @return an indication of success.
	 */
	protected boolean register(EntityProxy<Pylon> context) {
		try {
			pylonProxy = (WaveMessagingPylonProxy) context;
		} catch(ClassCastException e) {
			return ler(false, "Context not of appropriate type", PlatformUtils.printException(e));
		}
		if(pylonProxy == null)
			throw new IllegalStateException("No PylonProxy present");
		if(!pylonProxy.register(getName(), new WaveReceiver() {
			@Override
			public void receive(AgentWave wave) {
				processEvent(wave);
			}
		}))
			return ler(false, "Failed to register with", pylonProxy.getEntityName());
		return true;
	}
	
	/**
	 * Sends the {@link AgentWave}.
	 * 
	 * @param wave
	 *            - the wave to send.
	 * @return an indication of success.
	 */
	protected boolean send(AgentWave wave) {
		return pylonProxy.send(wave);
	}
}