package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import net.xqhs.flash.core.support.PylonProxy;

public interface GUIPylonProxy extends PylonProxy {
    Object generate(Element element);
}
