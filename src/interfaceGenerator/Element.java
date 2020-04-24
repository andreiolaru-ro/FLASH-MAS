package interfaceGenerator;

import interfaceGenerator.types.ElementType;
import interfaceGenerator.types.PortType;

import java.util.*;

public class Element {
    private String id;
    private List<Element> children = new ArrayList<>();
    private String type = ElementType.BLOCK.type;
    private Map<String, String> properties = new HashMap<>();
    private String text;
    private String port;
    private String role;

    public List<Element> getChildren() {
        return children;
    }

    public void setChildren(List<Element> children) {
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    private static int counter = 0;
    /*
        area for active input ports
     */
    private static HashMap<String, List<Element>> activePorts = new HashMap<>();

    public static void checkActivePorts(Element element) {
        if (element.getPort() != null) {
            if (element.getRole().equals(PortType.ACTIVE.type)) {
                if (activePorts.containsKey(element.getPort())) {
                    var value = activePorts.get(element.getPort());
                    value.add(element);
                    activePorts.put(element.getPort(), value);
                } else {
                    activePorts.put(element.getPort(), new ArrayList<>(Collections.singletonList(element)));
                }
            }
        }

        if (element.getChildren() != null) {
            for (var child : element.getChildren()) {
                checkActivePorts(child);
            }
        }
    }

    public static HashMap<String, List<Element>> getActivePorts() {
        return activePorts;
    }

    public static List<Element> findElementsByPort(Element element, String portName) {
        List<Element> elements = new ArrayList<>();
        if (element.getPort() != null) {
            if (element.getPort().equals(portName)) {
                elements.add(element);
            }
        }
        if (element.getChildren() != null) {
            for (var child : element.getChildren()) {
                elements.addAll(findElementsByPort(child, portName));
            }
        }
        return elements;
    }

    @Override
    public String toString() {
        String tab = "\t";
        StringBuilder result = new StringBuilder();
        result.append(tab.repeat(counter)).append("id: ").append(id).append('\n');
        result.append(tab.repeat(counter)).append("type: ").append(type).append('\n');
        result.append(tab.repeat(counter)).append("port: ").append(port).append('\n');
        result.append(tab.repeat(counter)).append("role: ").append(role).append('\n');
        result.append(tab.repeat(counter)).append("children: ");
        if (children != null) {
            if (children.isEmpty()) {
                result.append("[]").append('\n');
            } else {
                result.append('\n');
                ++Element.counter;
                for (var child : children) {
                    result.append(child.toString());
                }
                --Element.counter;
            }
        }
        result.append('\n');
        return result.toString();
    }
}
