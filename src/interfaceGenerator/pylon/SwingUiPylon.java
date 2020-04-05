package interfaceGenerator.pylon;

import interfaceGenerator.Element;
import interfaceGenerator.types.ElementType;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class SwingUiPylon {
    public static HashMap<String, Component> componentMap = new HashMap<>();
    private static HashSet<String> ids = new HashSet<>();

    public static JFrame generateWindow(Element element) {
        JFrame window = new JFrame();
        window.setSize(new Dimension(600, 600));
        JPanel windowPanel = new JPanel();
        windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));
        componentMap.put(element.getId(), windowPanel);
        ids.add(element.getId());
        if (element.getChildren() != null) {
            for (var child : element.getChildren()) {
                var panel = generate(child);
                windowPanel.add(panel);
            }
        }
        window.add(windowPanel);
        return window;
    }

    private static JPanel generate(Element element) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        var type = ElementType.valueOfLabel(element.getType());
        if (type != null) {
            ids.add(element.getId());
            switch (type) {
                case BUTTON:
                    JButton button = new JButton();
                    if (element.getText() != null) {
                        button.setText(element.getText());
                    } else {
                        button.setText(element.getId());
                    }
                    panel.add(button);
                    panel.putClientProperty(element.getId(), button);
                    break;
                case LABEL:
                    JLabel label = new JLabel();
                    if (element.getText() != null) {
                        label.setText(element.getText());
                    }
                    panel.add(label);
                    panel.putClientProperty(element.getId(), label);
                    break;
                case FORM:
                    JTextArea form = new JTextArea();
                    if (element.getText() != null) {
                        form.setText(element.getText());
                    }

                    // hack for a fixed size of form
                    form.setMaximumSize(new Dimension(100, 40));
                    form.setMinimumSize(new Dimension(100, 40));

                    panel.add(form);
                    panel.putClientProperty(element.getId(), form);
                    break;
                case BLOCK:
                    JPanel subPanel = new JPanel();
                    subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
                    if (element.getChildren() != null) {
                        for (var child : element.getChildren()) {
                            subPanel.add(generate(child));
                        }
                    }
                    panel.add(subPanel);
                    panel.putClientProperty(element.getId(), subPanel);
                    break;
                case SPINNER:
                    JSpinner spinner = new JSpinner();
                    spinner.setValue(0);
                    spinner.setMaximumSize(new Dimension(100, 40));
                    spinner.setMinimumSize(new Dimension(100, 40));
                    panel.add(spinner);
                    panel.putClientProperty(element.getId(), spinner);
                    break;
            }
        }
        return panel;
    }

    public static Component getComponentById(String id, JFrame frame) {
        componentMap.clear();
        mapElements(frame);
        return componentMap.get(id);
    }

    public static void mapElements(JFrame frame) {
        var contentPane = frame.getRootPane().getContentPane();
        if (contentPane instanceof JPanel) {
            var windowsPanel = (JPanel) contentPane;
            var mainComponent = windowsPanel.getComponents()[0];

            if (mainComponent instanceof JPanel) {
                var mainPanel = (JPanel) mainComponent;
                componentMap = mapElements(mainComponent);
            }
        }
    }

    private static HashMap<String, Component> mapElements(Component component) {
        HashMap<String, Component> map = new HashMap<>();
        if (component instanceof JPanel) {
            var panel = (JPanel) component;
            for (var comp : panel.getComponents()) {
                for (var id : ids) {
                    var obj = panel.getClientProperty(id);
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
}
