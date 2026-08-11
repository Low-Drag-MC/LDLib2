package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.gui.ui.utils.RawInputGate;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The keyboard half of {@link RawInputGate}. Neither of these callbacks checks whether the window is
 * active, so without this a keystroke aimed at the game window reaches a scripted run that is using
 * it.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$dropOsKey(long windowPointer, int key, int scanCode, int action, int modifiers,
                                  CallbackInfo ci) {
        if (RawInputGate.isBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped(JII)V", at = @At("HEAD"), cancellable = true)
    private void ldlib2$dropOsChar(long windowPointer, int codePoint, int modifiers, CallbackInfo ci) {
        if (RawInputGate.isBlocked()) {
            ci.cancel();
        }
    }
}
