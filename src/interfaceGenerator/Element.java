package interfaceGenerator;

import interfaceGenerator.types.ElementType;
import interfaceGenerator.types.PortType;

import java.util.*;

public class Element {
    private String id;
    private List<Element> children = new ArrayList<>();
    private String type = ElementType.BLOCK.type;
    private Map<String, String> properties = new HashMap<>();
    private String port;
    private String role;

    public List<Element> getChildren() {
        return children;
    }

    public void setChildren(List<Element> children) {
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    private static int counter = 0;
    private static HashMap<String, ArrayList<Element>> activePortsWithElements = new HashMap<>();
    private static Set<String> activePorts = new HashSet<>();
    private static Set<String> nonActivePorts = new HashSet<>();
    private static Set<String> ports = new HashSet<>();
    private static boolean checkedActivePorts = false;
    private String value;

    public static void checkActivePorts(Element element) {
        if (element.getPort() != null) {
            ports.add(element.getPort());
            if (element.getRole().equals(PortType.ACTIVE.type)) {
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    private String repeat(String str, int count) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < count; i++) {
            res.append(res);
        }
        return res.toString();
    }

    @Override
    public String toString() {
        String tab = "\t";
        StringBuilder result = new StringBuilder();
        result.append(repeat(tab, counter));
        result.append("id: ").append(id).append('\n');
        result.append(repeat(tab, counter));
        result.append("type: ").append(type).append('\n');
        result.append(repeat(tab, counter));
        result.append("port: ").append(port).append('\n');
        result.append(repeat(tab, counter));
        result.append("role: ").append(role).append('\n');
        result.append(repeat(tab, counter));
        result.append("children: ");
        if (children != null) {
            if (children.isEmpty()) {
                result.append("[]").append('\n');
            } else {
                result.append('\n');
                ++Element.counter;
                for (Element child : children) {
                    result.append(child.toString());
                }
                --Element.counter;
            }
        }
        result.append('\n');
        return result.toString();
    }

    public static String findRoleOfElementById(String id) {
        return findRoleOfElementById(PageBuilder.getInstance().getPage(), id);
    }

    public static Optional<String> randomPort() {
        return nonActivePorts.stream()
                .skip((int) (nonActivePorts.size() * Math.random()))
                .findFirst();
    }
}
