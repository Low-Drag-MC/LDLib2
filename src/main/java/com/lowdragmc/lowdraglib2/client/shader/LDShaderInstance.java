package com.lowdragmc.lowdraglib2.client.shader;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ShaderInstanceAccessor;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.*;

public class LDShaderInstance extends ShaderInstance implements ILDShaderInstance {
    public final ResourceLocation shaderLocation;
    public final Set<String> defines;
    @Getter
    @Nullable
    private Program geometry;
    // runtime
    @Nullable
    private LDShaderHolder holder;
    @Getter
    private boolean isSamplerCacheDirty = true;

    @Nullable
    public static LDShaderInstance create(ResourceLocation location, VertexFormat format) throws Throwable {
        return create(location, format, Collections.emptySet());
    }

    @Nullable
    public static LDShaderInstance create(ResourceLocation location, VertexFormat format, Set<String> defines) throws Throwable {
        return create(Minecraft.getInstance().getResourceManager(), location, format, defines);
    }

    /**
     * As {@link #create(ResourceLocation, VertexFormat, Set)} but reading the shader assets from an explicit
     * {@link ResourceProvider} instead of {@code Minecraft.getResourceManager()} — so a caller can serve a shader
     * that isn't a shipped asset (e.g. generated in-memory), while still getting the LDLib2 shader features
     * (defines, dynamic uniforms/samplers, geometry stage). The provider must resolve
     * {@code <ns>:shaders/core/<path>.json} plus the {@code .vsh}/{@code .fsh} it names and any {@code #moj_import}
     * includes (delegate those to the game resource manager).
     */
    @Nullable
    public static LDShaderInstance create(ResourceProvider resourceProvider, ResourceLocation location, VertexFormat format, Set<String> defines) throws Throwable {
        for (var define : defines) {
            LDProgramDefineManager.addProgramDefine(define);
        }
        var resourcelocation = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "shaders/core/" + location.getPath() + ".json");
        if (resourceProvider.getResource(resourcelocation).isEmpty()) return null;
        var shaderWithDefines = new LDShaderInstance(resourceProvider, location, format, defines);
        LDProgramDefineManager.clearProgramDefines();
        return shaderWithDefines;
    }

    private LDShaderInstance(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat, Set<String> defines) throws IOException {
        super(resourceProvider, shaderLocation, vertexFormat);
        this.shaderLocation = shaderLocation;
        this.defines = defines;
    }

    protected void setHolder(@Nonnull LDShaderHolder holder) {
        this.holder = holder;
    }

    @Override
    public void onCreateShader(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat vertexFormat, JsonObject json) {
        var geometryShader = GsonHelper.getAsString(json, "geometry", null);
        if (geometryShader != null) {
            try {
                this.geometry = ShaderInstanceAccessor.invokeGetOrCreate(resourceProvider, LDLibShaders.GEOMETRY_TYPE, geometryShader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void attachToProgram() {
        super.attachToProgram();
        if (this.geometry != null) {
            this.geometry.attachToShader(this);
        }
    }

    protected void markSamplerCacheDirty() {
        isSamplerCacheDirty = true;
    }

    @Override
    public void apply() {
        if (isSamplerCacheDirty) {
            applySamplers();
        }
        if (holder != null) {
            holder.dynamicSampler.forEach((name, supplier) ->
                    getShaderInstanceAccessor().getSamplerMap().put(name, supplier.get()));

            holder.dynamicUniform.forEach((name, consumer) -> {
                var uniform = getUniform(name);
                if (uniform != null) consumer.accept(uniform);
            });
        }
        super.apply();
    }

    public void applySamplers() {
        if (holder != null) {
            for (var entry : holder.samplerCache.entrySet()) {
                var name = entry.getKey();
                var sampler = entry.getValue();
                if (sampler instanceof ResourceLocation location) {
                    setSampler(name, Minecraft.getInstance().getTextureManager().getTexture(location));
                } else {
                    setSampler(name, sampler);
                }
            }
        }
        isSamplerCacheDirty = false;
    }
}
