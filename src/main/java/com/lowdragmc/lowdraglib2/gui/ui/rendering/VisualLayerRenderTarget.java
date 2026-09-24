package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

public class VisualLayerRenderTarget extends RenderTarget {
    public VisualLayerRenderTarget() {
        // Borrows the picture-in-picture renderer's textures (see bind), so the formats only describe
        // them; RGBA8 colour and a depth attachment are what that renderer allocates.
        super("ldlib2-visual-layer", GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    }

    public void bind(GpuTextureView color, GpuTexture colorTex,
                     GpuTextureView depth, GpuTexture depthTex,
                     int width, int height) {
        this.colorTextureView = color;
        this.colorTexture = colorTex;
        this.depthTextureView = depth;
        this.depthTexture = depthTex;
        this.width = width;
        this.height = height;
    }

    public void unbind() {
        this.colorTextureView = null;
        this.colorTexture = null;
        this.depthTextureView = null;
        this.depthTexture = null;
    }

    @Override
    public void destroyBuffers() {
        // PIP renderer owns the GpuTexture lifecycle; we only borrow views.
    }

    @Override
    public void createBuffers(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
