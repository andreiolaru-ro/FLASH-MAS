package interfaceGenerator.io;

import interfaceGenerator.PageBuilder;
import net.xqhs.flash.core.util.MultiTreeMap;

public class IOShardFactory {
    public static IOShard factoryIOShard(MultiTreeMap configuration) {
        switch (PageBuilder.getInstance().platformType) {
            case WEB:
                return new IOShardWeb(configuration);
            case DESKTOP:
                return new IOShardSwing(configuration);
            default:
                return null;
        }
    }
}
