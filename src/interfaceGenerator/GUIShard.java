package interfaceGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import interfaceGenerator.pylon.SwingUiPylon;
import interfaceGenerator.types.PlatformType;
import interfaceGenerator.web.Input;
import interfaceGenerator.web.Runner;
import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        this.configuration = configuration.getSingleTree("config").getTreeKeys().get(0);
    }

    private static List<Pair<String, String>> passiveDataInput;

    public static void sendPassiveInputToShard(List<Pair<String, String>> dataInput) {
        System.out.println("data input" + dataInput);
        passiveDataInput = dataInput;
        System.out.println("passive " + passiveDataInput);
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

    public void getActiveInput(ArrayList<Pair<String, String>> values) throws Exception {
        System.out.println("Generating AgentWave for active input...");
        AgentWave activeInput = new AgentWave(null, "/");
        activeInput.addSourceElementFirst("/gui/port");
        for (var value : values) {
            activeInput.add(value.getKey(), value.getValue());
        }
        super.getAgent().postAgentEvent(activeInput);
    }

    public AgentWave getInput(String portName) throws Exception {
        if (PageBuilder.getInstance().platformType.equals(PlatformType.HTML) && !Runner.connectionInit) {
            return null;
        }

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
        } else if (PageBuilder.getInstance().platformType.equals(PlatformType.HTML)) {
            if (Runner.connectionInit) {
                List<String> ids = new ArrayList<>();

                for (var element : elements) {
                    ids.add(element.getId());
                }

                HashMap<String, List<String>> data = new HashMap<>();
                data.put("data", ids);

                GsonBuilder gsonMapBuilder = new GsonBuilder();
                Gson gsonObject = gsonMapBuilder.create();
                String JSONObject = gsonObject.toJson(data);

                Input.runner.getVertx().eventBus().send("server-to-client", "passive-input: " + JSONObject);

                // TODO: add some waiting for getting info from client? + receive data from client
                System.out.println("passive data " + passiveDataInput);
                if (passiveDataInput != null) {
                    for (var passiveData : passiveDataInput) {
                        event.add(passiveData.getKey(), passiveData.getValue());
                    }
                }
            }
        }

        return event;
    }

    public void sendOutput(AgentWave agentWave) throws ParseException {
        if (PageBuilder.getInstance().platformType.equals(PlatformType.HTML) && !Runner.connectionInit) {
            return;
        }

        var port = agentWave.getCompleteDestination();
        var roles = agentWave.getKeys();
        roles.remove("EVENT_TYPE");

        for (var role : roles) {
            var elementsFromPort = Element.findElementsByRole(PageBuilder.getInstance().getPage(), role);

            if (elementsFromPort.size() == 0) {
                continue;
            }

            var values = agentWave.getValues(role);
            int size = Math.min(elementsFromPort.size(), values.size());

            if (PageBuilder.getInstance().platformType.equals(PlatformType.DESKTOP)) {
                for (int i = 0; i < size; i++) {
                    var elementId = elementsFromPort.get(i).getId();
                    var value = values.get(i);
                    SwingUiPylon.changeValueElement(elementId, value);
                }
            } else if (PageBuilder.getInstance().platformType.equals(PlatformType.HTML)) {
                HashMap<String, String> data = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    var elementId = elementsFromPort.get(i).getId();
                    var value = values.get(i);
                    data.put(elementId, value);
                }
                System.out.println(data);
                GsonBuilder gsonMapBuilder = new GsonBuilder();
                Gson gsonObject = gsonMapBuilder.create();

                String JSONObject = gsonObject.toJson(data);
                Input.runner.getVertx().eventBus().send("server-to-client", "output: " + JSONObject);
            }
        }
    }

    @Override
    public String getName() {
        return "GUIShard";
    }
}
