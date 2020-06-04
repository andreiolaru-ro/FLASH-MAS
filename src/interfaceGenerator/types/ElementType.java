package interfaceGenerator.types;

public enum ElementType {
    BUTTON("button"),
    FORM("form"),
    BLOCK("container"),
    OUTPUT("label"),
    SPINNER("spinner"),
    LIST("list");

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
