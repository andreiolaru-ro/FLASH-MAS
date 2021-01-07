package net.xqhs.flash.gui.structure.types;

public enum PlatformType {
    WEB("web"),
    ANDROID("android"),
    DESKTOP("desktop");

    public final String type;

    PlatformType(String type) {
        this.type = type;
    }

    public static PlatformType valueOfLabel(String type) {
        for (PlatformType e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
