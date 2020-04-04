package monitoringAndControl;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.util.logging.LoggerSimple;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;

import java.util.List;

public class MainBoard {

    private CentralMonitoringAndControlEntity centralEntityProxy;

    private JTextArea loggingAreaText;
    private JButton createAgentsButton;
    private JButton startAgentsButton;
    private JButton stopAgentsButton;
    private JButton exitButton;
    private JPanel panel;
    private JList<String> agentsList;

    private List<Node> nodes;
    private List<Agent> agents;

    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private DefaultListModel<String> listModel = new DefaultListModel<>();

    private String arg;

    public MainBoard(CentralMonitoringAndControlEntity centralEntityProxy) {
        /*
        * TODO: ARGs should be taken from GUI input.
        * */
        this.centralEntityProxy = centralEntityProxy;
        agentsList.setModel(listModel);

        Logging.getMasterLogging().setLogLevel(LoggerSimple.Level.ALL);
        GlobalLogWrapper.setLogStream(out);
    }


    private void addListenersToComponents(JFrame frame) {
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new JMenuItem("Start"));
        popupMenu.add(new JMenuItem("Stop"));

        agentsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)
                        && !agentsList.isSelectionEmpty()
                        && agentsList.locationToIndex(me.getPoint())
                        == agentsList.getSelectedIndex()) {
                    popupMenu.show(agentsList, me.getX(), me.getY());
                }
            }
        });



        createAgentsButton.addActionListener(actionEvent -> {
             centralEntityProxy.sendGUICommand("AgentC", "stop");
        });

        startAgentsButton.addActionListener(actionEvent -> {
            /*
            * Start all nodes and store all existing agents.
            * Update the logging text area and the list of running agents.
            * */
//            agents = new LinkedList<>();
//            for(Node node : nodes) {
//                node.start();
//                agents.addAll(node.getAgents());
//            }
//            loggingAreaText.setText(out.toString());
//
//            for (Agent agent : agents)
//                listModel.addElement("[" + agent.isRunning() + "] " + agent.getName());
        });


        stopAgentsButton.addActionListener(actionEvent -> {
            /*
            * Stop all nodes.
            * Update the logging area and the list with current running agents.
            * */
//            for(Agent agent : agents)
//                agent.stop();
//            loggingAreaText.setText(out.toString());
//            listModel.removeAllElements();
        });

        exitButton.addActionListener(actionEvent -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
    }


    public void createAndShowGUI() {
        JFrame frame = new JFrame("Monitoring and control");
        addListenersToComponents(frame);
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
