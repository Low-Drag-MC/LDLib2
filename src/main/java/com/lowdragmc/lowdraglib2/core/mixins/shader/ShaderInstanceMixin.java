package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    /**
     * While defines are active, a PLAIN-name cache hit in the vanilla body would silently reuse a
     * stage compiled WITHOUT the defines (e.g. the vanilla-registered copy of a shared vsh like
     * photon:particle, or a previously built no-defines variant of the same source) — the defines
     * never reach the program and e.g. an instanced variant links the non-instanced attribute
     * layout and renders nothing. Force a fresh compile instead: the ProgramMixin renames the
     * cache key at put time, and the defines-aware lookup above serves later requests.
     */
    @ModifyExpressionValue(method = "getOrCreate", at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object ldlib2$skipPlainCacheWhenDefinesActive(Object original) {
        return LDProgramDefineManager.hasProgramDefines() ? null : original;
    }
}