package com.flashmas.lib.agents.gui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GuiLinkShard extends AgentShardCore {
    private MultiTreeMap config;
    public static final String DESIGNATION = "gui_link_shards";
    public static final String SHARD_DESIGNATIONS_KEY = "SHARD_DESIGNATIONS_KEY";

    protected GuiLinkShard(AgentShardDesignation designation) {
        super(designation);
    }

    public GuiLinkShard() {
        this(AgentShardDesignation.autoDesignation(DESIGNATION));
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        super.configure(configuration);
        config = configuration;
        return true;
    }

    public View getView(Context context) {
        if (config.containsKey(SHARD_DESIGNATIONS_KEY)) {
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            for (String designation : config.getValues(SHARD_DESIGNATIONS_KEY)) {
                AgentShard shard = getAgent()
                        .getAgentShard(AgentShardDesignation.autoDesignation(designation));
                if (shard instanceof AgentGuiElement) {
                    View v = ((AgentGuiElement) shard).getView(context);
                    ll.addView(v);
                }
            }

            return ll;
        }

        return null;
    }
}
