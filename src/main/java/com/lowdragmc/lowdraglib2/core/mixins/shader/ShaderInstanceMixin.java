package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.client.shader.ILDShaderInstance;
import com.lowdragmc.lowdraglib2.client.shader.LDProgramDefineManager;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin implements ILDShaderInstance {
    @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
            require = 1,
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ShaderInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;",
                    ordinal = 1
            )})
    public void ldlib2$onCreateShader(ResourceProvider resourceProvider,
                                      ResourceLocation shaderLocation,
                                      VertexFormat vertexFormat,
                                      CallbackInfo ci,
                                      @Local JsonObject json) throws IOException {
        this.onCreateShader(resourceProvider, shaderLocation, vertexFormat, json);
    }

    @Inject(method = "getOrCreate", at = {@At(value = "HEAD")}, cancellable = true)
    private static void ldlib2$getOrCreate(ResourceProvider resourceProvider,
                                           Program.Type programType,
                                           String name,
                                           CallbackInfoReturnable<Program> cir) {
        if (LDProgramDefineManager.hasProgramDefines()) {
            var program = programType.getPrograms().get(LDProgramDefineManager.createProgramNameWithDefines(name));
            if (program != null) cir.setReturnValue(program);
        }
    }
}