package abms.wolfSheepPredation;

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

public class WolfSheepGroupLoader extends EntityGroupLoader {

    @Override
    public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
        return true;
    }

    @Override
    public WolfSheepGroup load(MultiTreeMap multiTreeMap, List<EntityProxy<? extends Entity<?>>> context,
                               List<MultiTreeMap> subordinateEntities) {
        if (context == null)
            return null;

        // Obtain spatial context
        @SuppressWarnings("unchecked")
        SpaceContext<GridPosition> space = (SpaceContext<GridPosition>) Loader.getClosestContext(context,
                SpaceContext.class);
        GridTopology topology = (GridTopology) space.getTopology();
        if (topology == null) {
            return null;
        }
        topology.setDisplayProvider(WolfSheepGroupLoader::getDisplayChar);

        // Obtain simulation and simulation contexts
        Simulation sim = (Simulation) Loader.getClosestContext(context, Simulation.class);

        RandomContext randomContext = null;
        AgentManagementContext agentManagement = null;
        ProximityCommunicationContext proximityCommunication = null;
        for (SimulationContext simulationContext : sim.getSimulationContexts()) {
            if (simulationContext instanceof RandomContext)
                randomContext = (RandomContext) simulationContext;
            if (simulationContext instanceof AgentManagementContext)
                agentManagement = (AgentManagementContext) simulationContext;
            if (simulationContext instanceof ProximityCommunicationContext)
                proximityCommunication = (ProximityCommunicationContext) simulationContext;
        }
        if (proximityCommunication != null)
            proximityCommunication.addGeneralContext(space.asContext());

        // Prepare shuffled grid positions
        int totalCells = topology.getWidth() * topology.getHeight();
        List<GridPosition> positions = new ArrayList<>(totalCells);
        for (int y = 0; y < topology.getHeight(); y++)
            for (int x = 0; x < topology.getWidth(); x++)
                positions.add(new GridPosition(x, y));
        randomContext.shuffle(positions);

        // Count total agents and patches needed
        int totalEntities = 0;
        List<String> agentTypes = new ArrayList<>();
        if (multiTreeMap.getTreeKeys().contains("agent")) {
            MultiTreeMap agentTree = multiTreeMap.getATree("agent");
            agentTypes = agentTree.getTreeKeys();
            for (String agentType : agentTypes)
                totalEntities += readInt(agentTree.getATree(agentType), "n", 0);
        }
        List<String> patchTypes = new ArrayList<>();
        if (multiTreeMap.getTreeKeys().contains("patch")) {
            MultiTreeMap patchTree = multiTreeMap.getATree("patch");
            patchTypes = patchTree.getTreeKeys();
            for (String patchType : patchTypes)
                totalEntities += readInt(patchTree.getATree(patchType), "n", 0);
        }
        if (totalEntities > totalCells)
            return null;

        // Build agent configurations for Deployment.loadEntities()
        List<MultiTreeMap> entityConfigs = new ArrayList<>();
        if (multiTreeMap.getTreeKeys().contains("agent")) {
            MultiTreeMap agentTree = multiTreeMap.getATree("agent");
            for (String agentType : agentTypes) {
                MultiTreeMap typeConfig = agentTree.getATree(agentType);
                int n = readInt(typeConfig, "n", 0);
                for (int i = 0; i < n; i++) {
                    MultiTreeMap entityConfig = new MultiTreeMap();
                    entityConfig.addSingleValue(DeploymentConfiguration.CATEGORY_ATTRIBUTE_NAME, "agent");
                    String id = agentType.toLowerCase() + i;
                    entityConfig.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
                            agentType + DeploymentConfiguration.NAME_SEPARATOR + id);
                    for (String key : typeConfig.getSimpleNames()) {
                        if (key.startsWith("#") || "n".equals(key)
                                || DeploymentConfiguration.NAME_ATTRIBUTE_NAME.equals(key)
                                || "package".equals(key) || "in-context-of".equals(key))
                            continue;
                        entityConfig.addOneValue(key, typeConfig.getAValue(key));
                    }
                    entityConfigs.add(entityConfig);
                }
            }
        }

        lp.lf("Loading [] agents...", Integer.valueOf(entityConfigs.size()));
        List<Entity<?>> agents = Deployment.get().loadEntities(entityConfigs, lp, new ArrayList<>(context));

        int idx = 0;
        for (Entity<?> entity : agents) {
            if (agentManagement != null)
                entity.addGeneralContext(agentManagement.asContext());
            if (randomContext != null)
                entity.addGeneralContext(randomContext.asContext());
            if (proximityCommunication != null)
                entity.addGeneralContext(proximityCommunication.asContext());
            space.place(entity.asContext(), positions.get(idx));
            sim.registerEntity("agent", entity, entity.getName());
            idx++;
        }

        // Build and place patches
        List<Entity<?>> patches = new ArrayList<>();
        if (multiTreeMap.getTreeKeys().contains("patch")) {
            MultiTreeMap patchTree = multiTreeMap.getATree("patch");
            for (String patchType : patchTypes) {
                MultiTreeMap typeConfig = patchTree.getATree(patchType);
                int n = readInt(typeConfig, "n", 0);
                for (int i = 0; i < n; i++) {
                    MultiTreeMap entityConfig = new MultiTreeMap();
                    entityConfig.addSingleValue(DeploymentConfiguration.CATEGORY_ATTRIBUTE_NAME, "patch");
                    String id = patchType.toLowerCase() + i;
                    entityConfig.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
                            patchType + DeploymentConfiguration.NAME_SEPARATOR + id);
                    for (String key : typeConfig.getSimpleNames()) {
                        if (key.startsWith("#") || "n".equals(key)
                                || DeploymentConfiguration.NAME_ATTRIBUTE_NAME.equals(key)
                                || "package".equals(key) || "in-context-of".equals(key))
                            continue;
                        entityConfig.addOneValue(key, typeConfig.getAValue(key));
                    }
                    entityConfigs.add(entityConfig);
                }
            }
            lp.lf("Loading [] patches...", Integer.valueOf(entityConfigs.size() - agents.size()));
            patches = Deployment.get().loadEntities(
                    entityConfigs.subList(agents.size(), entityConfigs.size()), lp, new ArrayList<>(context));

            for (Entity<?> patch : patches) {
                if (randomContext != null)
                    patch.addGeneralContext(randomContext.asContext());
                space.place(patch.asContext(), positions.get(idx));
                sim.registerEntity("patch", patch, patch.getName());
                idx++;
            }
        }

        List<Entity<?>> allEntities = new ArrayList<>(agents);
        allEntities.addAll(patches);

        WolfSheepGroup group = new WolfSheepGroup(allEntities);
        group.configure(multiTreeMap);
        return group;
    }

    @Override
    public WolfSheepGroup load(MultiTreeMap multiTreeMap) {
        return load(multiTreeMap, null, null);
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

    private static Character getDisplayChar(Set<EntityProxy<?>> entities) {
        char best = 0;
        int bestPriority = -1;
        for (EntityProxy<?> entity : entities) {
            String name = entity.getEntityName();
            if (name == null || name.isEmpty())
                continue;
            char first = Character.toUpperCase(name.charAt(0));
            int priority;
            if (first == 'W')
                priority = 3;
            else if (first == 'S')
                priority = 2;
            else if (first == 'G')
                priority = (entity instanceof GrassPatch && ((GrassPatch) entity).isGrown()) ? 1 : -1;
            else
                priority = 0;
            if (priority > bestPriority) {
                bestPriority = priority;
                best = first;
            }
        }
        return bestPriority >= 0 ? best : null;
    }
}
