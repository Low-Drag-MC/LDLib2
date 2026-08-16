package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRendererExt;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.neoforged.neoforge.client.gui.PictureInPictureRendererPool;

import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/// Surgical mixin to support visual-layer sub-renderers and UIs hosted in their own OS window:
/// (1) make `renderState` swappable so a sub-GuiRenderer can adopt a captured sub-state
/// (2) redirect main render target inside `draw()` to allow writing into an off-target
/// (3) redirect the window metrics so an off-target of a different size projects and clips correctly
///
/// 26.2: GuiRenderer no longer holds a MultiBufferSource.BufferSource / SubmitNodeCollector, and
/// `draw()` now resolves the target via `gameRenderer.mainRenderTarget()`.
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements IGuiRendererExt {
    @Shadow @Final @Mutable private GuiRenderState renderState;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> pictureInPictureRendererPools;

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
}
