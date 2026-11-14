package abms.lbForaging;

import java.util.List;

import net.xqhs.flash.abms.EntityGroup;
import net.xqhs.flash.core.Entity;

public class LBForagingGroup extends EntityGroup {
    private static final long serialVersionUID = 1L;

    public LBForagingGroup(List<Entity<?>> entityList) {
        super(entityList);
    }

    @Override
    protected void display() {
        // Grid display is handled by GridTopology's visualization
    }
}
