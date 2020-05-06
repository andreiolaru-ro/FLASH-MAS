package interfaceGenerator;

import interfaceGenerator.types.ElementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element {
    private String id;
    private List<Element> children = new ArrayList<>();
    private String type = ElementType.BLOCK.type;
    private Map<String, String> properties = new HashMap<>();
    private String port;
    private String role;
    private String value;
    private static int counter = 0;

    public List<Element> getChildren() {
        return children;
    }

    public void setChildren(List<Element> children) {
        this.children = children;
    }

    public void addChild(Element element) {
        this.children.add(element);
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String tab = "\t";
        StringBuilder result = new StringBuilder();
        result.append(Utils.repeat(tab, counter));
        result.append("id: ").append(id).append('\n');
        result.append(Utils.repeat(tab, counter));
        result.append("type: ").append(type).append('\n');
        result.append(Utils.repeat(tab, counter));
        result.append("port: ").append(port).append('\n');
        result.append(Utils.repeat(tab, counter));
        result.append("role: ").append(role).append('\n');
        result.append(Utils.repeat(tab, counter));
        result.append("children: ");
        if (children != null) {
            if (children.isEmpty()) {
                result.append("[]").append('\n');
            } else {
                result.append('\n');
                ++Element.counter;
                for (Element child : children) {
                    result.append(child.toString());
                }
                --Element.counter;
            }
        }
        result.append('\n');
        return result.toString();
    }
}
