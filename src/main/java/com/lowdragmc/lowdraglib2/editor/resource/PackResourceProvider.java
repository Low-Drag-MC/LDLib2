package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.utils.ResourceHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class PackResourceProvider<T> extends ResourceProvider<T>  {
    public final static class Manager implements ResourceManagerReloadListener {
        public static Manager INSTANCE = new Manager();
        private final List<PackResourceProvider<?>> providers = Collections.synchronizedList(new ArrayList<>());
        
        private Manager() {}
        
        public void registerProvider(PackResourceProvider<?> provider) {
            providers.add(provider);
        }
        
        public void unregisterProvider(PackResourceProvider<?> provider) {
            providers.remove(provider);
        }

        @Override
        @ParametersAreNonnullByDefault
        public void onResourceManagerReload(ResourceManager resourceManager) {
            for (var provider : providers) provider.contents.clear();
        }
    }
    
    public PackResourceProvider(ResourceInstance<T> resourceInstance) {
        super(resourceInstance);
        setName("editor.pack");
        setIcon(Icons.RESOURCE);
        Manager.INSTANCE.providers.add(this);
    }

    @Nullable
    private T getResourceByLocation(ResourceLocation location) {
        return ResourceHelper.getResourceManager().getResource(location).map(resource -> {
            try {
                try (var stream = resource.open()) {
                    try (var inputStream = new DataInputStream(stream)) {
                        var tag = NbtIo.read(inputStream);
                        return deserializeNBT(tag, Platform.getFrozenRegistry());
                    }
                }
            } catch (Exception e) {
                LDLib2.LOGGER.warn("Failed to read resource {} from {}: ", location, resource, e);
            }
            return null;
        }).orElse(null);
    }

    @Nullable
    private T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(resourceInstance.resource.getName())) {
            return resourceInstance.resource.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        return path instanceof FilePath filePath &&
                filePath.location != null &&
                filePath.file.getName().endsWith(resourceInstance.resource.getFileExtension());
    }

    @Override
    public T getResource(IResourcePath path) {
        if (supportResourcePath(path)) {
            if (!contents.containsKey(path)) {
                contents.put(path, getResourceByLocation(((FilePath)path).location));
            }
            return contents.get(path);
        }
        return null;
    }

    @Override
    public IResourcePath createPath(String name) {
        return new FilePath(name);
    }

    @Override
    public boolean canRemove(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canRename(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canEdit(IResourcePath path) {
        return false;
    }

    @Override
    public boolean canCopy(IResourcePath path) {
        return false;
    }

    @Override
    public boolean supportAdd() {
        return false;
    }

}
