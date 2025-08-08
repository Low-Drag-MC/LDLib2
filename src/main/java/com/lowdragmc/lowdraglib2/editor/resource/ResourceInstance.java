package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceInstance<T> implements INBTSerializable<CompoundTag> {
    public final Resource<T> resource;
    private final BuiltinResourceProvider<T> builtinProvider = new BuiltinResourceProvider<>(this);
    private final PackResourceProvider<T> packProvider = new PackResourceProvider<>(this);
    private final Map<File, FileResourceProvider<T>> fileResourceProviders = new LinkedHashMap<>();
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
                return;
            }
        } catch (Exception ignored) {}
        buildDefault();
    }

    protected void buildBuiltin() {
        this.resource.buildBuiltin(builtinProvider);
    }

    protected void buildDefault() {
        this.resource.buildDefault(this);
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

    public List<ResourceProvider<T>> getFileResourceProviders() {
        var list = new ArrayList<ResourceProvider<T>>(fileResourceProviders.values());
        list.addFirst(builtinProvider);
        return list;
    }

    @Nullable
    public T getResource(IResourcePath path) {
        if (path instanceof BuiltinPath builtinPath) {
            return builtinProvider.getResource(builtinPath);
        } else if (path instanceof FilePath filePath) {
            var key = filePath.file.getParentFile();
            var provider = fileResourceProviders.get(key);
            if (provider != null && provider.supportResourcePath(path)) {
                return provider.getResource(path);
            }
            if (packProvider.supportResourcePath(path)) {
                packProvider.getResource(path);
            }
        }
        return null;
    }

    /**
     * Add a resource provider to this resource.
     */
    public void addFileResourceProvider(FileResourceProvider<T> provider) {
        if (fileResourceProviders.containsKey(provider.resourceLocation)) return;
        fileResourceProviders.put(provider.resourceLocation, provider);
        saveResource();
    }

    /**
     * Remove a resource provider from this resource.
     */
    public void removeResourceProvider(FileResourceProvider<T> provider) {
        if (fileResourceProviders.remove(provider.resourceLocation) != null) {
            saveResource();
        }
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

    /**
     * Whether this resource can add a file resource provider. This is used to determine whether the button should be displayed in the UI.
     */
    public boolean canAddFileResourceProvider() {
        return true;
    }

    /**
     * Whether this resource can remove a resource provider. This is used to determine whether the remove button should be displayed in the UI.
     * By default, only FileResourceProvider can be removed.
     */
    public boolean canRemoveResourceProvider(ResourceProvider<T> provider) {
        return provider instanceof FileResourceProvider<T>;
    }

    /**
     * Create a new file resource provider for this resource. This is used to create a new resource provider that can read and write resources from files.
     */
    public FileResourceProvider<T> createNewFileResourceProvider(File directory) {
        return new FileResourceProvider<>(this, directory);
    }

    @Override
    public @Nonnull CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        var providerList = new ListTag();
        for (var resourceProvider : fileResourceProviders.values()) {
            providerList.add(resourceProvider.serializeNBT());
        }
        data.put("fileProviders", providerList);
        data.putString("displayMode", displayMode.name());
        data.putInt("uiWidth", uiWidth);
        return data;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
        fileResourceProviders.clear();
        var providerList = nbt.getList("fileProviders", Tag.TAG_COMPOUND);
        for (var tag : providerList) {
            var fileResourceProvider = FileResourceProvider.fromNBT(this, (CompoundTag) tag);
            fileResourceProviders.put(fileResourceProvider.resourceLocation, fileResourceProvider);
        }
        try {
            displayMode = Resource.DisplayMode.valueOf(nbt.getString("displayMode"));
        } catch (IllegalArgumentException ignored) {}
        uiWidth = nbt.getInt("uiWidth");
    }

}
