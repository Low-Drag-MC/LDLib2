package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.GlBufferAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@code GlCommandEncoder.copyTextureToBuffer} hard-wires the source texture into
 * the read FBO's {@code GL_COLOR_ATTACHMENT0} slot, then calls
 * {@code glReadPixels(..., GL_DEPTH_COMPONENT, ...)} when the format is a depth format. The
 * depth attachment slot is left empty, so {@code glReadPixels} fails with
 * {@code GL_INVALID_FRAMEBUFFER_OPERATION (1286)} for any depth texture.
 * <p>
 * This mixin detects depth-aspect formats at the entry of {@code copyTextureToBuffer} and
 * runs an equivalent flow that attaches the source to {@code GL_DEPTH_ATTACHMENT} instead,
 * making depth read-back work. Color readback is left untouched (falls through to vanilla).
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {

    @Final
    @Shadow
    private int readFbo;

    @Inject(
            method = "copyTextureToBuffer(Lcom/mojang/blaze3d/textures/GpuTexture;Lcom/mojang/blaze3d/buffers/GpuBuffer;JLjava/lang/Runnable;IIIII)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ldlib2$supportDepthCopy(GpuTexture source, GpuBuffer destination, long offset,
                                         Runnable callback, int mipLevel, int x, int y, int width, int height,
                                         CallbackInfo ci) {
        if (!source.getFormat().hasDepthAspect()) return;

        int sourceGlId = ((GlTexture) source).glId();
        int destHandle = ((GlBufferAccessor) (Object) destination).ldlib2$getHandle();

        GlStateManager.clearGlErrors();
        // GL_READ_FRAMEBUFFER = 36008, GL_DEPTH_ATTACHMENT = 36096, GL_COLOR_ATTACHMENT0 = 36064,
        // GL_TEXTURE_2D = 3553, GL_PIXEL_PACK_BUFFER = 35051, GL_PACK_ROW_LENGTH = 3330.
        GlStateManager._glBindFramebuffer(36008, this.readFbo);
        // Attach source as DEPTH (vanilla attaches as COLOR which is what crashes for depth formats).
        GlStateManager._glFramebufferTexture2D(36008, 36096, 3553, sourceGlId, mipLevel);
        // Make sure no stale color attachment confuses the completeness check.
        GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, mipLevel);
        GlStateManager._glBindBuffer(35051, destHandle);
        GlStateManager._pixelStore(3330, width);
        GlStateManager._readPixels(x, y, width, height,
                GlConst.toGlExternalId(source.getFormat()),
                GlConst.toGlType(source.getFormat()), offset);
        RenderSystem.queueFencedTask(callback);
        // Detach depth before unbinding.
        GlStateManager._glFramebufferTexture2D(36008, 36096, 3553, 0, mipLevel);
        GlStateManager._glBindFramebuffer(36008, 0);
        GlStateManager._glBindBuffer(35051, 0);
        int error = GlStateManager._getError();
        if (error != 0) {
            throw new IllegalStateException("Couldn't perform depth copyTobuffer for texture "
                    + source.getLabel() + ": GL error " + error);
        }

        ci.cancel();
    }
}
