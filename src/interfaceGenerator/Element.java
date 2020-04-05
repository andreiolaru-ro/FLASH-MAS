package interfaceGenerator;

import interfaceGenerator.types.ElementType;

import java.util.*;

public class Element {
    private String id;
    private List<Element> children = new ArrayList<>();
    private String type = ElementType.BLOCK.type;
    private Map<String, String> properties = new HashMap<>();
    private String text;

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

    @Override
    public String toString() {
        return "Element{" +
                "name='" + id + '\'' +
                ", children=" + children +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Element)) return false;
        Element element = (Element) o;
        return Objects.equals(id, element.id) &&
                Objects.equals(children, element.children) &&
                Objects.equals(type, element.type) &&
                Objects.equals(properties, element.properties) &&
                Objects.equals(text, element.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, children, type, properties, text);
    }
}
