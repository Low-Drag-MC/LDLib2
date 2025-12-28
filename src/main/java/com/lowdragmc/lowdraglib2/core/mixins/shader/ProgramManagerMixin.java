package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.lowdragmc.lowdraglib2.client.shader.LDShaderInstance;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Shader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgramManager.class)
public class ProgramManagerMixin {
    @Inject(method = "releaseProgram", at = {@At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V",
            remap = false)})
    private static void ldlib$releaseGeometry(Shader shader, CallbackInfo ci) {
        if (shader instanceof LDShaderInstance ldShaderInstance && ldShaderInstance.getGeometry() != null) {
            ldShaderInstance.getGeometry().close();
        }
    }
}
