package net.xqhs.flash.gui.structure;

import java.util.HashMap;

public class IdGenerator {
    private static HashMap<String, Integer> idCounter = new HashMap<>();

    private static String generateID(String port, String role) {
        String result = port + "_" + role + "_";
        if (idCounter.containsKey(result)) {
            int count = idCounter.get(result);
            idCounter.put(result, ++count);
            result += count;
        } else {
            idCounter.put(result, 0);
            result += 0;
        }
        return result;
    }

    public static Element attributeIds(Element element) {
        element.setId(generateID(element.getPort(), element.getRole()));
        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            for (int i = 0; i < element.getChildren().size(); i++) {
                element.getChildren().set(i, attributeIds(element.getChildren().get(i)));
            }
        }
        return element;
    }
}
