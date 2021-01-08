package net.xqhs.flash.gui.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import florin.Utils;

/**
 * Element class represents an element from GUI interface (e.g. button, label, container, text box, spinner)
 */
public class Element implements Cloneable {
	public static final String DEFAULT_ROLE = "content";
	/**
	 * counter of tabs, used in toString
	 */
	private static int			counter		= 0;
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
	/**
	 * favoriteAgent represents the name of the target agent in quick send message scenario
	 */
	private String				favoriteAgent;
	/**
	 * messageContent represents the content of the default message sent to the favoriteAgent in quick send scenario
	 */
	private String				messageContent;
	
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
	
	public String getFavoriteAgent() {
		return favoriteAgent;
	}
	
	public void setFavoriteAgent(String favoriteAgent) {
		this.favoriteAgent = favoriteAgent;
	}
	
	public String getMessageContent() {
		return messageContent;
	}
	
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
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
		String tab = "\t";
		StringBuilder result = new StringBuilder();
		result.append(Utils.repeat(tab, counter));
		result.append("id: ").append(id).append('\n');
		result.append(Utils.repeat(tab, counter));
		result.append("type: ").append(type).append('\n');
		result.append(Utils.repeat(tab, counter));
		result.append("port: ").append(port).append('\n');
		result.append(Utils.repeat(tab, counter));
		result.append("value: ").append(value).append('\n');
		result.append(Utils.repeat(tab, counter));
		result.append("role: ").append(role).append('\n');
		result.append(Utils.repeat(tab, counter));
		if(blockType != null) {
			result.append("blockType: ").append(blockType).append('\n');
			result.append(Utils.repeat(tab, counter));
		}
		result.append("children: ");
		if(children != null) {
			if(children.isEmpty()) {
				result.append("[]").append('\n');
			}
			else {
				result.append('\n');
				++Element.counter;
				for(Element child : children) {
					result.append(child.toString());
				}
				--Element.counter;
			}
		}
		result.append('\n');
		return result.toString();
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
