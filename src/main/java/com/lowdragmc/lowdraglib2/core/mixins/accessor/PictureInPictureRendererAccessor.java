package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The textures a picture-in-picture renderer draws into. Since 26.3 the base class opens its own render
 * pass on them after {@code renderToTexture} and hands that method only a submit collector, so a
 * renderer that draws immediately — a whole world scene, a nested gui renderer — reads them from here
 * and opens its own passes on them.
 */
@Mixin(PictureInPictureRenderer.class)
public interface PictureInPictureRendererAccessor {
    @Accessor("textureView")
    GpuTextureView ldlib2$getTextureView();

    @Accessor("depthTextureView")
    GpuTextureView ldlib2$getDepthTextureView();
}
