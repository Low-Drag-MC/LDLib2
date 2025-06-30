package com.lowdragmc.lowdraglib2.editor.resource;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class ResourceInstance<T> implements INBTSerializable<CompoundTag> {
    public final Resource<T> resource;
    @Getter
    private final List<ResourceProvider<T>> providers = new ArrayList<>();
    @Getter @Setter
    private Resource.DisplayMode displayMode;
    @Getter @Setter
    private int uiWidth;

    public ResourceInstance(Resource<T> resource) {
        this.resource = resource;
        this.displayMode = resource.getDefaultDisplayMode();
        this.uiWidth = resource.getDefaultUIWidth();
    }

    public void buildDefault() {
        this.resource.buildDefault(this);
    }

    /**
     * Add a resource provider to this resource.
     */
    public void addResourceProvider(ResourceProvider<T> provider) {
        providers.add(provider);
    }

    /**
     * Remove a resource provider from this resource.
     */
    public void removeResourceProvider(ResourceProvider<T> provider) {
        providers.remove(provider);
    }


    @Override
    public @Nonnull CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        var providerList = new ListTag();
        for (var resourceProvider : providers) {
            if (resourceProvider instanceof FileResourceProvider<T> fileResourceProvider) {
                providerList.add(fileResourceProvider.serializeNBT());
            }
        }
        data.put("providers", providerList);
        data.putBoolean("isList", isList);
        data.putInt("uiWidth", uiWidth);
        return data;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
        providers.removeIf(FileResourceProvider.class::isInstance); // Clear existing file resource providers
        var providerList = nbt.getList("providers", Tag.TAG_COMPOUND);
        for (var tag : providerList) {
            addResourceProvider(resource.createFileResourceProviderFromNBT((CompoundTag) tag));
        }
        isList = nbt.getBoolean("isList");
        uiWidth = nbt.getInt("uiWidth");
    }

}
