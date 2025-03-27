package com.lowdragmc.lowdraglib.utils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Builder3D is used to build 3D arrays.
 * @param <T> The type of the array
 * @param <B> The type of the builder
 */
public abstract class Builder3D<T, B extends Builder3D<T, B>> {
    protected List<String[]> shape = new ArrayList<>();
    protected Map<Character, T> symbolMap = new LinkedHashMap<>();

    public B aisle(String... data) {
        this.shape.add(data);
        return (B) this;
    }

    public B where(char symbol, T value) {
        this.symbolMap.put(symbol, value);
        return (B) this;
    }

    public T[][][] bakeArray(Class<T> clazz, T defaultValue) {
        T[][][] Ts = (T[][][]) Array.newInstance(clazz, shape.get(0)[0].length(), shape.get(0).length, shape.size());
        for (int z = 0; z < shape.size(); z++) { //z
            String[] aisleEntry = shape.get(z);
            for (int y = 0; y < shape.get(0).length; y++) {
                String columnEntry = aisleEntry[y];
                for (int x = 0; x < columnEntry.length(); x++) {
                    T info = symbolMap.getOrDefault(columnEntry.charAt(x), defaultValue);
                    Ts[x][y][z] = info;
                }
            }
        }
        return Ts;
    }

    public B shallowCopy() {
        Builder3D builder3D;
        try {
            builder3D = this.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        builder3D.shape = new ArrayList<>(this.shape);
        builder3D.symbolMap = new HashMap<>(this.symbolMap);
        return (B) builder3D;
    }

}
