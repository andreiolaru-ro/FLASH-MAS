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

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * Element class represents the structure of an element from GUI interface (e.g. whole interface, container (block),
 * button, label, text box, spinner, etc).
 * 
 * @author Florin Mihalache
 * @author andreiolaru
 * @author Valentin Mignot
 */
public class Element implements Cloneable {
	/**
	 * the default role of an element
	 */
	public static final String	DEFAULT_ROLE	= AgentWave.CONTENT;
	/**
	 * The default notify endpoint for an element.
	 */
	public static final String	DEFAULT_NOTIFY	= "$port/$role";
	/**
	 * Intent size to use when producing a {@link String} rendition of the structure.
	 */
	public static final int		INDENT_SIZE		= 8;
	
	/**
	 * The Gson instance used for conversions to/from JSON.
	 */
	public static final Gson gson = new Gson();
	
	/**
	 * ID of this element
	 */
	protected String					id;
	/**
	 * The list of child elements (applicable to block elements).
	 */
	protected List<Element>				children	= new ArrayList<>();
	/**
	 * The properties of this element (could affect rendering the element).
	 */
	protected HashMap<String, String>	properties	= new HashMap<>();
	/**
	 * Type of the element.
	 */
	protected String					type		= ElementType.CONTAINER.type;
	/**
	 * The port that this element is part of.
	 */
	protected String					port;
	/**
	 * The role with which this element is associated.
	 */
	protected String					role		= DEFAULT_ROLE;
	/**
	 * Value of the element (e.g. the text of a button / label).
	 */
	protected String					value;
	/**
	 * The endpoint of the shard that added this element and will receive the events from it
	 */
	protected String					notify		= DEFAULT_NOTIFY;
	
	/**
	 * Represents a style that the element can have
	 */
	protected static class ElementWhen {
		/** The list of conditions that must be met for the style to be applied. */
		protected HashMap<String, String> conditions = new HashMap<>();
		
		/** The style to apply to the element when the condition is met. */
		protected String style;
		
		/**
		 * A condition is a key-value pair where the key denotes the element whose value is used to determine the style
		 * of this element. The keys are in one of the following formats: port/role, /role, port. The second format is
		 * relative to the current element's port. The last of these formats is synonymous with the first where role =
		 * "content".
		 * 
		 * @return - the map of conditions that must be met for the style to be applied
		 */
		public HashMap<String, String> getConditions() {
			return conditions;
		}
		
		/**
		 * @return - the style to apply to the element when the condition is met
		 */
		public String getStyle() {
			return style;
		}
		
		/**
		 * Set the conditions that must be met for the style to be applied The special "style" key is stripped from the
		 * map.
		 * 
		 * @param conditions
		 *            - the map of conditions that must be met for the style to be applied
		 * @see #getConditions()
		 */
		public ElementWhen(HashMap<String, String> conditions) {
			this.conditions = conditions;
			if(conditions.containsKey("style")) {
				style = conditions.get("style");
				conditions.remove("style");
			}
		}
	}
	
	/**
	 * The list of styles that the element can have
	 */
	protected transient List<ElementWhen> when = new ArrayList<>();
	
	/**
	 * @return - the list of child elements
	 */
	public List<Element> getChildren() {
		return children;
	}
	
	/**
	 * Set the children of the element with the given list
	 *
	 * @param children
	 *            - the list of child elements
	 */
	public void setChildren(List<Element> children) {
		this.children = children;
	}
	
	/**
	 * Add a child element to the list of children
	 *
	 * @param element
	 *            - the child element to be added
	 */
	public void addChild(Element element) {
		this.children.add(element);
	}
	
	/**
	 * Add a list of child elements to the list of children
	 *
	 * @param _children
	 *            - the list of child elements to be added
	 */
	public void addAllChildren(List<Element> _children) {
		this.children.addAll(_children);
	}
	
	/**
	 * @return - the id of the element
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Set the id of the element
	 *
	 * @param id
	 *            - the id of the element
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return - the type of the element
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Set the type of the element
	 *
	 * @param type
	 *            - the type of the element
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * @return - the properties of the element
	 */
	public HashMap<String, String> getProperties() {
		return properties;
	}
	
	/**
	 * Set the properties of the element
	 *
	 * @param properties
	 *            - the properties of the element
	 */
	public void setProperties(HashMap<String, String> properties) {
		this.properties = properties;
	}
	
	/**
	 *
	 * @return - the port of the element
	 */
	public String getPort() {
		return port;
	}
	
	/**
	 * Set the port of the element
	 *
	 * @param port
	 *            - the port of the element
	 */
	public void setPort(String port) {
		this.port = port;
	}
	
	/**
	 * @return - the role of the element
	 */
	public String getRole() {
		return role;
	}
	
	/**
	 * Set the role of the element
	 *
	 * @param role
	 *            - the role of the element
	 */
	public void setRole(String role) {
		this.role = role;
	}
	
	/**
	 * @return - the value of the element
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Set the value of the element
	 *
	 * @param value
	 *            - the value of the element
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * @return - the endpoint of the shard that added this element
	 */
	public String getNotify() {
		return notify;
	}
	
	/**
	 * Set the endpoint of the shard that added this element
	 *
	 * @param endpoint
	 *            - the endpoint of the shard that added this element
	 */
	public void setNotify(String endpoint) {
		this.notify = endpoint;
	}
	
	/**
	 * @return - the list of styles that the element can have Note: this method must not be named getWhen() because it
	 *         would clash with the setter method for the when property used by SnakeYAML
	 */
	public List<ElementWhen> getWhenConditions() {
		return when;
	}
	
	/**
	 * Set the list of styles that the element can have
	 *
	 * @param whenMaps
	 *            - the list of styles that the element can have
	 */
	@JsonSetter
	public void setWhen(List<HashMap<String, String>> whenMaps) {
		this.when = new ArrayList<>();
		for(HashMap<String, String> e : whenMaps) {
			this.when.add(new ElementWhen(e));
		}
	}
	
	/**
	 * Return the children of the element if they match the given port
	 *
	 * @param childrenPort
	 *            - the port to match
	 * @return - the list of children that match the given port
	 */
	public List<Element> getChildren(String childrenPort) {
		List<Element> result = new LinkedList<>();
		for(Element e : children)
			if((childrenPort == null && e.getPort() == null)
					|| (childrenPort != null && childrenPort.equals(e.getPort())))
				result.add(e);
		return result;
	}
	
	/**
	 * Return the child of the element that matches the given port and role
	 *
	 * @param childPort
	 *            - the port to match
	 * @param childRole
	 *            - the role to match
	 * @return - the child that matches the given port and role
	 */
	public Element getChild(String childPort, String childRole) {
		Element result = null;
		for(Element e : children) {
			boolean portMatch = (childPort != null && childPort.equals(e.getPort()));
			portMatch = portMatch || (childPort == null && e.getPort() == null);
			boolean roleMatch = (childRole != null && childRole.equals(e.getRole()));
			roleMatch = roleMatch || (childRole == null && e.getRole() == null);
			if(portMatch && roleMatch) {
				result = e;
				break;
			}
			if(ElementType.CONTAINER.type.equals(e.type)) {
				result = e.getChild(childPort, childRole);
				if(result != null)
					break;
			}
		}
		return result;
	}
	
	/**
	 * Return the child of the element if it matches the given id
	 *
	 * @param childID
	 *            - the id to match. If <code>null</code>, the method will return <code>null</code> without searching.
	 * @return - the child that matches the given id, or <code>null</code> if none found.
	 */
	public Element getChildWithId(String childID) {
		return childID == null ? null
				: getChildren().stream().filter(c -> childID.equals(c.getId())).findFirst().orElse(null);
	}
	
	/**
	 * Searches recursively in all children to find the element with the given ID.
	 * 
	 * @param ID
	 *            - the identifier to look for. If <code>null</code>, the method will return <code>null</code> without
	 *            searching.
	 * @return - the element with that ID, or <code>null</code> if none found.
	 */
	public Element getElementWithId(String ID) {
		if(ID == null)
			return null;
		if(ID.equals(getId()))
			return this;
		return getChildren().stream().map(c -> c.getElementWithId(ID)).filter(r -> r != null).findFirst().orElse(null);
	}
	
	/**
	 * Applies an update to this element and its children, according to the given wave. The roles to which this update
	 * applies are the content elements of the wave.
	 */
	public void applyUpdate(String updatePort, AgentWave wave) {
		List<String> roles = wave.getContentElements();
		
		for(String updateRole : roles) {
			Element element = this.getChild(updatePort, updateRole);
			if(element == null)
				continue;
			element.setValue(wave.getValue(updateRole));
		}
	}
	
	/**
	 * Create a {@link JSONObject} from the element
	 *
	 * @return - a {@link JSONObject} representation of the element
	 */
	public JsonObject toJSON() {
		JsonObject result = new JsonObject();
		result.addProperty("id", id);
		result.addProperty("type", type);
		result.addProperty("value", value);
		result.addProperty("port", port);
		result.addProperty("role", role);
		result.addProperty("notify", notify);
		
		JsonArray whenArray = new JsonArray();
		result.add("when", whenArray);
		for(ElementWhen e : when) {
			JsonObject whenObject = new JsonObject();
			whenArray.add(whenObject);
			whenObject.addProperty("style", e.getStyle());
			
			JsonObject conditionsObject = gson.toJsonTree(e.getConditions()).getAsJsonObject();
			whenObject.add("conditions", conditionsObject);
		}
		
		JsonArray childrenArray = new JsonArray();
		result.add("children", childrenArray);
		if(children != null)
			for(Element child : children)
				childrenArray.add(child.toJSON());
		JsonObject propertiesObject = new JsonObject();
		result.add("properties", propertiesObject);
		for(Map.Entry<String, String> entry : properties.entrySet())
			propertiesObject.addProperty(entry.getKey(), entry.getValue());
		return result;
	}
	
	@Override
	public String toString() {
		return toString(0).toString();
	}
	
	/**
	 * Creates a String rendition of the element.
	 * 
	 * @param indent
	 *            - the indentation level.
	 * @return a string rendition.
	 */
	protected StringBuilder toString(int indent) {
		StringBuilder result = new StringBuilder();
		result.append(" ".repeat(indent * INDENT_SIZE));
		result.append(type);
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
	public Object clone() {
		Element result;
		try {
			result = (Element) super.clone();
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		result.children = new LinkedList<>();
		for(Element c : children)
			result.children.add((Element) c.clone());
		result.properties = new HashMap<>(properties);
		return result;
	}
}
