package com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2024/06/26
 * @implNote A ray that represents a line from start position to end position.
 */
public record Ray(Vector3f startPos, Vector3f endPos) {
    /**
     * Create a ray from start position to end position.
     */
    public static Ray create(Vector3f startPos, Vector3f endPos) {
        return new Ray(startPos, endPos);
    }

    /**
     * Create a ray from start position to direction with a specific length.
     */
    public static Ray create(Vector3f startPos, Vector3f direction, float length) {
        return new Ray(startPos, startPos.add(direction.normalize().mul(length, new Vector3f()), new Vector3f()));
    }

    /**
     * Create a ray from start position to infinite end position.
     */
    public static Ray createInfinite(Vector3f startPos, Vector3f direction) {
        return new Ray(startPos, startPos.add(direction.normalize().mul(100, new Vector3f()), new Vector3f()));
    }

    /**
     * Get the direction of the ray.
     */
    public Vector3f getDirection() {
        return endPos.sub(startPos, new Vector3f());
    }

    /**
     * Get the point at a specific distance from the start position.
     */
    public Vector3f getPoint(float t) {
        return startPos.add(getDirection().mul(t, new Vector3f()), new Vector3f());
    }

    /**
     * Create a ray from start position to infinite end position.
     */
    public Ray toInfinite() {
        return createInfinite(startPos, getDirection());
    }

    /**
     * Transform the ray with a matrix.
     */
    public Ray transform(Matrix4f transform) {
        return new Ray(transform.transformPosition(startPos, new Vector3f()), transform.transformPosition(endPos, new Vector3f()));
    }

    /**
     * Transform the ray with a transform. from world to local.
     */
    public Ray worldToLocal(Transform transform) {
        return transform(transform.worldToLocalMatrix());
    }

    /**
     * Transform the ray with a transform. from local to world.
     */
    public Ray localToWorld(Transform transform) {
        return transform(transform.localToWorldMatrix());
    }

    public BlockHitResult clip(VoxelShape shape) {
        return shape.clip(new Vec3(startPos), new Vec3(endPos), BlockPos.ZERO);
    }
}
