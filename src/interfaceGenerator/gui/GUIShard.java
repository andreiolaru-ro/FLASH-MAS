package interfaceGenerator.gui;

import interfaceGenerator.PageBuilder;
import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

public class GUIShard extends AgentShardCore {
    private String[] parameters = new String[2];
    private String configuration;

    public GUIShard(AgentShardDesignation designation) {
        super(designation);
    }

    public GUIShard() {
        super(AgentShardDesignation.autoDesignation("GUI"));
    }

    public GUIShard(MultiTreeMap configuration) {
        this();
        this.configuration = configuration.getSingleTree("config").getTreeKeys().get(0);
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);

        if (configuration == null) {
            configuration = super.getShardData().getSingleTree("config").getTreeKeys().get(0);
        }

        PageBuilder.getInstance().guiShard = this;

        this.parameters[1] = configuration;
        if (configuration.indexOf('{') != -1) {
            this.parameters[0] = BuildPageTest.INLINE;
        } else if (configuration.endsWith(".yml") || configuration.endsWith(".yaml")) {
            this.parameters[0] = BuildPageTest.FILE;
        } else {
            System.out.println("pklfafafas");
            this.parameters[0] = BuildPageTest.CLAIM;
        }

        try {
            if (!PageBuilder.getInstance().createdSwingPage && !PageBuilder.getInstance().createdWebPage) {
                BuildPageTest.main(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "GUIShard";
    }
}
