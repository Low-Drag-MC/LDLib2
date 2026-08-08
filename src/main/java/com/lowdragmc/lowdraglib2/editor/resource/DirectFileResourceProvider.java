package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last-resort file tier of {@link ResourceInstance#getResource(IResourcePath)}: reads a resource
 * file straight off the disk when NO registered {@link FileResourceProvider} owns it.
 * <p>
 * {@link FileResourceProvider#supportResourcePath} only matches files whose DIRECT parent is the
 * provider's directory, so any resource stored in a folder the user created themselves (e.g. through
 * the asset browser) would otherwise be unreachable. This provider closes that gap.
 * <p>
 * Like {@link PackFileResourceProvider}, it is deliberately NOT an {@link IResourceProvider}: it never
 * appears in the resource panel's provider list, never contributes to
 * {@link ResourceInstance#listAllResources()} and is never serialized into the resource meta file.
 */
public final class DirectFileResourceProvider<T> {
    /**
     * @param value        the loaded resource, or null when the file is missing/unreadable (negative caching).
     * @param lastModified the file mtime the value was read at, 0 when the file did not exist.
     * @param checkedAt    when the filesystem was last consulted, to throttle {@code stat} calls.
     */
    private record Entry<T>(@Nullable T value, long lastModified, long checkedAt) {}

    /** How long a cache entry is trusted before the file mtime is checked again. */
    private static final long REVALIDATE_MS = 500L;

    @Getter
    public final ResourceInstance<T> resourceInstance;
    private final Map<IResourcePath, Entry<T>> cache = new ConcurrentHashMap<>();

    public DirectFileResourceProvider(ResourceInstance<T> resourceInstance) {
        this.resourceInstance = resourceInstance;
    }

    /**
     * Only canonical game-relative paths are handled, see {@link FilePath#toGameRelative(String)}.
     * That is exactly "inside the game directory": it excludes the {@code assets/<namespace>/...} form
     * produced by {@link FilePath#FilePath(net.minecraft.resources.ResourceLocation)} (which belongs to
     * {@link PackFileResourceProvider}) and absolute paths outside the game directory (which belong to
     * an explicitly registered custom provider). Resource paths are decoded from project data that may
     * come from another machine, so this keeps the existing trust boundary.
     */
    public boolean supportResourcePath(IResourcePath path) {
        return path instanceof FilePath filePath &&
                filePath.path.startsWith("./") &&
                filePath.file.getName().endsWith(resourceInstance.resource.getFileExtension());
    }

    @Nullable
    public T getResource(IResourcePath path) {
        if (!supportResourcePath(path)) return null;
        var file = ((FilePath) path).file;
        var now = System.currentTimeMillis();
        var entry = cache.get(path);
        if (entry != null && now - entry.checkedAt() < REVALIDATE_MS) {
            return entry.value();
        }
        // 0 when the file does not exist, so creation and deletion both show up as an mtime change.
        var lastModified = file.lastModified();
        if (entry != null && entry.lastModified() == lastModified) {
            cache.put(path, new Entry<>(entry.value(), lastModified, now));
            return entry.value();
        }
        var value = lastModified == 0 ? null : readResourceFromFile(file);
        cache.put(path, new Entry<>(value, lastModified, now));
        return value;
    }

    public void clearCache() {
        cache.clear();
    }

    @Nullable
    private T readResourceFromFile(File file) {
        if (!file.isFile()) return null;
        try {
            var fileData = NbtIo.read(file.toPath());
            if (fileData != null) {
                var data = deserializeNBT(fileData, Platform.getFrozenRegistry());
                if (data != null) return data;
            }
            // reads are gated on the file mtime, so this logs once per actual change, not once per frame.
            LDLib2.LOGGER.error("Failed to load resource file {} as {}", file, resourceInstance.resource.getName());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load resource file {} as {}: ", file, resourceInstance.resource.getName(), e);
        }
        return null;
    }

    @Nullable
    private T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getStringOr("type", "").equals(resourceInstance.resource.getName())) {
            return resourceInstance.resource.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }
}
