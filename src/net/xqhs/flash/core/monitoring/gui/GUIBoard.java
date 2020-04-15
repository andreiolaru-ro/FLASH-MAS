package net.xqhs.flash.core.monitoring.gui;

import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;

import java.awt.*;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GUIBoard extends JFrame {
    private JPanel contentPane;
    private CentralMonitoringAndControlEntity centralEntityProxy;

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
                BorderFactory.createLineBorder(Color.decode("#34495E"), 2),
                BorderFactory.createLineBorder(Color.decode("#41B880"), 7)));
        return btn;
    }


    public GUIBoard(CentralMonitoringAndControlEntity entity) {
        centralEntityProxy = entity;

        setTitle("Monitoring and control");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JButton startSimulation = newButton("Start simulation");
        JButton startAgentsBtn  = newButton("Start agents");
        JButton stopAgentsBtn   = newButton("Stop agents");
        JButton exitBtn         = newButton("Exit");

        startSimulation.addActionListener(actionEvent ->
                centralEntityProxy.sendToAllAgents("simulation"));
        startAgentsBtn.addActionListener(actionEvent ->
                centralEntityProxy.sendToAllAgents("start"));
        stopAgentsBtn.addActionListener(actionEvent ->
                centralEntityProxy.sendToAllAgents("stop"));
        exitBtn.addActionListener(actionEvent ->
                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(5,5,5,5));
        buttonsPanel.setLayout(new FlowLayout());

        buttonsPanel.add(startSimulation);
        buttonsPanel.add(startAgentsBtn);
        buttonsPanel.add(stopAgentsBtn);
        buttonsPanel.add(exitBtn);

        contentPane.add(buttonsPanel, BorderLayout.SOUTH);

        JTextArea textArea = new JTextArea();
        textArea.setRows(30);
        textArea.setColumns(50);

        JScrollPane scrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        setSize(width / 2, height / 2);
        setLocationRelativeTo(null);

        setVisible(true);
    }

}
