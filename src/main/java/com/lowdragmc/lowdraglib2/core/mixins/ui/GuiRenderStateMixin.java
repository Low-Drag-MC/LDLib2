package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGuiRenderStateExt;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/// Remembers where the game's copy for {@code GameFrameCapture} is taken; see
/// {@link GuiRendererMixin} for where it is acted on.
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements IGuiRenderStateExt {
    @Shadow @Final private List<?> strata;
    @Shadow private int firstStratumAfterBlur;

    @Shadow public abstract void nextStratum();

    @Unique
    private int ldlib2$frameCaptureStratum = Integer.MAX_VALUE;

    @Override
    public void ldlib2$markFrameCaptureBoundary() {
        if (ldlib2$frameCaptureStratum != Integer.MAX_VALUE) return;
        // a stratum of its own, so nothing extracted from here on can land in a node the copy draws
        nextStratum();
        ldlib2$frameCaptureStratum = strata.size() - 1;
    }

    @Override
    public int ldlib2$getFrameCaptureStratum() {
        return ldlib2$frameCaptureStratum;
    }

    @Override
    public int ldlib2$getFirstStratumAfterBlur() {
        return firstStratumAfterBlur;
    }

    @Override
    public void ldlib2$setFirstStratumAfterBlur(int stratum) {
        firstStratumAfterBlur = stratum;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void ldlib2$resetFrameCapture(CallbackInfo ci) {
        ldlib2$frameCaptureStratum = Integer.MAX_VALUE;
    }
}
