package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public class UIVisualLayer {
    private final UIElement element;
    @Nullable
    private MainTarget target;
    @Nullable
    private TextureTarget mask;

    public UIVisualLayer(UIElement element) {
        this.element = element;
    }

    public void release() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        if (mask != null) {
            mask.destroyBuffers();
            mask = null;
        }
    }

    private void ensureTargetValid(int width, int height) {
        if (target == null) {
            target = new MainTarget(width, height);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        target.enableStencil();
    }

    public void clear() {
        if (target != null) {
            RenderSystem.clearColor(0, 0, 0, 0);
            int i = 16384;
            if (target.useDepth) {
                RenderSystem.clearDepth(1.0);
                i |= 256;
            }
            RenderSystem.clear(i, Minecraft.ON_OSX);
        }
    }

    public void bind() {
        var width = element.getSizeWidth();
        var height = element.getSizeHeight();
        ensureTargetValid(Math.max(1, (int) Math.ceil(width)), Math.max(1, (int) Math.ceil(height)));
        assert target != null;
        target.bindWrite(false);
    }

    public void unbind() {
        if (target != null) {
            target.unbindWrite();
        }
    }

    public int textureId() {
        if (target == null) return -1;
        return target.getColorTextureId();
    }

    private void drawMask() {

    }

    public void draw(PoseStack poseStack, float x, float y, float width, float height, float opacity) {
        var blitShader = LDLibShaders.getVisualLayerShader();
        blitShader.setSampler("DiffuseSampler", textureId());
        blitShader.safeGetUniform("Opacity").set(opacity);
        if (mask != null) {
            blitShader.setSampler("Mask", mask.getColorTextureId());
            blitShader.safeGetUniform("HasMask").set(1f);
        } else {
            blitShader.safeGetUniform("HasMask").set(0f);
        }

        blitShader.apply();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        var pose = poseStack.last().pose();
        bufferbuilder.addVertex(pose, x, y + height, 0).setUv(0, 1);
        bufferbuilder.addVertex(pose, x + width, y + height, 0).setUv(1, 1);
        bufferbuilder.addVertex(pose, x + width, y, 0).setUv(1, 0);
        bufferbuilder.addVertex(pose, x, y, 0).setUv(0, 0);
        BufferUploader.draw(bufferbuilder.buildOrThrow());

        blitShader.clear();
    }

}
