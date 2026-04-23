package abms.lbForaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiValueMap;

/**
 * Coordination context for the Level-Based Foraging environment.
 * Collects LOAD intents from agents during their step, then resolves them:
 * a food item is collected if the sum of adjacent loading agents' levels >= food level.
 * Rewards are queued for delivery to agents in the next step.
 */
public class ForagingContext extends SimulationContext.BaseContext
        implements SimulationContext, EntityProxy<ForagingContext> {

    public enum ForagingActionData implements ActionData {
        LOAD_ACTION, LOAD_TARGET_FOOD, AGENT_LEVEL;

        @Override
        public String s() {
            return this.toString();
        }
    }

    private SpaceContext<?> space;
    private Simulation simulation;
    private int currentStep = 0;
    private int totalFoodCount = 0;
    private int collectedFoodCount = 0;

    // Pending reward events: agent -> reward value
    private final Map<EntityProxy<?>, Double> pendingRewards = new HashMap<>();

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof SpaceContext)
            space = (SpaceContext<?>) context;
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        return super.addGeneralContext(context);
    }

    public void setTotalFoodCount(int count) {
        this.totalFoodCount = count;
    }

    public int getCollectedFoodCount() {
        return collectedFoodCount;
    }

    public int getTotalFoodCount() {
        return totalFoodCount;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    @Override
    public void validateAndExecutePendingActions() {
        if (space == null && simulation != null)
            for (SimulationContext ctx : simulation.getSimulationContexts())
                if (ctx instanceof SpaceContext)
                    space = (SpaceContext<?>) ctx;

        currentStep++;

        // Group load intents by target food item
        Map<FoodPatch, List<LoadIntent>> loadsByFood = new HashMap<>();

        for (ActionRecord action : pendingActions) {
            String actionType = action.getActionData().get(BaseActionData.ACTION.s());
            if (!ForagingActionData.LOAD_ACTION.s().equals(actionType))
                continue;

            FoodPatch food = (FoodPatch) action.getActionData()
                    .getObject(ForagingActionData.LOAD_TARGET_FOOD.s());
            int agentLevel = Integer.parseInt(
                    action.getActionData().get(ForagingActionData.AGENT_LEVEL.s()));

            if (food == null || food.isCollected())
                continue;

            // Verify the agent is adjacent to or on the same cell as the food
            Position agentPos = space.getPosition(action.getEntity());
            Position foodPos = space.getPosition(food);
            if (agentPos == null || foodPos == null)
                continue;

            // Check adjacency: distance <= 1 (orthogonal neighbors or same cell)
            @SuppressWarnings("unchecked")
            Topology<Position> topology = (Topology<Position>) space.getTopology();
            int dist = topology.getDistance(agentPos, foodPos);
            if (dist > 1)
                continue;

            loadsByFood.computeIfAbsent(food, k -> new ArrayList<>())
                    .add(new LoadIntent(action.getEntity(), agentLevel));
        }
        pendingActions.clear();

        // Resolve each food's load attempts
        for (Map.Entry<FoodPatch, List<LoadIntent>> entry : loadsByFood.entrySet()) {
            FoodPatch food = entry.getKey();
            List<LoadIntent> intents = entry.getValue();

            if (food.isCollected())
                continue;

            int totalLevel = 0;
            for (LoadIntent intent : intents)
                totalLevel += intent.agentLevel;

            if (totalLevel >= food.getLevel()) {
                // Food is collected
                food.setCollected(true);
                collectedFoodCount++;

                // Remove food from space
                space.removeEntity(food);

                // Deregister from simulation
                if (simulation != null)
                    simulation.deregisterEntity((Entity<?>) food);

                li("Food [] (level []) collected by [] agents (total level [])",
                        food.getEntityName(), food.getLevel(), intents.size(), totalLevel);

                // Distribute equal reward among participating agents
                double reward = (double) food.getLevel() / intents.size();
                for (LoadIntent intent : intents) {
                    pendingRewards.merge(intent.agent, reward, Double::sum);
                }
            }
        }
    }

    @Override
    public void sendEvents(Entity<?> entity) {
        EntityProxy<?> proxy = entity.asContext();
        if (proxy == null)
            proxy = (entity instanceof EntityProxy<?>) ? (EntityProxy<?>) entity : null;
        if (proxy == null)
            return;

        Double reward = pendingRewards.remove(proxy);
        if (reward != null && entity instanceof ForagerAgent) {
            ((ForagerAgent) entity).receiveReward(reward);
        }
    }

    @Override
    public String visualizeAsString() {
        return String.format("Step %d | Food collected: %d/%d", currentStep, collectedFoodCount, totalFoodCount);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Simulation>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public String getEntityName() {
        return name;
    }

    private static class LoadIntent {
        final EntityProxy<?> agent;
        final int agentLevel;

        LoadIntent(EntityProxy<?> agent, int agentLevel) {
            this.agent = agent;
            this.agentLevel = agentLevel;
        }
    }
}
