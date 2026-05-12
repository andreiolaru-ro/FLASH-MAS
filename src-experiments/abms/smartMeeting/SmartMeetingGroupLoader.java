package abms.smartMeeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import abms.common.GraphAbmsGroupLoaderSupport;
import abms.common.GraphAbmsGroupLoaderSupport.ResolvedGraphContexts;
import abms.common.GridAbmsGroupLoaderSupport.EntityConfigBundle;
import abms.common.GridAbmsGroupLoaderSupport.LoadedEntities;
import net.xqhs.flash.abms.EntityGroup.EntityGroupLoader;
import net.xqhs.flash.abms.space.graph.GraphPosition;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.util.MultiTreeMap;

public class SmartMeetingGroupLoader extends EntityGroupLoader {
    private static final String[] GRAPH_AGENT_CATEGORIES = {"agent"};
    private static final String[] ALL_CATEGORIES = {"agent"};

    @Override
    public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
        return true;
    }

    @Override
    public SmartMeetingGroup load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
            List<MultiTreeMap> subordinateEntities) {
        ResolvedGraphContexts graphContexts = GraphAbmsGroupLoaderSupport.resolveGraphContexts(context,
                SmartMeetingGroupLoader::getDisplayChar);
        if (graphContexts == null)
            return null;

        // Build all entity configs
        EntityConfigBundle allConfigs = GraphAbmsGroupLoaderSupport.buildEntityConfigs(configuration,
                ALL_CATEGORIES, false);

        // Separate graph-placed agents (Auction, Room) from off-graph agents (Person)
        EntityConfigBundle graphConfigs = new EntityConfigBundle();
        EntityConfigBundle offGraphConfigs = new EntityConfigBundle();
        for (int i = 0; i < allConfigs.size(); i++) {
            MultiTreeMap config = allConfigs.entityConfigs.get(i);
            String category = allConfigs.entityCategories.get(i);
            String name = config.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
            if (name != null && name.toLowerCase().contains("person")) {
                offGraphConfigs.entityConfigs.add(config);
                offGraphConfigs.entityCategories.add(category);
            } else {
                graphConfigs.entityConfigs.add(config);
                graphConfigs.entityCategories.add(category);
            }
        }

        List<GraphPosition> nodePositions = GraphAbmsGroupLoaderSupport.createShuffledNodePositions(
                graphContexts.topology, graphContexts.randomContext);
        if (graphConfigs.size() > nodePositions.size()) {
            lp.le("More graph agents ([]) than nodes ([])", Integer.valueOf(graphConfigs.size()),
                    Integer.valueOf(nodePositions.size()));
            return null;
        }

        lp.lf("Loading [] graph agents + [] off-graph agents on graph topology...",
                Integer.valueOf(graphConfigs.size()), Integer.valueOf(offGraphConfigs.size()));

        // Load and place graph agents on nodes
        List<Entity<?>> allEntities = new ArrayList<>();
        if (graphConfigs.size() > 0) {
            LoadedEntities loadedGraph = GraphAbmsGroupLoaderSupport.loadPlaceAndRegister(
                    graphConfigs, lp, context, graphContexts, nodePositions);
            allEntities.addAll(loadedGraph.entities);
        }

        // Load off-graph agents (PersonAgents) — register with simulation but no spatial placement
        if (offGraphConfigs.size() > 0) {
            List<Entity<?>> offGraphEntities = Deployment.get().loadEntities(offGraphConfigs.entityConfigs, lp,
                    new ArrayList<>(context));
            for (int i = 0; i < offGraphEntities.size(); i++) {
                Entity<?> entity = offGraphEntities.get(i);
                if (graphContexts.randomContext != null)
                    entity.addGeneralContext(graphContexts.randomContext.asContext());
                if (graphContexts.communication != null)
                    entity.addGeneralContext(graphContexts.communication.asContext());
                entity.addGeneralContext(graphContexts.simulation.asContext());
                graphContexts.simulation.registerEntity("agent", entity, entity.getName());
            }
            allEntities.addAll(offGraphEntities);
        }

        SmartMeetingGroup group = new SmartMeetingGroup(allEntities);
        group.configure(configuration);
        return group;
    }

    @Override
    public SmartMeetingGroup load(MultiTreeMap configuration) {
        return load(configuration, null, null);
    }

    private static Character getDisplayChar(Set<EntityProxy<?>> entities) {
        char best = 0;
        int bestPriority = -1;
        for (EntityProxy<?> entity : entities) {
            char candidate;
            int priority;
            if (entity instanceof AuctionAgent) {
                candidate = 'A';
                priority = 2;
            } else if (entity instanceof RoomAgent) {
                RoomAgent room = (RoomAgent) entity;
                candidate = room.isOccupied() ? 'X' : 'R';
                priority = 1;
            } else {
                String name = entity.getEntityName();
                if (name == null || name.isEmpty())
                    continue;
                candidate = Character.toUpperCase(name.charAt(0));
                priority = 0;
            }
            if (priority > bestPriority) {
                bestPriority = priority;
                best = candidate;
            }
        }
        return bestPriority >= 0 ? Character.valueOf(best) : null;
    }
}
