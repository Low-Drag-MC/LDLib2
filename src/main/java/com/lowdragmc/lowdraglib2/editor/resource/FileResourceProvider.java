package com.lowdragmc.lowdraglib2.editor.resource;

import com.mojang.blaze3d.Blaze3D;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FileResourceProvider<T> extends ResourceProvider<T>  {
    public static final ResourceProviderType TYPE = new ResourceProviderType() {
        @Override
        public String getTypeName() {
            return "file";
        }

        @Override
        public IGuiTexture getIcon() {
            return Icons.FILE;
        }

        @Override
        public IResourcePath createFullPath(String path) {
            return new FilePath(path);
        }

        @Override
        public <K> ResourceProvider<K> fromNbt(ResourceInstance<K> resourceHolder, CompoundTag tag) {
            return FileResourceProvider.fromNBT(resourceHolder, tag);
        }

        @Override
        public boolean supportCustom() {
            return true;
        }

        @Override
        public <K> void onCreateCustom(ResourceContainer<K> container) {
            Dialog.showFileDialog("ldlib.gui.resource.add_provider", LDLib2.getAssetsDir(), true, file -> true, result -> {
                if (result.isFile()) {
                    result = result.getParentFile();
                }
                if (result.isDirectory()) {
                    container.resourceInstance.addCustomProvider(new FileResourceProvider<>(container.resourceInstance, result));
                }
            }).show(container.editor);
        }
    };

    public final File resourceLocation;
    public final String resourceSuffix;
    private final Map<File, Long> resourcesLastModified = new LinkedHashMap<>();
    /**
     * Every file in the folder, in scan order — <b>what exists</b>, as opposed to
     * {@link #contents}, which is what has been read.
     *
     * <p>An immutable snapshot replaced wholesale by the scan, so a reader never walks a map that
     * is being written to.
     */
    private volatile Map<IResourcePath, File> known = Map.of();
    /** {@link #known}'s order, which an immutable map does not promise. */
    private volatile List<IResourcePath> order = List.of();
    @Getter @Setter
    private String name;

    public FileResourceProvider(ResourceInstance<T> resourceInstance, File resourceLocation) {
        super(resourceInstance);
        this.resourceLocation = resourceLocation;
        this.resourceSuffix = resourceInstance.resource.getFileExtension();
        setName(resourceLocation.getName());
        checkAndUpdateResourceProvider();
    }

    @Override
    public ResourceProviderType getType() {
        return TYPE;
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        if (path instanceof FilePath filePath) {
            if (filePath.file.getName().endsWith(resourceSuffix)) {
                return filePath.file.getParentFile().equals(resourceLocation);
            }
        }
        return false;
    }

    @Override
    public IResourcePath createSubPath(String name) {
        return new FilePath(new File(resourceLocation, name + resourceSuffix));
    }

    @Override
    public String getResourceName(IResourcePath path) {
        if (path instanceof FilePath filePath) {
            if (filePath.file.getName().endsWith(resourceSuffix)) {
                return filePath.file.getName().substring(0, filePath.file.getName().length() - resourceSuffix.length());
            }
        }
        return super.getResourceName(path);
    }

    @Nullable
    public CompoundTag serializeNBT(T value, HolderLookup.Provider provider) {
        var tag = resourceInstance.resource.serializeResource(value, provider);
        if (tag == null) return null;
        var nbt = new CompoundTag();
        nbt.put("data", tag);
        nbt.putString("type", resourceInstance.resource.getName());
        return nbt;
    }

    @Nullable
    public T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getStringOr("type", "").equals(resourceInstance.resource.getName())) {
            return resourceInstance.resource.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public boolean hasResource(IResourcePath path) {
        return supportResourcePath(path) && known.containsKey(path);
    }

    /**
     * The resource at {@code path}, <b>read the first time it is asked for</b> and remembered after.
     *
     * <p>A file that will not decode is dropped from the listing rather than re-read on every call —
     * which is what the eager scan did by only recording a file whose read succeeded. The next scan
     * picks it up again if it changes on disk.
     */
    @Override
    public T getResource(IResourcePath path) {
        if (!supportResourcePath(path)) {
            return null;
        }
        var loaded = contents.get(path);
        if (loaded != null) {
            return loaded;
        }
        var file = known.get(path);
        if (file == null) {
            return null;
        }
        var read = readResourceFromFile(file);
        if (read == null) {
            forget(path, file);
            return null;
        }
        contents.put(path, read);
        return read;
    }

    /** Every file in the folder, <b>reading one only if its value is taken</b>. */
    @Override
    public @Nonnull Iterator<Map.Entry<IResourcePath, T>> iterator() {
        var paths = order.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return paths.hasNext();
            }

            @Override
            public Map.Entry<IResourcePath, T> next() {
                var path = paths.next();
                return new Map.Entry<>() {
                    @Override
                    public IResourcePath getKey() {
                        return path;
                    }

                    @Override
                    public T getValue() {
                        return getResource(path);
                    }

                    @Override
                    public T setValue(T value) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private void forget(IResourcePath path, File file) {
        var paths = new LinkedHashMap<>(known);
        if (paths.remove(path) == null) {
            return;
        }
        var ordered = new java.util.ArrayList<>(order);
        ordered.remove(path);
        known = Map.copyOf(paths);
        order = java.util.List.copyOf(ordered);
        resourcesLastModified.remove(file);
        contents.remove(path);
    }

    @Override
    public boolean addResource(IResourcePath path, T content) {
        // A resource type is free to assume the thing it is asked to write exists — most reach
        // straight into a field of it — so null is refused here rather than handed on as a crash
        // inside somebody's serializeResource.
        if (content == null) return false;
        if (supportResourcePath(path) && path instanceof FilePath filePath) {
            var file = filePath.file;
            try {
                var nbt = this.serializeNBT(content, Platform.getFrozenRegistry());
                if (nbt != null) {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    NbtIo.write(nbt, file.toPath());
                    resourcesLastModified.put(file, file.lastModified());
                    // it exists from now on, without waiting for the next scan to notice the file
                    remember(path, file);
                    return super.addResource(path, content);
                } else {
                    LDLib2.LOGGER.error("Failed to serialize resource {} to file {}", content, file);
                }
            } catch (IOException e) {
                LDLib2.LOGGER.error("Failed to write resource {} to file {}", content, file, e);
            }
        }
        return false;
    }

    @Override
    public T removeResource(IResourcePath path) {
        if (supportResourcePath(path) && path instanceof FilePath filePath && filePath.file.isFile()) {
            // read before the file goes: the base class returns what was cached, and with a lazy
            // provider nothing may have read it yet — a caller relying on the removed value (an
            // undo entry) would otherwise get null for a resource that was perfectly good
            var removed = getResource(path);
            if (filePath.file.delete()) {
                forget(path, filePath.file);
                super.removeResource(path);
                return removed;
            }
        }
        return null;
    }

    private void remember(IResourcePath path, File file) {
        if (known.containsKey(path)) {
            return;
        }
        var paths = new LinkedHashMap<>(known);
        paths.put(path, file);
        var ordered = new java.util.ArrayList<>(order);
        ordered.add(path);
        known = Map.copyOf(paths);
        order = java.util.List.copyOf(ordered);
    }

    @Override
    public UIElement createProviderToggle() {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.width(9);
                    layout.height(9);
                }).style(style -> style.backgroundTexture(getType().getIcon())),
                new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                        .setText(getName())
                        .layout(layout -> layout.flex(1))
                        .setOverflowVisible(false),
                new Button().buttonStyle(style -> {
                    style.baseTexture(Icons.FOLDER);
                    style.hoverTexture(Icons.FOLDER.copy().setColor(ColorPattern.SLATE_PLUM.color));
                    style.pressedTexture(Icons.FOLDER);
                }).setOnClick(e -> {
                    // avoid bauble event propagation
                    e.stopPropagation();
                    if (!resourceLocation.exists()) {
                        resourceLocation.mkdirs();
                    }
                    Blaze3D.openPath(resourceLocation.toPath());
                }).noText().layout(layout -> {
                    layout.width(7);
                    layout.height(7);
                }).style(style -> style.tooltips("ldlib.gui.tips.open_folder"))
        );
    }

    /**
     * Load and update resource
     * @return true resource changes.
     */
    public boolean checkAndUpdateResourceProvider() {
        if (resourceLocation == null) {
            return false;
        }
        try {
            var changed = false;
            var found = new HashSet<File>();
            // built here and published together at the end — see the note on `known`
            var paths = new LinkedHashMap<IResourcePath, File>();
            var ordered = new ArrayList<IResourcePath>();
            var files = resourceLocation.listFiles((file, name) -> name.endsWith(resourceSuffix));
            if (files != null) {
                for (var file : files) {
                    var path = new FilePath(file);
                    found.add(file);
                    paths.put(path, file);
                    ordered.add(path);
                    var seen = resourcesLastModified.get(file);
                    var stamp = file.lastModified();
                    if (seen != null && seen == stamp) {
                        continue;
                    }
                    // new, or rewritten on disk. ⚠️ NOTHING is read here: whatever was decoded from
                    // the old bytes is dropped, and the next getResource pays for the new ones.
                    contents.remove(path);
                    resourcesLastModified.put(file, stamp);
                    changed = true;
                }
            }
            if (found.size() != resourcesLastModified.size()) {
                var removed = new HashSet<>(resourcesLastModified.keySet());
                removed.removeAll(found);
                removed.forEach(file -> {
                    resourcesLastModified.remove(file);
                    contents.remove(new FilePath(file));
                });
                changed = true;
            }
            known = Map.copyOf(paths);
            order = List.copyOf(ordered);
            if (changed) {
                resourceInstance.clearCache();
            }
            return changed;
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to tick file resources provider from {}: ", resourceLocation, e);
            return false;
        }
    }

    @Nullable
    private T readResourceFromFile(File file) {
        try {
            var fileData = NbtIo.read(file.toPath());
            if (fileData != null) {
                var data = deserializeNBT(fileData, Platform.getFrozenRegistry());
                if (data != null) return data;
            }
            LDLib2.LOGGER.error("Failed to load resource file {} from {}: ", file, this);
        } catch (IOException e) {
            LDLib2.LOGGER.error("Failed to load resource file {} from {}: ", file, this, e);
        }
        return null;
    }

    public @Nonnull CompoundTag serializeNBT() {
        var data = new CompoundTag();
        data.putString("name", getName());
        // store the canonical, portable game-relative form ("./ldlib2/assets/..."), matching FilePath
        // identity; fromNBT resolves it (with or without the leading "./") against the game dir
        data.putString("location", FilePath.toGameRelative(resourceLocation.getPath()));
        data.putInt("_version", 1);
        return data;
    }

    public static <T> FileResourceProvider<T> fromNBT(ResourceInstance<T> resourceInstance, @Nonnull CompoundTag nbt) {
        var locationStr = nbt.getStringOr("location", "").replace('\\', '/');
        var name = nbt.getString("name");

        File location;
        if (nbt.contains("_version") && nbt.getIntOr("_version", 0) >= 1) {
            // canonical "./..." or legacy relative-to-gamedir (no "./") — resolve both against the game
            // dir (an absolute string resolves to itself, so external custom providers still work)
            var rel = locationStr.startsWith("./") ? locationStr.substring(2) : locationStr;
            location = Platform.getGamePath().resolve(rel).toFile();
        } else {
            location = new File(locationStr);
        }

        var fileProvider = new FileResourceProvider<>(resourceInstance, location);
        name.ifPresent(fileProvider::setName);
        return fileProvider;
    }
}
