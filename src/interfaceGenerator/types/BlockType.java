package interfaceGenerator.types;

public enum BlockType {
    GLOBAL("global"),
    INTERFACES("interfaces");

    public final String type;

    BlockType(String type) {
        this.type = type;
    }

    public static BlockType valueOfLabel(String label) {
        for (BlockType e : values()) {
            if (e.type.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
