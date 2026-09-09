package com.modularmc.ten.api.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 配方 JSON 字段宽容读取工具：缺键/空值回退默认值，
 * 供数据包配方解析使用。
 */
public class JsonParser {

    public static String getStringOr(JsonObject json, String n, String fb) {
        JsonElement e = json.get(n);
        return e == null || e.isJsonNull() ? fb : e.getAsString();
    }

    public static String getString(JsonObject json, String n) {
        return getStringOr(json, n, "");
    }

    public static int getIntOr(JsonObject json, String n, int fb) {
        JsonElement e = json.get(n);
        return e == null || e.isJsonNull() ? fb : e.getAsInt();
    }

    public static int getInt(JsonObject json, String n) {
        return getIntOr(json, n, 1);
    }

    public static float getFloatOr(JsonObject json, String n, float fb) {
        JsonElement e = json.get(n);
        return e == null || e.isJsonNull() ? fb : e.getAsFloat();
    }

    public static float getFloat(JsonObject json, String n) {
        return getFloatOr(json, n, 1f);
    }

    public static boolean getBooleanOr(JsonObject json, String n, boolean fb) {
        JsonElement e = json.get(n);
        return e == null || e.isJsonNull() ? fb : e.getAsBoolean();
    }
}
