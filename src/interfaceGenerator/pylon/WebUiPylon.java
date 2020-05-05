package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import interfaceGenerator.types.ElementType;
import interfaceGenerator.types.PortType;
import net.xqhs.flash.core.shard.AgentShardDesignation;

public class WebUiPylon implements GUIPylonProxy {
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

    public Object generate(Element element) {
        StringBuilder result = new StringBuilder();

        if (indentLevel == 0) {
            result.append(head).append('\n');
            indentLevel++;
        }

        String type = element.getType();
        ElementType elementType = ElementType.valueOfLabel(type);
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

    private String generateButton(Element element) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("<button ");
        result.append("id = \"");
        result.append(element.getId());
        result.append("\"");

        if (element.getRole().equals(PortType.ACTIVE.type)) {
            // TODO: add callback
            result.append(" type = \"submit\" ");
            result.append("onclick=\"send_data(this.id)\"");
        }
        result.append(">\n");

        indentLevel++;
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        if (element.getValue() != null) {
            result.append(element.getValue());
        } else {
            result.append(element.getId());
        }
        --indentLevel;
        result.append('\n');
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("</button>\n");
        return result.toString();
    }

    private String generateDiv(Element element) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("<div ");
        result.append("id = \"");
        result.append(element.getId());
        result.append("\"");
        result.append(">\n");
        indentLevel++;
        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                ElementType type = ElementType.valueOfLabel(child.getType());
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
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("</div>\n");
        return result.toString();
    }

    private String generateLabel(Element element) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("<label ");
        result.append("id = \"");
        result.append(element.getId());
        result.append("\"");
        result.append(">\n");
        return result.toString();
    }

    private String generateSpinner(Element element) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("<input type = \"number\" ");
        result.append("id = ");
        result.append(element.getId());
        result.append(">\n");
        return result.toString();
    }

    private String generateForm(Element element) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            result.append(tab);
        }
        result.append("<input type = \"text\" ");
        result.append("id = ");
        result.append(element.getId());
        result.append(">\n");
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
