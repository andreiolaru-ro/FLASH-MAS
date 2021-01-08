package net.xqhs.flash.swingGui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementIdManager;
import net.xqhs.flash.gui.structure.ElementType;

public class SwingGuiShard extends GuiShard {
	
	interface ComponentConnect {
		void sendOutput(String value);
		
		String getInput();
	}
	
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -3741974077986177703L;
	
	JFrame window = null;
	
	protected Map<String, Map<String, List<ComponentConnect>>> portRoleComponents = new HashMap<>();
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(interfaceStructure != null) {
				new ElementIdManager().insertIdsInto(interfaceStructure);
				generate(interfaceStructure, null);
				window.setVisible(true);
			}
			break;
		default:
			break;
		}
	}
	
	@Override
	public AgentWave getInput(String sourcePort) {
		// TODO check for multiple sources (e.g. remote interfaces) ?
		if(!portRoleComponents.containsKey(sourcePort)) {
			le("Input source port [] not found.", sourcePort);
			return null;
		}
		AgentWave result = new AgentWave().addSourceElementFirst(sourcePort);
		for(String role : portRoleComponents.get(sourcePort).keySet())
			for(ComponentConnect comp : portRoleComponents.get(sourcePort).get(role))
				result.add(role, comp.getInput());
		return result;
	}
	
	@Override
	public void sendOutput(AgentWave wave) {
		super.sendOutput(wave);
		String targetport = wave.getFirstDestinationElement();
		if(targetport.equals(getShardDesignation().toString()))
			targetport = wave.removeFirstDestinationElement().getFirstDestinationElement();
		
		if(!portRoleComponents.containsKey(targetport)) {
			le("Output target port [] not found for content [].", targetport, wave.getContent());
			return;
		}
		
		Map<String, List<ComponentConnect>> roleMap = portRoleComponents.get(targetport);
		
		for(String role : wave.getContentElements())
			if(roleMap.containsKey(role)) {
				List<ComponentConnect> targetList = roleMap.get(role);
				if(targetList.size() != wave.getValues(role).size())
					lw("Wave number of values for role []/[] and number of available components differ: [] / [].",
							targetport, role, Integer.valueOf(wave.getValues(role).size()),
							Integer.valueOf(targetList.size()));
				for(int i = 0; i < Math.min(targetList.size(), wave.getValues(role).size()); i++)
					targetList.get(i).sendOutput(wave.getValues(role).get(i));
			}
			else
				le("Output role []/[] cannot be found.", targetport, role);
	}
	
	protected void generate(Element element, JPanel parent) {
		Component comp = null;
		ComponentConnect connector = null;
		switch(ElementType.valueOfLabel(element.getType())) {
		case BLOCK:
			if(parent == null) { // main window
				// lf("with ids: ", element);
				
				window = new JFrame();
				window.setSize(new Dimension(600, 600));
				window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				JPanel windowPanel = new JPanel();
				// if (PageBuilder.getInstance().layoutType.equals(LayoutType.HORIZONTAL)) {
				// windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.X_AXIS));
				// } else if (PageBuilder.getInstance().layoutType.equals(LayoutType.VERTICAL)) {
				// windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));
				// }
				windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.X_AXIS));
				windowPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
				window.add(windowPanel);
				comp = windowPanel;
			}
			else {
				comp = new JPanel();
			}
			if(element.getChildren() != null) {
				for(Element child : element.getChildren()) {
					generate(child, (JPanel) comp);
				}
			}
			break;
		case FORM: {
			JTextArea form = new JTextArea();
			if(element.getValue() != null)
				form.setText(element.getValue());
			else
				form.setText("");
			// hack for a fixed size of form
			form.setMaximumSize(new Dimension(100, 40));
			form.setMinimumSize(new Dimension(100, 40));
			comp = form;
			connector = new ComponentConnect() {
				@Override
				public void sendOutput(String value) {
					form.setText(value);
				}
				
				@Override
				public String getInput() {
					return form.getText();
				}
			};
			break;
		}
		case BUTTON: {
			JButton button = new JButton();
			
			if(element.getValue() != null)
				button.setText(element.getValue());
			else
				button.setText(element.getId());
			comp = button;
			connector = new ComponentConnect() {
				@Override
				public void sendOutput(String value) {
					button.setText(value);
				}
				
				@Override
				public String getInput() {
					return button.getText();
				}
			};
			if(element.getRole().equals("activate"))
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						postActiveInput(element.getPort(), getInput(element.getPort()));
					}
				});
			break;
		}
		case OUTPUT: {
			JLabel label = new JLabel();
			if(element.getValue() != null) {
				label.setText(element.getValue());
			}
			comp = label;
			connector = new ComponentConnect() {
				@Override
				public void sendOutput(String value) {
					label.setText(value);
				}
				
				@Override
				public String getInput() {
					return label.getText();
				}
			};
			break;
		}
		default:
			break;
		}
		// System.out.println("Added " + element.getType());
		if(parent != null)
			parent.add(comp);
		if(connector != null && element.getPort() != null) {
			String port = element.getPort();
			String role = element.getRole();
			if(role == null)
				role = "content";
			if(!portRoleComponents.containsKey(port))
				portRoleComponents.put(port, new HashMap<>());
			if(!portRoleComponents.get(port).containsKey(role))
				portRoleComponents.get(port).put(role, new ArrayList<>());
			portRoleComponents.get(port).get(role).add(connector);
		}
	}
}
