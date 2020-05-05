package interfaceGenerator;

import interfaceGenerator.pylon.SwingUiPylon;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

public class GUIShardSwing extends GUIShard {
    public GUIShardSwing() {
        super();
    }

    public GUIShardSwing(MultiTreeMap configuration) {
        super(configuration);
    }

    public AgentWave getInput(String portName) {
        List<Element> elements = Utils.findElementsByPort(PageBuilder.getInstance().getPage(), portName);
        AgentWave event = new AgentWave();

        for (Element element : elements) {
            Component input = SwingUiPylon.getComponentById(element.getId());
            if (input instanceof JTextArea) {
                JTextArea form = (JTextArea) input;
                String value = form.getText();
                event.add(element.getRole(), value);
            } else if (input instanceof JSpinner) {
                JSpinner spinner = (JSpinner) input;
                String value = spinner.getValue().toString();
                System.out.println(value);
                event.add(element.getRole(), value);
            }
        }

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        Set<String> roles = agentWave.getKeys();
        roles.remove("EVENT_TYPE");

        for (String role : roles) {
            List<Element> elementsFromPort = Utils.findElementsByRole(PageBuilder.getInstance().getPage(), role);

            if (elementsFromPort.size() == 0) {
                continue;
            }

            List<String> values = agentWave.getValues(role);
            int size = Math.min(elementsFromPort.size(), values.size());
            for (int i = 0; i < size; i++) {
                String elementId = elementsFromPort.get(i).getId();
                String value = values.get(i);
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
