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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Element class represents an element from GUI interface (e.g. button, label, container, text box, spinner)
 */
public class Element implements Cloneable {
	public static final String DEFAULT_ROLE = "content";
	public static final int		INDENT_SIZE		= 8;
	
	/**
	 * id of the element
	 */
	private String				id;
	/**
	 * the list of child elements (applicable to block elements)
	 */
	private List<Element>		children	= new ArrayList<>();
	private Map<String, String>	properties	= new HashMap<>();
	/**
	 * type of element
	 */
	private String				type		= ElementType.BLOCK.type;
	/**
	 * port where is the element
	 */
	private String				port;
	/**
	 * the role of the element in its port
	 */
	private String				role;
	/**
	 * value of the element (e.g. the text of a button / label)
	 */
	private String				value;
	/**
	 * the type of the block where the element is (global, agent interfaces)
	 */
	private String				blockType;

	public List<Element> getChildren() {
		return children;
	}

	public void setChildren(List<Element> children) {
		this.children = children;
	}

	public void addChild(Element element) {
		this.children.add(element);
	}

	public void addAllChildren(List<Element> _children) {
		this.children.addAll(_children);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getBlockType() {
		return blockType;
	}

	public void setBlockType(String blockType) {
		this.blockType = blockType;
	}

	public List<Element> getChildrenCopy() throws CloneNotSupportedException {
		List<Element> result = new ArrayList<>();
		for(Element e : children)
			result.add((Element) e.clone());
		return result;
	}
	public List<Element> getChildren(String port) {
		List<Element> result = new LinkedList<>();
		for(Element e : children)
			if((port == null && e.getPort() == null) || (port != null && port.equals(e.getPort())))
				result.add(e);
		return result;
	}

	public List<Element> getChildren(String port, String role) {
		List<Element> result = new LinkedList<>();
		for(Element e : children)
			if(((port == null && e.getPort() == null) || (port != null && port.equals(e.getPort())))
					&& ((role == null && e.getRole() == null) || (role != null && role.equals(e.getRole()))))
				result.add(e);
		return result;
	}

	public Element getChildWithId(String id) {
		for(Element e : children)
			if(id.equals(e.getId()))
				return e;
		return null;
	}

	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("id", id);
		result.put("type", type);
		result.put("value", value);
		result.put("port", port);
		result.put("role", role);
		JSONArray childrenArray = new JSONArray();
		result.put("children", childrenArray);
		if(children != null)
			for(Element child : children)
				childrenArray.add(child.toJSON());
		return result;
	}

	@Override
	public String toString() {
		return toString(0).toString();
	}
	
	protected StringBuilder toString(int indent) {
		StringBuilder result = new StringBuilder();
		result.append(" ".repeat(indent * INDENT_SIZE));
		result.append(type);
		if(blockType != null)
			result.append("|").append(blockType);
		result.append("#").append(id);
		result.append(" <").append(port).append("|").append(role);
		result.append("|").append(value).append("> ");
		result.append("[");
		if(children != null && !children.isEmpty())
			for(Element c : children)
				result.append("\n").append(c.toString(indent + 1));
		result.append("]  ");
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(port, role);
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(!(obj instanceof Element))
			return false;
		Element other = (Element) obj;
		return Objects.equals(port, other.port) && Objects.equals(role, other.role);
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Element result = (Element) super.clone();
		result.children = new LinkedList<Element>();
		for(Element c : children)
			result.children.add((Element) c.clone());
		result.properties = new HashMap<>(properties);
		return result;
	}
}
