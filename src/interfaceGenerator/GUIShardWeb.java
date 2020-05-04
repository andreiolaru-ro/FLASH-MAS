package interfaceGenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import interfaceGenerator.types.PlatformType;
import interfaceGenerator.web.Input;
import interfaceGenerator.web.Runner;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GUIShardWeb extends GUIShard {
    private static List<Pair<String, String>> passiveDataInput;

    public GUIShardWeb() {
        super();
    }

    public GUIShardWeb(MultiTreeMap configuration) {
        super(configuration);
    }

    public static void sendPassiveInputToShard(List<Pair<String, String>> dataInput) {
        passiveDataInput = dataInput;
    }

    public AgentWave getInput(String portName) {
        if (PageBuilder.getInstance().platformType.equals(PlatformType.WEB)) {
            if (!Runner.connectionInit) {
                return null;
            }
        }

        List<Element> elements = Element.findElementsByPort(PageBuilder.getInstance().getPage(), portName);
        AgentWave event = new AgentWave();

        List<String> ids = new ArrayList<>();
        for (Element element : elements) {
            ids.add(element.getId());
        }

        HashMap<String, List<String>> data = new HashMap<>();
        data.put("data", ids);

        GsonBuilder gsonMapBuilder = new GsonBuilder();
        Gson gsonObject = gsonMapBuilder.create();
        String JSONObject = gsonObject.toJson(data);

        Input.runner.getVertx().eventBus().send("server-to-client", "passive-input: " + JSONObject);

        System.out.println("passive data " + passiveDataInput);
        if (passiveDataInput != null) {
            for (Pair<String, String> passiveData : passiveDataInput) {
                event.add(passiveData.getKey(), passiveData.getValue());
            }
        }

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        if (!Runner.connectionInit) {
            return;
        }

        Set<String> roles = agentWave.getKeys();
        roles.remove("EVENT_TYPE");

        for (String role : roles) {
            List<Element> elementsFromPort = Element.findElementsByRole(PageBuilder.getInstance().getPage(), role);

            if (elementsFromPort.size() == 0) {
                continue;
            }

            List<String> values = agentWave.getValues(role);
            int size = Math.min(elementsFromPort.size(), values.size());

            HashMap<String, String> data = new HashMap<>();
            for (int i = 0; i < size; i++) {
                String elementId = elementsFromPort.get(i).getId();
                String value = values.get(i);
                data.put(elementId, value);
            }

            System.out.println(data);
            GsonBuilder gsonMapBuilder = new GsonBuilder();
            Gson gsonObject = gsonMapBuilder.create();

            String JSONObject = gsonObject.toJson(data);
            Input.runner.getVertx().eventBus().send("server-to-client", "output: " + JSONObject);
        }
    }

    @Override
    public String getName() {
        return "GUIShardWeb";
    }
}
