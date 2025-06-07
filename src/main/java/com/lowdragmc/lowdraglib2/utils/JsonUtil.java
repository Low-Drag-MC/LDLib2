package com.lowdragmc.lowdraglib2.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Array;

@UtilityClass
public final class JsonUtil {
    public static int[] getIntArray(JsonElement array) {
        if (array.isJsonArray()) {
            JsonArray jsonArray = array.getAsJsonArray();
            int[] result = new int[jsonArray.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = jsonArray.get(i).getAsInt();
            }
            return result;
        }
        return new int[0];
    }

    public static JsonArray setIntArray(int[] array) {
        JsonArray jsonArray = new JsonArray();
        for (int i : array) {
            jsonArray.add(i);
        }
        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T[] getEnumArray(JsonElement array, Class<T> enumClass) {
        if (array.isJsonArray()) {
            JsonArray jsonArray = array.getAsJsonArray();
            T[] result = (T[]) Array.newInstance(enumClass, jsonArray.size());
            for (int i = 0; i < result.length; i++) {
                result[i] = (T)((Enum<T>[])enumClass.getEnumConstants())[jsonArray.get(i).getAsInt()];
            }
            return result;
        }
        return (T[]) Array.newInstance(enumClass, 0) ;
    }

    public static <T extends Enum<T>> JsonArray setEnumArray(T[] array, Class<T> enumClass) {
        JsonArray jsonArray = new JsonArray();
        for (T i : array) {
            jsonArray.add(i.ordinal());
        }
        return jsonArray;
    }

    public static boolean[] getBooleanArray(JsonElement array) {
        if (array.isJsonArray()) {
            JsonArray jsonArray = array.getAsJsonArray();
            boolean[] result = new boolean[jsonArray.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = jsonArray.get(i).getAsBoolean();
            }
            return result;
        }
        return new boolean[0];
    }

    public static JsonArray setBooleanArray(boolean[] array) {
        JsonArray jsonArray = new JsonArray();
        for (boolean i : array) {
            jsonArray.add(i);
        }
        return jsonArray;
    }

    public static float[] getFloatArray(JsonElement array) {
        if (array.isJsonArray()) {
            JsonArray jsonArray = array.getAsJsonArray();
            float[] result = new float[jsonArray.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = jsonArray.get(i).getAsFloat();
            }
            return result;
        }
        return new float[0];
    }

    public static JsonArray setFloatArray(float[] array) {
        JsonArray jsonArray = new JsonArray();
        for (float i : array) {
            jsonArray.add(i);
        }
        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnumOr(JsonObject jsonObject, String key, Class<T> enumClass, T io) {
        JsonElement jsonElement = jsonObject.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            Enum<T>[] values = enumClass.getEnumConstants();
            if (primitive.isString()) {
                String name = primitive.getAsString();
                for (Enum<T> value : values) {
                    if (value.name().equals(name)) {
                        return (T)value;
                    }
                }
            } else if (primitive.isNumber()) {
                return (T)values[jsonElement.getAsInt()];
            }
        }
        return io;
    }
}
