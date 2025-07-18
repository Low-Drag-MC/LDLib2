package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.shader.LDProgramDefineManager;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GlslPreprocessor.class)
public abstract class GlslPreprocessorMixin {
    @Inject(method = "process", at = @At(value = "RETURN"))
    private void ldlib2$appendDefines(String shaderData, CallbackInfoReturnable<List<String>> cir) {
        var lines = cir.getReturnValue();
        if (LDProgramDefineManager.hasProgramDefines()) {
            lines.add(1, LDProgramDefineManager.createProgramDefinesString());
        }
    }
}
