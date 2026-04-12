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

/**
 * Loader for the WolfSheepGroup that uses autoFind (via {@link Deployment#loadEntities}) to discover and instantiate
 * agent classes automatically, instead of hardcoding agent creation.
 * <p>
 * Boot syntax example:
 *
 * <pre>
 * -WolfSheepGroup g -agent Sheep n:10 visionRange:2 -agent Wolf n:5 visionRange:3 -agent Grass n:15
 * </pre>
 */
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

        // Count total agents needed across all types
        int totalAgents = 0;
        List<String> agentTypes = new ArrayList<>();
        if (multiTreeMap.getTreeKeys().contains("agent")) {
            MultiTreeMap agentTree = multiTreeMap.getATree("agent");
            agentTypes = agentTree.getTreeKeys();
            for (String agentType : agentTypes)
                totalAgents += readInt(agentTree.getATree(agentType), "n", 0);
        }
        if (totalAgents > totalCells)
            return null;

        // Build entity configurations for Deployment.loadEntities() — autoFind will resolve classpaths
        List<MultiTreeMap> entityConfigs = new ArrayList<>();
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
                for (String key : typeConfig.getSimpleNames())
                    if (!"n".equals(key) && !DeploymentConfiguration.NAME_ATTRIBUTE_NAME.equals(key))
                        entityConfig.addOneValue(key, typeConfig.get(key));
                entityConfigs.add(entityConfig);
            }
        }

        lp.lf("Loading [] agents...", Integer.valueOf(entityConfigs.size()));
        List<Entity<?>> entities = Deployment.get().loadEntities(entityConfigs, lp, new ArrayList<>(context));

        int idx = 0;
        for (Entity<?> entity : entities) {
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

        WolfSheepGroup group = new WolfSheepGroup(entities);
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
                priority = (entity instanceof GrassAgent && ((GrassAgent) entity).isGrown()) ? 1 : -1;
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
