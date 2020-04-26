package interfaceGenerator;

import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

import java.util.List;

public class GUIShard extends AgentShardCore {
    protected GUIShard(AgentShardDesignation designation) {
        super(designation);
    }

    public GUIShard() {
        super(AgentShardDesignation.autoDesignation("GUI"));
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        var guiShardConfiguration = super.getShardData().getSingleTree("config");
        var configuration = guiShardConfiguration.getTreeKeys().get(0);
        PageBuilder.guiShard = this;

        String[] parameters = new String[2];
        parameters[1] = configuration;
        if (configuration.indexOf('{') == -1) {
            parameters[0] = BuildPageTest.FILE;
        } else {
            parameters[0] = BuildPageTest.INLINE;
        }

        try {
            // System.out.println(PageBuilder.platformType);
            if (!PageBuilder.createdSwingPage && !PageBuilder.createdWebPage) {
                BuildPageTest.main(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getActiveInput(String value) {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(value, "/");
        activeInput.addSourceElementFirst("/gui/port");
        super.getAgent().postAgentEvent(activeInput);
    }

    public void getActiveInput(List<String> values) {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (var value : values) {
            activeInput.addContent(value);
        }
        super.getAgent().postAgentEvent(activeInput);
    }

    public AgentWave getInput(String portName) {
        // TODO: pasive input

        var elements = Element.findElementsByPort(PageBuilder.getPage(), portName);
        AgentWave event = new AgentWave();

        // TODO: check the pasive ports

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        // TODO: output port
        var port = agentWave.getCompleteDestination();
        var content = agentWave.getContent();
    }
}
