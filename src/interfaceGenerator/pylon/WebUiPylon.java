package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import interfaceGenerator.types.ElementType;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.PylonProxy;

public class WebUiPylon implements PylonProxy {
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
            case SPINNER:
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
    public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
        return null;
    }

    @Override
    public String getEntityName() {
        return null;
    }
}
