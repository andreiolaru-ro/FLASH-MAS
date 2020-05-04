package interfaceGenerator;

import net.xqhs.flash.core.util.MultiTreeMap;

public class GUIShardFactory {
    public static GUIShard factoryGuiShard() {
        switch (PageBuilder.getInstance().platformType) {
            case WEB:
                return new GUIShardWeb();
            case DESKTOP:
                return new GUIShardSwing();
            default:
                return null;
        }
    }

    public static GUIShard factoryGuiShard(MultiTreeMap configuration) {
        switch (PageBuilder.getInstance().platformType) {
            case WEB:
                return new GUIShardWeb(configuration);
            case DESKTOP:
                return new GUIShardSwing(configuration);
            default:
                return null;
        }
    }
}
