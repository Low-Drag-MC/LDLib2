package com.lowdragmc.lowdraglib2.editor.resource;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PackResourceManager implements SimpleSynchronousResourceReloadListener {
    public static PackResourceManager INSTANCE = new PackResourceManager();
    private final List<PackFileResourceProvider<?>> providers = Collections.synchronizedList(new ArrayList<>());

    private PackResourceManager() {
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath("ldlib", "pack_resource_manager");
    }

    public void registerProvider(PackFileResourceProvider<?> provider) {
        providers.add(provider);
    }

    public void unregisterProvider(PackFileResourceProvider<?> provider) {
        providers.remove(provider);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void onResourceManagerReload(ResourceManager resourceManager) {
        for (var provider : providers) {
            provider.contents.clear();
            provider.resourceInstance.clearCache();
        }
    }
}
