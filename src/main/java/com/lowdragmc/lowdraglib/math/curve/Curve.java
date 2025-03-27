package com.lowdragmc.lowdraglib.math.curve;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/6/16
 * @implNote Curve
 */
public abstract class Curve<T> {

    public abstract T getPoint(float t);

    public List<T> getPoints(int size) {
        if (size < 2) throw new IllegalArgumentException("size should be greater than 2.");
        List<T> points = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            points.add(getPoint(i * 1f / (size - 1)));
        }
        return points;
    }

}
