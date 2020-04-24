package interfaceGenerator;

import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

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

        String[] parameters = new String[2];
        parameters[1] = configuration;
        if (configuration.indexOf('{') == -1) {
            parameters[0] = BuildPageTest.FILE;
        } else {
            parameters[0] = BuildPageTest.INLINE;
        }

        try {
            BuildPageTest.main(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // active input

        // shard context
        var context = super.getAgent();
        // System.err.println(context.getClass());

        // hashmap with ports and their elements
        var portsWithElements = Element.getPorts();
        for (var entry : portsWithElements.entrySet()) {
            var port = entry.getKey();
            var elementsList = entry.getValue();

            // TODO: AgentWave content

            AgentWave activeInput = new AgentWave("null", "/");
            activeInput.addSourceElementFirst("/gui/port");

            context.postAgentEvent(activeInput);
        }
    }

    public AgentWave getInput(String portName) {
        // TODO: pasive input
        var elements = Element.findElementsByPort(PageBuilder.getPage(), portName);
        AgentWave event = new AgentWave(null);
        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        // TODO: output port
        var port = agentWave.getCompleteDestination();
        var content = agentWave.getContent();
    }
}
