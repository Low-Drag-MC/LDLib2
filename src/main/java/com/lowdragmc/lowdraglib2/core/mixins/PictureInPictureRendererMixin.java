package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.scene.ScenePIPRenderer;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 26.2: vanilla's picture-in-picture renderer allocates its depth attachment as
 * {@code RENDER_ATTACHMENT | COPY_DST} only — it can be neither copied nor sampled. For a {@link ScenePIPRenderer}
 * that depth is a whole world scene's, and two things need to read it back: {@code WorldSceneRenderer}'s own
 * hover/pick depth read ({@code copyTextureToBuffer}, which otherwise falls back to a stale sample), and mods
 * rendering screen-space effects inside the scene (soft particles, scene-depth shader nodes), which copy it
 * before sampling. So scene PIP targets get {@code COPY_SRC} on their depth. Other PIP renderers are untouched.
 */
@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin {

    @ModifyArg(method = "prepareTexturesAndProjection",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/GpuFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
                    ordinal = 1),
            index = 1)
    private int ldlib2$copyableSceneDepth(int usage) {
        return (Object) this instanceof ScenePIPRenderer ? usage | GpuTexture.USAGE_COPY_SRC : usage;
    }
}
