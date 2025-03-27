package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.LDLib;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.net.URL;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote ResourceHelper
 */
@UtilityClass
public final class ResourceHelper {
    public static boolean isResourceExistRaw(ResourceLocation rs) {
        URL url = ResourceHelper.class.getResource(String.format("/assets/%s/%s", rs.getNamespace(), rs.getPath()));
        return url != null;
    }

    public static boolean isResourceExist(ResourceLocation rs) {
        if (LDLib.isClient()) {
            return Minecraft.getInstance().getResourceManager().getResource(rs).isPresent();
        } else {
            return false;
        }
    }

    public static boolean isTextureExist(@Nonnull ResourceLocation location) {
        var textureLocation = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "textures/%s.png".formatted(location.getPath()));
        return isResourceExist(textureLocation) || isResourceExistRaw(textureLocation);
    }

    public static boolean isModelExist(@Nonnull ResourceLocation location) {
        var modelLocation = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "models/%s.json".formatted(location.getPath()));
        return isResourceExist(modelLocation) || isResourceExistRaw(modelLocation);
    }

}
