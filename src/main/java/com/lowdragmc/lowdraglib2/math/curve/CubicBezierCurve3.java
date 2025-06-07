package com.lowdragmc.lowdraglib2.math.curve;

import com.lowdragmc.lowdraglib2.math.Interpolations;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2022/6/17
 * @implNote CubicBezierCurve3
 */
public class CubicBezierCurve3 extends Curve<Vector3f> {
    public Vector3f p0, c0, c1, p1;

    public CubicBezierCurve3(Vector3f start, Vector3f control1, Vector3f control2, Vector3f end) {
        this.p0 = start;
        this.c0 = control1;
        this.c1 = control2;
        this.p1 = end;
    }

    @Override
    public Vector3f getPoint(float t) {
        return new Vector3f(
                (float) Interpolations.CubicBezier(t, p0.x, c0.x, c1.x, p1.x),
                (float) Interpolations.CubicBezier(t, p0.y, c0.y, c1.y, p1.y),
                (float) Interpolations.CubicBezier(t, p0.z, c0.z, c1.z, p1.z)
        );
    }
}
