package com.flashmas.lib.agents.gui.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Element {
    private Integer id;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    @Override
    public String toString() {
        return "Element{" +
                "name='" + id + '\'' +
                ", children=" + children +
                ", type='" + type + '\'' +
                ", properties=" + properties +
                ", text='" + text + '\'' +
                ", port='" + port + '\'' +
                ", role='" + role + '\'' +
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
                Objects.equals(text, element.text) &&
                Objects.equals(port, element.port) &&
                Objects.equals(role, element.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, children, type, properties, text, port, role);
    }
}