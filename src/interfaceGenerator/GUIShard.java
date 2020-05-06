package interfaceGenerator;

import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;

public abstract class GUIShard extends AgentShardCore {
    private String[] parameters = new String[2];
    private String configuration;

    protected GUIShard(AgentShardDesignation designation) {
        super(designation);
    }

    protected GUIShard() {
        super(AgentShardDesignation.autoDesignation("GUI"));
    }

    protected GUIShard(MultiTreeMap configuration) {
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

    public void getActiveInput(ArrayList<Pair<String, String>> values) throws Exception {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (Pair<String, String> value : values) {
            activeInput.add(value.getKey(), value.getValue());
        }
        super.getAgent().postAgentEvent(activeInput);
    }

    public abstract AgentWave getInput(String portName);

    public abstract void sendOutput(AgentWave agentWave);

    @Override
    public String getName() {
        return "GUIShard";
    }
}
