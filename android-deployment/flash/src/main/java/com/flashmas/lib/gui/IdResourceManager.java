package com.flashmas.lib.gui;

import android.view.View;
import android.widget.EditText;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IdResourceManager {
    private static HashMap<String, Integer> idMap = new HashMap<>();
    private static HashMap<Integer, String> nameMap = new HashMap<>();
    private static HashMap<String, List<Integer>> portMap = new HashMap<>();
    private static int n = 123421;  // TODO replace with unique id generator

    public static Integer addId(String name, String port) {
        int id = n;
        idMap.put(name, id);
        nameMap.put(id, name);
        addIdToPortMap(port, id);
        n++;
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

    public static Integer getId(String name) {
        if (idMap.containsKey(name)) {
            return idMap.get(name);
        }

        return null;
    }

    public static Map<String, String> getPortValues(View view, String port) {
        HashMap<String, String> map = new HashMap<>();
        if (!portMap.containsKey(port)) {
            return map;
        }
        for (Integer id: portMap.get(port)) {
            View v = view.findViewById(id);
            if (v instanceof EditText) {
                map.put(nameMap.get(id), ((EditText) v).getText().toString());
            }
        }

        return map;
    }
}
