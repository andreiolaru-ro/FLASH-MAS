package net.xqhs.flash.swingGui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import net.xqhs.flash.core.monitoring.CentralGUI;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity.CentralEntityProxy;

// TODO: us agent events like in WebEntity

public class GUIBoard extends CentralGUI {
	protected static final long	PING_INITIAL_DELAY	= 0;
	protected static final long	PING_PERIOD			= 50;
	
	private JPanel					contentPane;
	private CentralEntityProxy		centralEntityProxy;
	private HashMap<JLabel, JLabel>	stateOfEntities	= new LinkedHashMap<>();
	private JTextArea				textArea		= null;
	private JScrollPane				scrollPane;
	Timer							pingTimer		= null;
	JFrame							frame			= null;
	
	class Pinger extends TimerTask {
		int tick = 0;
		
		@Override
		public void run() {
			// textArea.setText(FlashBoot.stream.toString());
			JScrollBar vertical = scrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());
		}
		
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUIBoard gui = new GUIBoard(null);
					gui.frame.setVisible(true);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public JButton newButton(String text) {
		JButton btn = new JButton(text);
		btn.setBackground(Color.decode("#41B880"));
		btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#34495E"), 1),
				BorderFactory.createLineBorder(Color.decode("#41B880"), 5)));
		btn.setFont(new Font("TimesRoman", Font.BOLD | Font.ITALIC, 11));
		return btn;
	}
	
	private void createSouthButtonsArea() {
		JButton startSimulation = newButton("Start simulation");
		JButton startAgentsBtn = newButton("Start agents");
		JButton stopAgentsBtn = newButton("Stop agents");
		JButton pauseSimulatin = newButton("Pause simulation");
		JButton stopSimulation = newButton("Stop simulation");
		JButton exitBtn = newButton("Exit");
		
		// TODO
		// startSimulation.addActionListener(actionEvent -> centralEntityProxy.sendToAllAgents("start_simulation"));
		// startAgentsBtn.addActionListener(actionEvent -> centralEntityProxy.sendToAllAgents("start"));
		// stopAgentsBtn.addActionListener(actionEvent -> centralEntityProxy.sendToAllAgents("stop"));
		// exitBtn.addActionListener(actionEvent -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
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
		textArea = new JTextArea();
		textArea.setRows(30);
		textArea.setColumns(50);
		
		scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		contentPane.add(scrollPane, BorderLayout.CENTER);
	}
	
	private JPanel createEntityPanel(String name, String status) {
		JLabel entityLabel = new JLabel(name);
		JLabel statusLabel = new JLabel(status);
		// JButton start = newButton("start");
		// start.addActionListener(actionEvent -> centralEntityProxy.sendToEntity(name, "start"));
		// JButton stop = newButton("stop");
		// stop.addActionListener(actionEvent -> centralEntityProxy.sendToEntity(name, "stop"));
		if(name.contains("agent"))
			entityLabel.setFont(new Font("TimesRoman", Font.PLAIN, 12));
		else
			entityLabel.setFont(new Font("TimesRoman", Font.BOLD | Font.PLAIN, 12));
		statusLabel.setForeground(Color.decode("#1C7C54"));
		statusLabel.setFont(new Font("Serif", Font.PLAIN, 12));
		stateOfEntities.put(entityLabel, statusLabel);
		
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
		p.add(entityLabel);
		p.add(statusLabel);
		// p.add(start);
		// p.add(stop);
		return p;
	}
	
	private void createControlPanelForEntities() {
		JPanel node1 = createEntityPanel("node1", "");
		JPanel node2 = createEntityPanel("node2", "");
		JPanel node3 = createEntityPanel("node3", "");
		JPanel agent1 = createEntityPanel("AgentA", "");
		JPanel agent2 = createEntityPanel("AgentB", "");
		JPanel agent3 = createEntityPanel("AgentC", "");
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(6, 1));
		panel.add(node1);
		panel.add(agent1);
		
		panel.add(node2);
		panel.add(agent2);
		
		panel.add(node3);
		panel.add(agent3);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.add(panel, BorderLayout.EAST);
	}
	
	public GUIBoard(CentralEntityProxy entity) {
		centralEntityProxy = entity;
		frame = new JFrame();
		frame.setName("MainGuiBoard");
		
		frame.setTitle("Monitoring and control");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		frame.setContentPane(contentPane);
		
		createSouthButtonsArea();
		createCentralLogArea();
		createControlPanelForEntities();
		pingTimer = new Timer();
		pingTimer.schedule(new Pinger(), PING_INITIAL_DELAY, PING_PERIOD);
		
		frame.pack();
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;
		frame.setSize(width, height);
		frame.setLocationRelativeTo(null);
		
		frame.setVisible(false);
	}
	
	public void updateStateOfEntity(String name, String status) {
		for(Map.Entry<JLabel, JLabel> entry : stateOfEntities.entrySet()) {
			if(entry.getKey().getText().equals(name)) {
				entry.getValue().setText(status);
			}
		}
	}
}
