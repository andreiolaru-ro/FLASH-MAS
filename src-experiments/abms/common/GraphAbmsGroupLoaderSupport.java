package abms.common;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.abms.AgentManagementContext;
import net.xqhs.flash.abms.RandomContext;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SimulationContext;
import net.xqhs.flash.abms.communication.CommunicationContext;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.graph.GraphPosition;
import net.xqhs.flash.abms.space.graph.GraphTopology;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.util.MultiTreeMap;

public final class GraphAbmsGroupLoaderSupport {
    private GraphAbmsGroupLoaderSupport() {
    }

    @SuppressWarnings("unchecked")
    public static ResolvedGraphContexts resolveGraphContexts(List<EntityProxy<? extends Entity<?>>> context,
            GraphTopology.CellDisplayProvider displayProvider) {
        if (context == null)
            return null;

        Simulation simulation = (Simulation) Loader.getClosestContext(context, Simulation.class);
        if (simulation == null)
            return null;

        SpaceContext<GraphPosition> space = null;
        RandomContext randomContext = null;
        AgentManagementContext agentManagement = null;
        CommunicationContext communication = null;
        for (SimulationContext simulationContext : simulation.getSimulationContexts()) {
            if (simulationContext instanceof SpaceContext)
                space = (SpaceContext<GraphPosition>) simulationContext;
            if (simulationContext instanceof RandomContext)
                randomContext = (RandomContext) simulationContext;
            if (simulationContext instanceof AgentManagementContext)
                agentManagement = (AgentManagementContext) simulationContext;
            if (simulationContext instanceof CommunicationContext)
                communication = (CommunicationContext) simulationContext;
        }

        if (space == null || randomContext == null)
            return null;
        if (!(space.getTopology() instanceof GraphTopology))
            return null;
        GraphTopology topology = (GraphTopology) space.getTopology();
        topology.setDisplayProvider(displayProvider);

        if (communication != null)
            communication.addGeneralContext(space.asContext());

        return new ResolvedGraphContexts(simulation, space, topology, randomContext,
                agentManagement, communication);
    }

    public static List<GraphPosition> createShuffledNodePositions(GraphTopology topology, RandomContext randomContext) {
        List<GraphPosition> positions = new ArrayList<>(topology.getAllNodes());
        randomContext.shuffle(positions);
        return positions;
    }

    public static GridAbmsGroupLoaderSupport.EntityConfigBundle buildEntityConfigs(MultiTreeMap configuration,
            String[] categoryNames, boolean includeNestedCategories) {
        return GridAbmsGroupLoaderSupport.buildEntityConfigs(configuration, categoryNames, includeNestedCategories);
    }

    public static GridAbmsGroupLoaderSupport.LoadedEntities loadPlaceAndRegister(
            GridAbmsGroupLoaderSupport.EntityConfigBundle configs, LoadPack loadPack,
            List<EntityProxy<? extends Entity<?>>> context, ResolvedGraphContexts graphContexts,
            List<GraphPosition> positions) {
        List<Entity<?>> entities = Deployment.get().loadEntities(configs.entityConfigs, loadPack,
                new ArrayList<>(context));

        int idx = 0;
        for (Entity<?> entity : entities) {
            String category = configs.entityCategories.get(idx);
            if (graphContexts.agentManagement != null && "agent".equals(category))
                entity.addGeneralContext(graphContexts.agentManagement.asContext());
            if (graphContexts.randomContext != null)
                entity.addGeneralContext(graphContexts.randomContext.asContext());
            if (graphContexts.communication != null)
                entity.addGeneralContext(graphContexts.communication.asContext());
            entity.addGeneralContext(graphContexts.space.asContext());
            entity.addGeneralContext(graphContexts.simulation.asContext());
            graphContexts.space.place(entity.asContext(), positions.get(idx));
            graphContexts.simulation.registerEntity(category, entity, entity.getName());
            idx++;
        }

        return new GridAbmsGroupLoaderSupport.LoadedEntities(entities, configs.entityCategories);
    }

    public static class ResolvedGraphContexts {
        public final Simulation simulation;
        public final SpaceContext<GraphPosition> space;
        public final GraphTopology topology;
        public final RandomContext randomContext;
        public final AgentManagementContext agentManagement;
        public final CommunicationContext communication;

        ResolvedGraphContexts(Simulation simulation, SpaceContext<GraphPosition> space,
                GraphTopology topology, RandomContext randomContext, AgentManagementContext agentManagement,
                CommunicationContext communication) {
            this.simulation = simulation;
            this.space = space;
            this.topology = topology;
            this.randomContext = randomContext;
            this.agentManagement = agentManagement;
            this.communication = communication;
        }
    }
}
