package com.lowdragmc.lowdraglib2.client.scene;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A thread-confined (render-thread) hand-off of the camera a custom off-screen renderer is currently
 * drawing with, so consumers that compute their own matrices from the "current camera" can reflect it
 * instead of Minecraft's main world camera.
 *
 * <p>Minecraft's main render pipeline uses {@code Minecraft.gameRenderer.getMainCamera()}, but an
 * off-screen renderer like {@link WorldSceneRenderer} sets up its own view/projection on
 * {@code RenderSystem} while keeping the game's main {@code Camera} object untouched. Anything that reads
 * the main camera directly (e.g. a mod's custom UBO derived from the view/projection matrices) would then
 * use the wrong camera during such a render. A renderer publishes its camera matrices here for the
 * duration of its draw (see {@link WorldSceneRenderer#drawWorld}); consumers check {@link #isActive()} and
 * prefer these over the main camera. Generic on purpose — any mod doing custom rendering can set it.</p>
 */
public final class SceneCameraContext {

    @Nullable private static Matrix4f viewRotation; // world -> view rotation (camera orientation, no translation)
    @Nullable private static Matrix4f projection;   // view -> clip

    private SceneCameraContext() {}

    /** Publish the active renderer's camera matrices (copies are stored). Pair with {@link #clear()} in a finally. */
    public static void set(Matrix4f viewRotationMatrix, Matrix4f projectionMatrix) {
        viewRotation = new Matrix4f(viewRotationMatrix);
        projection = new Matrix4f(projectionMatrix);
    }

    /** Clear the published matrices, restoring consumers to the main world camera. */
    public static void clear() {
        viewRotation = null;
        projection = null;
    }

    /** Whether a custom renderer has published its camera (so consumers should use these matrices). */
    public static boolean isActive() {
        return viewRotation != null && projection != null;
    }

    /** The active world&rarr;view rotation matrix, or {@code null} if none is published. */
    @Nullable
    public static Matrix4f viewRotation() {
        return viewRotation;
    }

    /** The active view&rarr;clip projection matrix, or {@code null} if none is published. */
    @Nullable
    public static Matrix4f projection() {
        return projection;
    }
}
