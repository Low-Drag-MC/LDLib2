package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2023/6/9
 * @implNote Vector3fHelper
 */
@UtilityClass
public final class Vector3fHelper {
    public static float min(Vector3f vec) {
        return vec.x < vec.y ? Math.min(vec.x, vec.z) : Math.min(vec.y, vec.z);
    }

    public static float max(Vector3f vec) {
        return vec.x > vec.y ? Math.max(vec.x, vec.z) : Math.max(vec.y, vec.z);
    }

    public static Vector3f rotateYXY(Vector3f vec, Vector3f rotation) {
        return vec.rotateY(rotation.y).rotateX(rotation.x).rotateY(rotation.z);
    }

    public static boolean isZero(Vector3f vec) {
        return vec.x == 0 && vec.y ==0 && vec.z == 0;
    }

    public static BlockPos toBlockPos(Vector3f vec) {
        return new BlockPos((int) vec.x, (int) vec.y, (int) vec.z);
    }

    public static Vector3f project(Vector3f a, Vector3f b) {
        float l = b.lengthSquared();
        if (l == 0.0D) {
            a.set(0.0D, 0.0D, 0.0D);
        } else {
            float m = a.dot(b) / l;
            a.set(b).mul(m);
        }
        return a;
    }

    /**
     * Find the point on the infinite line {@code lineOrigin + t * lineDir} that is closest to the infinite
     * ray/line {@code rayOrigin + s * rayDir}. Used to drag a scene object along a gizmo axis so it tracks
     * the mouse ray exactly (accounting for axis foreshortening).
     *
     * @param lineDir direction of the target line; need not be normalized (t is measured in its units).
     * @return the closest point on the {@code (lineOrigin, lineDir)} line.
     */
    public static Vector3f closestPointOnLine(Vector3f rayOrigin, Vector3f rayDir, Vector3f lineOrigin, Vector3f lineDir) {
        Vector3f w0 = new Vector3f(rayOrigin).sub(lineOrigin);
        float a = rayDir.dot(rayDir);
        float b = rayDir.dot(lineDir);
        float c = lineDir.dot(lineDir);
        float d = rayDir.dot(w0);
        float e = lineDir.dot(w0);
        float denom = a * c - b * b;
        float t;
        if (Math.abs(denom) < 1.0e-9f) {
            // ray and line are (nearly) parallel: project w0 onto the line
            t = c < 1.0e-9f ? 0f : e / c;
        } else {
            t = (a * e - b * d) / denom;
        }
        return new Vector3f(lineOrigin).add(new Vector3f(lineDir).mul(t));
    }

    /**
     * Signed angle (radians) rotating {@code from} to {@code to} around {@code axis}, following the right-hand
     * rule. {@code from} and {@code to} are expected to lie (roughly) in the plane whose normal is {@code axis}.
     */
    public static float signedAngle(Vector3f from, Vector3f to, Vector3f axis) {
        Vector3f cross = new Vector3f(from).cross(to);
        float angle = (float) Math.atan2(cross.length(), from.dot(to));
        return axis.dot(cross) < 0 ? -angle : angle;
    }

}
