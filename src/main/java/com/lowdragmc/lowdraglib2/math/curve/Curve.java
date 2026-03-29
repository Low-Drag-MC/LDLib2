package com.lowdragmc.lowdraglib2.math.curve;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/6/16
 * @implNote Curve
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
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
