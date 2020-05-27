package net.xqhs.flash.core.monitoring.gui;

import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity.CentralEntityProxy;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultMutableTreeNode;

public class GUIBoard extends JFrame {
    private JPanel contentPane;
    private CentralEntityProxy centralEntityProxy;
    //private ArrayList<JLabel> agents

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    GUIBoard frame = new GUIBoard(null);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public JButton newButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.decode("#41B880"));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#34495E"), 1),
                BorderFactory.createLineBorder(Color.decode("#41B880"), 5)));
        btn.setFont(new Font("TimesRoman", Font.BOLD | Font.ITALIC, 11));
        return btn;
    }

    private void createSouthButtonsArea() {
        JButton startSimulation = newButton("Start simulation");
        JButton startAgentsBtn  = newButton("Start agents");
        JButton stopAgentsBtn   = newButton("Stop agents");
        JButton pauseSimulatin  = newButton("Pause simulation");
        JButton stopSimulation  = newButton("Stop simulation");
        JButton exitBtn         = newButton("Exit");

        startSimulation.addActionListener(actionEvent ->
                centralEntityProxy.sendToAll("start_simulation"));
        startAgentsBtn.addActionListener(actionEvent ->
                centralEntityProxy.sendToAll("start"));
        stopAgentsBtn.addActionListener(actionEvent ->
                centralEntityProxy.sendToAll("stop"));
        exitBtn.addActionListener(actionEvent ->
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(5,5,5,5));
        buttonsPanel.setLayout(new FlowLayout());

        buttonsPanel.add(startSimulation);
        buttonsPanel.add(startAgentsBtn);
        buttonsPanel.add(stopAgentsBtn);
        buttonsPanel.add(pauseSimulatin);
        buttonsPanel.add(stopSimulation);
        buttonsPanel.add(exitBtn);

        contentPane.add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void createCentralLogArea() {
        JTextArea textArea = new JTextArea();
        textArea.setRows(30);
        textArea.setColumns(50);

        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createEntityPanel(String name, String status) {
        JLabel  entityLabel  = new JLabel(name);
        JLabel  statusLabel  = new JLabel(status);
        JButton start = newButton("start");
        JButton stop  = newButton("stop");
        if(name.contains("agent"))
            entityLabel.setFont(new Font("TimesRoman", Font.PLAIN, 12));
        else
            entityLabel.setFont(new Font("TimesRoman", Font.BOLD | Font.PLAIN, 12));
        statusLabel.setForeground(Color.decode("#1C7C54"));
        statusLabel.setFont(new Font("Serif", Font.PLAIN | Font.ITALIC, 12));

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        p.add(entityLabel);
        p.add(statusLabel);
        p.add(start);
        p.add(stop);
        return  p;
    }

    private void createControlPanelForEntities() {
        JPanel node1  = createEntityPanel("node1",  "RUNNING");
        JPanel node2  = createEntityPanel("node2",  "RUNNING");
        JPanel agent1 = createEntityPanel("agent1", "RUNNING");
        JPanel agent2 = createEntityPanel("agent2", "RUNNING");
        JPanel agent3 = createEntityPanel("agent3", "RUNNING");
        JPanel panel  = new JPanel();
        panel.setLayout(new GridLayout(5, 1));
        panel.add(node1);
        panel.add(agent1);
        panel.add(agent3);
        panel.add(node2);
        panel.add(agent2);
        panel.setBorder(new EmptyBorder(5,5,5,5));
        contentPane.add(panel, BorderLayout.EAST);
    }


    public GUIBoard(CentralEntityProxy entity) {
        centralEntityProxy = entity;
        setName("MainGuiBoard");

        setTitle("Monitoring and control");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        createSouthButtonsArea();
        createCentralLogArea();
        createControlPanelForEntities();

        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        setSize(width / 2, height / 2);
        setLocationRelativeTo(null);

        setVisible(false);
    }

}
