package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PackResourceManager implements ResourceManagerReloadListener {
    public static Identifier RESOURCE_ID = LDLib2.id("pack_resource");
    public static PackResourceManager INSTANCE = new PackResourceManager();
    private final List<PackFileResourceProvider<?>> providers = Collections.synchronizedList(new ArrayList<>());

    private PackResourceManager() {
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
