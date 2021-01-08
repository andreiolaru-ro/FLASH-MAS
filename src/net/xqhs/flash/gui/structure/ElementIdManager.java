package net.xqhs.flash.gui.structure;

import java.util.HashMap;

public class ElementIdManager {
	protected HashMap<String, Integer> idCounter = new HashMap<>();

	protected String generateID(String entity, String port, String role) {
		String result = (entity != null ? entity + "_" : "") + port + "_" + role + "_";
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

	public Element insertIdsInto(Element element) {
		element.setId(generateID(null, element.getPort(), element.getRole()));
        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            for (int i = 0; i < element.getChildren().size(); i++) {
                element.getChildren().set(i, insertIdsInto(element.getChildren().get(i)));
            }
        }
        return element;
    }
	
	public Element insertIdsInto(Element element, String entity) {
		element.setId(generateID(entity, element.getPort(), element.getRole()));
		if(element.getChildren() != null && !element.getChildren().isEmpty()) {
			for(int i = 0; i < element.getChildren().size(); i++) {
				element.getChildren().set(i, insertIdsInto(element.getChildren().get(i), entity));
			}
		}
		return element;
	}
}
