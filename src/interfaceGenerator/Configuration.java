package interfaceGenerator;

import java.util.List;

public class Configuration {
    private String layout;
    private Element node;
    private List<Element> global;
    private List<Element> interfaces;

    public Element getNode() {
        return node;
    }

    public void setNode(Element node) {
        this.node = node;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public List<Element> getGlobal() {
        return global;
    }

    public void setGlobal(List<Element> global) {
        this.global = global;
    }

    public List<Element> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<Element> interfaces) {
        this.interfaces = interfaces;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "layout='" + layout + '\'' +
                ", node=" + node +
                ", global=" + global +
                ", interfaces=" + interfaces +
                '}';
    }
}