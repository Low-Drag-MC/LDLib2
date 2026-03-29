package com.lowdragmc.lowdraglib2.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;
public class MsaaTarget {
    @Getter
    private final boolean useDepth;
    @Getter
    private final boolean useStencil;

    @Getter
    private int samples;

    @Getter
    private int width;
    @Getter
    private int height;

    @Getter
    private int frameBufferId;
    @Getter
    private int colorRboId;
    @Getter
    private int depthRboId;

    /**
     * @param samples MSAA sampler (e.g. 2/4/8) will do clamp  [1, GL_MAX_SAMPLES]
     */
    public MsaaTarget(int width, int height, int samples, boolean useDepth, boolean useStencil) {
        this.useDepth = useDepth;
        this.useStencil = useStencil;
        RenderSystem.assertOnRenderThreadOrInit();
        resize(width, height, samples);
    }

    public void resize(int width, int height, int samples) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid size " + width + "x" + height);
        }

        this.width = width;
        this.height = height;
        this.samples = clampSamples(samples);

        destroyBuffers();
        createBuffers();
    }

    public void bindWrite(boolean setViewport) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        if (setViewport) {
            RenderSystem.viewport(0, 0, this.width, this.height);
        }
    }

    public void unbindWrite() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil) {
        RenderSystem.assertOnRenderThreadOrInit();
        bindWrite(false);

        int mask = 0;
        if (clearColor) mask |= GL30.GL_COLOR_BUFFER_BIT;
        if (clearDepth && useDepth) mask |= GL30.GL_DEPTH_BUFFER_BIT;
        if (clearStencil && useDepth && useStencil) mask |= GL30.GL_STENCIL_BUFFER_BIT;

        GlStateManager._clear(mask, Minecraft.ON_OSX);
        unbindWrite();
    }

    /**
     * Resolve (multiple sampling) to a normal RenderTarget (single-sampling texture FBO).
     * @param dst dest RenderTarget
     * @param mask GL_COLOR_BUFFER_BIT / GL_DEPTH_BUFFER_BIT
     */
    public void resolveTo(RenderTarget dst, int mask) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (dst == null) return;

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.frameBufferId);

        int dstW = dst.width;
        int dstH = dst.height;

        // MSAA resolve for color NEAREST；depth/stencil blit must be NEAREST
        GlStateManager._glBlitFrameBuffer(
                0, 0, this.width, this.height,
                0, 0, dstW, dstH,
                mask,
                GL30.GL_NEAREST
        );

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void destroy() {
        RenderSystem.assertOnRenderThreadOrInit();
        destroyBuffers();
    }

    // -----------------
    // Internals
    // -----------------
    private void createBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();

        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);

        // Color: 8-bit RGBA
        this.colorRboId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.colorRboId);
        GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, this.samples, GL30.GL_RGBA8, this.width, this.height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, this.colorRboId);

        // Depth / (Depth+Stencil)
        if (useDepth) {
            this.depthRboId = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.depthRboId);

            int depthInternalFormat = useStencil ? GL30.GL_DEPTH24_STENCIL8 : GL30.GL_DEPTH_COMPONENT24;
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, this.samples, depthInternalFormat, this.width, this.height);

            int attachment = useStencil ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT;
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attachment, GL30.GL_RENDERBUFFER, this.depthRboId);
        }

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            destroyBuffers();
            throw new IllegalStateException("MSAA framebuffer incomplete, status=" + status);
        }

        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void destroyBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();

        if (this.frameBufferId != 0) {
            GlStateManager._glDeleteFramebuffers(this.frameBufferId);
            this.frameBufferId = 0;
        }
        if (this.colorRboId != 0) {
            GL30.glDeleteRenderbuffers(this.colorRboId);
            this.colorRboId = 0;
        }
        if (this.depthRboId != 0) {
            GL30.glDeleteRenderbuffers(this.depthRboId);
            this.depthRboId = 0;
        }
    }

    private static int clampSamples(int requested) {
        RenderSystem.assertOnRenderThreadOrInit();
        int max = GlStateManager._getInteger(GL30.GL_MAX_SAMPLES);
        if (max <= 0) max = 1;
        if (requested <= 1) return 1;
        return Math.min(requested, max);
    }
}
