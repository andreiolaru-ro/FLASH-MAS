package interfaceGenerator;

import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.types.PlatformType;
import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import javax.swing.*;
import java.util.ArrayList;

public class GUIShard extends AgentShardCore {
    private String[] parameters = new String[2];
    public static boolean testing;
    private String configuration;

    protected GUIShard(AgentShardDesignation designation) {
        super(designation);
    }

    public GUIShard() {
        super(AgentShardDesignation.autoDesignation("GUI"));
    }

    public GUIShard(MultiTreeMap configuration) {
        this();
        var guiShard = configuration.getSingleTree("shard");
        var guiTrees = guiShard.getTrees("GUIShard");
        this.configuration = guiTrees.get(0).getSingleTree("config").getHierarchicalNames().get(0);
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        if (configuration == null) {
            var guiShardConfiguration = super.getShardData().getSingleTree("config");
            configuration = guiShardConfiguration.getTreeKeys().get(0);
        }

        PageBuilder.getInstance().guiShard = this;

        this.parameters[1] = configuration;
        if (configuration.indexOf('{') == -1) {
            this.parameters[0] = BuildPageTest.FILE;
        } else {
            this.parameters[0] = BuildPageTest.INLINE;
        }

        try {
            // System.out.println(PageBuilder.platformType);
            if (!PageBuilder.getInstance().createdSwingPage && !PageBuilder.getInstance().createdWebPage) {
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

    public void getActiveInput(ArrayList<Pair<String, String>> values) throws InterruptedException {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (var value : values) {
            activeInput.add(value.getKey(), value.getValue());
        }
        System.out.println(activeInput.getKeys());
        super.getAgent().postAgentEvent(activeInput);

        if (testing) {
            // TODO: add code test for passive input and output - to be deleted in final code
            for (int i = 0; i < 5; i++) {
                var port = Element.randomPort();
                if (port.isPresent()) {
                    System.out.println("Testing passive input");
                    var passiveInput = getInput(port.get());
                    var contentKeys = passiveInput.getKeys();
                    contentKeys.remove("EVENT_TYPE");
                    System.out.println(contentKeys);
                    for (var key : contentKeys) {
                        System.out.println(key + ": " + passiveInput.getValues(key));
                    }
                }

                Thread.sleep(3000);

                if (i == 4) {
                    System.out.println("Testing passive input and output finished.");
                }
            }
        }
    }

    public AgentWave getInput(String portName) {
        var elements = Element.findElementsByPort(PageBuilder.getInstance().getPage(), portName);
        AgentWave event = new AgentWave();

        if (PageBuilder.getInstance().platformType.equals(PlatformType.DESKTOP)) {
            for (var element : elements) {
                var input = SwingUiPylon.getComponentById(element.getId());
                if (input instanceof JTextArea) {
                    var form = (JTextArea) input;
                    String value = form.getText();
                    event.add(element.getRole(), value);
                } else if (input instanceof JSpinner) {
                    var spinner = (JSpinner) input;
                    var value = spinner.getValue().toString();
                    System.out.println(value);
                    event.add(element.getRole(), value);
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
            var elementsFromPort = Element.findElementsByRole(PageBuilder.getInstance().getPage(), role);
            var values = agentWave.getValues(role);
            int size = Math.min(elementsFromPort.size(), values.size());
            // TODO: fill the elements
        }
    }

    @Override
    public String getName() {
        return "GUIShard";
    }
}
