package com.lowdragmc.lowdraglib2.editor.resource;

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
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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
        if (nbt.getString("type").equals(resourceInstance.resource.getName())) {
            return resourceInstance.resource.deserializeResource(nbt.get("data"), provider);
        }
        return null;
    }

    @Override
    public boolean addResource(IResourcePath path, T content) {
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
            if (filePath.file.delete()) {
                return super.removeResource(path);
            }
        }
        return null;
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
                    Util.getPlatform().openFile(resourceLocation);
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
        var gamePath = Platform.getGamePath().toAbsolutePath().normalize();
        var resultPath = resourceLocation.toPath().toAbsolutePath().normalize();
        var realPath = resourceLocation.getPath();
        if (resultPath.startsWith(gamePath)) {
            realPath = gamePath.relativize(resultPath).toFile().getPath();
            data.putInt("_version", 1);
        }
        data.putString("location", realPath.replace('\\', '/'));
        return data;
    }

    public static <T> FileResourceProvider<T> fromNBT(ResourceInstance<T> resourceInstance, @Nonnull CompoundTag nbt) {
        var locationStr = nbt.getString("location").replace('\\', '/');
        var name = nbt.getString("name");

        File location;
        if (nbt.contains("_version") && nbt.getInt("_version") >= 1) {
            location = Platform.getGamePath().resolve(locationStr).toFile();
        } else {
            location = new File(locationStr);
        }

        var fileProvider = new FileResourceProvider<>(resourceInstance, location);
        fileProvider.setName(name);
        return fileProvider;
    }
}
