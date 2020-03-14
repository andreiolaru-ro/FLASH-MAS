package monitoringAndControl;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainBoard {
    private JTextArea loggingAreaText;
    private JButton createAgentsButton;
    private JButton startAgentsButton;
    private JButton stopAgentsButton;
    private JButton exitButton;
    private JPanel panel;
    private JList<String> agentsList;

    private ByteArrayOutputStream out;
    private List<Node> nodes;
    private List<Agent> agents;
    private DefaultListModel<String> listModel;

    private String arg;

    public MainBoard(String arg) {
        this.arg = arg;
        listModel = new DefaultListModel<String>();
        out = new ByteArrayOutputStream();
        agentsList.setModel(listModel);

        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);
        GlobalLogWrapper.setLogStream(out);
    }


    private void addListenersToComponents() {
        createAgentsButton.addActionListener(actionEvent -> {
            String[] args = arg.split(" ");
            nodes = new NodeLoader().loadDeployment(Arrays.asList(args));
            loggingAreaText.setText(out.toString());
        });

        startAgentsButton.addActionListener(actionEvent -> {
            agents = new LinkedList<>();
            for(Node node : nodes) {
                node.start();
                agents.addAll(node.getAgents());
            }
            loggingAreaText.setText(out.toString());

            for (Agent agent : agents)
                listModel.addElement(agent.getName());
        });


        stopAgentsButton.addActionListener(actionEvent -> JOptionPane.showMessageDialog(null, "Hello!"));
        exitButton.addActionListener(actionEvent -> JOptionPane.showMessageDialog(null, "Hello!"));
    }


    public void createAndShowGUI() {
        JFrame frame = new JFrame("Monitoring and control");
        addListenersToComponents();
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        frame.setSize(width/2, height/2);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

}
