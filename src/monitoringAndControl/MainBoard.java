package monitoringAndControl;

import javax.swing.*;
import java.awt.*;

public class MainBoard {
    private JTextArea loggingArea;
    private JButton createAgents;
    private JButton startAgents;
    private JButton stopAgents;
    private JButton exitButton;
    private JPanel panel1;
    private JList agentsList;


    public void addListenersToComponents() {
        createAgents.addActionListener(actionEvent -> {
        });
        startAgents.addActionListener(actionEvent -> {
        });
        stopAgents.addActionListener(actionEvent -> JOptionPane.showMessageDialog(null, "Hello!"));
        exitButton.addActionListener(actionEvent -> JOptionPane.showMessageDialog(null, "Hello!"));
    }


    private void createAndShowGUI() {
        JFrame frame = new JFrame("Monitoring and control");
        addListenersToComponents();
        frame.setContentPane(panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        frame.setSize(width/2, height/2);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new MainBoard().createAndShowGUI());
    }

}
