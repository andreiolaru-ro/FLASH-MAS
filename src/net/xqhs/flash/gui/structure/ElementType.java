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
package net.xqhs.flash.gui.structure;

/**
 * Type of an interface element.
 */
public enum ElementType {
	/**
	 * A button. It has a label and can be pressed.
	 */
    BUTTON("button"),
	/**
	 * A form containing multiple fields.
	 */
    FORM("form"),
	/**
	 * A container with multiple elements.
	 */
    BLOCK("container"),
	/**
	 * A static output for one value.
	 */
    OUTPUT("label"),
	/**
	 * A field containing one value which can be incremented or decremented.
	 */
    SPINNER("spinner"),
	/**
	 * A list of elements.
	 */
    LIST("list");

	/**
	 * The textual type of the element.
	 */
    public final String type;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            - the textual type of the element.
	 */
    ElementType(String type) {
        this.type = type;
    }

	/**
	 * Retrieves the type with the given name.
	 * 
	 * @param label
	 *            - the name.
	 * @return the corresponding {@link ElementType} instance.
	 */
    public static ElementType valueOfLabel(String label) {
        for (ElementType e : values()) {
            if (e.type.equals(label)) {
                return e;
            }
        }
        return null;
    }
}
