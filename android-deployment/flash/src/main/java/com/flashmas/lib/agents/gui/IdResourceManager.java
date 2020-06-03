package com.flashmas.lib.agents.gui;

import android.view.View;
import android.widget.EditText;

import com.flashmas.lib.agents.gui.generator.Element;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdResourceManager {
    private static HashMap<String, List<Integer>> portMap = new HashMap<>();
    private static HashMap<Integer, Element> elementMap = new HashMap<>();

    public static int getNewId(Element element) {
        int id = View.generateViewId();
        addIdToPortMap(element.getPort(), id);
        elementMap.put(id, element);
        return id;
    }

    public static Element getElement(Integer id) {
        if (elementMap.containsKey(id)) {
            return elementMap.get(id);
        }

        return null;
    }

    private static void addIdToPortMap(String port, int id) {
        if (!portMap.containsKey(port)) {
            portMap.put(port, new LinkedList<>());
        }

        List<Integer> idsList = portMap.get(port);
        if (!idsList.contains(id)) {
            idsList.add(id);
        }
    }

    public static Map<Integer, String> getPortValues(View view, String port) {
        HashMap<Integer, String> map = new HashMap<>();
        if (!portMap.containsKey(port)) {
            return map;
        }
        for (Integer id: portMap.get(port)) {
            View v = view.findViewById(id);
            if (v instanceof EditText) {
                map.put(id, ((EditText) v).getText().toString());
            }
        }

        return map;
    }
}
