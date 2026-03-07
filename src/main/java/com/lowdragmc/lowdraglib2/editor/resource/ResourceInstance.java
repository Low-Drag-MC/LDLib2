package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ResourceInstance<T> implements ValueIOSerializable {
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
                try(var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    deserialize(TagValueInput.create(reporter, Platform.getFrozenRegistry(), data));
                }
            }
        } catch (Exception ignored) {}
    }

    protected void buildBuiltin() {
        this.resource.buildBuiltin(this);
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
            try(var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                var output = TagValueOutput.createWithContext(reporter, Platform.getFrozenRegistry());
                serialize(output);
                var data = output.buildResult();
                NbtIo.write(data, metaFile.toPath());
            }
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save resource {} meta file", resource, e);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    @Nullable
    public T getResource(IResourcePath path) {
        if (path == null) {
            return null;
        }
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
            var resource = packFileProvider.getResource(path);
            if (resource == null) return null;
            cache.put(path, resource);
        }
        return cache.get(path);
    }

    public List<Map.Entry<IResourcePath, T>> listAllResources() {
        var resources = new ArrayList<Map.Entry<IResourcePath, T>>();
        listProviderResources(resources, builtinProviders);
        listProviderResources(resources, customProviders);
        return Collections.unmodifiableList(resources);
    }

    private void listProviderResources(List<Map.Entry<IResourcePath, T>> resources, Map<ResourceProviderType, List<IResourceProvider<T>>> customProviders) {
        for (List<IResourceProvider<T>> providers : customProviders.values()) {
            for (IResourceProvider<T> provider : providers) {
                for (Map.Entry<IResourcePath, T> entry : provider) {
                    resources.add(entry);
                }
            }
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

    /**
     * Creates a selector dialog to allow users to select a resource.
     *
     * @param mouseX the x-coordinate of the mouse position to display the dialog.
     * @param mouseY the y-coordinate of the mouse position to display the dialog.
     * @param onValueSelect a callback function of type {@code Consumer<T>}
     *                      that is triggered when a resource is selected.
     * @return an instance of {@link Dialog} configured with the resource selector.
     */
    public Dialog createSelectorDialog(float mouseX, float mouseY, Consumer<T> onValueSelect, @Nullable Runnable onCancel) {
        var resourceContainer = new ResourceContainer<>(this, Editor.emptyEditor());
        resourceContainer.getLayout().widthPercent(100).widthPercent(100).flexAuto();
        resourceContainer.setOnResourceSelect(onValueSelect);
        resourceContainer.splitView.setPercentage(30);
        var dialog = new Dialog()
                .windowMode(mouseX, mouseY)
                .setTitle("select texture")
                .addContent(resourceContainer);
        dialog.addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.confirm"));
        dialog.addButton(new Button().setOnClick(e -> {
            if (onCancel != null) {
                onCancel.run();
            }
            dialog.close();
        }).setText("ldlib.gui.tips.cancel"));
        return dialog;
    }

    @Override
    public void serialize(@NotNull ValueOutput output) {
        output.putString("displayMode", displayMode.name());
        output.putInt("uiWidth", uiWidth);

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
        output.store("customProviders", ExtraCodecs.NBT, customProviders);
    }

    @Override
    public void deserialize(@NotNull ValueInput input) {
        clearCache();
        customProviders.clear();

        try {
            input.getString("displayMode").ifPresent(mode -> displayMode = Resource.DisplayMode.valueOf(mode));
        } catch (IllegalArgumentException ignored) {}
        uiWidth = input.getInt("uiWidth").orElse(uiWidth);

        // compatible with previous
        if (input.read("customProviders", ExtraCodecs.NBT).orElse(null) instanceof CompoundTag customProviders) {
            for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
                if (type.supportCustom()) {
                    var list = customProviders.getListOrEmpty(type.getTypeName());
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
