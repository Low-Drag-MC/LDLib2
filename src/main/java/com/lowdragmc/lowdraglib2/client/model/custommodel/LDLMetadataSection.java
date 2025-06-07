package com.lowdragmc.lowdraglib2.client.model.custommodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.model.ModelFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class LDLMetadataSection {
    public static final String SECTION_NAME = LDLib2.MOD_ID;
    private static final Map<ResourceLocation, LDLMetadataSection> METADATA_CACHE = new ConcurrentHashMap<>();
    public static final LDLMetadataSection MISSING = new LDLMetadataSection(false, null);

    public final boolean emissive;
    public final ResourceLocation connection;

    public LDLMetadataSection(boolean emissive, ResourceLocation connection) {
        this.emissive = emissive;
        this.connection = connection;
    }

    public static void clearCache() {
        METADATA_CACHE.clear();
    }

    public boolean isMissing() {
        return this == MISSING;
    }

    @Nonnull
    public static LDLMetadataSection getMetadata(ResourceLocation res) {
        if (METADATA_CACHE.containsKey(res)) {
            return METADATA_CACHE.get(res);
        }
        LDLMetadataSection ret = MISSING;
        var resourceOptional = Minecraft.getInstance().getResourceManager().getResource(res);
        if (resourceOptional.isPresent()) {
            var resource = resourceOptional.get();
            try {
                ret = resource.metadata().getSection(Serializer.INSTANCE).get();
            } catch (Exception ignored) {
            }
        }
        METADATA_CACHE.put(res, ret);
        return ret;
    }

    @Nonnull
    public static LDLMetadataSection getMetadata(TextureAtlasSprite sprite) {
        return getMetadata(spriteToAbsolute(sprite.contents().name()));
    }

    public static boolean isEmissive(TextureAtlasSprite sprite) {
        LDLMetadataSection ret = getMetadata(spriteToAbsolute(sprite.contents().name()));
        return ret.emissive;
    }

    @Nullable
    public static TextureAtlasSprite getConnection(TextureAtlasSprite sprite) {
        LDLMetadataSection ret = getMetadata(spriteToAbsolute(sprite.contents().name()));
        return ret.connection == null ? null : ModelFactory.getBlockSprite(ret.connection);
    }

    public static ResourceLocation spriteToAbsolute(ResourceLocation sprite) {
        if (!sprite.getPath().startsWith("textures/")) {
            sprite = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath());
        }
        if (!sprite.getPath().endsWith(".png")) {
            sprite = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), sprite.getPath() + ".png");
        }
        return sprite;
    }

    public static class Serializer implements MetadataSectionSerializer<LDLMetadataSection> {
        static Serializer INSTANCE = new Serializer();

        @Override
        @Nonnull
        public String getMetadataSectionName() {
            return SECTION_NAME;
        }

        @Override
        @Nonnull
        public LDLMetadataSection fromJson(@Nonnull JsonObject json) {
            boolean emissive = false;
            ResourceLocation connection = null;
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("emissive")) {
                    JsonElement element = obj.get("emissive");
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
                        emissive = element.getAsBoolean();
                    }
                }
                if (obj.has("connection")) {
                    JsonElement element = obj.get("connection");
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                        connection = ResourceLocation.parse(element.getAsString());
                    }
                }
            }
            return new LDLMetadataSection(emissive, connection);
        }
    }
}
