package monitoringAndControl.gui;

import monitoringAndControl.CentralMonitoringAndControlEntity;

import java.awt.*;
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

        JButton createAgentsBtn = newButton("Create agents");
        JButton startAgentsBtn  = newButton("Start agents");
        JButton stopAgentsBtn   = newButton("Stop agents");
        JButton exitBtn         = newButton("Exit");

        createAgentsBtn.addActionListener(actionEvent ->
                centralEntityProxy.sendGUICommand("AgentC", "stop"));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(new EmptyBorder(5,5,5,5));
        buttonsPanel.setLayout(new FlowLayout());

        buttonsPanel.add(createAgentsBtn);
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
