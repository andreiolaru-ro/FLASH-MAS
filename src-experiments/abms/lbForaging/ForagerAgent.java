package abms.lbForaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import abms.lbForaging.ForagingContext.ForagingActionData;
import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext.ActionRecord;
import net.xqhs.flash.abms.SimulationContext.BaseContext.BaseActionData;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.abms.space.Position;
import net.xqhs.flash.abms.space.Topology;
import net.xqhs.flash.abms.space.gridworld.GridOrientation;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;

/**
 * Forager agent for the Level-Based Foraging environment.
 * Uses tabular Independent Q-Learning (IQL) with epsilon-greedy exploration.
 *
 * State representation: a string encoding of the agent's local observation
 * (relative positions and levels of visible food items and agents).
 */
public class ForagerAgent extends BaseAgent implements SteppableEntity, ShardContainer {

    public enum ForagerAction {
        NONE(null),
        NORTH(GridOrientation.NORTH),
        SOUTH(GridOrientation.SOUTH),
        WEST(GridOrientation.WEST),
        EAST(GridOrientation.EAST),
        LOAD(null);

        private final GridOrientation direction;

        ForagerAction(GridOrientation direction) {
            this.direction = direction;
        }

        public GridOrientation getDirection() {
            return direction;
        }

        public boolean isMovement() {
            return direction != null;
        }
    }

    private static final ForagerAction[] ACTIONS = ForagerAction.values();
    private static final int NUM_ACTIONS = ACTIONS.length;

    // Agent parameters
    private int level = 1;
    private int visionRange = 2;

    // Q-Learning parameters
    private double alpha = 0.1;      // learning rate
    private double gamma = 0.95;     // discount factor
    private double epsilon = 0.15;   // exploration rate
    private double epsilonDecay = 0.999;
    private double epsilonMin = 0.01;

    // Q-table: state -> action (by ordinal) -> Q-value
    private final Map<String, double[]> qTable = new HashMap<>();

    // State tracking for Q-learning updates
    private String previousState = null;
    private ForagerAction previousAction = null;
    private double pendingReward = 0.0;
    private boolean hasNewReward = false;

    // Cumulative reward for logging
    private double cumulativeReward = 0.0;

    // Environment link
    protected EnvironmentLinkShard e = new EnvironmentLinkShard();
    protected Simulation simulation;
    private ForagingContext foragingContext;

    public ForagerAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("level"))
            level = Integer.parseInt(configuration.getAValue("level"));
        if (configuration.containsKey("visionRange"))
            visionRange = Integer.parseInt(configuration.getAValue("visionRange"));
        if (configuration.containsKey("alpha"))
            alpha = Double.parseDouble(configuration.getAValue("alpha"));
        if (configuration.containsKey("gamma"))
            gamma = Double.parseDouble(configuration.getAValue("gamma"));
        if (configuration.containsKey("epsilon"))
            epsilon = Double.parseDouble(configuration.getAValue("epsilon"));
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        if (context instanceof ForagingContext)
            foragingContext = (ForagingContext) context;
        e.addGeneralContext(context);
        return super.addGeneralContext(context);
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        return false;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    /**
     * Called by ForagingContext to deliver reward from the previous step's load actions.
     */
    public void receiveReward(double reward) {
        pendingReward += reward;
        hasNewReward = true;
    }

    public int getLevel() {
        return level;
    }

    public double getCumulativeReward() {
        return cumulativeReward;
    }

    @Override
    public void step() {
        Position currentPos = e.getCurrentPosition();
        if (currentPos == null)
            return;

        // Observe current state
        String currentState = encodeState(currentPos);

        // Q-learning update from previous step
        if (previousState != null) {
            double reward = hasNewReward ? pendingReward : -0.01; // small step penalty
            cumulativeReward += reward;
            updateQ(previousState, previousAction.ordinal(), reward, currentState);
            pendingReward = 0.0;
            hasNewReward = false;
        }

        // Select action
        ForagerAction action = selectAction(currentState);

        // Execute action
        executeAction(action, (GridPosition) currentPos);

        // Store for next update
        previousState = currentState;
        previousAction = action;

        // Decay epsilon
        epsilon = Math.max(epsilonMin, epsilon * epsilonDecay);
    }

    /**
     * Encode the agent's local observation into a state string.
     * Encodes: own level + relative positions and levels of visible food + relative positions and levels of visible agents.
     */
    private String encodeState(Position currentPos) {
        GridPosition myPos = (GridPosition) currentPos;
        StringBuilder sb = new StringBuilder();
        sb.append("L").append(level).append("|");

        @SuppressWarnings("unchecked")
        Topology<Position> topology = (Topology<Position>) e.getTopology();
        Map<Position, Set<EntityProxy<?>>> visible = e.observe(visionRange);

        // Encode food items (sorted by relative position for consistency)
        TreeMap<String, String> foodEntries = new TreeMap<>();
        TreeMap<String, String> agentEntries = new TreeMap<>();

        for (Map.Entry<Position, Set<EntityProxy<?>>> entry : visible.entrySet()) {
            GridPosition pos = (GridPosition) entry.getKey();
            int dx = pos.getX() - myPos.getX();
            int dy = pos.getY() - myPos.getY();
            String posKey = dx + "," + dy;

            for (EntityProxy<?> entity : entry.getValue()) {
                if (entity instanceof FoodPatch) {
                    FoodPatch food = (FoodPatch) entity;
                    if (!food.isCollected())
                        foodEntries.put(posKey, "f" + posKey + ":" + food.getLevel());
                } else if (entity instanceof ForagerAgent && entity != this) {
                    ForagerAgent other = (ForagerAgent) entity;
                    agentEntries.put(posKey, "a" + posKey + ":" + other.getLevel());
                }
            }
        }

        sb.append("F[");
        for (String f : foodEntries.values())
            sb.append(f).append(";");
        sb.append("]A[");
        for (String a : agentEntries.values())
            sb.append(a).append(";");
        sb.append("]");

        return sb.toString();
    }

    /**
     * Epsilon-greedy action selection.
     */
    private ForagerAction selectAction(String state) {
        if (e.nextDouble() < epsilon) {
            return ACTIONS[e.nextInt(NUM_ACTIONS)];
        }
        double[] qValues = getQValues(state);
        int bestIdx = 0;
        double bestValue = qValues[0];
        for (int a = 1; a < NUM_ACTIONS; a++) {
            if (qValues[a] > bestValue) {
                bestValue = qValues[a];
                bestIdx = a;
            }
        }
        return ACTIONS[bestIdx];
    }

    /**
     * Tests whether the given action can be successfully executed from the given position.
     */
    public boolean testAction(ForagerAction action, GridPosition currentPos) {
        if (action == ForagerAction.NONE)
            return true;
        if (action == ForagerAction.LOAD)
            return hasFoodInRange(currentPos);
        // Movement action
        GridPosition targetPos = currentPos.getNeighborPosition(action.getDirection());
        return e.getValidNeighborPositions(currentPos).contains(targetPos);
    }

    /**
     * Execute the selected action.
     */
    private void executeAction(ForagerAction action, GridPosition currentPos) {
        if (action == ForagerAction.NONE)
            return;
        if (action == ForagerAction.LOAD) {
            tryLoad(currentPos);
            return;
        }
        tryMove(currentPos, action.getDirection());
    }

    private void tryMove(GridPosition currentPos, GridOrientation direction) {
        GridPosition targetPos = currentPos.getNeighborPosition(direction);
        if (e.getValidNeighborPositions(currentPos).contains(targetPos))
            e.moveToPosition(targetPos);
    }

    private boolean hasFoodInRange(GridPosition currentPos) {
        Set<EntityProxy<?>> here = e.getEntitiesAt(currentPos);
        for (EntityProxy<?> entity : here)
            if (entity instanceof FoodPatch && !((FoodPatch) entity).isCollected())
                return true;
        Set<Position> vicinity = e.getVicinity(currentPos);
        for (Position vPos : vicinity)
            for (EntityProxy<?> entity : e.getEntitiesAt(vPos))
                if (entity instanceof FoodPatch && !((FoodPatch) entity).isCollected())
                    return true;
        return false;
    }

    private void tryLoad(GridPosition currentPos) {
        if (foragingContext == null)
            return;

        // Find the closest food item at current position or adjacent
        FoodPatch targetFood = null;
        int bestDist = Integer.MAX_VALUE;

        // Check current position first
        Set<EntityProxy<?>> here = e.getEntitiesAt(currentPos);
        for (EntityProxy<?> entity : here) {
            if (entity instanceof FoodPatch && !((FoodPatch) entity).isCollected()) {
                targetFood = (FoodPatch) entity;
                bestDist = 0;
                break;
            }
        }

        // Check adjacent positions if no food at current position
        if (targetFood == null) {
            @SuppressWarnings("unchecked")
            Topology<Position> topology = (Topology<Position>) e.getTopology();
            Set<Position> vicinity = e.getVicinity(currentPos);
            for (Position vPos : vicinity) {
                Set<EntityProxy<?>> entities = e.getEntitiesAt(vPos);
                for (EntityProxy<?> entity : entities) {
                    if (entity instanceof FoodPatch && !((FoodPatch) entity).isCollected()) {
                        int dist = topology.getDistance(currentPos, vPos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            targetFood = (FoodPatch) entity;
                        }
                    }
                }
            }
        }

        if (targetFood != null) {
            // Submit load intent to ForagingContext
            foragingContext.addPendingAction(new ActionRecord(this,
                    new MultiValueMap()
                            .add(BaseActionData.ACTION.s(), ForagingActionData.LOAD_ACTION.s())
                            .addObject(ForagingActionData.LOAD_TARGET_FOOD.s(), targetFood)
                            .add(ForagingActionData.AGENT_LEVEL.s(), String.valueOf(level))));
        }
    }

    // Q-learning methods

    private double[] getQValues(String state) {
        return qTable.computeIfAbsent(state, k -> new double[NUM_ACTIONS]);
    }

    private void updateQ(String state, int action, double reward, String nextState) {
        double[] qValues = getQValues(state);
        double[] nextQValues = getQValues(nextState);

        // Find max Q-value for next state
        double maxNextQ = nextQValues[0];
        for (int a = 1; a < NUM_ACTIONS; a++)
            if (nextQValues[a] > maxNextQ)
                maxNextQ = nextQValues[a];

        // Q-learning update
        qValues[action] += alpha * (reward + gamma * maxNextQ - qValues[action]);
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Forager";
    }
}
