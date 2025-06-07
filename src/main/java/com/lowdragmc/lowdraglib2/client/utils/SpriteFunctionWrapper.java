package com.lowdragmc.lowdraglib2.client.utils;

import com.lowdragmc.lowdraglib2.client.ClientProxy;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class SpriteFunctionWrapper implements Function<Material, TextureAtlasSprite> {

    private final Function<Material, TextureAtlasSprite> internal;
    private final ResourceLocation modelLocation;

    public SpriteFunctionWrapper(Function<Material, TextureAtlasSprite> internal, ResourceLocation modelLocation) {
        if (internal instanceof SpriteFunctionWrapper wrapper) {
            this.internal = wrapper.internal;
        } else {
            this.internal = internal;
        }
        this.modelLocation = modelLocation;
    }

    @Override
    public TextureAtlasSprite apply(Material material) {
        ClientProxy.textureScraped(modelLocation, material);
        return internal.apply(material);
    }
}
