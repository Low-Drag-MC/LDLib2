package com.lowdragmc.lowdraglib2.client.scene;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Camera variant for {@link WorldSceneRenderer}. Forces {@link #position()} to
 * {@link Vec3#ZERO} so vanilla extraction code (particles / entities) that does
 * {@code world - camera.position()} doesn't double-subtract the eye position — our view
 * matrix is {@code lookAt(eyePos, ...)} which already encodes that translation.
 */
@OnlyIn(Dist.CLIENT)
public class SceneCamera extends Camera {
    @Override
    public Vec3 position() {
        return Vec3.ZERO;
    }

    /** Public wrapper around protected {@link Camera#setRotation} for callers that drive
     *  the camera off a backing entity's yaw/pitch (used by particle billboard math). */
    public void setSceneRotation(float yRot, float xRot) {
        super.setRotation(yRot, xRot);
    }
}
