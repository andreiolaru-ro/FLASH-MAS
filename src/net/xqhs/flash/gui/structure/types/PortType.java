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

public enum PortType {
    ACTIVE_INPUT("activate"),
    PASSIVE_INPUT("passive"),
    CONTENT("content"),
    OUTPUT("output"),
    EXTENDED_INTERFACES("extended-interfaces"),
    ENTITIES("entities"),
    START_ENTITY("start-entity"),
    STOP_ENTITY("stop-entity"),
    PAUSE("pause-entity");

    public final String type;

    PortType(String type) {
        this.type = type;
    }

    public static PortType valueOfLabel(String type) {
        for (PortType e : values()) {
            if (e.type.equals(type)) {
                return e;
            }
        }
        return null;
    }
}
