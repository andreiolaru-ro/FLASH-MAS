package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import interfaceGenerator.types.ElementType;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Config;
import net.xqhs.util.config.Configurable;

import java.util.Set;

public class WebUiPylon implements Pylon {
    private final static String head = "<!DOCTYPE html>\n" +
            "    <html lang=\"en\">\n" +
            "    <head>\n" +
            "        <meta charset=\"UTF-8\">\n" +
            "    <title>Page</title>\n" +
            "    </head>\n" +
            "    <body>";
    private static int indentLevel = 0;

    public static String getTag(ElementType type) {
        switch (type) {
            case BLOCK:
                return "div";
            case FORM:
            case LABEL:
            case BUTTON:
                return type.type;
        }
        return null;
    }

    public static String generate(Element element) throws Exception {
        StringBuilder result = new StringBuilder();
        String tab = "\t";

        if (indentLevel == 0) {
            result.append(head).append('\n');
            indentLevel++;
        }

        var type = element.getType();
        var elementType = ElementType.valueOfLabel(type);
        if (elementType != null) {
            var tag = getTag(elementType);

            if (element.getProperties() == null || element.getProperties().isEmpty()) {
                result.append(tab.repeat(indentLevel))
                        .append('<')
                        .append(tag)
                        .append(" id = \"")
                        .append(element.getId())
                        .append("\">\n");
            } else {
                result.append(tab.repeat(indentLevel))
                        .append('<')
                        .append(tag)
                        .append(" id = \"")
                        .append(element.getId())
                        .append("\"")
                        .append(' ');
                for (var pair : element.getProperties().entrySet()) {
                    result.append(pair.getKey())
                            .append(" = \"")
                            .append(pair.getValue())
                            .append('\"');
                }
                result.append(">\n");
            }
            indentLevel++;
            for (var child : element.getChildren()) {
                result.append(generate(child)).append('\n');
            }
            indentLevel--;
            result.append(tab.repeat(indentLevel))
                    .append("</")
                    .append(tag)
                    .append(">\n");

            if (indentLevel == 1) {
                --indentLevel;
                result.append("</body>");
            }
        } else {
            throw new Exception("Invalid element type");
        }

        return result.toString();
    }

    @Override
    public Set<String> getSupportedServices() {
        return null;
    }

    @Override
    public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
        return null;
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean addContext(EntityProxy<Node> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Node> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Node>> EntityProxy<C> asContext() {
        return null;
    }

    @Override
    public Configurable makeDefaults() {
        return null;
    }

    @Override
    public Config lock() {
        return null;
    }

    @Override
    public Config build() {
        return null;
    }

    @Override
    public void ensureLocked() {

    }

    @Override
    public void locked() throws Config.ConfigLockedException {

    }
}
