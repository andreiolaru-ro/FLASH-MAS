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

    public Element getNode() {
        return node;
    }

    public void setNode(Element node) {
        this.node = node;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public List<Element> getGlobal() {
        return global;
    }

    public void setGlobal(List<Element> global) {
        this.global = global;
    }

    public List<Element> getInterfaces() {
        return interfaces;
    }

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
