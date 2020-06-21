package interfaceGenerator.generator;

import interfaceGenerator.Element;

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
