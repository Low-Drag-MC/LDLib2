package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL30;
public class HDRTarget extends RenderTarget {
    @Getter
    private int attachedDepthTexture = -1;
    public HDRTarget(int width, int height) {
        this(width, height, GL30.GL_NEAREST, true);
    }

    public HDRTarget(int width, int height, int filterMode, boolean useDepth) {
        super(useDepth);
        this.filterMode = filterMode;
        RenderSystem.assertOnRenderThreadOrInit();
        this.resize(width, height, Minecraft.ON_OSX);
    }

    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        int i = RenderSystem.maxSupportedTextureSize();
        if (width > 0 && width <= i && height > 0 && height <= i) {
            this.viewWidth = width;
            this.viewHeight = height;
            this.width = width;
            this.height = height;
            this.frameBufferId = GlStateManager.glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            if (this.useDepth) {
                this.depthBufferId = TextureUtil.generateTextureId();
                GlStateManager._bindTexture(this.depthBufferId);
                GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
                GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
                GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_COMPARE_MODE, GL30.GL_NONE);
                GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
                GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
                if (!false)
                    GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT, this.width, this.height, 0, GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, null);
                else
                    GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH32F_STENCIL8, this.width, this.height, 0, org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV, null);
            }

            this.setFilterMode(this.filterMode, true);
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, this.width, this.height, 0, GL30.GL_RGBA, GL30.GL_FLOAT, null);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, this.colorTextureId, 0);
            if (this.useDepth) {
                attachDepthBufferInternal(this.depthBufferId,
                        false,
                        false);
            }
            this.attachedDepthTexture = -1;

            this.checkStatus();
            this.clear(clearError);
            this.unbindRead();
        } else {
            throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void setFilterMode(int filterMode, boolean force) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (force || filterMode != this.filterMode) {
            this.filterMode = filterMode;
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, filterMode);
            GlStateManager._texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, filterMode);
            GlStateManager._bindTexture(0);
        }
    }

    public void attachDepthBuffer(int depthTexture) {
        int previousTextureBinding = GlStateManager._getInteger(GL30.GL_TEXTURE_BINDING_2D);
        GlStateManager._bindTexture(depthTexture);
        int parameter = GlStateManager._getTexLevelParameter(3553, 0, GL30.GL_TEXTURE_INTERNAL_FORMAT);
        GlStateManager._bindTexture(previousTextureBinding);
        var useStencil = false;
        var useCombinedDepthStencil = false;
        switch (parameter) {
            case 34041, 36013, 35056 -> {
                useStencil = true;
                useCombinedDepthStencil = true;
            }
        }
        attachDepthBufferInternal(depthTexture, useStencil, useCombinedDepthStencil);
    }

    public void attachDepthBuffer(RenderTarget srcTarget) {
        attachDepthBufferInternal(srcTarget.getDepthTextureId(), false, false);
    }

    public void attachDepthBufferInternal(int depthTexture, boolean useStencil, boolean useCombinedDepthStencil) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId);
        if (!useStencil)
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_TEXTURE_2D, depthTexture, 0);
        else if (useCombinedDepthStencil) {
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_TEXTURE_2D, depthTexture, 0);
        } else {
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_TEXTURE_2D, depthTexture, 0);
            GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_TEXTURE_2D, depthTexture, 0);
        }
        attachedDepthTexture = depthTexture;
    }

    public boolean hasOtherAttachedDepthTexture() {
        return attachedDepthTexture != -1 && attachedDepthTexture != this.depthBufferId;
    }

    public void restoreDepthTexture() {
        if (hasOtherAttachedDepthTexture()) {
            attachDepthBufferInternal(this.depthBufferId,
                    false,
                    false);
        }
    }

    public void copyFromInternal(int id, int srcWidth, int srcHeight, int mask, int filter) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, id);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, srcWidth, srcHeight,
                0, 0, this.width, this.height, mask, filter);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void copyDepthFrom(RenderTarget otherTarget) {
        copyDepthFrom(otherTarget.frameBufferId, otherTarget.width, otherTarget.height);
    }

    public void copyDepthFrom(int id, int srcWidth, int srcHeight) {
        copyFromInternal(id, srcWidth, srcHeight, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
    }

    public void copyColorFrom(RenderTarget otherTarget) {
        copyColorFrom(otherTarget.frameBufferId, otherTarget.width, otherTarget.height);
    }

    public void copyColorFrom(int id, int srcWidth, int srcHeight) {
        copyFromInternal(id, srcWidth, srcHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
    }

    public void copyDepthAndColorFrom(RenderTarget otherTarget) {
        copyDepthAndColorFrom(otherTarget.frameBufferId, otherTarget.width, otherTarget.height);
    }

    public void copyDepthAndColorFrom(int id, int srcWidth, int srcHeight) {
        copyFromInternal(id, srcWidth, srcHeight, GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
    }
}
