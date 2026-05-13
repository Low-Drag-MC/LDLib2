package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Per-frame context handed to {@link SceneRenderHook} callbacks. Bundles every primitive
 * a custom render path might need: pose, submission storage, raw buffer source, camera
 * render state, and partial ticks.
 * <p>
 * Hook timing — see {@link WorldSceneRenderer} {@code drawWorld}:
 * <ul>
 *   <li>{@code beforeAllSubmit} — before any builtin submit; rarely needed</li>
 *   <li>{@code afterBuiltinSubmit} — after entity/BESR/particle submit, before dispatch;
 *       use {@code submitStorage} (vanilla pipeline)</li>
 *   <li>{@code afterTranslucentDispatch} — after translucent features endBatch, before
 *       particle dispatch; raw {@code bufferSource.getBuffer(noDepthRT)} draws auto-flushed</li>
 *   <li>{@code afterAllDispatch} — after all dispatch; raw draws need manual
 *       {@code bufferSource.endBatch()}</li>
 * </ul>
 */
public record SceneRenderContext(
        WorldSceneRenderer renderer,
        PoseStack poseStack,
        SubmitNodeStorage submitStorage,
        MultiBufferSource.BufferSource bufferSource,
        CameraRenderState cameraState,
        float partialTicks) {

    /** Default order — sits with builtin entity / BESR / particle submissions. */
    public static final int LAYER_DEFAULT = 0;
    /** Suggested order for selection / hover borders. */
    public static final int LAYER_OVERLAY = 1000;
    /** Suggested order for transform gizmo axes. */
    public static final int LAYER_GIZMO = 10000;
    /** Last possible order within a dispatch phase. */
    public static final int LAYER_LAST = Integer.MAX_VALUE;
}
