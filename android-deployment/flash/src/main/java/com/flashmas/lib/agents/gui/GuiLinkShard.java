package com.flashmas.lib.agents.gui;

import com.flashmas.lib.agents.gui.generator.Element;
import com.flashmas.lib.agents.gui.generator.ElementType;

import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.LinkedList;
import java.util.List;

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

    public Element getShardsView() {
        if (config.containsKey(SHARD_DESIGNATIONS_KEY)) {
            Element container = new Element();
            container.setType(ElementType.BLOCK.type);
            List<Element> children = new LinkedList<>();

            for (String designation : config.getValues(SHARD_DESIGNATIONS_KEY)) {
                AgentShard shard = getAgent()
                        .getAgentShard(AgentShardDesignation.autoDesignation(designation));
                if (shard instanceof AgentGuiElement) {
                    Element v = ((AgentGuiElement) shard).getAgentGuiElement();
                    children.add(v);
                }
            }

            container.setChildren(children);
            return container;
        }

        return null;
    }
}
