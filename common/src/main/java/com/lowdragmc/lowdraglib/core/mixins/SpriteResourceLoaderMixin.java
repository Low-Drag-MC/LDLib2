package com.lowdragmc.lowdraglib.core.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/7/20
 * @implNote SpriteResourceLoaderMixin
 */
@Mixin(SpriteResourceLoader.class)
public class SpriteResourceLoaderMixin {

    // load ctm textures
    @Inject(method = "list", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;")
    )
    private void injectList(ResourceManager resourceManager, CallbackInfoReturnable<List<Supplier<SpriteContents>>> cir,
                            @Local Map<ResourceLocation, SpriteSource.SpriteSupplier> map,
                            @Local SpriteSource.Output output) {
        for (ResourceLocation spriteName : map.keySet()) {
            var data = LDLMetadataSection.getMetadata(LDLMetadataSection.spriteToAbsolute(spriteName));
            if (data.connection != null) {
                new SingleFile(data.connection, Optional.empty()).run(resourceManager, output);
            }
        }
    }

    // try to load all renderer textures
    @Inject(method = "load", at = @At(value = "RETURN"))
    private static void injectLoad(ResourceManager resourceManager, ResourceLocation location, CallbackInfoReturnable<SpriteResourceLoader> cir,
                                   @Local List<SpriteSource> list) {
        ResourceLocation atlas = new ResourceLocation(location.getNamespace(), "textures/atlas/%s.png".formatted(location.getPath()));
        Set<ResourceLocation> sprites = new HashSet<>();
        for (var renderer : IRenderer.EVENT_REGISTERS) {
            renderer.onPrepareTextureAtlas(atlas, sprites::add);
        }
        for (ResourceLocation sprite : sprites) {
            list.add(new SingleFile(sprite, Optional.empty()));
        }
    }
}
