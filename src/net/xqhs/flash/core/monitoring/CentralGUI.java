package net.xqhs.flash.core.monitoring;

import net.xqhs.flash.gui.GuiShard;
import net.xqhs.flash.gui.structure.Element;

/**
 * Interface for possible Central GUIs.
 * 
 * @author Andrei Olaru
 */
public abstract class CentralGUI extends GuiShard {
	
	// TODO: move this to use Element instead of JSON.
	public abstract boolean updateGui(String entity, Element guiSpecification);
}
