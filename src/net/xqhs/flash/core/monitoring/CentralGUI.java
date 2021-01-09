package net.xqhs.flash.core.monitoring;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.gui.structure.Element;

/**
 * Interface for possible Central GUIs.
 * 
 * @author Andrei Olaru
 */
public abstract class CentralGUI extends GuiShard {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -8874092747023941934L;
	
	protected Map<String, Element> entityGUIs = new HashMap<>();
	
	public boolean updateGui(String entity, Element guiSpecification) {
		// lf("Update for []: ", entity, interfaceStructure);
		// lf("Update processed for []: ", entity, interfaceStructure);
		entityGUIs.put(entity, guiSpecification);
		return true;
	}
	
	@Override
	public void sendOutput(AgentWave wave) {
		// this here just to block any calls to the underlying GuiShard.
	}
}
