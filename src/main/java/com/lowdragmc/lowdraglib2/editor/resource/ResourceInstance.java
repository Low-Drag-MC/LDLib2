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
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ResourceInstance<T> implements INBTSerializable<CompoundTag> {
    public final Resource<T> resource;

    // the provider collections are read-only snapshots, replaced on change, so a read on another thread can walk them
    @Getter
    private volatile Map<ResourceProviderType, List<IResourceProvider<T>>> builtinProviders = Map.of();
    @Getter
    private volatile Map<ResourceProviderType, List<IResourceProvider<T>>> customProviders = Map.of();
    // runtime
    private final Map<IResourcePath, T> cache = new ConcurrentHashMap<>();
    // what the providers were asked to release, looked up without asking them again
    private final Map<IResourcePath, WeakReference<T>> releasedCache = new ConcurrentHashMap<>();
    private final AtomicInteger clears = new AtomicInteger();
    private final PackFileResourceProvider<T> packFileProvider = new PackFileResourceProvider<>(this);
    private final DirectFileResourceProvider<T> directFileProvider = new DirectFileResourceProvider<>(this);
    // providers shown without being registered, e.g. the asset browser's own over a folder no provider covers
    private volatile Set<IResourceProvider<T>> unlistedProviders = Set.of();
    // bumped whenever a provider is added or removed (unlisted ones aside), so what was built over them can tell
    // it is stale
    @Getter
    private int providersVersion;

    @Getter
    private Resource.DisplayMode displayMode;
    @Getter
    private int uiWidth;
    @Getter
    private Resource.SortMode sortMode = Resource.SortMode.DEFAULT;
    @Getter
    private boolean sortAscending = true;

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
        clears.incrementAndGet();
        cache.clear();
        releasedCache.clear();
        directFileProvider.clearCache();
    }

    @Nullable
    public T getResource(IResourcePath path) {
        if (path == null) {
            return null;
        }
        var cached = cache.get(path);
        if (cached != null) {
            return cached;
        }
        var let = releasedCache.get(path);
        var held = let == null ? null : let.get();
        if (held != null) {
            return held;
        }
        var cleared = clears.get();
        var found = find(path);
        if (found == null) return null;
        // let go of: held by nothing here either, or releasing it would hold it all the same
        if (isReleased(path)) {
            releasedCache.put(path, new WeakReference<>(found));
            return found;
        }
        var won = cache.putIfAbsent(path, found);
        if (won != null) return won;
        // cleared while it was being read (a save, a rescan): what was read may be what was just dropped
        if (clears.get() != cleared) {
            cache.remove(path, found);
        }
        return found;
    }

    @Nullable
    private T find(IResourcePath path) {
        var type = path.getType();
        var result = builtinProviders.getOrDefault(type, Collections.emptyList()).stream()
                .map(provider -> provider.getResource(path))
                .filter(Objects::nonNull).findFirst();
        if (result.isPresent()) {
            return result.get();
        }
        result = customProviders.getOrDefault(type, Collections.emptyList()).stream()
                .map(provider -> provider.getResource(path))
                .filter(Objects::nonNull).findFirst();
        if (result.isPresent()) {
            return result.get();
        }
        // a folder the asset browser has open: one copy, the one it edits and saves, rather than a second read
        for (var provider : unlistedProviders) {
            if (!provider.hasResource(path)) continue;
            var unlisted = provider.getResource(path);
            if (unlisted != null) {
                return unlisted;
            }
        }
        // a resource file inside the game dir that no provider owns, e.g. one stored in a folder the
        // user created through the asset browser. Checked before the pack tier because
        // PackFileResourceProvider caches for the whole session and only invalidates on a resource
        // reload, which would hide edits to the very same file on disk.
        var direct = directFileProvider.getResource(path);
        if (direct != null) {
            return direct;
        }
        return packFileProvider.getResource(path);
    }

    /**
     * Stops holding what was read at {@code path}, here and in every provider: while anything else holds it, it is
     * still what {@link #getResource} hands out; once nothing does it can be collected, and is read again when next
     * asked for. A game unloading what a level used, say.
     */
    public void release(IResourcePath path) {
        forEachProvider(provider -> provider.release(path));
        directFileProvider.release(path);
        packFileProvider.release(path);
        // a read in flight past its isReleased check would cache it again, unless it sees this
        clears.incrementAndGet();
        cache.remove(path);
    }

    /** Holds what was read at {@code path} again, if it was {@linkplain #release released} and is still around. */
    public void keep(IResourcePath path) {
        forEachProvider(provider -> provider.keep(path));
        directFileProvider.keep(path);
        packFileProvider.keep(path);
        releasedCache.remove(path);
    }

    /** @return whether what was read at {@code path} is {@linkplain #release released} and still around. */
    public boolean isReleased(IResourcePath path) {
        var released = new boolean[1];
        forEachProvider(provider -> released[0] |= provider.isReleased(path));
        return released[0] || directFileProvider.isReleased(path) || packFileProvider.isReleased(path);
    }

    private void forEachProvider(Consumer<IResourceProvider<T>> action) {
        builtinProviders.values().forEach(providers -> providers.forEach(action));
        customProviders.values().forEach(providers -> providers.forEach(action));
        unlistedProviders.forEach(action);
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
     * The providers a new resource can be written into, builtin first, in display order.
     *
     * <p>The destinations offered when copying a resource. A read-only provider (the builtin library a
     * mod ships) reports {@link IResourceProvider#supportAdd() supportAdd() == false} and is left out,
     * which is what makes "copy" the way to fork one of its resources into somewhere writable.</p>
     */
    public List<IResourceProvider<T>> listWritableProviders() {
        var providers = new ArrayList<IResourceProvider<T>>();
        for (var list : builtinProviders.values()) {
            list.stream().filter(IResourceProvider::supportAdd).forEach(providers::add);
        }
        for (var list : customProviders.values()) {
            list.stream().filter(IResourceProvider::supportAdd).forEach(providers::add);
        }
        return Collections.unmodifiableList(providers);
    }

    /**
     * Looks up the entry of the given resource. The resource is matched by identity first, then by
     * {@link Object#equals(Object)}. What an {@link #addUnlistedProvider unlisted provider} has read is matched by
     * identity too.
     *
     * @param value the resource to look up, can be null.
     * @return the entry of the resource, or null if no provider of this instance, listed or not, holds it.
     */
    @Nullable
    public ResourceEntry<T> findResourceEntry(@Nullable T value) {
        if (value == null) return null;
        var entries = listAllResourceEntries();
        // what has been read already comes first: a provider that reads lazily reads a file on getResource,
        // so asking every entry reads everything listed before the one the value came from
        for (var entry : entries) {
            if (entry.provider().getLoadedResource(entry.path()) == value || cache.get(entry.path()) == value) {
                return entry;
            }
        }
        for (var provider : unlistedProviders) {
            for (var entry : provider) {
                if (provider.getLoadedResource(entry.getKey()) == value) {
                    return new ResourceEntry<>(provider, entry.getKey());
                }
            }
        }
        ResourceEntry<T> equalsMatch = null;
        for (var entry : entries) {
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

    public void setSortMode(Resource.SortMode sortMode) {
        if (this.sortMode == sortMode) return;
        this.sortMode = sortMode;
        saveResource();
    }

    public void setSortAscending(boolean sortAscending) {
        if (this.sortAscending == sortAscending) return;
        this.sortAscending = sortAscending;
        saveResource();
    }

    /** Writes the display settings of this instance to its meta file. */
    public void saveSettings() {
        saveResource();
    }

    public void addBuiltinProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            builtinProviders = withProvider(builtinProviders, provider);
        }
    }

    public void addCustomProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            customProviders = withProvider(customProviders, provider);
        }
        saveResource();
    }

    public void removeBuiltinProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            builtinProviders = withoutProvider(builtinProviders, provider);
        }
        clearCache();
    }

    public void removeCustomProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            customProviders = withoutProvider(customProviders, provider);
        }
        saveResource();
        clearCache();
    }

    /**
     * A provider over a folder no registered provider covers, e.g. the asset browser's own, that is known without
     * being listed or saved. While it is, {@link #getResource} hands out what it reads, so the copy the browser
     * shows and edits is the one everything else draws with; and {@link #findResourceEntry} maps it back to its path.
     */
    public void addUnlistedProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            var providers = Collections.newSetFromMap(new IdentityHashMap<IResourceProvider<T>, Boolean>());
            providers.addAll(unlistedProviders);
            if (!providers.add(provider)) return;
            unlistedProviders = providers;
        }
        // what was resolved before has to be resolved again, through it
        clearCache();
    }

    public void removeUnlistedProvider(IResourceProvider<T> provider) {
        synchronized (this) {
            var providers = Collections.newSetFromMap(new IdentityHashMap<IResourceProvider<T>, Boolean>());
            providers.addAll(unlistedProviders);
            if (!providers.remove(provider)) return;
            unlistedProviders = providers;
        }
        clearCache();
    }

    /**
     * Writes a resource that no registered provider owns: through the unlisted provider holding it, else straight to
     * its file. A provider is not required to edit a file the editor reached, wherever it lies in the game folder.
     *
     * @return whether it was written.
     */
    public boolean writeUnowned(IResourcePath path, T value) {
        for (var provider : unlistedProviders) {
            if (provider.hasResource(path) && provider.canEdit(path)) {
                return provider.addResource(path, value);
            }
        }
        if (!directFileProvider.writeResource(path, value)) return false;
        releasedCache.remove(path);
        cache.put(path, value);
        return true;
    }

    private Map<ResourceProviderType, List<IResourceProvider<T>>> withProvider(
            Map<ResourceProviderType, List<IResourceProvider<T>>> resourceProviders, IResourceProvider<T> provider) {
        var type = provider.getType();
        var providers = resourceProviders.getOrDefault(type, List.of());
        if (providers.contains(provider)) return resourceProviders;
        var list = new ArrayList<>(providers);
        list.add(provider);
        var copy = new LinkedHashMap<>(resourceProviders);
        copy.put(type, List.copyOf(list));
        providersVersion++;
        return Collections.unmodifiableMap(copy);
    }

    private Map<ResourceProviderType, List<IResourceProvider<T>>> withoutProvider(
            Map<ResourceProviderType, List<IResourceProvider<T>>> resourceProviders, IResourceProvider<T> provider) {
        var type = provider.getType();
        var providers = resourceProviders.get(type);
        if (providers == null || !providers.contains(provider)) return resourceProviders;
        var list = new ArrayList<>(providers);
        list.remove(provider);
        var copy = new LinkedHashMap<>(resourceProviders);
        if (list.isEmpty()) {
            copy.remove(type);
        } else {
            copy.put(type, List.copyOf(list));
        }
        providersVersion++;
        return Collections.unmodifiableMap(copy);
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
                // one size for every resource type's selector
                .rememberSize("resource_selector")
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
        data.putString("sortMode", sortMode.name());
        data.putBoolean("sortAscending", sortAscending);

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
        synchronized (this) {
            customProviders = Map.of();
            providersVersion++;
        }

        try {
            displayMode = Resource.DisplayMode.valueOf(nbt.getString("displayMode"));
        } catch (IllegalArgumentException ignored) {}
        uiWidth = nbt.getInt("uiWidth");
        if (nbt.contains("sortMode")) {
            try {
                sortMode = Resource.SortMode.valueOf(nbt.getString("sortMode"));
            } catch (IllegalArgumentException ignored) {}
        }
        if (nbt.contains("sortAscending")) {
            sortAscending = nbt.getBoolean("sortAscending");
        }

        // compatible with previous
        if (nbt.contains("fileProviders")) {
            var providerList = nbt.getList("fileProviders", Tag.TAG_COMPOUND);
            for (var tag : providerList) {
                var fileResourceProvider = FileResourceProvider.fromNBT(this, (CompoundTag) tag);
                if (fileResourceProvider.getName().equals("global")) continue;
                synchronized (this) {
                    this.customProviders = withProvider(this.customProviders, fileResourceProvider);
                }
            }
        } else {
            var customProviders = nbt.getCompound("customProviders");
            for (var type : LDLib2Registries.RESOURCE_PROVIDER_TYPES) {
                if (type.supportCustom()) {
                    var list = customProviders.getList(type.getTypeName(), Tag.TAG_COMPOUND);
                    for (var tag : list) {
                        var rp = type.fromNbt(this, (CompoundTag) tag);
                        if (rp != null) {
                            synchronized (this) {
                                this.customProviders = withProvider(this.customProviders, rp);
                            }
                        }
                    }
                }
            }
        }
    }
}
