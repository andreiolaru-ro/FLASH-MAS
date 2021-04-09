package florin.generator;

import net.xqhs.flash.gui.structure.Element;

/**
 * interface for automated UI generation
 */
public interface UIGenerator {
    /**
     * @param element represents the specification of elements for generated page
     * @return the object with generated page
     */
    Object generate(Element element);
}
