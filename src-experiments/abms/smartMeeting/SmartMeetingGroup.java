package abms.smartMeeting;

import java.util.List;

import net.xqhs.flash.abms.EntityGroup;
import net.xqhs.flash.core.Entity;

public class SmartMeetingGroup extends EntityGroup {
    private static final long serialVersionUID = 1L;

    public SmartMeetingGroup(List<Entity<?>> entities) {
        super(entities);
    }

    @Override
    protected void display() {
        // Grid display is handled by the SpaceContext visualization.
    }
}
