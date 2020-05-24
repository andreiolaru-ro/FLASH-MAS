package interfaceGenerator.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import interfaceGenerator.PageBuilder;
import interfaceGenerator.io.IOShard;
import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.lang.reflect.Type;
import java.util.HashMap;

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
            System.err.println(super.getShardData());
            MultiTreeMap singleTree = super.getShardData().getSingleTree("config");
            if (singleTree != null) {
                configuration = singleTree.getTreeKeys().get(0);
            } else {
                // if we have reduced interface
                singleTree = super.getShardData().getSingleTree("quickMessageContent");
                // values for simple interface
                String quickMessages = singleTree.getTreeKeys().get(0);
                System.out.println(quickMessages);
                // interface specification
                configuration = singleTree.getTrees(quickMessages).get(0).getSingleTree("config").getTreeKeys().get(0);

                Gson gson = new Gson();
                Type empMapType = new TypeToken<HashMap<String, String>>() {
                }.getType();
                IOShard.reducedInterfacesValues = gson.fromJson(quickMessages, empMapType);
            }
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
