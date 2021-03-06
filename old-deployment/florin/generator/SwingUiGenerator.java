package florin.generator;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import florin.PageBuilder;
import florin.Pair;
import florin.Utils;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementType;
import net.xqhs.flash.gui.structure.types.PortType;

/**
 * class for Swing UI generation
 */
public class SwingUiGenerator implements UIGenerator {
    /**
     * componentMap is a HashMap, where id is associated with tis corresponding Swing element
     */
    private static HashMap<String, Component> componentMap = new HashMap<>();
    /**
     * ids represents a HashSet with elements' ids, used for mapping Swing elements with ids
     */
    private static HashSet<String> ids = new HashSet<>();

    /**
     * @param element represents a label element
     * @return JPanel which contains a JLabel
     */
    private static JPanel generatePanel(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
        ElementType type = ElementType.valueOfLabel(element.getType());
        if (type != null) {
            ids.add(element.getId());
            switch (type) {
                case BUTTON:
                    panel = generateButton(element);
                    break;
                case OUTPUT:
                    panel = generateLabel(element);
                    break;
                case FORM:
                    panel = generateForm(element);
                    break;
                case BLOCK:
                    JPanel subPanel = new JPanel();
                    if (element.getRole() != null) {
                        if (element.getRole().equals("global")) {
                            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
                            subPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
                            subPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        }
                    } else {
                        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
                    }

                    boolean generateChildren = true;
                    if (element.getPort() != null) {
                        if (element.getPort().equals("entities")
                        ) {
                            generateChildren = false;
                        }
                    }

                    if (generateChildren) {
                        if (element.getChildren() != null) {
                            for (Element child : element.getChildren()) {
                                subPanel.add(generatePanel(child));
                            }
                        }
                    }
                    panel.add(subPanel);
                    panel.putClientProperty(element.getId(), subPanel);
                    break;
                case SPINNER:
                    panel = generateSpinner(element);
                    break;
            }
        }
        System.out.println(panel);
        return panel;
    }

    /**
     * @param element represents a button element
     * @return JPanel which contains a JButton
     */
    private static JPanel generateButton(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JButton button = new JButton();

        if (element.getValue() != null) {
            button.setText(element.getValue());
        } else {
            button.setText(element.getId());
        }

        if (element.getRole().equals(PortType.ACTIVE.type)) {
            button.addActionListener(e -> {
                System.out.println(element.getRole());

                String port = element.getPort();
                HashMap<String, ArrayList<Element>> activePorts = Utils.getActivePortsWithElements();
                ArrayList<Element> elements = activePorts.get(port);

                ArrayList<Pair<String, String>> inputIds = new ArrayList<>();
                ArrayList<Pair<String, String>> values = new ArrayList<>();

                for (Element elem : elements) {
                    if (elem.getType().equals(ElementType.SPINNER.type)
                            || elem.getType().equals(ElementType.FORM.type)) {
                        inputIds.add(new Pair<>(elem.getId(), elem.getRole()));
                    }
                }

                for (Pair<String, String> inputId : inputIds) {
                    Component component = getComponentById(inputId.getKey(), PageBuilder.window);
                    if (component instanceof JTextArea) {
                        JTextArea form = (JTextArea) component;
                        String value = form.getText();
                        values.add(new Pair<>(value, inputId.getValue()));
                    } else if (component instanceof JSpinner) {
                        JSpinner spinner = (JSpinner) component;
                        String value = spinner.getValue().toString();
                        values.add(new Pair<>(value, inputId.getValue()));
                    }
                }
                System.out.println(values);
				// try {
				// PageBuilder.getInstance().ioShard.postActiveInput(values);
				// } catch (Exception ex) {
				// ex.printStackTrace();
				// }
            });
        }

        panel.add(button);
        panel.putClientProperty(element.getId(), button);
        return panel;
    }

    /**
     * @param frame represents the Swing generated page
     *              this method maps the ids with the corresponding Swing element,
     *              which is useful for searching an element by its id
     */
    private static void mapElements(JFrame frame) {
        Container contentPane = frame.getRootPane().getContentPane();
        if (contentPane instanceof JPanel) {
            JPanel windowsPanel = (JPanel) contentPane;
            Component mainComponent = windowsPanel.getComponents()[0];

            if (mainComponent instanceof JPanel) {
                componentMap = mapElements(mainComponent);
            }
        }
    }

    /**
     * @param element represents a label element
     * @return JPanel which contains a JLabel
     */
    private static JPanel generateLabel(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel label = new JLabel();
        if (element.getValue() != null) {
            label.setText(element.getValue());
        }
        panel.add(label);
        panel.putClientProperty(element.getId(), label);
        return panel;
    }

    /**
     * @param element represents a text box element
     * @return JPanel which contains a JTextArea
     */
    private static JPanel generateForm(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JTextArea form = new JTextArea();
        if (element.getValue() != null) {
            form.setText(element.getValue());
        } else {
            form.setText("");
        }

        //form

        // hack for a fixed size of form
        form.setMaximumSize(new Dimension(100, 40));
        form.setMinimumSize(new Dimension(100, 40));

        panel.add(form);
        panel.putClientProperty(element.getId(), form);
        return panel;
    }

    /**
     * @param element represents a spinner element
     * @return JPanel which contains a JSpinner
     */
    private static JPanel generateSpinner(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JSpinner spinner = new JSpinner();
        spinner.setValue(0);
        spinner.setMaximumSize(new Dimension(100, 40));
        spinner.setMinimumSize(new Dimension(100, 40));
        panel.add(spinner);
        panel.putClientProperty(element.getId(), spinner);
        return panel;
    }

    /**
     * @param id represents the id of the element to be searched in page
     * @return the component with the respective id, if found, otherwise returns null
     */
    public static Component getComponentById(String id) {
        return getComponentById(id, PageBuilder.window);
    }

    /**
     * @param id    represents the id of the element to be searched in given page
     * @param frame represents the page where the element is searched
     * @return the component with the respective id, if found, otherwise returns null
     */
    public static Component getComponentById(String id, JFrame frame) {
        componentMap.clear();
        mapElements(frame);
        return componentMap.get(id);
    }

    private static HashMap<String, Component> mapElements(Component component) {
        HashMap<String, Component> map = new HashMap<>();
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            for (Component comp : panel.getComponents()) {
                for (String id : ids) {
                    Object obj = panel.getClientProperty(id);
                    if (obj != null) {
                        if (obj instanceof Component) {
                            map.put(id, (Component) obj);
                        }
                    }
                }
                if (comp instanceof JPanel) {
                    map.putAll(mapElements(comp));
                }
            }
        }
        return map;
    }

    private static JFrame changeValueElement(JFrame frame, String id, String value) throws ParseException {
        Container contentPane = frame.getRootPane().getContentPane();
        if (contentPane instanceof JPanel) {
            JPanel windowsPanel = (JPanel) contentPane;
            Component mainComponent = windowsPanel.getComponents()[0];
            if (mainComponent instanceof JPanel) {
                windowsPanel.remove(mainComponent);
                mainComponent = changeValueElement(mainComponent, id, value);
                windowsPanel.add(mainComponent);
            }
            JFrame temp = new JFrame();
            temp.setSize(new Dimension(600, 600));
            temp.add(windowsPanel);
            frame.dispose();
            return temp;
        }
        return frame;
    }

    public static void changeValueElement(String id, String value) throws ParseException {
        PageBuilder.window.setVisible(false);
        PageBuilder.window.dispose();
        PageBuilder.window = changeValueElement(PageBuilder.window, id, value);
        PageBuilder.window.setVisible(true);
    }

    private static Component changeValueElement(Component component, String idElement, String value) throws ParseException {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            ArrayList<Component> componentList = new ArrayList<>();
            Pair<Component, Component> pair = new Pair<>();

            for (String id : ids) {
                Object obj = panel.getClientProperty(id);
                if (obj != null) {
                    if (obj instanceof Component) {
                        if (id.equals(idElement)) {
                            if (obj instanceof JLabel) {
                                JLabel label = (JLabel) obj;
                                pair.setKey((Component) obj);
                                label.setText(value);
                                pair.setValue(label);
                                componentMap.put(id, label);
                                panel.putClientProperty(id, label);
                            }

                            /*
                            if (obj instanceof JTextArea) {
                                JTextArea form = (JTextArea) obj;
                                pair.setKey((Component) obj);
                                form.setText(value);
                                pair.setValue(form);
                                componentMap.put(id, form);
                                panel.putClientProperty(id, form);
                            } else if (obj instanceof JSpinner) {
                                JSpinner spinner = (JSpinner) obj;
                                pair.setKey((Component) obj);
                                //spinner.commitEdit();
                                //spinner.setValue(value);
                                pair.setValue(spinner);
                                componentMap.put(id, spinner);
                                panel.putClientProperty(id, spinner);
                            }
                             */
                        }
                    }
                }
            }

            for (Component comp : panel.getComponents()) {
                if (comp.equals(pair.getKey())) {
                    componentList.add(pair.getValue());
                } else {
                    if (comp instanceof JPanel) {
                        comp = changeValueElement(comp, idElement, value);
                    }
                    componentList.add(comp);
                }
            }

            for (int i = componentList.size() - 1; i >= 1; i--) {
                panel.remove(i);
            }

            for (Component comp : componentList) {
                panel.add(comp);
            }

            return panel;
        }
        return component;
    }

    public static void addEntity(Element element) {
        PageBuilder.window.setVisible(false);
        PageBuilder.window.dispose();
        PageBuilder.window = addEntity(PageBuilder.window, element);
        PageBuilder.window.setVisible(true);
    }

    public static JFrame addEntity(JFrame frame, Element element) {
        return frame;
    }

    public static void addExtendedInterface(Element element) {
        PageBuilder.window.setVisible(false);
        PageBuilder.window.dispose();
        PageBuilder.window = addExtendedInterface(PageBuilder.window, element);
        PageBuilder.window.setVisible(true);
    }

    public static JFrame addExtendedInterface(JFrame frame, Element element) {
        return frame;
    }

    public Object generate(Element element) {
        JFrame window = new JFrame();
        window.setSize(new Dimension(600, 600));
        JPanel windowPanel = new JPanel();
		// if (PageBuilder.getInstance().layoutType.equals(LayoutType.HORIZONTAL)) {
		// windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.X_AXIS));
		// } else if (PageBuilder.getInstance().layoutType.equals(LayoutType.VERTICAL)) {
		// windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));
		// }
        windowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        componentMap.put(element.getId(), windowPanel);
        ids.add(element.getId());
        if (element.getChildren() != null) {
            for (Element child : element.getChildren()) {
                JPanel panel = generatePanel(child);
                windowPanel.add(panel);
            }
        }
        window.add(windowPanel);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return window;
    }
}
