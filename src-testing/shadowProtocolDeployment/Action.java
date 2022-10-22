package shadowProtocolDeployment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class that describe an action.
 */
class Action implements Serializable {
	public enum Actions {
		/**
		 * An agent sends a message to another agent.
		 */
		SEND_MESSAGE,
		/**
		 * An agent moves on another node.
		 */
		MOVE_TO_ANOTHER_NODE,
		/**
		 * No action.
		 */
		NOP,
	}

	String source, destination, content;
	Actions type;

	public Action(String source, String destination, String content, Actions type) {
		this.source = source;
		this.destination = destination;
		this.content = content;
		this.type = type;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Actions getType() {
		return type;
	}

	public void setType(Actions type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Action{" +
				"source='" + source + '\'' +
				", destination='" + destination + '\'' +
				", content='" + content + '\'' +
				", type=" + type +
				'}';
	}

	public String toJsonString() {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		Map<String, String> data = new HashMap<>();
		data.put("source", source);
		data.put("destination", destination);
		data.put("content", content);
		data.put("type", String.valueOf(type));
		return gson.toJson(data);
	}

	public static Action jsonStringToAction(String element) {
		Object obj = JSONValue.parse(element);
		JSONObject elem = (JSONObject) obj;
		if (elem != null) {
			return new Action((String) elem.get("source"), (String) elem.get("destination"), (String) elem.get("content"), Actions.valueOf((String) elem.get("type")));
		}
		return null;
	}


}
