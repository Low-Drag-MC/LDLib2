package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Driving a real cursor at points in a {@link Scene}, for the scenarios that test the transform gizmo.
 *
 * <p>The gizmo is 3D geometry with no element of its own, so none of it can be clicked by selector: a
 * handle is aimed at by working out a point on it in world space and projecting that onto the screen.
 * Every gizmo scenario needs the same projection, which is why it lives here rather than three times over.
 *
 * <p>⚠️ Deliberately <b>not</b> {@code SceneEditor#project}: that one reads the modelview, projection and
 * viewport straight out of the live GL state, so it is only meaningful <em>inside</em> the scene's own
 * render pass. Called from a scenario step it silently hands back the caller's own x and y, which is how
 * the first version of this moved the cursor off screen and then reported that nothing was hovered. What
 * follows is recomputed from the camera the scene was set up with, which is the same view
 * {@code setupCamera} builds: {@code lookAt(eye, target, worldUp)} under one of its two projections.
 */
final class SceneAiming {

    private SceneAiming() {
    }

    static WorldSceneRenderer renderer(Scene scene) {
        var renderer = scene.<WorldSceneRenderer>getRenderer();
        if (renderer == null) throw new IllegalStateException("the scene has no renderer");
        return renderer;
    }

    /** The screen's right direction in world space, built the way {@code lookAt} builds it. */
    static Vector3f screenRight(Scene scene) {
        var renderer = renderer(scene);
        var forward = new Vector3f(renderer.getLookAt()).sub(renderer.getEyePos()).normalize();
        return forward.cross(renderer.getWorldUp(), new Vector3f()).normalize();
    }

    /**
     * A world point as a gui-scaled screen point, or {@code null} if it does not land inside the scene.
     *
     * <p>The bounds test is not pedantry. A point that misses the element still has screen coordinates,
     * and driving the cursor there puts it on some other part of the UI, where "nothing is hovered" is
     * perfectly true and says nothing about the gizmo — so a scenario that had aimed wrongly would report
     * a plausible-looking failure somewhere else entirely. Better to fail where the aim was computed.
     */
    @Nullable
    static Vector2f project(Scene scene, Vector3f world) {
        var renderer = renderer(scene);
        var eye = renderer.getEyePos();
        var forward = new Vector3f(renderer.getLookAt()).sub(eye);
        var right = new Vector3f();
        var up = new Vector3f();
        if (forward.lengthSquared() < 1.0e-9f) return null;
        forward.normalize().cross(renderer.getWorldUp(), right);
        if (right.lengthSquared() < 1.0e-9f) return null;
        right.normalize().cross(forward, up);
        up.normalize();

        var offset = new Vector3f(world).sub(eye);
        var width = scene.getContentWidth();
        var height = scene.getPaddingHeight();
        if (width <= 0 || height <= 0) return null;
        float ndcX;
        float ndcY;
        if (scene.isUseOrtho()) {
            // setupCamera builds setOrtho(-h, h, -h / aspect, h / aspect, ...) with h = range * zoom,
            // so the horizontal half-extent is the fixed one and depth does not come into it at all
            var halfWidth = scene.getRange() * scene.getZoom();
            var halfHeight = halfWidth / (width / height);
            ndcX = offset.dot(right) / halfWidth;
            ndcY = offset.dot(up) / halfHeight;
        } else {
            var depth = offset.dot(forward);
            if (depth <= 1.0e-4f) return null; // behind the camera
            var tanHalfFov = (float) Math.tan(Math.toRadians(renderer.getFov() * 0.5));
            ndcX = offset.dot(right) / (depth * tanHalfFov * (width / height));
            ndcY = offset.dot(up) / (depth * tanHalfFov);
        }
        if (Math.abs(ndcX) > 1 || Math.abs(ndcY) > 1) return null;
        return new Vector2f(scene.getContentX() + (ndcX * 0.5f + 0.5f) * width,
                scene.getContentY() + (0.5f - ndcY * 0.5f) * height);
    }

    static Vector2f require(Scene scene, Vector3f world) {
        var screen = project(scene, world);
        if (screen == null) throw new IllegalStateException("could not project " + world + " onto the screen");
        return screen;
    }

    static void moveTo(TestContext ctx, Scene scene, Vector3f world) {
        var screen = require(scene, world);
        ctx.input().moveTo(screen.x, screen.y);
    }

    static void press(TestContext ctx, Scene scene, Vector3f world) {
        var screen = require(scene, world);
        ctx.input().mouseDown(screen.x, screen.y, Keys.MOUSE_LEFT);
    }

    static void drag(TestContext ctx, Scene scene, Vector3f world) {
        var screen = require(scene, world);
        ctx.input().dragTo(screen.x, screen.y, Keys.MOUSE_LEFT);
    }

    static void release(TestContext ctx, Scene scene, Vector3f world) {
        var screen = require(scene, world);
        ctx.input().mouseUp(screen.x, screen.y, Keys.MOUSE_LEFT);
    }
}
