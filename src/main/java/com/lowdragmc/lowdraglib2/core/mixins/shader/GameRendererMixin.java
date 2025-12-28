package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "reloadShaders", at = {@At(value = "HEAD")})
    private static void ldlib$reloadShaders(ResourceProvider resourceProvider, CallbackInfo ci) {
        LDLibShaders.GEOMETRY_TYPE.getPrograms().values().forEach(Program::close);
    }
}
