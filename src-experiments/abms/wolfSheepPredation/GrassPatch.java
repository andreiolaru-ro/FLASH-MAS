package abms.wolfSheepPredation;

import net.xqhs.flash.abms.Patch;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GrassPatch extends EntityCore<Pylon> implements Patch, EntityProxy<Patch> {

    private static final int DEFAULT_REGROWTH_TIME = 3; //every 3 steps

    private boolean grown = true;
    private int regrowthCountdown = 0;
    private int regrowthTime = DEFAULT_REGROWTH_TIME;

    public GrassPatch() {
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("regrowthTime"))
            regrowthTime = Integer.parseInt(configuration.getAValue("regrowthTime"));
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return super.addGeneralContext(context);
    }

    // Step-based countdown: each step decrements the counter; grass regrows when it reaches zero.
    @Override
    public void step() {
        if (!grown) {
            regrowthCountdown--;
            if (regrowthCountdown <= 0) {
                grown = true;
                li("grass regrown");
            }
        }
    }

    public boolean isGrown() {
        return grown;
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        if (event.getType() == AgentEventType.AGENT_WAVE) {
            String content = event.get("content");
            if ("EAT".equals(content) && grown) {
                grown = false;
                regrowthCountdown = regrowthTime;
                li("grass eaten, regrowing in [] steps", regrowthTime);
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
        return getName() != null ? getName() : "Grass";
    }
}
