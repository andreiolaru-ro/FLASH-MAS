package net.xqhs.flash.json;

import java.io.Serializable;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * An extension of {@link AgentWave} that keeps a {@link JsonObject} instance with the same data, in parallel with the
 * {@link MultiValueMap}.
 */
public class AgentWaveJson extends AgentWave {
	JsonObject json;
	
	public static final String IS_SERIALIZED_OBJECT = "is-serialized-object";
	
	public AgentWaveJson() {
		super();
		json = new JsonObject();
	}
	
	public JsonObject getJson() {
		return json;
	}
	
	@Override
	protected MultiValueMap addItem(String name, Object value, boolean insertFirst) {
		// TODO it is a multi-value map, must add a value
		super.addItem(name, value, insertFirst);
		Object toAdd;
		
		if(value instanceof String)
			toAdd = (String) value;
		else if(value instanceof Serializable) {
			JsonObject serial = new JsonObject();
			serial.addProperty(IS_SERIALIZED_OBJECT, PlatformUtils.serializeObject(value));
			toAdd = serial;
		}
		else
			toAdd = value.toString();
		JsonArray array;
		if(insertFirst) {
			array = new JsonArray();
			if(toAdd instanceof String)
				array.add((String) toAdd);
			else
				array.add((JsonObject) toAdd);
			if(json.has(name))
				array.addAll(json.getAsJsonArray(name));
		}
		else {
			array = json.has(name) ? json.get(name).getAsJsonArray() : new JsonArray();
			if(toAdd instanceof String)
				array.add((String) toAdd);
			else
				array.add((JsonObject) toAdd);
		}
		if(insertFirst)
			json.add(name, array);
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
}
