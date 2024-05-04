package net.xqhs.flash.json;

import java.io.Serializable;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * An extension of {@link AgentWave} that keeps a {@link JsonObject} instance with the same data, in parallel with the
 * {@link MultiValueMap}.
 */
public class AgentWaveJson extends AgentWave {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -9093494937192523540L;
	
	/**
	 * A parallel storage of the information in this wave.
	 */
	JsonObject json = null;
	
	/**
	 * Special key meaning that the value is actually a serialized object.
	 */
	public static final String IS_SERIALIZED_OBJECT = "is-serialized-object";
	
	/**
	 * Constructor.
	 */
	public AgentWaveJson() {
		super();
	}
	
	/**
	 * @return the {@link JsonObject} containing the information in this {@link AgentWave}.
	 */
	public JsonObject getJson() {
		return json;
	}
	
	@Override
	protected MultiValueMap addItem(String name, Object value, boolean insertFirst) {
		super.addItem(name, value, insertFirst);
		if(json == null)
			json = new JsonObject();
		Object toAdd = valueToJson(value);
		
		JsonArray array;
		if(insertFirst) {
			array = new JsonArray();
			if(toAdd instanceof String)
				array.add((String) toAdd);
			else
				array.add((JsonObject) toAdd);
			if(json.has(name))
				array.addAll(json.getAsJsonArray(name));
			json.add(name, array);
		}
		else {
			if(json.has(name))
				array = json.get(name).getAsJsonArray();
			else {
				array = new JsonArray();
				json.add(name, array);
			}
			if(toAdd instanceof String)
				array.add((String) toAdd);
			else
				array.add((JsonObject) toAdd);
		}
		return this;
	}
	
	@Override
	public MultiValueMap removeKey(String name) {
		super.removeKey(name);
		if(json.has(name))
			json.remove(name);
		return this;
	}
	
	@Override
	public MultiValueMap removeFirst(String name) {
		super.removeFirst(name);
		if(json.has(name)) {
			JsonArray array = json.get(name).getAsJsonArray();
			if(array.size() == 1)
				json.remove(name);
			else
				array.remove(0);
		}
		return this;
	}
	
	@Override
	public MultiValueMap remove(String name, Object value) {
		super.remove(name, value);
		if(json.has(name)) {
			JsonArray array = json.get(name).getAsJsonArray();
			if(array.size() == 1)
				json.remove(name);
			else
				for(Iterator<JsonElement> it = array.iterator(); it.hasNext();)
					if(it.next().equals(value)) {
						it.remove();
						break;
					}
		}
		return this;
	}
	
	/**
	 * Create a {@link JsonObject} containing all the information in the {@link AgentWave}.
	 * <p>
	 * The event type is not included.
	 * 
	 * @param wave
	 *            - the {@link AgentWave}.
	 * @return the {@link JsonObject}.
	 */
	public static JsonObject toJson(AgentWave wave) {
		JsonObject res = new JsonObject();
		for(String key : wave.getKeys())
			if(!key.equals(AgentEvent.EVENT_TYPE_PARAMETER_NAME)) {
				JsonArray array = new JsonArray();
				res.add(key, array);
				for(Object value : wave.getObjects(key)) {
					Object toAdd = valueToJson(value);
					if(toAdd instanceof String)
						array.add((String) toAdd);
					else
						array.add((JsonObject) toAdd);
				}
			}
		return res;
	}
	
	/**
	 * Create an {@link AgentWave} containing all the information in the {@link JsonObject}.
	 * 
	 * @param json
	 *            - the {@link JsonObject}.
	 * @return the {@link AgentWave}.
	 */
	public static AgentWave toAgentWave(JsonObject json) {
		AgentWave res = new AgentWave();
		for(String key : json.keySet())
			for(Iterator<JsonElement> it = json.get(key).getAsJsonArray().iterator(); it.hasNext();) {
				JsonElement value = it.next();
				if(value.isJsonObject() && value.getAsJsonObject().has(IS_SERIALIZED_OBJECT))
					res.addObject(key, PlatformUtils
							.deserializeObject(value.getAsJsonObject().get(IS_SERIALIZED_OBJECT).getAsString()));
				else
					res.add(key, value.getAsString());
			}
		return res;
	}
	
	/**
	 * Computes the object to add to a Json structure, depending on the type of the value.
	 * <ul>
	 * <li>if the value is a {@link String}, it is added as such.
	 * <li>if the value is a serialized object, the serialized form is added inside a one-key dictionary, under the key
	 * {@link #IS_SERIALIZED_OBJECT}.
	 * <li>otherwise, a string is added, as returned by {@link Object#toString()}.
	 * </ul>
	 * 
	 * @param value
	 *            - the value it is wished to add to the Json structure.
	 * @return a {@link String} of a {@link JsonObject}.
	 */
	static Object valueToJson(Object value) {
		if(value instanceof String)
			return value;
		else if(value instanceof Serializable) {
			JsonObject serial = new JsonObject();
			serial.addProperty(IS_SERIALIZED_OBJECT, PlatformUtils.serializeObject(value));
			return serial;
		}
		else
			return value.toString();
	}
}
