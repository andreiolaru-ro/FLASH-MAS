package com.flashmas.lib.gui.generator;

public enum ElementType {
    BUTTON("button"),
    FORM("form"),
    BLOCK("block"),
    LABEL("label"),
    SPINNER("spinner");

    public final String type;

    ElementType(String type) {
        this.type = type;
    }

    public static ElementType valueOfLabel(String label) {
        for (ElementType e : values()) {
            if (e.type.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
