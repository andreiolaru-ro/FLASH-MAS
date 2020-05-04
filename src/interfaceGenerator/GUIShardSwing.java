package interfaceGenerator;

import interfaceGenerator.pylon.SwingUiPylon;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;

import javax.swing.*;
import java.text.ParseException;

public class GUIShardSwing extends GUIShard {
    public GUIShardSwing() {
        super();
    }

    public GUIShardSwing(MultiTreeMap configuration) {
        super(configuration);
    }

    public AgentWave getInput(String portName) {
        var elements = Element.findElementsByPort(PageBuilder.getInstance().getPage(), portName);
        AgentWave event = new AgentWave();

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

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
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
            for (int i = 0; i < size; i++) {
                var elementId = elementsFromPort.get(i).getId();
                var value = values.get(i);
                try {
                    SwingUiPylon.changeValueElement(elementId, value);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "GUIShardSwing";
    }
}
