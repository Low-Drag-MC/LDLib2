package com.lowdragmc.lowdraglib2.editor.ui.browser;

import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Bridges a plain directory on disk to the per-resource-type behavior the editor already defines.
 * <p>
 * Everything a resource type does with its own resources — the thumbnail, the double-click editor, the
 * "create new" menu entries, the drag payload, writing the file back — lives in
 * {@link Resource#createResourceProviderContainer(IResourceProvider)}. Rather than reimplementing any
 * of that, this cache builds a real {@link ResourceProviderContainer} per (directory, resource type)
 * pair and uses it as a behavior object.
 * <p>
 * The containers are {@link ResourceProviderContainer#isHeadless() headless} (they build no resource UI
 * of their own) and are added to the host element with display turned off. They must be real children:
 * some resource types show dialogs through {@code getModularUI()}, which is only populated when an
 * element is part of the UI tree. Because the framework skips ticking hidden children, the owner has to
 * call {@link #tick()} itself — that is what keeps dirty resources flushing to disk.
 */
public class ResourceBehaviorCache {
    /**
     * A resource type bound to one directory, together with its provider and behavior container.
     * <p>
     * The menu helpers live here rather than on the caller because they need the container and its
     * callbacks to agree on {@code T}; through a {@code Behavior<?>} each access would capture a fresh
     * wildcard and refuse to compile.
     */
    public record Behavior<T>(Resource<T> resource,
                              ResourceInstance<T> instance,
                              IResourceProvider<T> provider,
                              ResourceProviderContainer<T> container) {

        /** Whether the resource of the given path actually loaded. */
        public boolean isLoaded(@Nullable IResourcePath path) {
            return path != null && provider.hasResource(path);
        }

        /** The contextual entries this resource type adds to a resource's menu, e.g. "copy color". */
        public void appendContextMenu(TreeBuilder.Menu menu) {
            var onMenu = container.getOnMenu();
            if (onMenu != null) {
                onMenu.accept(container, menu);
            }
        }

        /** The entries that create a new resource of this type, as the resource panel offers them. */
        public void appendCreateMenu(TreeBuilder.Menu menu, Runnable onCreated) {
            var addDefault = container.getAddDefault();
            if (container.getSupportAdd().getAsBoolean() && addDefault != null) {
                menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", () -> {
                    var value = addDefault.get();
                    if (value != null) {
                        container.addNewResource(value);
                        onCreated.run();
                    }
                });
            }
            var onCreateMenu = container.getOnCreateMenu();
            if (onCreateMenu != null) {
                onCreateMenu.accept(container, menu);
            }
        }
    }

    private final UIElement host;
    private final Editor editor;
    private final Map<Resource<?>, Behavior<?>> behaviors = new LinkedHashMap<>();
    @Getter
    @Nullable
    private File directory;
    /** Notified with the affected path, or null meaning "everything", when a resource changed. */
    @Setter
    @Nullable
    private Consumer<IResourcePath> onResourceInvalidated;

    public ResourceBehaviorCache(UIElement host, Editor editor) {
        this.host = host;
        this.editor = editor;
    }

    /**
     * The resource types the current editor knows about. Resolved lazily: the resource view is not
     * assigned yet while it is still being constructed.
     */
    public Collection<Resource<?>> availableResources() {
        if (editor == null || editor.resourceView == null) return List.of();
        return editor.resourceView.getResources().keySet();
    }

    public void setDirectory(@Nullable File directory) {
        if (Objects.equals(this.directory, directory)) return;
        dispose();
        this.directory = directory;
    }

    /**
     * The resource type able to load the given file, matched on the file extension. The LONGEST match
     * wins, so a type whose extension ends with another's (e.g. {@code .graph.nbt} vs
     * {@code .test_graph.nbt}) still resolves correctly.
     *
     * @return the owning resource type, or null when no loaded type claims this file.
     */
    @Nullable
    public Resource<?> resourceOf(@Nullable File file) {
        if (file == null || file.isDirectory()) return null;
        var name = file.getName();
        Resource<?> best = null;
        var bestLength = -1;
        for (var resource : availableResources()) {
            var extension = resource.getFileExtension();
            // strictly longer, so a file literally named ".texture.nbt" isn't treated as a resource
            if (name.length() > extension.length() && name.endsWith(extension) && extension.length() > bestLength) {
                best = resource;
                bestLength = extension.length();
            }
        }
        return best;
    }

    @Nullable
    public Behavior<?> forFile(@Nullable File file) {
        var resource = resourceOf(file);
        return resource == null ? null : get(resource);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Behavior<T> get(@Nullable Resource<T> resource) {
        if (resource == null || directory == null) return null;
        var cached = behaviors.get(resource);
        if (cached != null) return (Behavior<T>) cached;
        var behavior = create(resource, directory);
        behaviors.put(resource, behavior);
        return behavior;
    }

    private <T> Behavior<T> create(Resource<T> resource, File directory) {
        var instance = resource.getResourceInstance();
        var provider = findRegisteredProvider(instance, directory);
        if (provider == null) {
            var created = new FileResourceProvider<T>(instance, directory);
            created.setName(directory.getName());
            provider = created;
        }
        var container = resource.createResourceProviderContainer(provider);
        container.setHeadless(true);
        container.setEditor(editor);
        container.setOnResourceInvalidated(path -> {
            // a resource was added/removed/edited on disk. The shared lookup cache may hold a stale
            // value, or a negative entry from before the file existed.
            instance.clearCache();
            if (onResourceInvalidated != null) {
                onResourceInvalidated.accept(path);
            }
        });
        container.setDisplay(false);
        host.addChild(container);
        return new Behavior<>(resource, instance, provider, container);
    }

    /**
     * Prefers a provider the resource instance already has for this exact directory, so browsing into a
     * folder that is also shown as a provider tab shares one file cache and one write path instead of
     * running two providers over the same directory.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <T> IResourceProvider<T> findRegisteredProvider(ResourceInstance<T> instance, File directory) {
        for (var providers : instance.getBuiltinProviders().values()) {
            for (var provider : providers) {
                if (provider instanceof FileResourceProvider<?> fileProvider &&
                        fileProvider.resourceLocation.equals(directory)) {
                    return (IResourceProvider<T>) provider;
                }
            }
        }
        for (var providers : instance.getCustomProviders().values()) {
            for (var provider : providers) {
                if (provider instanceof FileResourceProvider<?> fileProvider &&
                        fileProvider.resourceLocation.equals(directory)) {
                    return (IResourceProvider<T>) provider;
                }
            }
        }
        return null;
    }

    /**
     * The resource path of the given file within the current directory. Built from the {@link File}
     * object itself, because {@link FileResourceProvider#supportResourcePath} matches on the parent
     * directory of the file.
     */
    public IResourcePath pathOf(File file) {
        return new FilePath(file);
    }

    /**
     * Drives the behavior containers. They are hidden children, which the framework skips while
     * ticking, so this has to be called by the owner. It flushes dirty resources to disk and picks up
     * external changes to the directory.
     */
    public void tick() {
        // copied: a container's tick may add resources, which can create further behaviors
        for (var behavior : List.copyOf(behaviors.values())) {
            behavior.container().screenTick();
        }
    }

    public void dispose() {
        for (var behavior : behaviors.values()) {
            host.removeChild(behavior.container());
        }
        behaviors.clear();
    }
}
