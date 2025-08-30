package easyLog.configuration.entry.selector.level;

public class Level {
    private String type; // can be # [ERROR], > [INFO], * [WARN]

    public Level(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
