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
            // no simulation (e.g. distributed deployment): no modeled space, agents only use the load context
            return new ResolvedGraphContexts(null, null, null,
                    (RandomContext) Loader.getClosestContext(context, RandomContext.class), null,
                    (CommunicationContext) Loader.getClosestContext(context, CommunicationContext.class));

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

    public static GridAbmsGroupLoaderSupport.EntityConfigBundle buildEntityConfigs(MultiTreeMap configuration,
            String[] categoryNames, boolean includeNestedCategories) {
        return GridAbmsGroupLoaderSupport.buildEntityConfigs(configuration, categoryNames, includeNestedCategories);
    }

    public static GridAbmsGroupLoaderSupport.LoadedEntities loadPlaceAndRegister(
            GridAbmsGroupLoaderSupport.EntityConfigBundle configs, LoadPack loadPack,
            List<EntityProxy<? extends Entity<?>>> context, ResolvedGraphContexts graphContexts,
            List<GraphPosition> positions) {
        List<Entity<?>> entities = Deployment.get().loadEntities(configs.entityConfigs, loadPack, new ArrayList<>());

        int idx = 0;
        for (Entity<?> entity : entities) {
            String category = configs.entityCategories.get(idx);
            GraphPosition position = positions == null || idx >= positions.size() ? null : positions.get(idx);
            GridAbmsGroupLoaderSupport.addContextsAndRegister(entity, category, context, graphContexts.simulation,
                    graphContexts.space == null || position == null ? null
                            : () -> graphContexts.space.place(entity.asContext(), position));
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
