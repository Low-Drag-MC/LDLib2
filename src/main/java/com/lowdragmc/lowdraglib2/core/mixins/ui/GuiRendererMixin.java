package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowdragmc.lowdraglib2.client.GameFrameCapture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRenderStateExt;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRendererExt;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IPreciseScissor;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
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
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Surgical mixin to support visual-layer sub-renderers and UIs hosted in their own OS window:
/// (1) make `renderState` swappable so a sub-GuiRenderer can adopt a captured sub-state
/// (2) redirect main render target inside `draw()` to allow writing into an off-target
/// (3) redirect the window metrics so an off-target of a different size projects and clips correctly
/// (4) split the game's pass where its screen begins, for {@link GameFrameCapture} to copy the game there
///
/// 26.2: GuiRenderer no longer holds a MultiBufferSource.BufferSource / SubmitNodeCollector, and
/// `draw()` now resolves the target via `gameRenderer.mainRenderTarget()`.
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements IGuiRendererExt {
    @Shadow @Final @Mutable private GuiRenderState renderState;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pictureInPictureRendererPools;
    @Shadow @Final private List<?> draws;
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
    public FeatureRenderDispatcher ldlib2$getFeatureRenderDispatcher() {
        return this.featureRenderDispatcher;
    }

    @Override
    public Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> ldlib2$getPictureInPictureRendererPools() {
        return this.pictureInPictureRendererPools;
    }

    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget ldlib2$redirectRenderTarget(GameRenderer gameRenderer) {
        RenderTarget override = IGuiRendererExt.ldlib2$peekTargetOverride();
        return override != null ? override : gameRenderer.mainRenderTarget();
    }

    /// Resolve every size-dependent decision against the surface being drawn into.
    ///
    /// 26.2 funnels all four of them — the orthographic projection, the scissor box, the gui scale
    /// handed to picture-in-picture renderers, and the one the item atlas is rasterised at —
    /// through this one field, so substituting it is enough where 26.1 needed a patch per call site.
    /// It is also what keeps them consistent with each other: patching only the projection leaves a
    /// clipped element with a box flipped against the wrong height, which does not draw at all.
    ///
    /// Null outside a hosted window, so the game's own frame takes vanilla's path untouched.
    @Redirect(
            method = {
                    "draw",
                    "enableScissor",
                    "preparePictureInPicture",
                    "getGuiScaleInvalidatingItemAtlasIfChanged"
            },
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/renderer/state/GameRenderState;windowRenderState:Lnet/minecraft/client/renderer/state/WindowRenderState;")
    )
    private WindowRenderState ldlib2$redirectWindowState(GameRenderState gameRenderState) {
        WindowRenderState override = IGuiRendererExt.ldlib2$peekWindowOverride();
        return override != null ? override : gameRenderState.windowRenderState;
    }

    /// Quantise a clip rectangle against the physical pixel grid rather than the gui one.
    ///
    /// A {@code ScreenRectangle} is an integer in gui units, so at guiScale 3 the clip edge can only
    /// fall on every third physical pixel the target has. Where LDLib recorded the unrounded
    /// rectangle ({@link IPreciseScissor}), that is quantised here instead — once, against the real
    /// grid — so a clip inside a zoomed graph view tracks the content instead of snapping to the gui
    /// pixel grid as it pans.
    ///
    /// This is the only half of 26.1's version left. The other half was clipping against the right
    /// *target* for a hosted window, and the {@code windowRenderState} redirect above already does
    /// that for vanilla's own {@code enableScissor} — so an untagged rectangle needs no help here and
    /// is left to take vanilla's path untouched.
    ///
    /// One scale factor, not the ratio 26.1 had to reconstruct: 26.2 builds the projection from this
    /// same state as `width / guiScale` exactly, on the game window and a hosted one alike, so a gui
    /// unit is exactly {@code guiScale} pixels either way.
    @Inject(method = "enableScissor", at = @At("HEAD"), cancellable = true)
    private void ldlib2$scissorPrecisely(ScreenRectangle rectangle, RenderPass renderPass, CallbackInfo ci) {
        var clip = IPreciseScissor.of(rectangle);
        if (clip == null) return;

        // Resolved here rather than through the redirect above: cancelling at HEAD means vanilla's
        // own read of the field never runs.
        var override = IGuiRendererExt.ldlib2$peekWindowOverride();
        var window = override != null ? override
                : Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;

        // An empty box means this draw is invisible, which 26.2 has no way to say: RenderPass throws
        // on a zero-area scissor rather than clipping everything away as the GL call behind 26.1's
        // did. See PreciseScissor#atLeastOnePixel for why the smallest legal box is the same answer.
        var box = PreciseScissor.atLeastOnePixel(
                PreciseScissor.quantize(clip, window.guiScale, window.guiScale, window.width, window.height),
                window.width, window.height);
        // Still empty only for a target with no area at all, which has no pixel to describe and none
        // to draw into either. Cancelling without a scissor is right there; falling through to
        // vanilla would hand the same degenerate numbers to the same throwing call.
        if (box.width() > 0 && box.height() > 0) {
            renderPass.enableScissor(box.x(), box.y(), box.width(), box.height());
        }
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
                ldlib2$frameCaptureDrawIndex = draws.size();
                original.call(self, GuiRenderState.TraverseRange.AFTER_BLUR);
            } finally {
                // vanilla's own after-blur pass then finds nothing left to mesh, and its draw runs as one range
                state.ldlib2$setFirstStratumAfterBlur(Integer.MAX_VALUE);
            }
        } else {
            original.call(self, range);
            ldlib2$frameCaptureDrawIndex = draws.size();
        }
    }

    /// Take the game's copy between the draws {@link #ldlib2$meshAroundFrameCapture} put apart.
    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;II)V",
                    ordinal = 0)
    )
    private void ldlib2$copyFrameBetweenDraws(GuiRenderer self, Supplier<String> label, RenderTarget target,
                                              GpuBufferSlice dynamicTransforms, int start, int end,
                                              Operation<Void> original) {
        var at = ldlib2$frameCaptureDrawIndex;
        if (ldlib2$frameCaptured || at < start || at >= end) {
            original.call(self, label, target, dynamicTransforms, start, end);
            return;
        }
        if (at > start) {
            original.call(self, label, target, dynamicTransforms, start, at);
        }
        // between two render passes: the copy is a pass of its own and cannot be taken inside one
        ldlib2$copyFrame(target);
        original.call(self, label, target, dynamicTransforms, at, end);
    }

    /// A screen that blurs the game begins at its blur: copy what is under it before it is blurred.
    @Inject(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V")
    )
    private void ldlib2$copyFrameBeforeBlur(CallbackInfo ci) {
        if (!ldlib2$frameCaptured && ldlib2$frameCaptureDrawIndex >= 0
                && ldlib2$frameCaptureDrawIndex <= firstDrawIndexAfterBlur) {
            ldlib2$copyFrame(Minecraft.getInstance().gameRenderer.mainRenderTarget());
        }
    }

    /// Nothing marked where the screen begins — there is none — or nothing drawn after it: the
    /// whole frame is the game.
    @Inject(method = "draw", at = @At("TAIL"))
    private void ldlib2$copyFrameAtEnd(CallbackInfo ci) {
        if (!ldlib2$frameCaptured && ldlib2$isGameGuiPass()) {
            ldlib2$frameCaptured = true;
            GameFrameCapture.copyAtEnd(Minecraft.getInstance().gameRenderer.mainRenderTarget());
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
        return renderState == Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState
                && IGuiRendererExt.ldlib2$peekTargetOverride() == null;
    }
}
