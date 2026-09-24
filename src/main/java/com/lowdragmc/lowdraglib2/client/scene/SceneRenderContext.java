package com.lowdragmc.lowdraglib2.client.scene;

import org.jetbrains.annotations.Nullable;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Per-frame context handed to {@link SceneRenderHook} callbacks. Bundles every primitive
 * a custom render path might need: pose, submission storage, camera render state, partial ticks and —
 * once drawing has started — the render pass.
 * <p>
 * Scene geometry, builtin and custom alike, is normally <em>submitted</em> into {@link #submitStorage},
 * which the {@code FeatureRenderDispatcher} drains. A hook that has to draw immediately instead — an
 * overlay drawn with the depth test off, in an order it controls — draws into {@link #renderPass()},
 * the pass the whole scene is being rendered in. Since 26.3 a pass names its target explicitly, so this
 * is the only way to reach the scene's textures; there is no global output to redirect any more.
 * <p>
 * Hook timing — see {@link WorldSceneRenderer} {@code drawWorld}:
 * <ul>
 *   <li>{@code beforeAllSubmit} — before any builtin submit; rarely needed</li>
 *   <li>{@code afterBuiltinSubmit} — after entity/BESR/particle submit, before dispatch;
 *       submit into {@code submitStorage}</li>
 *   <li>{@code afterTranslucentDispatch} — between the translucent and after-terrain feature phases;
 *       {@link #renderPass()} is open</li>
 *   <li>{@code afterAllDispatch} — after all feature phases dispatched; {@link #renderPass()} is open</li>
 * </ul>
 */
public record SceneRenderContext(
        WorldSceneRenderer renderer,
        PoseStack poseStack,
        SubmitNodeStorage submitStorage,
        CameraRenderState cameraState,
        float partialTicks,
        @Nullable RenderPass pass) {

    /** Default order — sits with builtin entity / BESR / particle submissions. */
    public static final int LAYER_DEFAULT = 0;
    /** Suggested order for selection / hover borders. */
    public static final int LAYER_OVERLAY = 1000;
    /** Suggested order for transform gizmo axes. */
    public static final int LAYER_GIZMO = 10000;
    /** Last possible order within a dispatch phase. */
    public static final int LAYER_LAST = Integer.MAX_VALUE;

    /**
     * The pass the scene is being drawn in.
     *
     * @throws IllegalStateException from a submit-phase hook, which runs before the pass opens
     */
    public RenderPass renderPass() {
        if (pass == null) {
            throw new IllegalStateException("The scene's render pass is only open during the dispatch hooks");
        }
        return pass;
    }

    SceneRenderContext withRenderPass(RenderPass pass) {
        return new SceneRenderContext(renderer, poseStack, submitStorage, cameraState, partialTicks, pass);
    }
}
