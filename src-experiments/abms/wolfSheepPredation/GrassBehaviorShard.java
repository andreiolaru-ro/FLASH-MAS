package abms.wolfSheepPredation;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

public class GrassBehaviorShard extends AgentShardCore {

    private static final long REGROWTH_DELAY_MS = 3000;

    private Timer regrowthTimer;

    public GrassBehaviorShard() {
        super(AgentShardDesignation.customShard("GrassBehavior"));
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        switch (event.getType()) {
            case AGENT_START:
                ((GrassAgent) getAgent()).setGrown(true);
                li("grass is grown");
                break;
            case AGENT_WAVE:
                String content = ((AgentWave) event).getContent();
                if ("EAT".equals(content)) {
                    GrassAgent grass = (GrassAgent) getAgent();
                    grass.setGrown(false);
                    li("grass eaten, scheduling regrowth");
                    scheduleRegrowth();
                } else if ("REGROW".equals(content)) {
                    ((GrassAgent) getAgent()).setGrown(true);
                    li("grass regrown");
                }
                break;
            case AGENT_STOP:
                cancelTimer();
                break;
            default:
                break;
        }
    }

    private void scheduleRegrowth() {
        cancelTimer();
        regrowthTimer = new Timer(true);
        regrowthTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getAgent().postAgentEvent(new AgentWave("REGROW"));
            }
        }, REGROWTH_DELAY_MS);
    }

    private void cancelTimer() {
        if (regrowthTimer != null) {
            regrowthTimer.cancel();
            regrowthTimer = null;
        }
    }
}
