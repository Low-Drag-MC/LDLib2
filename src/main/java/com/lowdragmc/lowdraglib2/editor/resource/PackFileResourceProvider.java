package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.ResourceHelper;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import org.jetbrains.annotations.Nullable;
import java.io.DataInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PackFileResourceProvider<T>  {
    @Getter
    public final ResourceInstance<T> resourceInstance;
    @Getter
    public final Map<IResourcePath, T> contents = new LinkedHashMap<>();

    public PackFileResourceProvider(ResourceInstance<T> resourceInstance) {
        this.resourceInstance = resourceInstance;
        PackResourceManager.INSTANCE.registerProvider(this);
    }

    @Nullable
    private T getResourceByLocation(ResourceLocation location) {
        for (PackResources pack : ResourceHelper.getResourceManager().listPacks().toList()) {
            var resource = pack.getResource(PackType.CLIENT_RESOURCES, location);
            if (resource != null) {
                try {
                    try (var stream = resource.get()) {
                        try (var inputStream = new DataInputStream(stream)) {
                            var tag = NbtIo.read(inputStream);
                            return deserializeNBT(tag, Platform.getFrozenRegistry());
                        }
                    }
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("Failed to read resource {} from {}: ", location, resource, e);
                }
            }
        }
        return null;
    }

    @Nullable
    private T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(resourceInstance.resource.getName())) {
            return resourceInstance.resource.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }

    public boolean supportResourcePath(IResourcePath path) {
        return path instanceof FilePath filePath &&
                filePath.location != null &&
                filePath.file.getName().endsWith(resourceInstance.resource.getFileExtension());
    }

    public T getResource(IResourcePath path) {
        if (supportResourcePath(path)) {
            if (!contents.containsKey(path)) {
                contents.put(path, getResourceByLocation(((FilePath)path).location));
            }
            return contents.get(path);
        }
        return null;
    }
}
