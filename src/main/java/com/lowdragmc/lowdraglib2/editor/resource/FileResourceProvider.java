package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FileResourceProvider<T> extends ResourceProvider<T>  {
    public final File resourceLocation;
    public final String resourceSuffix;
    private final Map<File, Long> resourcesLastModified = new LinkedHashMap<>();

    public FileResourceProvider(Resource<T> resource, File resourceLocation, String resourceSuffix) {
        super(resource);
        this.resourceLocation = resourceLocation;
        this.resourceSuffix = resourceSuffix;
        setName(resourceLocation.getName());
        setIcon(Icons.FILE);
    }

    @Override
    public boolean supportResourcePath(IResourcePath path) {
        if (path instanceof FilePath(File file)) {
            if (file.getName().endsWith(resourceSuffix)) {
                return file.getParentFile().equals(resourceLocation);
            }
        }
        return false;
    }

    @Override
    public IResourcePath createPath(String name) {
        return new FilePath(new File(resourceLocation, name + resourceSuffix));
    }

    @Override
    public String getResourceName(IResourcePath path) {
        if (path instanceof FilePath(File file)) {
            if (file.getName().endsWith(resourceSuffix)) {
                return file.getName().substring(0, file.getName().length() - resourceSuffix.length());
            }
        }
        return super.getResourceName(path);
    }

    @Nullable
    public CompoundTag serializeNBT(T value, HolderLookup.Provider provider) {
        var tag = resourceHolder.serializeResource(value, provider);
        if (tag == null) return null;
        var nbt = new CompoundTag();
        nbt.put("data", tag);
        nbt.putString("type", resourceHolder.getName());
        return nbt;
    }

    @Nullable
    public T deserializeNBT(CompoundTag nbt, HolderLookup.Provider provider) {
        if (nbt.getString("type").equals(resourceHolder.getName())) {
            return resourceHolder.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public boolean addResource(IResourcePath path, T content) {
        if (supportResourcePath(path) && path instanceof FilePath(File file)) {
            try {
                var nbt = this.serializeNBT(content, Platform.getFrozenRegistry());
                if (nbt != null) {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    NbtIo.write(nbt, file.toPath());
                    resourcesLastModified.put(file, file.lastModified());
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
        if (supportResourcePath(path) && path instanceof FilePath filePath && filePath.file().isFile()) {
            if (filePath.file().delete()) {
                return super.removeResource(path);
            }
        }
        return null;
    }

    @Override
    public UIElement createProviderToggle() {
        return new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.setWidth(9);
                    layout.setHeight(9);
                }).style(style -> style.backgroundTexture(getIcon())),
                new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                        .setText(getName())
                        .layout(layout -> layout.setFlex(1))
                        .setOverflow(YogaOverflow.HIDDEN),
                new Button().buttonStyle(style -> {
                    style.defaultTexture(Icons.FOLDER);
                    style.hoverTexture(Icons.FOLDER.copy().setColor(ColorPattern.SLATE_PLUM.color));
                    style.pressedTexture(Icons.FOLDER);
                }).setOnClick(e -> {
                    // avoid bauble event propagation
                    e.stopPropagation();
                    Util.getPlatform().openFile(resourceLocation);
                }).noText().layout(layout -> {
                    layout.setWidth(7);
                    layout.setHeight(7);
                }).style(style -> style.setTooltips("ldlib.gui.tips.open_folder"))
        );
    }

    /**
     * Load and update resource
     * @return true resource changes.
     */
    public boolean tickResourceProvider() {
        if (resourceLocation == null) {
            return false;
        }
        try {
            var changed = false;
            var found = new HashSet<File>();
            var files = resourceLocation.listFiles((file, name) -> name.endsWith(resourceSuffix));
            if (files != null) {
                for (var file : files) {
                    var path = new FilePath(file);
                    if (contents.containsKey(path)) {
                        if (!resourcesLastModified.containsKey(file) || resourcesLastModified.get(file) != file.lastModified()) {
                            var res = readResourceFromFile(file);
                            if (res != null) {
                                contents.put(path, res);
                                resourcesLastModified.put(file, file.lastModified());
                                changed = true;
                                found.add(file);
                            }
                        } else {
                            found.add(file);
                        }
                    } else {
                        var resource = readResourceFromFile(file);
                        if (resource != null) {
                            contents.put(path, resource);
                            resourcesLastModified.put(file, file.lastModified());
                            changed = true;
                            found.add(file);
                        }
                    }
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
        data.putString("location", resourceLocation.getPath());
        data.putString("suffix", resourceSuffix);
        return data;
    }

    public static <T> FileResourceProvider<T> fromNBT(Resource<T> resourceHolder, @Nonnull CompoundTag nbt) {
        var location = new File(nbt.getString("location"));
        var resourceSuffix = nbt.getString("suffix");
        return (FileResourceProvider<T>) new FileResourceProvider<T>(resourceHolder, location, resourceSuffix).setName(nbt.getString("name"));
    }
}
