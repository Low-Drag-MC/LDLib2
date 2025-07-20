package com.lowdragmc.lowdraglib2.client.shader;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ShaderInstanceAccessor;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

public interface ILDShaderInstance {

    default ShaderInstanceAccessor getShaderInstanceAccessor() {
        return (ShaderInstanceAccessor) this;
    }

    default void onCreateShader(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat, JsonObject json) {}

}
