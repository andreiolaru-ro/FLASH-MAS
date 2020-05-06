package interfaceGenerator.types;

public enum ElementType {
    BUTTON("button"),
    FORM("form"),
    BLOCK("block"),
    OUTPUT("output"),
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
