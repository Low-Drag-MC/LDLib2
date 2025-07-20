package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Mixin(ShaderInstance.class)
public interface ShaderInstanceAccessor {
    @Accessor
    List<String> getSamplerNames();
    @Accessor
    Map<String, Uniform> getUniformMap();
    @Accessor
    Map<String, Object> getSamplerMap();
    @Invoker
    static Program invokeGetOrCreate(final ResourceProvider resourceProvider, Program.Type programType, String name) throws IOException {
        throw new AssertionError();
    }
}
