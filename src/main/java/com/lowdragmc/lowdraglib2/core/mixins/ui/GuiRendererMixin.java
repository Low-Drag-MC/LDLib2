package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowdragmc.lowdraglib2.client.GameFrameCapture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRenderStateExt;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRendererExt;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IPreciseScissor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererPool;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/// Surgical mixin to support visual-layer sub-renderers:
/// (1) make `renderState` swappable so a sub-GuiRenderer can adopt a captured sub-state
/// (2) redirect main render target inside `draw()` to allow writing into an off-target
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements IGuiRendererExt {
    @Shadow @Final @Mutable private GuiRenderState renderState;
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private SubmitNodeCollector submitNodeCollector;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final @Mutable private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pictureInPictureRendererPools;
    @Shadow @Final private List<?> meshesToDraw;
    @Shadow private int firstDrawIndexAfterBlur;

    /// The draw the game's copy is taken before ({@link GameFrameCapture}), or -1 for none this frame.
    @Unique
    private int ldlib2$frameCaptureDrawIndex = -1;
    @Unique
    private boolean ldlib2$frameCaptured;

    @Override
    public void ldlib2$setRenderState(GuiRenderState state) {
        this.renderState = state;
    }

    @Override
    public MultiBufferSource.BufferSource ldlib2$getBufferSource() {
        return this.bufferSource;
    }

    @Override
    public SubmitNodeCollector ldlib2$getSubmitNodeCollector() {
        return this.submitNodeCollector;
    }

    @Override
    public FeatureRenderDispatcher ldlib2$getFeatureRenderDispatcher() {
        return this.featureRenderDispatcher;
    }

    @Override
    public Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> ldlib2$getPictureInPictureRendererPools() {
        return this.pictureInPictureRendererPools;
    }

    @Override
    public void ldlib2$setPictureInPictureRendererPools(
            Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pools) {
        this.pictureInPictureRendererPools = pools;
    }

    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget ldlib2$redirectRenderTarget(Minecraft mc) {
        RenderTarget override = IGuiRendererExt.ldlib2$peekTargetOverride();
        return override != null ? override : mc.getMainRenderTarget();
    }

    /// Size the orthographic projection to the off-target being drawn into, when there is one.
    ///
    /// Vanilla takes the extent from {@code windowRenderState}, i.e. the game window. That is right
    /// for the main pass and for a visual layer (whose off-target is window-sized), and wrong for a
    /// UI hosted in its own OS window, which is laid out at that window's size — with the game
    /// window's extent its contents are scaled by the ratio between the two and clipped to a corner.
    @ModifyArgs(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V")
    )
    private void ldlib2$overrideOrtho(Args args) {
        var override = IGuiRendererExt.ldlib2$peekOrthoOverride();
        if (override == null) return;
        args.set(2, override.width());
        args.set(3, override.height());
    }

    /// Resolve a clip rectangle against the target actually being drawn into, at that target's
    /// resolution.
    ///
    /// Two corrections, and they compose:
    ///
    ///  - {@code enableScissor} takes the pixel height and gui scale from the game window and flips
    ///    the rectangle against them, unconditionally. Drawing a smaller off-target therefore puts
    ///    the box somewhere outside it and the draw disappears — silently, and only for clipped
    ///    elements, so a window kept its unclipped chrome and lost everything inside a scrolling or
    ///    clipping container.
    ///  - the rectangle is an integer in gui units, so at guiScale 3 the clip edge can only fall on
    ///    every third physical pixel the target has. Where LDLib recorded the unrounded rectangle
    ///    ({@link IPreciseScissor}), that is quantised here instead — once, against the real pixel
    ///    grid — so a clip inside a zoomed graph view tracks the content instead of snapping to the
    ///    gui pixel grid as it pans.
    ///
    /// Left entirely alone when neither applies, so vanilla screens keep vanilla behaviour.
    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void ldlib2$scissorPrecisely(ScreenRectangle rectangle, RenderPass renderPass, CallbackInfo ci) {
        var override = IGuiRendererExt.ldlib2$peekOrthoOverride();
        if (override == null && IPreciseScissor.of(rectangle) == null) return;

        int targetWidth, targetHeight;
        double scaleX, scaleY;
        if (override != null) {
            targetWidth = override.framebufferWidth();
            targetHeight = override.framebufferHeight();
            // Derived, not the gui scale. ldlib2$overrideOrtho projects gui [0, guiScaledWidth] onto
            // the whole target, and guiScaledWidth is ceil(pixels / guiScale) — so whenever the
            // framebuffer is not an exact multiple of the scale, which is the normal case at 3 or 4,
            // a gui unit is slightly less than guiScale pixels. Quantising against the target's real
            // grid is the entire point, so use the mapping the projection actually established.
            scaleX = targetWidth / (double) override.width();
            scaleY = targetHeight / (double) override.height();
        } else {
            var window = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState;
            targetWidth = window.width;
            targetHeight = window.height;
            // Vanilla's own ortho is width / guiScale exactly, so here the mapping is the gui scale.
            scaleX = scaleY = window.guiScale;
        }

        var box = PreciseScissor.quantize(IPreciseScissor.clipOf(rectangle),
                scaleX, scaleY, targetWidth, targetHeight);
        renderPass.enableScissor(box.x(), box.y(), box.width(), box.height());
        ci.cancel();
    }

    /// Two clip rectangles that round to the same integer box are not the same clip.
    ///
    /// {@code addElementToMesh} batches consecutive elements into one mesh and records a single
    /// scissor for the lot, so without this an element clipped at x=10.2 and one clipped at x=10.8
    /// merge and both get whichever box arrived first — easy to hit at low zoom, where neighbouring
    /// nodes floor onto the same gui pixel.
    ///
    /// Hooked here rather than on {@code ScreenRectangle#equals}, which is a record's value equality
    /// and is relied on well outside the gui renderer.
    /// {@code @ModifyReturnValue} rather than a cancellable {@code @Inject}: this runs once per gui
    /// element per frame, and a cancellable inject allocates a {@code CallbackInfoReturnable} at the
    /// injection point every time. Taking {@code original} also means vanilla's {@code equals} —
    /// a nested record comparison — is not run a second time here just to find out whether it
    /// already reported a change.
    @ModifyReturnValue(method = "scissorChanged", at = @At("RETURN"))
    private boolean ldlib2$preciseScissorChanged(boolean original,
                                                 @Nullable ScreenRectangle newScissor,
                                                 @Nullable ScreenRectangle oldScissor) {
        if (original || newScissor == null || oldScissor == null) return original;
        return !Objects.equals(IPreciseScissor.of(newScissor), IPreciseScissor.of(oldScissor));
    }

    /// Keep elements that share an integer box but not a precise clip from interleaving.
    ///
    /// {@code ELEMENT_SORT_COMPARATOR} groups by the integer rectangle, which no longer identifies a
    /// clip on its own; with the sort blind to the difference and {@code scissorChanged} no longer
    /// blind to it, two such elements alternate and cost a mesh flush each time. Reordering within a
    /// layer node is something vanilla already does — it sorts by pipeline and texture — so this is
    /// safe on the same grounds.
    @ModifyArg(
            method = "prepare",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;sortElements(Ljava/util/Comparator;)V")
    )
    private Comparator<GuiElementRenderState> ldlib2$groupByPreciseScissor(Comparator<GuiElementRenderState> original) {
        return original.thenComparing(GuiElementRenderState::scissorArea, IPreciseScissor.COMPARATOR);
    }

    /// Mesh what comes before the {@link GameFrameCapture} boundary apart from what comes after it.
    ///
    /// Elements are batched into meshes across strata, so without this a mesh can hold the last of
    /// the HUD and the first of the screen, and there is no point in the draw list between them. The
    /// split vanilla already has is the blur one, so the boundary borrows it: with no blur this
    /// frame the before-blur range is meshed in two at the boundary. A screen that does blur has its
    /// blur where it begins anyway — the copy is taken there, before the blur is applied.
    ///
    /// A frame that marked no boundary (nobody wants a copy, or no screen is open) is meshed exactly
    /// as vanilla meshes it.
    @WrapOperation(
            method = "prepare",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;addElementsToMeshes(Lnet/minecraft/client/renderer/state/gui/GuiRenderState$TraverseRange;)V",
                    ordinal = 0)
    )
    private void ldlib2$meshAroundFrameCapture(GuiRenderer self, GuiRenderState.TraverseRange range,
                                               Operation<Void> original) {
        ldlib2$frameCaptureDrawIndex = -1;
        ldlib2$frameCaptured = false;
        var state = (IGuiRenderStateExt) renderState;
        var boundary = state.ldlib2$getFrameCaptureStratum();
        if (boundary == Integer.MAX_VALUE) {
            original.call(self, range);
            return;
        }
        if (state.ldlib2$getFirstStratumAfterBlur() == Integer.MAX_VALUE) {
            state.ldlib2$setFirstStratumAfterBlur(boundary);
            try {
                original.call(self, GuiRenderState.TraverseRange.BEFORE_BLUR);
                ldlib2$frameCaptureDrawIndex = meshesToDraw.size();
                original.call(self, GuiRenderState.TraverseRange.AFTER_BLUR);
            } finally {
                // vanilla's own after-blur pass then finds nothing left to mesh, and its draw runs as one range
                state.ldlib2$setFirstStratumAfterBlur(Integer.MAX_VALUE);
            }
        } else {
            original.call(self, range);
            ldlib2$frameCaptureDrawIndex = meshesToDraw.size();
        }
    }

    /// Take the game's copy between the draws {@link #ldlib2$meshAroundFrameCapture} put apart.
    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V",
                    ordinal = 0)
    )
    private void ldlib2$copyFrameBetweenDraws(GuiRenderer self, Supplier<String> label, RenderTarget target,
                                              GpuBufferSlice fogBuffer, GpuBufferSlice dynamicTransforms,
                                              GpuBuffer indexBuffer, VertexFormat.IndexType indexType,
                                              int start, int end, Operation<Void> original) {
        var at = ldlib2$frameCaptureDrawIndex;
        if (ldlib2$frameCaptured || at < start || at >= end) {
            original.call(self, label, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, start, end);
            return;
        }
        if (at > start) {
            original.call(self, label, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, start, at);
        }
        ldlib2$copyFrame(target);
        original.call(self, label, target, fogBuffer, dynamicTransforms, indexBuffer, indexType, at, end);
    }

    /// A screen that blurs the game begins at its blur: copy what is under it before it is blurred.
    @Inject(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V")
    )
    private void ldlib2$copyFrameBeforeBlur(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (!ldlib2$frameCaptured && ldlib2$frameCaptureDrawIndex >= 0
                && ldlib2$frameCaptureDrawIndex <= firstDrawIndexAfterBlur) {
            ldlib2$copyFrame(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    /// Nothing marked where the screen begins — there is none — or nothing drawn after it: the
    /// whole frame is the game.
    @Inject(method = "draw", at = @At("TAIL"))
    private void ldlib2$copyFrameAtEnd(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (!ldlib2$frameCaptured && ldlib2$isGameGuiPass()) {
            ldlib2$frameCaptured = true;
            GameFrameCapture.copyAtEnd(Minecraft.getInstance().getMainRenderTarget());
        }
    }

    @Unique
    private void ldlib2$copyFrame(RenderTarget target) {
        if (!ldlib2$isGameGuiPass()) return;
        ldlib2$frameCaptured = true;
        GameFrameCapture.copyFrom(target);
    }

    /// Only the game's own pass is the game: not a visual layer, not a UI in a window of its own.
    @Unique
    private boolean ldlib2$isGameGuiPass() {
        return renderState == Minecraft.getInstance().gameRenderer.getGameRenderState().guiRenderState
                && IGuiRendererExt.ldlib2$peekTargetOverride() == null;
    }

    /// Cache the latest fog buffer passed to {@code render()} so sub-renderers
    /// (visual layers) can reuse it without needing access to the private
    /// {@code GameRenderer.fogRenderer}. Stored on IGuiRendererExt because
    /// mixins cannot hold public static members.
    @Inject(method = "render", at = @At("HEAD"))
    private void ldlib2$captureFogBuffer(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        IGuiRendererExt.ldlib2$setLastFogBuffer(fogBuffer);
    }
}
