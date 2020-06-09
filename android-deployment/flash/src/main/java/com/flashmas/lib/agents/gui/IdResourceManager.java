package com.flashmas.lib.agents.gui;

import android.view.View;
import android.widget.EditText;

import com.flashmas.lib.agents.gui.generator.Element;

import net.xqhs.flash.core.agent.AgentWave;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdResourceManager {
    private HashMap<String, List<Integer>> portMap = new HashMap<>();
    private HashMap<Integer, Element> elementMap = new HashMap<>();

    public int getNewId(Element element) {
        int id = View.generateViewId();
        addIdToPortMap(element.getPort(), id);
        elementMap.put(id, element);
        return id;
    }

    public Element getElement(Integer id) {
        if (elementMap.containsKey(id)) {
            return elementMap.get(id);
        }

        return null;
    }

    private void addIdToPortMap(String port, int id) {
        if (!portMap.containsKey(port)) {
            portMap.put(port, new LinkedList<>());
        }

        List<Integer> idsList = portMap.get(port);
        if (!idsList.contains(id)) {
            idsList.add(id);
        }
    }

    public Map<String, String> getPortValues(View view, String port) {
        HashMap<String, String> map = new HashMap<>();
        if (!portMap.containsKey(port)) {
            return map;
        }

        for (Integer id: portMap.get(port)) {
            View v = view.findViewById(id);
            if (v instanceof EditText) {
                map.put(((Element)elementMap.get(id)).getRole(), ((EditText) v).getText().toString());
            }
        }

        return map;
    }

    AgentWave buildAgentWave(View agentView, String port) {
        if (port == null) {
            return null;
        }

        AgentWave wave = new AgentWave();
        wave.addSourceElementFirst("/gui/" + port);

        Map<String, String> formMap = getPortValues(agentView, port);
        for (Map.Entry<String, String> entry: formMap.entrySet()) {
            wave.add(entry.getKey(), entry.getValue());
        }

        return wave;
    }

    public Integer getId(String port, String role) {
        if (port == null || role == null) {
            return null;
        }

        List<Integer> portIds = portMap.get(port);
        if (portIds == null) {
            return null;
        }

        for (Integer id: portIds) {
            if (elementMap.get(id).getRole().contentEquals(role)) {
                return id;
            }
        }

        return null;
    }
}
