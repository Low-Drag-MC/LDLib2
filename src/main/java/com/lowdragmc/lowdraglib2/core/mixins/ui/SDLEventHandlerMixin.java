package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.mojang.blaze3d.platform.SDLEventHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Files dragged over the game window from outside. SDL reports where they are while they move, which
 * vanilla hands to {@code Window#handleEvent} and ignores, and completes the drag whether they were
 * dropped or taken away again; LDLib's UIs use both to show where a drop would land.
 *
 * <p>Only the game window's events get here: those of LDLib's own windows are taken out of the queue
 * first, see {@code OsWindowEvents}. Dispatched through {@code Minecraft#execute} like vanilla's own
 * input, so the end of a drag stays ordered before the drop it may carry.
 */
@Mixin(SDLEventHandler.class)
public class SDLEventHandlerMixin {

    @Inject(method = "pollEvents()V", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;handleEvent(Lorg/lwjgl/sdl/SDL_Event;)V"))
    private void ldlib2$fileDragPosition(CallbackInfo ci, @Local SDL_Event event) {
        if (event.type() != SDLEvents.SDL_EVENT_DROP_POSITION) return;
        var x = event.drop().x();
        var y = event.drop().y();
        Minecraft.getInstance().execute(() -> ModularUIClientAccess.onGameWindowFileDrag(x, y));
    }

    @Inject(method = "handleDropCompleteEvent(Lorg/lwjgl/sdl/SDL_Event;)V", at = @At("HEAD"))
    private void ldlib2$fileDragEnd(SDL_Event event, CallbackInfo ci) {
        Minecraft.getInstance().execute(ModularUIClientAccess::onGameWindowFileDragEnd);
    }
}
