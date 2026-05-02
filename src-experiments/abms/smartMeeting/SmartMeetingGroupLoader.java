package abms.smartMeeting;

import java.util.List;
import java.util.Set;

import abms.common.GridAbmsGroupLoaderSupport;
import abms.common.GridAbmsGroupLoaderSupport.EntityConfigBundle;
import abms.common.GridAbmsGroupLoaderSupport.LoadedEntities;
import abms.common.GridAbmsGroupLoaderSupport.ResolvedGridContexts;
import net.xqhs.flash.abms.EntityGroup.EntityGroupLoader;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class SmartMeetingGroupLoader extends EntityGroupLoader {
    private static final String[] CATEGORY_NAMES = {"agent"};

    @Override
    public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
        return true;
    }

    @Override
    public SmartMeetingGroup load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
            List<MultiTreeMap> subordinateEntities) {
        ResolvedGridContexts gridContexts = GridAbmsGroupLoaderSupport.resolveGridContexts(context,
                SmartMeetingGroupLoader::getDisplayChar);
        if (gridContexts == null)
            return null;

        List<GridPosition> positions = GridAbmsGroupLoaderSupport.createShuffledPositions(gridContexts.topology,
                gridContexts.randomContext);
        EntityConfigBundle entityConfigs = GridAbmsGroupLoaderSupport.buildEntityConfigs(configuration,
                CATEGORY_NAMES, false);
        if (entityConfigs.size() > positions.size())
            return null;

        lp.lf("Loading [] SmartMeeting entities...", Integer.valueOf(entityConfigs.size()));
        LoadedEntities loadedEntities = GridAbmsGroupLoaderSupport.loadPlaceAndRegister(entityConfigs, lp, context,
                gridContexts, positions);

        SmartMeetingGroup group = new SmartMeetingGroup(loadedEntities.entities);
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
