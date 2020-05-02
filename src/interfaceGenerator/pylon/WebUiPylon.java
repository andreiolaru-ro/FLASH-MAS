package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import interfaceGenerator.types.ElementType;
import interfaceGenerator.types.PortType;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.PylonProxy;

public class WebUiPylon implements PylonProxy {
    private final static String head = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <title>Input</title>\n" +
            "    <script src=\"https://code.jquery.com/jquery-3.4.1.min.js\"></script>\n" +
            "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.4.0/sockjs.js\"></script>\n" +
            "    <script src=\"js/vertx-eventbus.js\"></script>\n" +
            "    <script src=\"js/client.js\"></script>\n" +
            "</head>\n" +
            "<body onload=\"init()\">";
    private static int indentLevel = 0;
    private final static String tab = "\t";

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

        if (indentLevel == 0) {
            result.append(head).append('\n');
            indentLevel++;
        }

        var type = element.getType();
        var elementType = ElementType.valueOfLabel(type);
        if (elementType != null) {
            switch (elementType) {
                case BLOCK:
                    result.append(generateDiv(element));
                    break;
                case SPINNER:
                    result.append(generateSpinner(element));
                    break;
                case FORM:
                    result.append(generateForm(element));
                    break;
                case LABEL:
                    result.append(generateLabel(element));
                    break;
                case BUTTON:
                    result.append(generateButton(element));
                    break;
            }
        }

        if (indentLevel == 1) {
            --indentLevel;
            result.append("</body>");
        }

        return result.toString();
    }

    private static String generateButton(Element element) {
        String result = "";
        result += tab.repeat(indentLevel);
        result += "<button ";
        result += "id = \"";
        result += element.getId();
        result += "\"";

        if (element.getRole().equals(PortType.ACTIVE.type)) {
            // TODO: add callback
            result += " type = \"submit\" ";
            result += "onclick=\"send_data(this.id)\"";
        }
        result += ">\n";

        indentLevel++;
        result += tab.repeat(indentLevel);
        if (element.getValue() != null) {
            result += element.getValue();
        } else {
            result += element.getId();
        }
        --indentLevel;
        result += '\n';
        result += tab.repeat(indentLevel);
        result += "</button>\n";
        return result;
    }

    private static String generateDiv(Element element) {
        StringBuilder result = new StringBuilder();
        result.append(tab.repeat(indentLevel));
        result.append("<div ");
        result.append("id = \"");
        result.append(element.getId());
        result.append("\"");
        result.append(">\n");
        indentLevel++;
        if (element.getChildren() != null) {
            for (var child : element.getChildren()) {
                var type = ElementType.valueOfLabel(child.getType());
                if (type != null) {
                    switch (type) {
                        case BUTTON:
                            result.append(generateButton(child));
                            break;
                        case LABEL:
                            result.append(generateLabel(child));
                            break;
                        case FORM:
                            result.append(generateForm(child));
                            break;
                        case SPINNER:
                            result.append(generateSpinner(child));
                            break;
                        case BLOCK:
                            result.append(generateDiv(child));
                            break;
                    }
                }
            }
        }
        indentLevel--;
        result.append(tab.repeat(indentLevel));
        result.append("</div>\n");
        return result.toString();
    }

    private static String generateLabel(Element element) {
        String result = "";
        result += tab.repeat(indentLevel);
        result += "<label ";
        result += "id = \"";
        result += element.getId();
        result += "\"";
        result += ">\n";
        return result;
    }

    private static String generateSpinner(Element element) {
        String result = "";
        result += tab.repeat(indentLevel);
        result += "<input type = \"number\" ";
        result += "id = ";
        result += element.getId();
        result += ">\n";
        return result;
    }

    private static String generateForm(Element element) {
        String result = "";
        result += tab.repeat(indentLevel);
        result += "<input type = \"text\" ";
        result += "id = ";
        result += element.getId();
        result += ">\n";
        return result;
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
