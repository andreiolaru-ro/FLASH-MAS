package maria;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;

import java.util.Timer;
import java.util.TimerTask;

public class MobilityTestShard extends AgentShardGeneral {

	class MobilityTimer extends TimerTask
	{
		@Override
		public void run() {
			if (destination != null) {
				((MobileCompositeAgent.MobileCompositeAgentShardContainer) getAgent()).moveTo(destination);
			}
		}
	}

	/**
	 * Timer for moving.
	 */
	transient Timer	pingTimer	= null;

	String destination = null;
	public static final String TARGET = "TARGET";

	public MobilityTestShard() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}

	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);

		switch (event.getType()) {
			case BEFORE_MOVE -> {
				destination = event.getValue(TARGET);
				pingTimer = new Timer();
				pingTimer.schedule(new MobilityTimer(), 5000);
			}
			case AFTER_MOVE -> pingTimer.cancel();
		}
	}
}
