package org.figuramc.figura.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class JsonUtils {

    // Json -> Java

    @SuppressWarnings("SameParameterValue")
    @Contract("_, _, !null -> !null")
    public static @Nullable String getStringOrDefault(JsonObject object, String key, @Nullable String defaultVal) {
        JsonElement elem = object.get(key);
        if (elem == null || !elem.isJsonPrimitive()) return defaultVal;
        JsonPrimitive prim = elem.getAsJsonPrimitive();
        if (!prim.isString()) return defaultVal;
        return prim.getAsString();
    }

    @SuppressWarnings("SameParameterValue")
    public static int getIntOrDefault(JsonObject object, String key, int defaultVal) {
        JsonElement elem = object.get(key);
        if (elem == null || !elem.isJsonPrimitive()) return defaultVal;
        JsonPrimitive prim = elem.getAsJsonPrimitive();
        if (!prim.isNumber()) return defaultVal;
        return prim.getAsInt();
    }

    @SuppressWarnings("SameParameterValue")
    public static boolean getBooleanOrDefault(JsonObject object, String key, boolean defaultVal) {
        JsonElement elem = object.get(key);
        if (elem == null || !elem.isJsonPrimitive()) return defaultVal;
        JsonPrimitive prim = elem.getAsJsonPrimitive();
        if (!prim.isBoolean()) return defaultVal;
        return prim.getAsBoolean();
    }

    public static Vector2f parseVec2f(JsonArray arr) {
        if (arr.size() != 2) throw new IllegalArgumentException("Vector is not length 2");
        Vector2f res = new Vector2f();
        for (int i = 0; i < 2; i++)
            res.setComponent(i, arr.get(i).getAsFloat());
        return res;
    }

    public static Vector3f parseVec3f(JsonArray arr) {
        if (arr.size() != 3) throw new IllegalArgumentException("Vector is not length 3");
        Vector3f res = new Vector3f();
        for (int i = 0; i < 3; i++)
            res.setComponent(i, arr.get(i).getAsFloat());
        return res;
    }

    // Java -> Json
    public static JsonArray toJson(Vector3fc vec) {
        return toJson(vec.x(), vec.y(), vec.z());
    }

    public static JsonArray toJson(Vector2fc vec) {
        return toJson(vec.x(), vec.y());
    }

    public static final JsonArray ZERO_VEC_3 = toJson(0, 0, 0);

    public static JsonArray toJson(float... values) {
        JsonArray arr = new JsonArray();
        for (float f : values) arr.add(f);
        return arr;
    }

}
