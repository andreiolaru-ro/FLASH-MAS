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
package net.xqhs.flash.gui.structure.types;

/**
 * The types of containers that can appear in a UI.
 */
public enum BlockType {
	/**
	 * The global interface (e.g. the whole web interface).
	 */
	GLOBAL("global"),
	/**
	 * The container for the various agent interfaces.
	 */
	INTERFACES("interfaces");
	
	/**
	 * The label of the type.
	 */
	public final String type;
	
	/**
	 * Constructor.
	 * 
	 * @param type
	 *            - the label.
	 */
	BlockType(String type) {
		this.type = type;
	}
	
	/**
	 * Get the type with the given label.
	 * 
	 * @param label
	 *            - the label
	 * @return the corresponding type.
	 */
	public static BlockType valueOfLabel(String label) {
		for(BlockType e : values()) {
			if(e.type.equals(label)) {
				return e;
			}
		}
		return null;
	}
}
