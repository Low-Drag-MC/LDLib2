package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ResourceInstance<T> implements INBTSerializable<CompoundTag> {
    public final Resource<T> resource;

    @Getter
    private final Map<ResourceProviderType, List<IResourceProvider<T>>> builtinProviders = new LinkedHashMap<>();
    @Getter
    private final Map<ResourceProviderType, List<IResourceProvider<T>>> customProviders = new LinkedHashMap<>();
    // runtime
    private final Map<IResourcePath, T> cache = new ConcurrentHashMap<>();
    private final PackFileResourceProvider<T> packFileProvider = new PackFileResourceProvider<>(this);
    private final DirectFileResourceProvider<T> directFileProvider = new DirectFileResourceProvider<>(this);

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
            var data = serializeNBT(Platform.getFrozenRegistry());
            NbtIo.write(data, metaFile.toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save resource {} meta file", resource, e);
        }
    }

    public void clearCache() {
        cache.clear();
        directFileProvider.clearCache();
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
            // a resource file inside the game dir that no provider owns, e.g. one stored in a folder the
            // user created through the asset browser. Checked before the pack tier because
            // PackFileResourceProvider caches for the whole session and only invalidates on a resource
            // reload, which would hide edits to the very same file on disk.
            var direct = directFileProvider.getResource(path);
            if (direct != null) {
                cache.put(path, direct);
                return direct;
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

    /**
     * A resource path together with the {@link IResourceProvider} holding it.
     */
    public record ResourceEntry<T>(IResourceProvider<T> provider, IResourcePath path) {
        public String getResourceName() {
            return provider.getResourceName(path);
        }

        @Nullable
        public T getResource() {
            return provider.getResource(path);
        }
    }

    /**
     * Lists all resource paths of this instance together with the provider holding them.
     * They are ordered as they are displayed in the {@link ResourceContainer} (builtin providers first).
     */
    public List<ResourceEntry<T>> listAllResourceEntries() {
        var entries = new ArrayList<ResourceEntry<T>>();
        listProviderResourceEntries(entries, builtinProviders);
        listProviderResourceEntries(entries, customProviders);
        return Collections.unmodifiableList(entries);
    }

    private void listProviderResourceEntries(List<ResourceEntry<T>> entries, Map<ResourceProviderType, List<IResourceProvider<T>>> resourceProviders) {
        for (var providers : resourceProviders.values()) {
            for (var provider : providers) {
                for (var entry : provider) {
                    entries.add(new ResourceEntry<>(provider, entry.getKey()));
                }
            }
        }
    }

    /**
     * Looks up the entry of the given resource. The resource is matched by identity first, then by
     * {@link Object#equals(Object)}.
     *
     * @param value the resource to look up, can be null.
     * @return the entry of the resource, or null if it's not provided by this instance.
     */
    @Nullable
    public ResourceEntry<T> findResourceEntry(@Nullable T value) {
        if (value == null) return null;
        ResourceEntry<T> equalsMatch = null;
        for (var entry : listAllResourceEntries()) {
            var resource = entry.getResource();
            if (resource == value) {
                return entry;
            }
            if (equalsMatch == null && Objects.equals(resource, value)) {
                equalsMatch = entry;
            }
        }
        return equalsMatch;
    }

    /**
     * Looks up the path of the given resource.
     *
     * @param value the resource to look up, can be null.
     * @return the path of the resource, or null if it's not provided by this instance.
     * @see #findResourceEntry(Object)
     */
    @Nullable
    public IResourcePath findResourcePath(@Nullable T value) {
        var entry = findResourceEntry(value);
        return entry == null ? null : entry.path();
    }

    public void setDisplayMode(Resource.DisplayMode displayMode) {
        if (this.displayMode == displayMode) return;
        this.displayMode = displayMode;
        saveResource();
    }

    public void setUiWidth(int uiWidth) {
        setUiWidth(uiWidth, true);
    }

    /**
     * @param persist whether the change is written to the resource meta file right away. Pass false
     *                while a value is still being dragged and call {@link #saveSettings()} on release,
     *                so a slider does not write the file on every pixel.
     */
    public void setUiWidth(int uiWidth, boolean persist) {
        if (this.uiWidth == uiWidth) return;
        this.uiWidth = uiWidth;
        if (persist) {
            saveResource();
        }
    }

    /** Writes the display settings of this instance to its meta file. */
    public void saveSettings() {
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
        return createSelectorDialog(mouseX, mouseY, onValueSelect, onCancel, (IResourcePath) null);
    }

    /**
     * Creates a selector dialog to allow users to select a resource, with the given resource selected and located
     * at the beginning.
     *
     * @param defaultValue the resource that should be selected when the dialog opens, can be null. It is looked up
     *                     via {@link #findResourcePath(Object)}, so it has to be provided by this instance.
     * @see #createSelectorDialog(float, float, Consumer, Runnable)
     */
    public Dialog createSelectorDialog(float mouseX, float mouseY, Consumer<T> onValueSelect, @Nullable Runnable onCancel,
                                       @Nullable T defaultValue) {
        return createSelectorDialog(mouseX, mouseY, onValueSelect, onCancel, findResourcePath(defaultValue));
    }

    /**
     * Creates a selector dialog to allow users to select a resource, with the resource of the given path selected and
     * located at the beginning.
     *
     * @param defaultSelected the path of the resource that should be selected when the dialog opens, can be null.
     * @see #createSelectorDialog(float, float, Consumer, Runnable)
     */
    public Dialog createSelectorDialog(float mouseX, float mouseY, Consumer<T> onValueSelect, @Nullable Runnable onCancel,
                                       @Nullable IResourcePath defaultSelected) {
        var resourceContainer = new ResourceContainer<>(this, Editor.emptyEditor());
        // it shares the content with the search component now, so it grows into the remaining space instead of
        // taking the full height.
        resourceContainer.getLayout().widthPercent(100).heightAuto().flex(1);
        resourceContainer.splitView.setPercentage(30);
        var searchComponent = createSelectorSearchComponent(resourceContainer);
        resourceContainer.setOnResourceSelect(value -> {
            onValueSelect.accept(value);
            // keep the search bar in sync with the selection made in the resource container.
            var entry = findResourceEntry(value);
            if (entry != null && !entry.equals(searchComponent.getValue())) {
                searchComponent.setSelected(entry, false);
            }
        });
        var dialog = new Dialog()
                .windowMode(mouseX, mouseY)
                .setTitle("resource.selector.select_resource")
                .addContent(new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flex(1);
                    layout.gapAll(2);
                }).addChildren(searchComponent, resourceContainer));
        // the search dropdown is anchored to the root element, don't dismiss the dialog while using it.
        dialog.addExternalElement(searchComponent.dialog);
        dialog.addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        dialog.addButton(new Button().setOnClick(e -> {
            if (onCancel != null) {
                onCancel.run();
            }
            dialog.close();
        }).setText("ldlib.gui.tips.cancel").addClass("__cancel-button__"));

        if (defaultSelected != null) {
            for (var entry : listAllResourceEntries()) {
                if (entry.path().equals(defaultSelected)) {
                    // it's the value we already have, don't notify the selection back.
                    resourceContainer.locateResource(entry.provider(), entry.path(), false);
                    searchComponent.setSelected(entry, false);
                    break;
                }
            }
        }
        return dialog;
    }

    /**
     * Creates the search component of the {@link #createSelectorDialog} to quickly locate a resource among all
     * providers of this instance.
     */
    protected SearchComponent<ResourceEntry<T>> createSelectorSearchComponent(ResourceContainer<T> resourceContainer) {
        // searching runs on another thread, so the resources are snapshotted on the main thread before it starts.
        var snapshot = new AtomicReference<>(listAllResourceEntries());
        var searchComponent = new SearchComponent<>(new SearchComponent.ISearchUI<ResourceEntry<T>>() {
            @Override
            public void search(String word, IResultHandler<ResourceEntry<T>> searchHandler) {
                var lowerWord = word.toLowerCase(Locale.ROOT);
                for (var entry : snapshot.get()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (entry.getResourceName().toLowerCase(Locale.ROOT).contains(lowerWord) ||
                            entry.path().getPath().toLowerCase(Locale.ROOT).contains(lowerWord)) {
                        searchHandler.acceptResult(entry);
                    }
                }
            }

            @Override
            public String resultText(ResourceEntry<T> value) {
                return value.getResourceName();
            }

            @Override
            public void onResultSelected(@Nullable ResourceEntry<T> value) {
                if (value != null) {
                    resourceContainer.locateResource(value.provider(), value.path(), true);
                }
            }
        });
        searchComponent.setCandidateUIProvider(UIElementProvider.iconText(
                entry -> entry.provider().getType().getIcon(),
                entry -> Component.literal(entry.getResourceName())
                        .append(Component.literal(" (%s)".formatted(entry.provider().getName()))
                                .withColor(ColorPattern.GRAY.color))));
        searchComponent.getLayout().widthPercent(100);
        searchComponent.style(style -> style.tooltips(Component.translatable("resource.selector.search")));
        // refresh the snapshot on the main thread each time the search begins.
        searchComponent.textField.addEventListener(UIEvents.FOCUS, e -> snapshot.set(listAllResourceEntries()));
        return searchComponent;
    }

    @Override
    public @Nonnull CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var data = new CompoundTag();

        data.putString("displayMode", displayMode.name());
        data.putInt("uiWidth", uiWidth);

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
        data.put("customProviders", customProviders);
        return data;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
        clearCache();
        customProviders.clear();

        try {
            displayMode = Resource.DisplayMode.valueOf(nbt.getString("displayMode"));
        } catch (IllegalArgumentException ignored) {}
        uiWidth = nbt.getInt("uiWidth");

        // compatible with previous
        if (nbt.contains("fileProviders")) {
            var providerList = nbt.getList("fileProviders", Tag.TAG_COMPOUND);
            for (var tag : providerList) {
                var fileResourceProvider = FileResourceProvider.fromNBT(this, (CompoundTag) tag);
                if (fileResourceProvider.getName().equals("global")) continue;
                addResourceProvider(customProviders, fileResourceProvider);
            }
        } else {
            var customProviders = nbt.getCompound("customProviders");
            for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
                if (type.supportCustom()) {
                    var list = customProviders.getList(type.getTypeName(), Tag.TAG_COMPOUND);
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
