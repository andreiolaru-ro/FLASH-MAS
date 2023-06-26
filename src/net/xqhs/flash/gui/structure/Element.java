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
	/**
	 * the default role of an element
	 */
	public static final String DEFAULT_ROLE = "content";
	public static final int		INDENT_SIZE		= 8;
	/**
	 * prefix for elements that are disabled
	 */
	public static String DISABLED_ROLE_PREFIX = "disabled-";
	
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
	 * Used to identify an element in the GUI with the role.
	 */
	private String				port;
	/**
	 * the role of the element in its port
	 * Used to identify an element in the GUI with the port.
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

	/**
	 * @return
	 * 		- the list of child elements
	 */
	public List<Element> getChildren() {
		return children;
	}

	/**
	 * Set the children of the element with the given list
	 *
	 * @param children
	 * 		- the list of child elements
	 */
	public void setChildren(List<Element> children) {
		this.children = children;
	}

	/**
	 * Add a child element to the list of children
	 *
	 * @param element
	 * 		- the child element to be added
	 */
	public void addChild(Element element) {
		this.children.add(element);
	}

	/**
	 * Add a list of child elements to the list of children
	 *
	 * @param _children
	 * 		- the list of child elements to be added
	 */
	public void addAllChildren(List<Element> _children) {
		this.children.addAll(_children);
	}

	/**
	 * @return
	 * 		- the id of the element
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the id of the element
	 *
	 * @param id
	 * 		- the id of the element
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return
	 * 		- the type of the element
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of the element
	 *
	 * @param type
	 * 		- the type of the element
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return
	 *       - the properties of the element
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Set the properties of the element
	 *
	 * @param properties
	 * 		- the properties of the element
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	/**
	 *
	 * @return
	 * 		- the port of the element
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Set the port of the element
	 *
	 * @param port
	 * 		- the port of the element
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return
	 * 		- the role of the element
	 */
	public String getRole() {
		return role;
	}

	/**
	 * Set the role of the element
	 *
	 * @param role
	 * 		- the role of the element
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return
	 * 		- the value of the element
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set the value of the element
	 *
	 * @param value
	 * 		- the value of the element
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return
	 * 		- the type of the block where the element is
	 */
	public String getBlockType() {
		return blockType;
	}

	/**
	 * Set the type of the block where the element is
	 *
	 * @param blockType
	 * 		- the type of the block where the element is
	 */
	public void setBlockType(String blockType) {
		this.blockType = blockType;
	}

	/**
	 * Return the children of the element if they match the given port
	 *
	 * @param port
	 * 		- the port to match
	 *
	 * @return
	 * 		- the list of children that match the given port
	 */
	public List<Element> getChildren(String port) {
		List<Element> result = new LinkedList<>();
		for(Element e : children)
			if((port == null && e.getPort() == null) || (port != null && port.equals(e.getPort())))
				result.add(e);
		return result;
	}

	/**
	 * Return the children of the element if they match the given port and role
	 *
	 * @param port
	 * 		- the port to match
	 * @param role
	 * 		- the role to match
	 *
	 * @return
	 * 		- the list of children that match the given port and role
	 */
	public List<Element> getChildren(String port, String role) {
		List<Element> result = new LinkedList<>();
		for(Element e : children)
			if(((port == null && e.getPort() == null) || (port != null && port.equals(e.getPort())))
					&& ((role == null && e.getRole() == null) || (role != null && role.equals(e.getRole()))))
				result.add(e);
		return result;
	}

	/**
	 * Return the child of the element if it matches the given id
	 *
	 * @param id
	 * 		- the id to match
	 *
	 * @return
	 * 		- the child that matches the given id
	 */
	public Element getChildWithId(String id) {
		for(Element e : children)
			if(id.equals(e.getId()))
				return e;
		return null;
	}

	/**
	 * Create a {@link JSONObject} from the element
	 *
	 * @return
	 *        - a {@link JSONObject} representation of the element
	 */
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
