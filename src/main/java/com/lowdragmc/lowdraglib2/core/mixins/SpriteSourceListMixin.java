package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

/**
 * @author KilaBash
 * @date 2023/7/20
 * @implNote SpriteSourceListMixin
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@Mixin(SpriteSourceList.class)
public abstract class SpriteSourceListMixin {

    // try to load all renderer textures
    @Inject(method = "load", at = @At(value = "RETURN"))
    private static void ldlib2$injectLoad(ResourceManager resourceManager, ResourceLocation location, CallbackInfoReturnable<SpriteResourceLoader> cir,
                                   @Local List<SpriteSource> list) {
        ResourceLocation atlas = location.withPath("textures/atlas/%s.png"::formatted);
        Set<ResourceLocation> sprites = new HashSet<>();
        for (var renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onPrepareTextureAtlas(atlas, sprites::add);
        }
        for (ResourceLocation sprite : sprites) {
            list.add(new SingleFile(sprite, Optional.empty()));
        }
    }
}
