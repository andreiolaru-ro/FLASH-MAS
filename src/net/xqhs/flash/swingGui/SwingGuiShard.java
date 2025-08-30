/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.swingGui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementIdManager;
import net.xqhs.flash.gui.structure.ElementType;

/**
 * Java Swing implementation of the {@link GuiShard}. It creates a window for this shard to which it adds Swing
 * components according to the specification.
 * 
 * @author andreiolaru
 */
public class SwingGuiShard extends GuiShard {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -3741974077986177703L;
	
	/**
	 * The window containing the controls.
	 */
	JFrame window = null;
	
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
	
	/**
	 * Generates the elements of the interface based on the {@link Element} specification.
	 * 
	 * @param element
	 *            - the specification.
	 * @param parent
	 *            - the parent {@link JPanel}, if this is not a top element.
	 */
	protected void generate(Element element, JPanel parent) {
		Component comp = null;
		ComponentConnect connector = null;
		switch(ElementType.valueOfLabel(element.getType())) {
		case CONTAINER:
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
				String layout = element.getProperties().getOrDefault("layout", "column");
				int layoutType = "row".equals(layout) ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
				windowPanel.setLayout(new BoxLayout(windowPanel, layoutType));
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
			if("activate".equals(element.getRole()))
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
		case SPINNER: {
			JButton incrementButton = new JButton("+");
			JButton decrementButton = new JButton("-");
			JLabel label = new JLabel();
			if (element.getValue() != null) {
				label.setText(element.getValue());
			}
			JPanel spinnerPanel = new JPanel();
			spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
			spinnerPanel.add(decrementButton);
			spinnerPanel.add(label);
			spinnerPanel.add(incrementButton);
			comp = spinnerPanel;
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
			incrementButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int value = Integer.parseInt(label.getText());
					value++;
					label.setText(Integer.toString(value));
					postActiveInput(element.getPort(), getInput(element.getPort()));
				}
			});
			decrementButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int value = Integer.parseInt(label.getText());
					value--;
					label.setText(Integer.toString(value));
					postActiveInput(element.getPort(), getInput(element.getPort()));
				}
			});
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
			if(portRoleComponents.get(port).get(role).size() != 0)
				portRoleComponents.get(port).get(role).remove(0);
			portRoleComponents.get(port).get(role).add(connector);
		}
	}
}
