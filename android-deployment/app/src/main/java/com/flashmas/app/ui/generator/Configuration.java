package com.flashmas.app.ui.generator;


public class Configuration {
    private String platformType;
    private Element node;

    public Element getNode() {
        return node;
    }

    public void setNode(Element node) {
        this.node = node;
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }
}