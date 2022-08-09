package test.compositeMobility;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent.MobileCompositeAgentShardContainer;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Test agent mobility. After the given delay, the agent moves to the specified node. Then, again after the same delay,
 * the agent moves back to the original container.
 * 
 * @author Andrei Olaru
 * @author Maria-Claudia Buiac
 */
public class MobilityTestShard extends AgentShardGeneral {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -8576274703529501390L;
	
	/**
	 * Timer to the next move (to <code>destination</code>).
	 * 
	 * @author Andrei Olaru
	 */
	class MobilityTimer extends TimerTask {
		@Override
		public void run() {
			if(destination != null) {
				getMobileAgent().moveTo(destination);
				pingTimer.cancel();
			}
		}
	}
	
	/**
	 * Name of the parameter specifying the destination node.
	 */
	public static final String	TARGET_PARAMETER_NAME	= "to";
	/**
	 * Name of the parameter specifying the delay to the move.
	 */
	public static final String	TIME_PARAMETER_NAME		= "time";
	
	/**
	 * Timer for moving.
	 */
	transient Timer	pingTimer		= null;
	/**
	 * The destination node to move to next.
	 */
	String			destination		= null;
	/**
	 * The destination node to move to the second time (the original node).
	 */
	String			nextDestination	= null;
	/**
	 * The delay between start and movement.
	 */
	int				delay;
	
	/**
	 * The constructor.
	 */
	public MobilityTestShard() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		destination = configuration.getFirstValue(TARGET_PARAMETER_NAME);
		delay = Integer.parseInt(configuration.getFirstValue(TIME_PARAMETER_NAME));
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		
		switch(event.getType()) {
		case AGENT_START:
			if(nextDestination != null) {
				if(destination.equals(nextDestination))
					return; // the agent had already moved back to the original node
				destination = nextDestination; // the next move will be to the original node
			}
			else
				// remember the current node, to move back to it
				nextDestination = getMobileAgent().getCurrentNode();
			pingTimer = new Timer();
			pingTimer.schedule(new MobilityTimer(), delay);
			break;
		case AFTER_MOVE:
			pingTimer.cancel();
			break;
		default:
			// nothing to do
		}
	}
	
	/**
	 * A proxy to {@link #getAgent()} that also checks that the parent agent is indeed mobile.
	 * 
	 * @return the proxy to the parent agent.
	 */
	MobileCompositeAgentShardContainer getMobileAgent() {
		return (MobileCompositeAgentShardContainer) getAgent();
	}
}
