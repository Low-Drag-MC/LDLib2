package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.opengl.GlBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected {@code handle} field of {@link GlBuffer} so we can read the underlying
 * GL buffer id from outside the {@code com.mojang.blaze3d.opengl} package. Used by
 * {@code GlCommandEncoderMixin} when implementing depth-aware {@code copyTextureToBuffer}.
 */
@Mixin(GlBuffer.class)
public interface GlBufferAccessor {
    @Accessor("handle")
    int ldlib2$getHandle();
}
