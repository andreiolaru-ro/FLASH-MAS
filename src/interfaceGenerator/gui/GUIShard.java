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
        // this.configuration = configuration.getSingleTree("config").getTreeKeys().get(0);
        MultiTreeMap singleTree = configuration.getSingleTree("config");
        if (singleTree != null) {
            this.configuration = singleTree.getTreeKeys().get(0);
        } else {
            // if we have reduced interface
            singleTree = configuration.getSingleTree("quickMessageContent");
            // values for simple interface
            String quickMessages = singleTree.getTreeKeys().get(0);
            // System.out.println("content: " + quickMessages);
            // interface specification
            // System.out.println(singleTree.getTrees(quickMessages).get(0).getSingleTree("favoriteAgent").getTreeKeys().get(0));
            String favoriteAgent = singleTree.getTrees(quickMessages)
                    .get(0)
                    .getSingleTree("favoriteAgent")
                    .getTreeKeys()
                    .get(0);
            this.configuration = singleTree.getTrees(quickMessages)
                    .get(0)
                    .getSingleTree("favoriteAgent")
                    .getTrees(favoriteAgent)
                    .get(0)
                    .getSingleTree("config")
                    .getTreeKeys()
                    .get(0);
                /*
                Gson gson = new Gson();
                Type empMapType = new TypeToken<HashMap<String, String>>() {
                }.getType();
                IOShard.reducedInterfacesValues = gson.fromJson(quickMessages, empMapType);
                 */
        }
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
                // System.out.println("content: " + quickMessages);
                // interface specification
                // System.out.println(singleTree.getTrees(quickMessages).get(0).getSingleTree("favoriteAgent").getTreeKeys().get(0));
                String favoriteAgent = singleTree.getTrees(quickMessages)
                        .get(0)
                        .getSingleTree("favoriteAgent")
                        .getTreeKeys()
                        .get(0);
                configuration = singleTree.getTrees(quickMessages)
                        .get(0)
                        .getSingleTree("favoriteAgent")
                        .getTrees(favoriteAgent)
                        .get(0)
                        .getSingleTree("config")
                        .getTreeKeys()
                        .get(0);
                /*
                Gson gson = new Gson();
                Type empMapType = new TypeToken<HashMap<String, String>>() {
                }.getType();
                IOShard.reducedInterfacesValues = gson.fromJson(quickMessages, empMapType);
                 */
            }
        }

        PageBuilder.getInstance().guiShard = this;

        this.parameters[1] = configuration;
        System.out.println(configuration);
        if (configuration.indexOf('{') != -1) {
            this.parameters[0] = BuildPageTest.INLINE;
        } else if (configuration.endsWith(".yml") || configuration.endsWith(".yaml")) {
            this.parameters[0] = BuildPageTest.FILE;
        } else {
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
