package net.xqhs.flash.json;

import java.io.Serializable;

import com.google.gson.JsonArray;
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
	
	@Override
	protected MultiValueMap addItem(String name, Object value, boolean insertFirst) {
		// TODO it is a multi-value map, must add a value
		super.addItem(name, value, insertFirst);
		if(!json.has(name))
			json.add(name, new JsonArray());
		JsonArray array = json.get(name).getAsJsonArray();
		if(value instanceof String)
			array.add((String) value);
		else if(value instanceof Serializable) {
			JsonObject serial = new JsonObject();
			serial.addProperty(IS_SERIALIZED_OBJECT, PlatformUtils.serializeObject(value));
			array.add(serial);
		}
		else
			array.add(value.toString());
		return this;
	}
	
	@Override
	public MultiValueMap removeKey(String name) {
		super.removeKey(name);
		// TODO
		return this;
	}
}
