package interfaceGenerator;

import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

public class GuiShard extends AgentShardCore {
    protected GuiShard(AgentShardDesignation designation) {
        super(designation);
    }

    public GuiShard() {
        super(AgentShardDesignation.autoDesignation("GUI"));
    }

}
