package net.xqhs.flash.gui.structure.types;

public enum DisplayType {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    public final String type;

    DisplayType(String type) {
        this.type = type;
    }

    public static DisplayType valueOfLabel(String label) {
        for (DisplayType e : values()) {
            if (e.type.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
