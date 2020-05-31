package interfaceGenerator.types;

public enum LayoutType {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    public final String type;

    LayoutType(String type) {
        this.type = type;
    }

    public static LayoutType valueOfLabel(String type) {
        for (LayoutType e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
