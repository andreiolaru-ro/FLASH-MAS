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

import java.util.List;

public class GlobalConfiguration {
    private String layout;
    private Element node;
    private List<Element> global;
    private List<Element> interfaces;

    /**
     * @return the node
     */
    public Element getNode() {
        return node;
    }

    /**
     * @param node the node to set
     */
    public void setNode(Element node) {
        this.node = node;
    }

    /**
     * @return the layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout the layout to set
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * @return the global
     */
    public List<Element> getGlobal() {
        return global;
    }

    /**
     * @param global the global to set
     */
    public void setGlobal(List<Element> global) {
        this.global = global;
    }

    /**
     * @return the interfaces
     */
    public List<Element> getInterfaces() {
        return interfaces;
    }

    /**
     * @param interfaces the interfaces to set
     */
    public void setInterfaces(List<Element> interfaces) {
        this.interfaces = interfaces;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "layout='" + layout + '\'' +
                ", node=" + node +
                ", global=" + global +
                ", interfaces=" + interfaces +
                '}';
    }
}
