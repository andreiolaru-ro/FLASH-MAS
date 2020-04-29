package interfaceGenerator;

import interfaceGenerator.types.PortType;
import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;

import java.util.ArrayList;

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

    public void getActiveInput(ArrayList<Pair<String, String>> values) {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (var value : values) {
            activeInput.add(value.getKey(), value.getValue());
        }
        System.out.println(activeInput.getKeys());
        super.getAgent().postAgentEvent(activeInput);
    }

    public AgentWave getInput(String portName) {
        var elements = Element.findElementsByPort(PageBuilder.getPage(), portName);
        AgentWave event = new AgentWave();

        for (var element : elements) {
            if (element.getRole().equals(PortType.CONTENT.name())) {
                if (element.getValue() != null) {
                    event.add(element.getRole(), element.getValue());
                }
            }
        }

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        // TODO: output port
        var port = agentWave.getCompleteDestination();
        var roles = agentWave.getKeys();
        /*
        TODO
        find elements with the respective roles - a map with role and list of elements with the respective role
        for each role - fill the elements in the interface
         */
        for (var role : roles) {
            var elementsFromPort = Element.findElementsByRole(PageBuilder.getPage(), role);
            var values = agentWave.getValues(role);
            int size = Math.min(elementsFromPort.size(), values.size());
            // TODO: fill the elements
        }
    }
}
