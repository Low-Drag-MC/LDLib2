package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes {@code guiWidth()}/{@code guiHeight()} report the surface actually being drawn into.
 *
 * <p>Both read {@code Minecraft#getWindow()} directly, which is correct exactly as long as there is
 * one place a UI can appear. A {@code ModularUI} hosted in its own OS window is drawn into an
 * off-screen target of a different size, and these two are the only thing vanilla's tooltip code has
 * to go on: they decide where a tooltip is flipped away from an edge and how wide its text wraps. Off
 * by the difference between the two windows, a tooltip near the small window's right edge is never
 * flipped and simply runs off it.
 *
 * <p>Scoped as tightly as it looks: {@link UISurface#current()} is only ever something other than the
 * game window inside {@code ModularUI}'s own render pass, so outside one — and for every UI drawn in
 * the game window — this changes nothing. Within {@code GuiGraphics} these two methods are used by
 * nothing but tooltip positioning and tooltip wrapping.
 *
 * @see com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Inject(method = "guiWidth", at = @At("HEAD"), cancellable = true)
    private void ldlib2$guiWidth(CallbackInfoReturnable<Integer> cir) {
        var surface = UISurface.current();
        if (!surface.isMainWindow()) {
            cir.setReturnValue(surface.guiScaledWidth());
        }
    }

    @Inject(method = "guiHeight", at = @At("HEAD"), cancellable = true)
    private void ldlib2$guiHeight(CallbackInfoReturnable<Integer> cir) {
        var surface = UISurface.current();
        if (!surface.isMainWindow()) {
            cir.setReturnValue(surface.guiScaledHeight());
        }
    }
}
