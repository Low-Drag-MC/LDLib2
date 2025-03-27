package com.lowdragmc.lowdraglib.utils;

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

}
