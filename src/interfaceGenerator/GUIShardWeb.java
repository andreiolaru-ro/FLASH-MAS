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

        var elements = Element.findElementsByPort(PageBuilder.getInstance().getPage(), portName);
        AgentWave event = new AgentWave();

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

        System.out.println("passive data " + passiveDataInput);
        if (passiveDataInput != null) {
            for (var passiveData : passiveDataInput) {
                event.add(passiveData.getKey(), passiveData.getValue());
            }
        }

        return event;
    }

    public void sendOutput(AgentWave agentWave) {
        if (!Runner.connectionInit) {
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

    @Override
    public String getName() {
        return "GUIShardWeb";
    }
}
