package florin;

import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementType;
import net.xqhs.flash.gui.structure.types.BlockType;
import net.xqhs.flash.gui.structure.types.PortType;
import net.xqhs.flash.sclaim.constructs.*;

import java.util.*;

public class Utils {
    private static int counter = 0;
    private static HashMap<String, ArrayList<Element>> activePortsWithElements = new HashMap<>();
    private static Set<String> activePorts = new HashSet<>();
    private static Set<String> nonActivePorts = new HashSet<>();
    private static Set<String> ports = new HashSet<>();
    private static boolean checkedActivePorts = false;

    public static void checkActivePorts(Element element) {
        if (element.getPort() != null) {
            ports.add(element.getPort());
            if (element.getRole() != null && element.getRole().equals(PortType.ACTIVE.type)) {
                activePorts.add(element.getPort());
            }
        }

        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                checkActivePorts(child);
            }
        }
    }

    public static void checkActivePortsWithElement(Element element) {
        if (!checkedActivePorts) {
            checkActivePorts(element);
            checkedActivePorts = true;

            // checking for non-active ports
            for (String port : ports) {
                if (!activePorts.contains(port)) {
                    nonActivePorts.add(port);
                }
            }
        }

        if (element.getPort() != null) {
            if (activePorts.contains(element.getPort())) {
                if (activePortsWithElements.containsKey(element.getPort())) {
                    ArrayList<Element> value = activePortsWithElements.get(element.getPort());
                    value.add(element);
                    activePortsWithElements.put(element.getPort(), value);
                } else {
                    activePorts.add(element.getPort());
                    activePortsWithElements.put(element.getPort(), new ArrayList<>(Collections.singletonList(element)));
                }
            }
        }

        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                checkActivePortsWithElement(child);
            }
        }
    }

    public static HashMap<String, ArrayList<Element>> getActivePortsWithElements() {
        return activePortsWithElements;
    }

    public static String identifyActivePortOfElement(String id) {
        System.out.println(id);
        System.out.println(activePortsWithElements);
        for (Map.Entry<String, ArrayList<Element>> entry : activePortsWithElements.entrySet()) {
            String port = entry.getKey();
            for (Element element : entry.getValue()) {
                if (id.equals(element.getId())) {
                    return port;
                }
            }
        }
        return null;
    }

    public static String findActiveInputIdFromPort(String port) {
        for (Map.Entry<String, ArrayList<Element>> entry : activePortsWithElements.entrySet()) {
            if (port.equals(entry.getKey())) {
                for (Element element : entry.getValue()) {
                    if (element.getType().equals(ElementType.FORM.type)
                            || element.getType().equals(ElementType.SPINNER.type)) {
                        return element.getId();
                    }
                }
            }
        }
        return null;
    }

    public static List<Element> findElementsByPort(Element element, String portName) {
        List<Element> elements = new ArrayList<>();
        if (element.getPort() != null) {
            if (element.getPort().equals(portName)) {
                elements.add(element);
            }
        }
        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                elements.addAll(findElementsByPort(child, portName));
            }
        }
        return elements;
    }

    public static List<String> findActiveInputIdsFromPort(String port) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Element>> entry : activePortsWithElements.entrySet()) {
            if (port.equals(entry.getKey())) {
                for (Element element : entry.getValue()) {
                    if (element.getType().equals(ElementType.FORM.type)
                            || element.getType().equals(ElementType.SPINNER.type)) {
                        list.add(element.getId());
                    }
                }
            }
        }
        return list;
    }

    public static List<Element> findElementsByRole(Element element, String role) {
        List<Element> elements = new ArrayList<>();

        if (element.getRole() != null && element.getRole().equals(role)) {
            elements.add(element);
        }

        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                elements.addAll(findElementsByRole(child, role));
            }
        }

        return elements;
    }

    private static String findRoleOfElementById(Element element, String id) {
        if (element.getId().equals(id)) {
            return element.getRole();
        }

        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                String port = findRoleOfElementById(child, id);
                if (port != null) {
                    return port;
                }
            }
        }

        return null;
    }

    public static String findRoleOfElementById(String id) {
        return findRoleOfElementById(PageBuilder.getInstance().getPage(), id);
    }

    public static Optional<String> randomPort() {
        return nonActivePorts.stream()
                .skip((int) (nonActivePorts.size() * Math.random()))
                .findFirst();
    }

    public static String repeat(String str, int count) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < count; i++) {
            res.append(str);
        }
        return res.toString();
    }

    public static Element convertClaimAgentDefinitionToElement(ClaimAgentDefinition claimAgentDefinition) {
        // TODO: convert S-CLAIM (input and output) to Element
        Element element = new Element();
        for (ClaimBehaviorDefinition def : claimAgentDefinition.getBehaviors()) {
            // System.out.println(def);
            Vector<ClaimConstruct> statements = def.getStatements();
            for (ClaimConstruct statement : statements) {
                if (statement instanceof ClaimFunctionCall) {
                    ClaimFunctionCall functionCall = (ClaimFunctionCall) statement;
                    Vector<ClaimConstruct> arguments = functionCall.getArguments();
                    if (functionCall.getFunctionType().equals(ClaimFunctionType.INPUT)) {
                        Element button = new Element();
                        button.setType(ElementType.BUTTON.type);
                        String port = null, role = null;

                        for (ClaimConstruct arg : arguments) {
                            // System.out.println(arg.getClass() + " " + arg);
                            if (arg instanceof ClaimValue) {
                                ClaimValue claimValue = (ClaimValue) arg;
                                String value = claimValue.getValue().toString();
                                // System.out.println(value);
                                if (value.charAt(0) == '@') {
                                    // port
                                    port = value.replace("@", "");
                                    button.setPort(port);
                                    button.setRole("activate");
                                } else {
                                    role = value.replace("/", "");
                                }
                            } else if (arg instanceof ClaimVariable) {
                                ClaimVariable claimVariable = (ClaimVariable) arg;
                                String variable = claimVariable.getName();
                                // System.out.println(variable);
                                Element elem = new Element();
                                elem.setType(ElementType.FORM.type);
                                if (role != null) {
                                    elem.setRole(role);
                                    role = null;
                                } else {
                                    elem.setRole("content");
                                }
                                elem.setPort(port);
                                elem.setValue(variable);
                                element.addChild(elem);
                            }
                        }
                        element.addChild(button);
                    } else if (functionCall.getFunctionType().equals(ClaimFunctionType.OUTPUT)) {
                        String port = null, role = null;
                        for (ClaimConstruct arg : arguments) {
                            System.out.println(arg.getClass() + " " + arg);
                            if (arg instanceof ClaimValue) {
                                ClaimValue claimValue = (ClaimValue) arg;
                                String value = claimValue.getValue().toString();
                                System.err.println(value);
                                if (value.charAt(0) == '@') {
                                    port = value.replace("@", "");
                                } else {
                                    role = value.replace("/", "");
                                }

                                System.err.println(port + " " + role);
                            } else if (arg instanceof ClaimVariable) {
                                ClaimVariable claimVariable = (ClaimVariable) arg;
                                String variable = claimVariable.getName();
                                Element elem = new Element();
                                elem.setType(ElementType.OUTPUT.type);
                                if (role != null) {
                                    elem.setRole(role);
                                }
                                if (port != null) {
                                    elem.setPort(port);
                                }
                                elem.setValue(variable);
                                element.addChild(elem);
                            }
                        }
                    }
                }
            }
        }
        System.out.println(element);
        return element;
    }

    public static Element attributeBlockType(Element element, BlockType type) {
        element.setBlockType(type.type);
        if (element.getChildren() != null) {
            List<Element> children = new ArrayList<>();
            for (Element child : element.getChildren()) {
                children.add(attributeBlockType(child, type));
            }
            element.setChildren(children);
        }
        return element;
    }
}
