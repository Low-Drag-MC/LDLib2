package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceInstance<T> implements INBTSerializable<CompoundTag> {
    public final Resource<T> resource;

    @Getter
    private final Map<ResourceProviderType, List<IResourceProvider<T>>> builtinProviders = new LinkedHashMap<>();
    @Getter
    private final Map<ResourceProviderType, List<IResourceProvider<T>>> customProviders = new LinkedHashMap<>();
    // runtime
    private final Map<IResourcePath, T> cache = new ConcurrentHashMap<>();
    private final PackFileResourceProvider<T> packFileProvider = new PackFileResourceProvider<>(this);

    @Getter
    private Resource.DisplayMode displayMode;
    @Getter
    private int uiWidth;

    public ResourceInstance(Resource<T> resource) {
        this.resource = resource;
        this.displayMode = resource.getDefaultDisplayMode();
        this.uiWidth = resource.getDefaultUIWidth();
        this.loadResource();
    }

    protected void loadResource() {
        buildBuiltin();
        var metaFile = new File(LDLib2.getAssetsDir(), "ldlib2/resources/" + resource.getName() + ".meta.nbt");
        try {
            var data = NbtIo.read(metaFile.toPath());
            if (data != null) {
                deserializeNBT(Platform.getFrozenRegistry(), data);
            }
        } catch (Exception ignored) {}
    }

    protected void buildBuiltin() {
        var builtinProvider = new BuiltinResourceProvider<>("built-in", this);
        this.resource.buildBuiltin(builtinProvider);

        var global = new FileResourceProvider<>(this, new File(LDLib2.getAssetsDir(), "ldlib2/resources/global"));
        global.setName("global");

        addBuiltinProvider(builtinProvider);
        addBuiltinProvider(global);
    }

    protected void saveResource() {
        var metaFile = new File(LDLib2.getAssetsDir(), "ldlib2/resources/" + resource.getName() + ".meta.nbt");
        try {
            if (!metaFile.getParentFile().exists()) {
                if (!metaFile.getParentFile().mkdirs()) {
                    LDLib2.LOGGER.error("Failed to create directory {}", metaFile.getParentFile());
                    return;
                }
            }
            var data = serializeNBT(Platform.getFrozenRegistry());
            NbtIo.write(data, metaFile.toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save resource {} meta file", resource, e);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    @Nullable
    public T getResource(IResourcePath path) {
        if (!cache.containsKey(path)) {
            var type = path.getType();
            var result = builtinProviders.getOrDefault(type, Collections.emptyList()).stream()
                    .map(provider -> provider.getResource(path))
                    .filter(Objects::nonNull).findFirst();
            if (result.isPresent()) {
                cache.put(path, result.get());
                return result.get();
            }
            result = customProviders.getOrDefault(type, Collections.emptyList()).stream()
                    .map(provider -> provider.getResource(path))
                    .filter(Objects::nonNull).findFirst();
            if (result.isPresent()) {
                cache.put(path, result.get());
                return result.get();
            }
            cache.put(path, packFileProvider.getResource(path));
        }
        return cache.get(path);
    }

    public void setDisplayMode(Resource.DisplayMode displayMode) {
        if (this.displayMode == displayMode) return;
        this.displayMode = displayMode;
        saveResource();
    }

    public void setUiWidth(int uiWidth) {
        if (this.uiWidth == uiWidth) return;
        this.uiWidth = uiWidth;
        saveResource();
    }

    public void addBuiltinProvider(IResourceProvider<T> provider) {
        addResourceProvider(builtinProviders, provider);
    }

    public void addCustomProvider(IResourceProvider<T> provider) {
        addResourceProvider(customProviders, provider);
        saveResource();
    }

    public void removeBuiltinProvider(IResourceProvider<T> provider) {
        removeResourceProvider(builtinProviders, provider);
        clearCache();
    }

    public void removeCustomProvider(IResourceProvider<T> provider) {
        removeResourceProvider(customProviders, provider);
        saveResource();
        clearCache();
    }

    private void addResourceProvider(Map<ResourceProviderType, List<IResourceProvider<T>>> resourceProviders, IResourceProvider<T> provider) {
        var type = provider.getType();
        if (resourceProviders.containsKey(type)) {
            var providers = resourceProviders.get(type);
            if (!providers.contains(provider)) {
                providers.add(provider);
            }
        } else {
            var list = new ArrayList<IResourceProvider<T>>();
            list.add(provider);
            resourceProviders.put(type, list);
        }
    }

    private void removeResourceProvider(Map<ResourceProviderType, List<IResourceProvider<T>>> resourceProviders, IResourceProvider<T> provider) {
        var type = provider.getType();
        if (resourceProviders.containsKey(type)) {
            var providers = resourceProviders.get(type);
            providers.remove(provider);
            if (providers.isEmpty()) {
                resourceProviders.remove(type);
            }
        }
    }

    @Override
    public @Nonnull CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var data = new CompoundTag();

        data.putString("displayMode", displayMode.name());
        data.putInt("uiWidth", uiWidth);

        var customProviders = new CompoundTag();
        for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
            if (type.supportCustom()) {
                var list = new ListTag();
                for (var rp : this.customProviders.getOrDefault(type, Collections.emptyList())) {
                    var nbt = rp.serializeNBT();
                    if (nbt == null) continue;
                    list.add(nbt);
                }
                if (!list.isEmpty()) {
                    customProviders.put(type.getTypeName(), list);
                }
            }
        }
        data.put("customProviders", customProviders);
        return data;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
        clearCache();
        customProviders.clear();

        try {
            displayMode = Resource.DisplayMode.valueOf(nbt.getString("displayMode"));
        } catch (IllegalArgumentException ignored) {}
        uiWidth = nbt.getInt("uiWidth");

        // compatible with previous
        if (nbt.contains("fileProviders")) {
            var providerList = nbt.getList("fileProviders", Tag.TAG_COMPOUND);
            for (var tag : providerList) {
                var fileResourceProvider = FileResourceProvider.fromNBT(this, (CompoundTag) tag);
                if (fileResourceProvider.getName().equals("global")) continue;
                addResourceProvider(customProviders, fileResourceProvider);
            }
        } else {
            var customProviders = nbt.getCompound("customProviders");
            for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
                if (type.supportCustom()) {
                    var list = customProviders.getList(type.getTypeName(), Tag.TAG_COMPOUND);
                    for (var tag : list) {
                        var rp = type.fromNbt(this, (CompoundTag) tag);
                        if (rp != null) {
                            addResourceProvider(this.customProviders, rp);
                        }
                    }
                }
            }
        }
    }
}
