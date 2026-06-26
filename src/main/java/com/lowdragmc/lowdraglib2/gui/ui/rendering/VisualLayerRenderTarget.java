package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public class VisualLayerRenderTarget extends RenderTarget {
    public VisualLayerRenderTarget() {
        // 26.2: RenderTarget ctor now takes the color GpuFormat.
        super("ldlib2-visual-layer", true, GpuFormat.RGBA8_UNORM);
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
