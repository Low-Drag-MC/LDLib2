package com.lowdragmc.lowdraglib2.utils;


import lombok.experimental.UtilityClass;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2022/6/17
 * @implNote ShapeUtils
 */
@UtilityClass
public final class ShapeUtils {

    public static AABB rotate(AABB AABB, Direction facing) {
        switch (facing) {
            case SOUTH -> {
                return rotate(AABB, new Vector3f(0, 1, 0), 180);
            }
            case EAST -> {
                return rotate(AABB, new Vector3f(0, 1, 0), -90);
            }
            case WEST -> {
                return rotate(AABB, new Vector3f(0, 1, 0), 90);
            }
            case UP -> {
                return rotate(AABB, new Vector3f(1, 0, 0), 90);
            }
            case DOWN -> {
                return rotate(AABB, new Vector3f(1, 0, 0), -90);
            }
        }
        return AABB;
    }

    public static AABB rotate(AABB AABB, Vector3f axis, double degree) {
        Vector3f min = new Vector3f((float) AABB.minX, (float) AABB.minY, (float) AABB.minZ).sub(0.5f, 0.5f, 0.5f);
        Vector3f max = new Vector3f((float) AABB.maxX, (float) AABB.maxY, (float) AABB.maxZ).sub(0.5f, 0.5f, 0.5f);
        float radians = (float) Math.toRadians(degree);
        min.rotateAxis(radians, axis.x, axis.y, axis.z);
        max.rotateAxis(radians, axis.x, axis.y, axis.z);
        min.add(0.5f, 0.5f, 0.5f);
        max.add(0.5f, 0.5f, 0.5f);
        return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public static VoxelShape rotate(VoxelShape shape, Direction facing) {
        return shape.toAabbs().stream().map(AABB -> Shapes.create(rotate(AABB, facing))).reduce(Shapes.empty(), Shapes::or);
    }

    public static VoxelShape rotate(VoxelShape shape, Vector3f axis, double degree) {
        return shape.toAabbs().stream().map(AABB -> Shapes.create(rotate(AABB, axis, degree))).reduce(Shapes.empty(), Shapes::or);
    }
}
