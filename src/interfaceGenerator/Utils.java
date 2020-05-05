package interfaceGenerator;

import interfaceGenerator.types.ElementType;
import interfaceGenerator.types.PortType;

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

    private String repeat(String str, int count) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < count; i++) {
            res.append(res);
        }
        return res.toString();
    }
}
