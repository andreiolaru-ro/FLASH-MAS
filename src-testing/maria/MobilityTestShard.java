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
		public void run()
		{
			((MobileCompositeAgent.MobileCompositeAgentShardContainer)getAgent()).moveTo("nodeB");
		}
	}

	/**
	 * Timer for moving.
	 */
	transient Timer	pingTimer	= null;

	public MobilityTestShard() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}

	@Override
	public void signalAgentEvent(AgentEvent event)
	{
		super.signalAgentEvent(event);
		System.out.println("######## Event in MobilityTestShard: " + event);

		switch (event.getType()) {
			case BEFORE_MOVE -> {
				System.out.println("buna ziua");
				pingTimer = new Timer();
				pingTimer.schedule(new MobilityTimer(), 5000);
			}
			case AFTER_MOVE -> pingTimer.cancel();
		}
	}
}
