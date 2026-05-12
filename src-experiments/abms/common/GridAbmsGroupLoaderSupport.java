package abms.common;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.abms.AgentManagementContext;
import net.xqhs.flash.abms.RandomContext;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.communication.CommunicationContext;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.abms.space.gridworld.GridTopology;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.util.MultiTreeMap;
//later this class will be used to remove duplicate code from WolfSheepGroupLoader
//and LBForagingGroupLoader
public final class GridAbmsGroupLoaderSupport {
    private GridAbmsGroupLoaderSupport() {
    }

    @SuppressWarnings("unchecked")
    public static ResolvedGridContexts resolveGridContexts(List<EntityProxy<? extends Entity<?>>> context,
            GridTopology.CellDisplayProvider displayProvider) {
        if (context == null)
            return null;

        Simulation simulation = (Simulation) Loader.getClosestContext(context, Simulation.class);
        if (simulation == null)
            return null;

        SpaceContext<GridPosition> space = null;
        RandomContext randomContext = null;
        AgentManagementContext agentManagement = null;
        CommunicationContext communication = null;
        for (SimulationContext simulationContext : simulation.getSimulationContexts()) {
            if (simulationContext instanceof SpaceContext)
                space = (SpaceContext<GridPosition>) simulationContext;
            if (simulationContext instanceof RandomContext)
                randomContext = (RandomContext) simulationContext;
            if (simulationContext instanceof AgentManagementContext)
                agentManagement = (AgentManagementContext) simulationContext;
            if (simulationContext instanceof CommunicationContext)
                communication = (CommunicationContext) simulationContext;
        }

        if (space == null || randomContext == null)
            return null;
        GridTopology topology = (GridTopology) space.getTopology();
        if (topology == null)
            return null;
        topology.setDisplayProvider(displayProvider);

        if (communication != null)
            communication.addGeneralContext(space.asContext());

        return new ResolvedGridContexts(simulation, space, topology, randomContext,
                agentManagement, communication);
    }

    public static List<GridPosition> createShuffledPositions(GridTopology topology, RandomContext randomContext) {
        int totalCells = topology.getWidth() * topology.getHeight();
        List<GridPosition> positions = new ArrayList<>(totalCells);
        for (int y = 0; y < topology.getHeight(); y++)
            for (int x = 0; x < topology.getWidth(); x++)
                positions.add(new GridPosition(x, y));
        randomContext.shuffle(positions);
        return positions;
    }

    public static EntityConfigBundle buildEntityConfigs(MultiTreeMap configuration, String[] categoryNames,
            boolean includeNestedCategories) {
        EntityConfigBundle bundle = new EntityConfigBundle();
        for (String category : categoryNames) {
            if (configuration.getTreeKeys().contains(category)) {
                MultiTreeMap categoryTree = configuration.getATree(category);
                addEntityConfigs(categoryTree, category, bundle);
            }
            if (includeNestedCategories)
                addNestedEntityConfigs(configuration, categoryNames, category, bundle);
        }
        return bundle;
    }

    public static LoadedEntities loadPlaceAndRegister(EntityConfigBundle configs, LoadPack loadPack,
            List<EntityProxy<? extends Entity<?>>> context, ResolvedGridContexts gridContexts,
            List<GridPosition> positions) {
        List<Entity<?>> entities = Deployment.get().loadEntities(configs.entityConfigs, loadPack,
                new ArrayList<>(context));

        int idx = 0;
        for (Entity<?> entity : entities) {
            String category = configs.entityCategories.get(idx);
            if (gridContexts.agentManagement != null && "agent".equals(category))
                entity.addGeneralContext(gridContexts.agentManagement.asContext());
            if (gridContexts.randomContext != null)
                entity.addGeneralContext(gridContexts.randomContext.asContext());
            if (gridContexts.communication != null)
                entity.addGeneralContext(gridContexts.communication.asContext());
            entity.addGeneralContext(gridContexts.space.asContext());
            entity.addGeneralContext(gridContexts.simulation.asContext());
            gridContexts.space.place(entity.asContext(), positions.get(idx));
            gridContexts.simulation.registerEntity(category, entity, entity.getName());
            idx++;
        }

        return new LoadedEntities(entities, configs.entityCategories);
    }

    private static void addNestedEntityConfigs(MultiTreeMap configuration, String[] categoryNames, String category,
            EntityConfigBundle bundle) {
        for (String otherCategory : categoryNames) {
            if (otherCategory.equals(category) || !configuration.getTreeKeys().contains(otherCategory))
                continue;
            MultiTreeMap otherTree = configuration.getATree(otherCategory);
            for (String typeName : otherTree.getTreeKeys()) {
                MultiTreeMap typeConfig = otherTree.getATree(typeName);
                if (typeConfig.getTreeKeys().contains(category)) {
                    MultiTreeMap nestedTree = typeConfig.getATree(category);
                    addEntityConfigs(nestedTree, category, bundle);
                }
            }
        }
    }

    private static void addEntityConfigs(MultiTreeMap categoryTree, String category, EntityConfigBundle bundle) {
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
                bundle.entityConfigs.add(entityConfig);
                bundle.entityCategories.add(category);
            }
        }
    }

    private static int readInt(MultiTreeMap configuration, String key, int fallback) {
        if (configuration == null || !configuration.containsKey(key))
            return fallback;
        try {
            return Integer.parseInt(configuration.getAValue(key));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static class ResolvedGridContexts {
        public final Simulation simulation;
        public final SpaceContext<GridPosition> space;
        public final GridTopology topology;
        public final RandomContext randomContext;
        public final AgentManagementContext agentManagement;
        public final CommunicationContext communication;

        ResolvedGridContexts(Simulation simulation, SpaceContext<GridPosition> space,
                GridTopology topology, RandomContext randomContext, AgentManagementContext agentManagement,
                CommunicationContext communication) {
            this.simulation = simulation;
            this.space = space;
            this.topology = topology;
            this.randomContext = randomContext;
            this.agentManagement = agentManagement;
            this.communication = communication;
        }
    }

    public static class EntityConfigBundle {
        public final List<MultiTreeMap> entityConfigs = new ArrayList<>();
        public final List<String> entityCategories = new ArrayList<>();

        public int size() {
            return entityConfigs.size();
        }
    }

    public static class LoadedEntities {
        public final List<Entity<?>> entities;
        public final List<String> entityCategories;

        LoadedEntities(List<Entity<?>> entities, List<String> entityCategories) {
            this.entities = entities;
            this.entityCategories = entityCategories;
        }
    }
}
