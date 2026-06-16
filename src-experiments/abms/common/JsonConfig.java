package abms.common;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * JSON loader for scenario configuration files.
 * Configuration files live under {@code resources/config/<scenario>/}; values are
 * read explicitly by the scenario boot class so the JSON schema stays scenario-specific.
 */
public final class JsonConfig {
    private final JSONObject root;
    private final String path;

    public JsonConfig(JSONObject root, String path) {
        this.root = root;
        this.path = path;
    }

    public static JsonConfig load(String path) {
        JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader(path)) {
            Object parsed = parser.parse(reader);
            if (!(parsed instanceof JSONObject))
                throw new IllegalArgumentException("Top-level JSON must be an object: " + path);
            return new JsonConfig((JSONObject) parsed, path);
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to read JSON config at " + path, e);
        }
    }

    public String getPath() {
        return path;
    }

    public JSONObject raw() {
        return root;
    }

    public int getInt(String key, int fallback) {
        return getInt(root, key, fallback);
    }

    public long getLong(String key, long fallback) {
        return getLong(root, key, fallback);
    }

    public String getString(String key, String fallback) {
        Object v = root.get(key);
        return v == null ? fallback : v.toString();
    }

    public double getDouble(String key, double fallback) {
        Object v = root.get(key);
        if (v == null) return fallback;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }

    public JSONObject getObject(String key) {
        Object v = root.get(key);
        return v instanceof JSONObject ? (JSONObject) v : null;
    }

    public List<String> getStringList(String key) {
        Object v = root.get(key);
        List<String> result = new ArrayList<>();
        if (v instanceof JSONArray)
            for (Object item : (JSONArray) v)
                if (item != null) result.add(item.toString());
        return result;
    }

    public List<JSONObject> getObjectList(String key) {
        Object v = root.get(key);
        List<JSONObject> result = new ArrayList<>();
        if (v instanceof JSONArray)
            for (Object item : (JSONArray) v)
                if (item instanceof JSONObject) result.add((JSONObject) item);
        return result;
    }

    public static int getInt(JSONObject json, String key, int fallback) {
        Object v = json == null ? null : json.get(key);
        if (v == null) return fallback;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    public static long getLong(JSONObject json, String key, long fallback) {
        Object v = json == null ? null : json.get(key);
        if (v == null) return fallback;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    public static String getString(JSONObject json, String key, String fallback) {
        Object v = json == null ? null : json.get(key);
        return v == null ? fallback : v.toString();
    }

    public static double getDouble(JSONObject json, String key, double fallback) {
        Object v = json == null ? null : json.get(key);
        if (v == null) return fallback;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }

    public static List<String> getStringList(JSONObject json, String key) {
        Object v = json == null ? null : json.get(key);
        List<String> result = new ArrayList<>();
        if (v instanceof JSONArray)
            for (Object item : (JSONArray) v)
                if (item != null) result.add(item.toString());
        return result;
    }
}
