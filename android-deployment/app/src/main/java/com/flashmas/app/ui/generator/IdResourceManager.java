package com.flashmas.app.ui.generator;

import java.util.HashMap;

public class IdResourceManager {
    private static HashMap<String, Integer> idMap = new HashMap<>();
    private static int n = 123421;

    public static Integer addId(String name) {
        int id = n;
        idMap.put(name, id);
        n++;
        return id;
    }

    public static Integer getId(String name) {
        if (idMap.containsKey(name)) {
            return idMap.get(name);
        }

        return null;
    }


}
