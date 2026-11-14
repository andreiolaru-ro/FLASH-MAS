package abms.lbForaging;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * A food item in the Level-Based Foraging environment.
 * Each food has a level; it can only be collected when the sum of adjacent
 * loading agents' levels meets or exceeds the food's level.
 */
public class FoodPatch extends EntityCore<Pylon> implements Patch, EntityProxy<Patch> {

    public static final String COLLECTED_WAVE_CONTENT = "COLLECTED";

    private int level = 1;
    private boolean collected = false;

    public FoodPatch() {
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("level"))
            level = Integer.parseInt(configuration.getAValue("level"));
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return super.addGeneralContext(context);
    }

    @Override
    public void step() {
        // Food patches are passive; coordination is handled by ForagingContext
    }

    public int getLevel() {
        return level;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        if (event.getType() == AgentEventType.AGENT_WAVE) {
            String content = event.get(AgentWave.CONTENT);
            if (COLLECTED_WAVE_CONTENT.equals(content) && !collected) {
                collected = true;
                li("food [] collected!", getEntityName());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMainContext(Object context) {
        return context instanceof PylonProxy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Food";
    }
}
