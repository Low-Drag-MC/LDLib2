package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public abstract class LanguageMixin {

    @Inject(method = "getOrDefault", at = @At(value = "HEAD"), cancellable = true)
    private void injectGet(String key, String defaultText, CallbackInfoReturnable<String> cir) {
        if (LocalizationUtils.RESOURCE != null && LocalizationUtils.RESOURCE.hasBuiltinResource(key)) {
            cir.setReturnValue(LocalizationUtils.RESOURCE.getBuiltinResource(key));
        }
    }

}
