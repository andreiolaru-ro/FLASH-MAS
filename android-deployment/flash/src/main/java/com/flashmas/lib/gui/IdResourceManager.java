package com.flashmas.lib.gui;

import android.view.View;
import android.widget.EditText;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdResourceManager {
    private static HashMap<Integer, String> idMap = new HashMap<>();
    private static HashMap<String, List<Integer>> portMap = new HashMap<>();

    public static int getNewId(String port) {
        int id = View.generateViewId();
        addIdToPortMap(port, id);
        return id;
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

    public static String getPort(int id) {
        if (idMap.containsKey(id)) {
            return idMap.get(id);
        }

        return null;
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
