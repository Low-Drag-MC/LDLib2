package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;

public class UIVisualLayer {
    private static final Queue<MainTarget> TARGET_POOL = new ArrayDeque<>();
    private static final Queue<TextureTarget> MASK_POOL = new ArrayDeque<>();
    private static final int MAX_POOL_SIZE = 10;

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
            if (TARGET_POOL.size() < MAX_POOL_SIZE) {
                TARGET_POOL.offer(target);
            } else {
                target.destroyBuffers();
            }
            target = null;
        }
        if (mask != null) {
            if (MASK_POOL.size() < MAX_POOL_SIZE) {
                MASK_POOL.offer(mask);
            } else {
                mask.destroyBuffers();
            }
            mask = null;
        }
    }

    private void ensureTargetValid(int width, int height) {
        if (target == null) {
            target = TARGET_POOL.poll();
            if (target == null) {
                target = new MainTarget(width, height);
            }
        }
        if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private void ensureMaskValid(int width, int height) {
        if (mask == null) {
            mask = MASK_POOL.poll();
            if (mask == null) {
                mask = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            }
        }
        if (mask.width != width || mask.height != height) {
            mask.resize(width, height, Minecraft.ON_OSX);
        }
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

    public void bind(GUIContext guiContext) {
        ensureTargetValid(guiContext.mc.getMainRenderTarget().width, guiContext.mc.getMainRenderTarget().height);
        assert target != null;

        var overflowClip = element.getStyle().overflowClip();
        // render mask first
        var hasClip = overflowClip != IGuiTexture.EMPTY;
        if (hasClip) {
            drawMask(guiContext, overflowClip);
        }

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

    private void drawMask(GUIContext guiContext, IGuiTexture maskTexture) {
        assert target != null;
        ensureMaskValid(target.width, target.height);
        if (mask != null) {
            mask.bindWrite(false);
            RenderSystem.clearColor(0, 0, 0, 0);
            int i = 16384;
            RenderSystem.clear(i, Minecraft.ON_OSX);

            var x = element.getPositionX();
            var y = element.getPositionY();
            var width = element.getSizeWidth();
            var height = element.getSizeHeight();
            guiContext.drawTexture(maskTexture, x, y, width, height);
        }
    }

    public void draw(GUIContext guiContext) {
        var opacity = element.getStyle().opacity();
        var hasClip = element.getStyle().overflowClip() != IGuiTexture.EMPTY;

        var x = element.getPositionX();
        var y = element.getPositionY();
        var width = element.getSizeWidth();
        var height = element.getSizeHeight();

        RenderSystem.setShader(LDLibShaders::getVisualLayerShader);
        RenderSystem.setShaderTexture(0, textureId());
        var blitShader = LDLibShaders.getVisualLayerShader();

//        blitShader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
//        blitShader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
//        blitShader.setSampler("DiffuseSampler", textureId());
        blitShader.safeGetUniform("Opacity").set(opacity);
        if (hasClip && mask != null) {
            blitShader.setSampler("Mask", mask.getColorTextureId());
            blitShader.safeGetUniform("HasMask").set(1f);
        } else {
            blitShader.safeGetUniform("HasMask").set(0f);
        }

//        blitShader.apply();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        var pose = guiContext.pose.last().pose();
        bufferbuilder.addVertex(pose, x, y + height, 0);
        bufferbuilder.addVertex(pose, x + width, y + height, 0);
        bufferbuilder.addVertex(pose, x + width, y, 0);
        bufferbuilder.addVertex(pose, x, y, 0);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());

//        blitShader.clear();
    }

}
