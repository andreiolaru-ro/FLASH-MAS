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
