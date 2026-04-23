package abms.lbForaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.xqhs.flash.abms.AgentManagementContext;
import net.xqhs.flash.abms.EntityGroup.EntityGroupLoader;
import net.xqhs.flash.abms.RandomContext;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.communication.ProximityCommunicationContext;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.util.MultiTreeMap;

public class LBForagingGroupLoader extends EntityGroupLoader {

    private static final String[] CATEGORY_NAMES = {"agent", "patch"};

    @Override
    public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
        return true;
    }

    @Override
    public LBForagingGroup load(MultiTreeMap multiTreeMap, List<EntityProxy<? extends Entity<?>>> context,
                                List<MultiTreeMap> subordinateEntities) {
        if (context == null)
            return null;

        // Obtain simulation and all contexts from it
        Simulation sim = (Simulation) Loader.getClosestContext(context, Simulation.class);
        if (sim == null)
            return null;

        SpaceContext<GridPosition> space = null;
        RandomContext randomContext = null;
        AgentManagementContext agentManagement = null;
        ProximityCommunicationContext proximityCommunication = null;
        ForagingContext foragingContext = null;
        for (SimulationContext simulationContext : sim.getSimulationContexts()) {
            if (simulationContext instanceof SpaceContext)
                space = (SpaceContext<GridPosition>) simulationContext;
            if (simulationContext instanceof RandomContext)
                randomContext = (RandomContext) simulationContext;
            if (simulationContext instanceof AgentManagementContext)
                agentManagement = (AgentManagementContext) simulationContext;
            if (simulationContext instanceof ProximityCommunicationContext)
                proximityCommunication = (ProximityCommunicationContext) simulationContext;
            if (simulationContext instanceof ForagingContext)
                foragingContext = (ForagingContext) simulationContext;
        }

        if (space == null)
            return null;
        GridTopology topology = (GridTopology) space.getTopology();
        if (topology == null)
            return null;
        topology.setDisplayProvider(LBForagingGroupLoader::getDisplayChar);

        if (proximityCommunication != null)
            proximityCommunication.addGeneralContext(space.asContext());

        // Prepare shuffled grid positions
        int totalCells = topology.getWidth() * topology.getHeight();
        List<GridPosition> positions = new ArrayList<>(totalCells);
        for (int y = 0; y < topology.getHeight(); y++)
            for (int x = 0; x < topology.getWidth(); x++)
                positions.add(new GridPosition(x, y));
        randomContext.shuffle(positions);

        // Build entity configs
        List<MultiTreeMap> entityConfigs = new ArrayList<>();
        List<String> entityCategories = new ArrayList<>();

        for (String category : CATEGORY_NAMES) {
            if (multiTreeMap.getTreeKeys().contains(category)) {
                MultiTreeMap categoryTree = multiTreeMap.getATree(category);
                addEntityConfigs(categoryTree, category, entityConfigs, entityCategories);
            }
            for (String otherCategory : CATEGORY_NAMES) {
                if (otherCategory.equals(category) || !multiTreeMap.getTreeKeys().contains(otherCategory))
                    continue;
                MultiTreeMap otherTree = multiTreeMap.getATree(otherCategory);
                for (String typeName : otherTree.getTreeKeys()) {
                    MultiTreeMap typeConfig = otherTree.getATree(typeName);
                    if (typeConfig.getTreeKeys().contains(category)) {
                        MultiTreeMap nestedTree = typeConfig.getATree(category);
                        addEntityConfigs(nestedTree, category, entityConfigs, entityCategories);
                    }
                }
            }
        }

        if (entityConfigs.size() > totalCells)
            return null;

        // Load all entities
        lp.lf("Loading [] entities...", Integer.valueOf(entityConfigs.size()));
        List<Entity<?>> allEntities = Deployment.get().loadEntities(entityConfigs, lp, new ArrayList<>(context));

        // Count food items for the foraging context
        int foodCount = 0;

        // Place and register all entities
        int idx = 0;
        for (Entity<?> entity : allEntities) {
            String category = entityCategories.get(idx);
            if (agentManagement != null && "agent".equals(category))
                entity.addGeneralContext(agentManagement.asContext());
            if (randomContext != null)
                entity.addGeneralContext(randomContext.asContext());
            if (proximityCommunication != null)
                entity.addGeneralContext(proximityCommunication.asContext());
            if (foragingContext != null)
                entity.addGeneralContext(foragingContext.asContext());
            entity.addGeneralContext(space.asContext());
            entity.addGeneralContext(sim.asContext());
            space.place(entity.asContext(), positions.get(idx));
            sim.registerEntity(category, entity, entity.getName());

            if (entity instanceof FoodPatch)
                foodCount++;

            idx++;
        }

        // Tell ForagingContext how many food items there are
        if (foragingContext != null)
            foragingContext.setTotalFoodCount(foodCount);

        LBForagingGroup group = new LBForagingGroup(allEntities);
        group.configure(multiTreeMap);
        return group;
    }

    @Override
    public LBForagingGroup load(MultiTreeMap multiTreeMap) {
        return load(multiTreeMap, null, null);
    }

    private static void addEntityConfigs(MultiTreeMap categoryTree, String category,
                                         List<MultiTreeMap> entityConfigs, List<String> entityCategories) {
        for (String typeName : categoryTree.getTreeKeys()) {
            MultiTreeMap typeConfig = categoryTree.getATree(typeName);
            int n = readInt(typeConfig, "n", 0);
            for (int i = 0; i < n; i++) {
                MultiTreeMap entityConfig = new MultiTreeMap();
                entityConfig.addSingleValue(DeploymentConfiguration.CATEGORY_ATTRIBUTE_NAME, category);
                String id = typeName.toLowerCase() + i;
                entityConfig.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
                        typeName + DeploymentConfiguration.NAME_SEPARATOR + id);
                for (String key : typeConfig.getSimpleNames()) {
                    if (key.startsWith("#") || "n".equals(key)
                            || DeploymentConfiguration.NAME_ATTRIBUTE_NAME.equals(key)
                            || "package".equals(key) || "in-context-of".equals(key))
                        continue;
                    entityConfig.addOneValue(key, typeConfig.getAValue(key));
                }
                entityConfigs.add(entityConfig);
                entityCategories.add(category);
            }
        }
    }

    private static int readInt(MultiTreeMap multiTreeMap, String key, int fallback) {
        if (multiTreeMap == null || !multiTreeMap.containsKey(key))
            return fallback;
        try {
            return Integer.parseInt(multiTreeMap.getAValue(key));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Display provider for the grid visualization.
     * F = Food (uncollected), A/digit = Forager agent
     */
    private static Character getDisplayChar(Set<EntityProxy<?>> entities) {
        char best = 0;
        int bestPriority = -1;
        for (EntityProxy<?> entity : entities) {
            String name = entity.getEntityName();
            if (name == null || name.isEmpty())
                continue;
            int priority;
            if (entity instanceof ForagerAgent) {
                best = (char) ('0' + ((ForagerAgent) entity).getLevel());
                priority = 2;
            } else if (entity instanceof FoodPatch) {
                FoodPatch food = (FoodPatch) entity;
                if (!food.isCollected()) {
                    best = (char) ('0' + food.getLevel());
                    priority = 1;
                } else {
                    priority = -1;
                    continue;
                }
            } else {
                priority = 0;
            }
            if (priority > bestPriority) {
                bestPriority = priority;
                if (entity instanceof ForagerAgent)
                    best = 'A';
                else if (entity instanceof FoodPatch && !((FoodPatch) entity).isCollected())
                    best = 'F';
            }
        }
        return bestPriority >= 0 ? best : null;
    }
}
