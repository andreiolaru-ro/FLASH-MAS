package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.PylonProxy;

public class AndroidUiPylon implements PylonProxy {
    private final static String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static int indentLevel = 0;

    public static String generate(Element element) {
        // TODO: Android interface generator
        return null;
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
        return null;
    }


    @Override
    public String getEntityName() {
        return null;
    }
}

