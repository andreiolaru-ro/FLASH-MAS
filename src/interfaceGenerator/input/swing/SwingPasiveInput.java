package interfaceGenerator.input.swing;

import interfaceGenerator.PageBuilder;
import interfaceGenerator.pylon.SwingUiPylon;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SwingPasiveInput {
    public static void main(String[] args) throws Exception {
        // creating some demo window for pasive input

        if (args.length == 0) {
            System.err.println("No file path specified");
            return;
        }

        var file = PageBuilder.buildPageFile(args[0]);

        if (file != null) {
            var page = PageBuilder.buildPage(file);
            if (page instanceof JFrame) {
                var window = (JFrame) page;

                var element1 = SwingUiPylon.getComponentById("spinner1", window);
                if (element1 instanceof JSpinner) {
                    var spinner = (JSpinner) element1;
                    var element2 = SwingUiPylon.getComponentById("child1", window);
                    if (element2 instanceof JButton) {
                        var button = (JButton) element2;
                        button.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                var value = spinner.getValue();
                                System.out.println(value);
                            }
                        });
                    }
                }
            }
        }
    }
}
