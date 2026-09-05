package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceImportContext;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.*;

@Accessors(chain = true)
public class ResourceProviderContainer<T> extends UIElement {
    /** The smallest and largest preview size the bottom bar's slider offers. */
    public static final int MIN_UI_WIDTH = 10;
    public static final int MAX_UI_WIDTH = 100;

    public final ScrollerView scrollerView = new ScrollerView();
    public final ResourceBottomBar bottomBar = new ResourceBottomBar(MIN_UI_WIDTH, MAX_UI_WIDTH);
    public final IResourceProvider<T> resourceProvider;
    private final Map<IResourcePath, UIElement> resourceUIs = new HashMap<>();
    @Setter @Getter
    protected UIElementProvider<IResourcePath> uiSupplier = path -> new UIElement().layout(layout -> {
        layout.widthPercent(100);
        layout.heightPercent(100);
    }).style(style -> style.backgroundTexture(Icons.FILE));
    @Getter
    protected Function<IResourcePath, String> nameSupplier;
    @Setter @Getter
    protected Predicate<IResourcePath> canRemove;
    @Setter @Getter
    protected Predicate<IResourcePath> canRename;
    @Setter @Getter
    protected Predicate<IResourcePath> canEdit;
    /**
     * Whether double-clicking a resource opens it at all, as opposed to opening it <em>for editing</em>.
     *
     * <p>Defaults to {@link #canEdit} — for a resource type whose editor has no viewing mode, "cannot
     * edit" and "cannot open" are the same thing, and opening one would offer a Save that writes into a
     * provider that refuses writes. A container whose editor can open read-only (see
     * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResourceProviderContainer}) widens
     * this to everything, which is what lets a built-in resource be read without being changed.</p>
     */
    @Setter @Getter
    protected Predicate<IResourcePath> canOpen;
    @Setter @Getter
    protected Predicate<IResourcePath> canCopy;
    @Setter @Getter
    protected Function<IResourcePath, ?> onDragProvider;
    @Setter @Getter
    protected BooleanSupplier supportAdd;
    @Setter @Getter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, IResourcePath> onEdit = null;
    @Setter @Getter
    @Nullable
    protected Supplier<T> addDefault = null;
    @Setter @Getter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, TreeBuilder.Menu> onMenu;
    /**
     * Menu entries that create a NEW resource of this type. Rendered by {@link #getMenu()} in the same
     * place they used to be authored via {@link #onMenu}, and reused on their own by file browsers that
     * offer a "create resource here" menu without the rest of this container's entries.
     */
    @Setter @Getter
    @Nullable
    protected BiConsumer<ResourceProviderContainer<T>, TreeBuilder.Menu> onCreateMenu;
    @Setter
    @Nullable
    protected Consumer<T> onResourceSelect = null;
    /**
     * Headless mode: this container is used purely as a behavior object (thumbnail factory, edit action,
     * create menu, drag payload, resource writer) by e.g. the asset browser, and builds no per-resource
     * UI of its own. Everything that would have rebuilt a resource UI reports through
     * {@link #onResourceInvalidated} instead, so the real owner can refresh its own view.
     */
    @Setter @Getter
    protected boolean headless = false;
    /**
     * Notified with the affected path, or null meaning "everything", whenever a resource UI would have
     * been (re)built, and whenever a resource is removed. Only {@link #headless} owners wire this up;
     * for a normal container the resource UI is rebuilt in place instead.
     */
    @Setter
    @Nullable
    protected Consumer<IResourcePath> onResourceInvalidated = null;
    /**
     * Drawn over the selected cell. A tinted fill plus a solid outline, so the selection reads at a
     * glance without hiding the thumbnail underneath it.
     */
    @Setter @Getter
    protected IGuiTexture selectedTexture = defaultSelectedTexture();
    // runtime
    @Getter
    protected HashSet<IResourcePath> dirtyResources = new HashSet<>();
    @Getter @Nullable
    protected IResourcePath selected = null;
    @Getter @Setter
    protected Editor editor;
    @Nullable
    protected IResourcePath lastClickPath;
    /** The cell the mouse is over, so a click on empty space can be told apart from a click on a cell. */
    @Nullable
    protected IResourcePath hoveredPath;

    public ResourceProviderContainer(IResourceProvider<T> resourceProvider) {
        getLayout().widthPercent(100);
        getLayout().flex(1);
        getLayout().flexDirection(FlexDirection.COLUMN);
        this.resourceProvider = resourceProvider;
        this.nameSupplier = resourceProvider::getResourceName;
        this.canRemove = resourceProvider::canRemove;
        this.canRename = resourceProvider::canRename;
        this.canEdit = resourceProvider::canEdit;
        this.canOpen = path -> canEdit.test(path);
        this.canCopy = resourceProvider::canCopy;
        this.supportAdd = resourceProvider::supportAdd;
        this.onDragProvider = resourceProvider::getResource;

        this.scrollerView.scrollerStyle(style -> {
            style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.ALWAYS);
        }).layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        });
        this.scrollerView.viewContainer.layout(layout -> {
           layout.flexDirection(FlexDirection.ROW);
           layout.wrap(FlexWrap.WRAP);
        });
        // dragging the slider only previews; the value is written to the meta file on release
        this.bottomBar.setOnValueChanged(width -> setUiWidth(width, false));
        this.bottomBar.setOnValueCommitted(() -> resourceProvider.getResourceInstance().saveSettings());
        updateBottomBar();

        addChildren(scrollerView, bottomBar);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.FILE_DROP, this::onFilesDropped);
        // clicking the empty space around the cells clears the selection. Listening on the scroller
        // rather than the whole container keeps the bottom bar's own controls out of it.
        scrollerView.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 && hoveredPath == null) {
                selectResource(null);
            }
        });
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 1 && editor != null) {
            editor.openMenu(this, event.x, event.y, getMenu());
        }
    }

    /** The shared selection look: a light blue wash over the cell, no outline. */
    public static IGuiTexture defaultSelectedTexture() {
        return new ColorRectTexture(0x554852ff);
    }

    /**
     * Briefly tints an element to show where a dropped file landed.
     * <p>
     * This is the only drop feedback there can be: the operating system does not tell the game anything
     * while files are being dragged over the window, so nothing can light up beforehand.
     */
    public static void flashDropTarget(UIElement element) {
        var flash = new UIElement();
        flash.getLayout().positionType(TaffyPosition.ABSOLUTE);
        flash.getLayout().widthPercent(100);
        flash.getLayout().heightPercent(100);
        flash.setAllowHitTest(false);
        flash.style(style -> style.backgroundTexture(new ColorRectTexture(0x554852ff)));
        flash.addClass("__drop-flash__");
        var expiry = System.currentTimeMillis() + 400;
        flash.addEventListener(UIEvents.TICK, e -> {
            if (System.currentTimeMillis() > expiry) {
                flash.removeSelf();
            }
        });
        element.addChild(flash);
    }

    /**
     * Imports dropped files one after another, so that the dialog one import may open is answered before
     * the next begins.
     *
     * @param owner the element dialogs are shown on.
     */
    public void importFiles(List<File> files, UIElement owner) {
        importFile(new ArrayDeque<>(files), owner);
    }

    private void importFile(Deque<File> remaining, UIElement owner) {
        var file = remaining.poll();
        if (file == null) return;
        var resource = resourceProvider.getResourceInstance().resource;
        if (!resource.canImportFile(file)) {
            importFile(remaining, owner);
            return;
        }
        Runnable next = () -> importFile(remaining, owner);
        resource.importFile(new ResourceImportContext<>(resource, resourceProvider, file, owner, editor,
                (name, value) -> {
                    addNewResource(value, name);
                    next.run();
                }, next));
    }

    protected void onFilesDropped(UIEvent event) {
        // a headless container is a behavior object, its owner decides what a drop means
        if (headless || event.droppedFiles.isEmpty()) return;
        var resource = resourceProvider.getResourceInstance().resource;
        var accepted = event.droppedFiles.stream().filter(resource::canImportFile).toList();
        if (accepted.isEmpty()) return;
        event.stopPropagation();
        if (!resourceProvider.supportAdd()) {
            Dialog.showNotification("editor.resource.import_failed", "editor.resource.import_read_only", null).show(this);
            return;
        }
        flashDropTarget(this);
        importFiles(accepted, this);
    }

    /**
     * The visual shell of a resource cell: a square thumbnail box with the name label under it in grid
     * mode, or beside it in list mode. Listeners are the caller's business, so that a file browser can
     * build the very same cell for a file on disk and stay in step with the resource panel.
     * <p>
     * The label is the LAST child, {@link #renameResource} relies on that to swap in its text field.
     *
     * @param name shown verbatim — a file name or a name the user gave a resource, never a
     *             localisation key
     */
    public static UIElement createResourceCell(Resource.DisplayMode mode, int uiWidth,
                                               UIElement thumbnail, String name) {
        var isList = mode == Resource.DisplayMode.LIST;
        var thumbnailBox = new UIElement().layout(layout -> {
            layout.width(uiWidth);
            layout.height(uiWidth);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        }).addChild(thumbnail);
        thumbnailBox.addClass("__resource-cell_thumbnail__").moveInlineAsDefault();

        var label = new Label();
        label.textStyle(style -> {
            style.font(LDLibFonts.JETBRAINS_MONO_BOLD);
            style.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL);
            if (isList) {
                style.textAlignHorizontal(Horizontal.LEFT);
                style.fontSize(9);
            } else {
                style.textAlignHorizontal(Horizontal.CENTER);
                style.fontSize(5);
            }
        }).setText(name, false).setOverflowVisible(false).layout(layout -> {
            if (isList) {
                layout.flex(1);
                layout.heightPercent(100);
                layout.justifyContent(AlignContent.CENTER);
            } else {
                layout.height(14);
            }
        });
        label.addClass("__resource-cell_label__").moveInlineAsDefault();

        var cell = new UIElement().layout(layout -> {
            if (isList) {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.marginVertical(1);
            } else {
                layout.width(uiWidth);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.marginAll(3);
            }
            layout.gapAll(2);
        }).addChildren(thumbnailBox, label);
        cell.addClass(isList ? "__resource-cell_list__" : "__resource-cell_grid__");
        cell.addClass("__resource-cell__").moveInlineAsDefault();
        return cell;
    }

    protected UIElement createResourceUI(IResourcePath key) {
        var instance = resourceProvider.getResourceInstance();
        return createResourceCell(instance.getDisplayMode(), instance.getUiWidth(),
                uiSupplier.apply(key), nameSupplier.apply(key))
                .addEventListener(UIEvents.MOUSE_ENTER, e -> hoveredPath = key, true)
                .addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                    if (hoveredPath == key) hoveredPath = null;
                }, true)
                .addEventListener(UIEvents.MOUSE_DOWN, e -> selectResource(key))
                .addEventListener(UIEvents.DOUBLE_CLICK, e-> editResource(key))
                .addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) {
                        lastClickPath = key;
                    }
                }).addEventListener(UIEvents.MOUSE_UP, e -> {
                    lastClickPath = null; // Reset click time
                }).addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                    if (lastClickPath == key && isMouseDown(0)) {
                        e.currentElement.startDrag(onDragProvider.apply(key), TextTexture.raw(nameSupplier.apply(key)));
                    }
                    lastClickPath = null;
                }, true);
    }

    /**
     * Call this method to reload the resource container.
     */
    public void reloadResourceContainer() {
        resourceUIs.clear();
        hoveredPath = null;
        if (headless) {
            notifyInvalidated(null);
            return;
        }
        scrollerView.clearAllScrollViewChildren();
        resourceProvider.forEach(entry -> appendResourceUI(entry.getKey()));
        // the cells are new, so the highlight has to be put back on the one that is still selected,
        // and dropped if that resource is no longer here
        var previous = selected;
        selected = null;
        selectResource(previous, false);
    }

    protected void notifyInvalidated(@Nullable IResourcePath path) {
        if (onResourceInvalidated != null) {
            onResourceInvalidated.accept(path);
        }
    }

    /**
     * Reloads a specific resource UI by its path.
     * If the resource does not exist, it will not do anything.
     * @param path the resource path to reload
     */
    public void reloadSpecificResource(IResourcePath path) {
        if (path == null) return;
        if (headless) {
            notifyInvalidated(path);
            return;
        }
        if (!resourceUIs.containsKey(path) || !resourceProvider.hasResource(path)) return;
        var ui = createResourceUI(path);
        var index = scrollerView.viewContainer.getChildren().indexOf(resourceUIs.get(path));
        scrollerView.removeScrollViewChild(resourceUIs.get(path));
        scrollerView.addScrollViewChildAt(ui, index);
        resourceUIs.put(path, ui);
        if (selected != null && selected.equals(path)) {
            selectResource(path);
        }
    }

    public void appendResourceUI(IResourcePath resourcePath) {
        if (resourcePath == null || resourceUIs.containsKey(resourcePath) || !resourceProvider.hasResource(resourcePath)) return;
        if (headless) {
            notifyInvalidated(resourcePath);
            return;
        }
        var ui = createResourceUI(resourcePath);
        resourceUIs.put(resourcePath, ui);
        scrollerView.addScrollViewChild(ui);
    }

    public void selectResource(IResourcePath resourcePath) {
        selectResource(resourcePath, true);
    }

    /**
     * Selects the given resource.
     *
     * @param resourcePath the resource path to select.
     * @param notify whether the {@link #onResourceSelect} callback should be notified.
     */
    public void selectResource(@Nullable IResourcePath resourcePath, boolean notify) {
        // null clears the selection, and third party providers are not expected to take a null path
        var res = resourcePath == null ? null : resourceProvider.getResource(resourcePath);
        if (notify && onResourceSelect != null && res != null) {
            onResourceSelect.accept(res);
        }
        if (headless) {
            // no UI to highlight, but keep track of the selection so contextual menu entries
            // (e.g. the ones authored through onMenu) still see it.
            selected = resourcePath;
            return;
        }
        if (!resourceUIs.containsKey(resourcePath)) {
            resourcePath = null;
        }
        if (selected != null) {
            var previousUI = resourceUIs.get(selected);
            if (previousUI != null) {
                previousUI.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                previousUI.removeClass("__selected__");
            }
        }
        selected = resourcePath;
        if (selected != null) {
            var selectedUI = resourceUIs.get(selected);
            if (selectedUI != null) {
                selectedUI.style(style -> style.overlayTexture(selectedTexture));
                selectedUI.addClass("__selected__");
            }
        }
        updateBottomBar();
    }

    protected void updateBottomBar() {
        bottomBar.setText(selected == null ? "" : selected.getPathWithType());
        bottomBar.setValue(resourceProvider.getResourceInstance().getUiWidth());
    }

    /**
     * Selects the given resource and scrolls it into the view.
     *
     * @param resourcePath the resource path to locate.
     * @param notify whether the {@link #onResourceSelect} callback should be notified.
     */
    public void locateResource(IResourcePath resourcePath, boolean notify) {
        selectResource(resourcePath, notify);
        scrollToResource(resourcePath);
    }

    /**
     * Scrolls the view to make the given resource visible.
     */
    public void scrollToResource(@Nullable IResourcePath resourcePath) {
        // it may be delayed, as the layout is not available if the container was just created.
        scrollerView.scrollToChildDelayed(resourceUIs.get(resourcePath));
    }

    public void setUiWidth(int uiWidth) {
        setUiWidth(uiWidth, true);
    }

    /**
     * @param persist false while a slider is still being dragged, so the resource meta file is not
     *                rewritten on every pixel. See {@link ResourceInstance#saveSettings()}.
     */
    public void setUiWidth(int uiWidth, boolean persist) {
        var instance = resourceProvider.getResourceInstance();
        if (instance.getUiWidth() == uiWidth || uiWidth <= 0) return;
        instance.setUiWidth(uiWidth, persist);
        bottomBar.setValue(uiWidth);
        reloadResourceContainer();
    }

    /**
     * Marks a resource as dirty, which means it will be reloaded on the next tick.
     */
    public void markResourceDirty(IResourcePath resourcePath) {
        if (resourcePath != null && resourceProvider.hasResource(resourcePath)) {
            dirtyResources.add(resourcePath);
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (resourceProvider.checkAndUpdateResourceProvider()) {
            reloadResourceContainer();
            dirtyResources.clear();
        }
        // check for dirty resources and reload them
        for (var dirtyResource : dirtyResources) {
            var resource = resourceProvider.getResource(dirtyResource);
            if (resource == null) continue;
            resourceProvider.addResource(dirtyResource, resource);
            reloadSpecificResource(dirtyResource);
        }
        dirtyResources.clear();
    }

    public void setDisplayMode(Resource.DisplayMode mode) {
        if (resourceProvider.getResourceInstance().getDisplayMode() != mode) {
            resourceProvider.getResourceInstance().setDisplayMode(mode);
            reloadResourceContainer();
        }
    }

    protected TreeBuilder.Menu getMenu() {
        var menu = TreeBuilder.Menu.start();
        var isList = resourceProvider.getResourceInstance().getDisplayMode() == Resource.DisplayMode.LIST;
        var uiWidth = resourceProvider.getResourceInstance().getUiWidth();
        menu.leaf(isList ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.list", () -> setDisplayMode(isList ?
                Resource.DisplayMode.GRID : Resource.DisplayMode.LIST));
        menu.branch("ldlib.gui.editor.group.size", m -> {
            m.leaf(uiWidth == 15 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.small", () -> setUiWidth(15));
            m.leaf(uiWidth == 30 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.medium", () -> setUiWidth(30));
            m.leaf(uiWidth == 50 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.large", () -> setUiWidth(50));
            m.leaf(uiWidth == 100 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.extra_large", () -> setUiWidth(100));
        });
        menu.crossLine();
        if (selected != null) {
            menu.leaf("ldlib.gui.editor.menu.copy_path", () ->
                    ClipboardManager.INSTANCE.copyDirect(selected.getPathWithType())
            );
        }
        if (selected != null && canOpen.test(selected) && onEdit != null) {
            // Same action, honest label: a read-only resource opens, it does not become editable.
            menu.leaf(Icons.EDIT_FILE, canEdit.test(selected)
                            ? "ldlib.gui.editor.menu.edit" : "ldlib.gui.editor.menu.view",
                    () -> editResource(selected));
        }
        if (selected != null && canRename.test(selected)) {
            menu.leaf("ldlib.gui.editor.menu.rename", () -> renameResource(selected));
        }
        menu.crossLine();
        if (selected != null && canCopy.test(selected)) {
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyResource(selected));
        }
        if (supportAdd.getAsBoolean() && addDefault != null) {
            menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", this::addNewResource);
        }
        if (onCreateMenu != null) {
            onCreateMenu.accept(this, menu);
        }
        if (selected != null && canRemove.test(selected)) {
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> removeResource(selected, true));
        }
        resourceProvider.onMenu(menu);
        if (onMenu != null) {
            onMenu.accept(this, menu);
        }
        return menu;
    }

    protected void addNewResource() {
        if (supportAdd.getAsBoolean() && addDefault != null) {
            var newResource = addDefault.get();
            if (newResource != null) {
                addNewResource(newResource);
            }
        }
    }

    public void addNewResource(T value) {
        addNewResource(value, null);
    }

    /**
     * @param name the name to create the resource under, made unique if it is taken. Null falls back to
     *             the generic "new_res", which is what creating one from the menu uses.
     */
    public void addNewResource(T value, @Nullable String name) {
        if (value == null) return;
        var base = name == null || name.isBlank() ? "new_res" : name;
        var key = resourceProvider.createSubPath(base);
        var count = 1;
        while (resourceProvider.hasResource(key)) {
            key = resourceProvider.createSubPath(base + "_" + count);
            count++;
        }
        IResourcePath finalKey = key;
        editor.historyView.pushHistory(Component.translatable("editor.new_resource"), EditAction.of(
                () -> addResourceInternal(value, finalKey), () -> removeResourceInternal(finalKey)));
    }

    /**
     * Asks where the copy should go and what it should be called, then makes it.
     *
     * <p>Copying used to land beside the original under a {@code _copy} suffix, which cannot express
     * the case that matters most: a resource in a read-only builtin library, where "beside the
     * original" is nowhere you are allowed to write. So the destination is a choice — over every
     * writable provider of this resource type, not just this container's — and the name comes with it,
     * because a copy you are about to put somewhere else is a copy you already have a name for.</p>
     */
    /**
     * Copies a resource into this same provider under a {@code _copy} name, with no prompt.
     *
     * <p>"Duplicate this, here" — the behaviour {@link #copyResource} used to have, kept as its own
     * method because callers depend on it being <em>synchronous</em>: the asset browser rebuilds its
     * grid on the next line, which a dialog-driven copy would do before the user had answered.</p>
     */
    public void duplicateResource(IResourcePath key) {
        if (key == null || !canCopy.test(key)) return;
        var copied = detachedCopy(key);
        if (copied == null) return;
        copyResourceTo(copied, resourceProvider, resourceProvider.getResourceName(key) + "_copy");
    }

    /**
     * Asks where a copy should go and what to call it, then makes it.
     *
     * @see #duplicateResource for the in-place version
     */
    public void copyResource(IResourcePath key) {
        if (key == null || !canCopy.test(key)) return;
        var instance = resourceProvider.getResourceInstance();
        var copied = detachedCopy(key);
        if (copied == null) return;

        var targets = instance.listWritableProviders();
        if (targets.isEmpty()) {
            Dialog.showNotification("ldlib.gui.editor.menu.copy", "editor.copy.no_target", null).show(this);
            return;
        }
        // The current provider when it can take the copy — the old behaviour, still the common case.
        var defaultTarget = targets.contains(resourceProvider) ? resourceProvider : targets.getFirst();
        showCopyDialog(targets, defaultTarget, resourceProvider.getResourceName(key) + "_copy",
                (target, name) -> copyResourceTo(copied, target, name));
    }

    /** A detached duplicate of a resource: serialized and read back, so the two share no state. */
    @Nullable
    protected T detachedCopy(IResourcePath key) {
        var resource = resourceProvider.getResourceInstance().resource;
        var value = resourceProvider.getResource(key);
        if (value == null) return null;
        var tag = resource.serializeResource(value, Platform.getFrozenRegistry());
        if (tag == null) return null;
        return resource.deserializeResource(tag, Platform.getFrozenRegistry());
    }

    /**
     * The "copy to" prompt: which provider, under what name.
     *
     * @param onConfirm receives the chosen provider and the typed name, which is not yet known to be
     *                  free — {@link #copyResourceTo} is what makes it unique.
     */
    protected void showCopyDialog(List<IResourceProvider<T>> targets, IResourceProvider<T> defaultTarget,
                                  String defaultName, BiConsumer<IResourceProvider<T>, String> onConfirm) {
        // Null-safe mappers: Selector renders its preview through the same provider, and does so once
        // while it still has no value — setCandidates forces a refresh before anything is selected.
        var selector = new Selector<IResourceProvider<T>>()
                .setCandidateUIProvider(UIElementProvider.iconText(
                        provider -> provider == null ? IGuiTexture.EMPTY : provider.getType().getIcon(),
                        provider -> Component.literal(provider == null ? "" : provider.getName())))
                .setCandidates(targets)
                .setSelected(defaultTarget, false);
        selector.getLayout().widthPercent(100);
        var nameField = new TextField().setText(defaultName, false)
                .setCharValidator(Identifier::isAllowedInIdentifier);
        nameField.getLayout().widthPercent(100);
        // Named so a stylesheet or a UI test can reach it — an editor screen holds dozens of text
        // fields and "the one in the dialog" is not something a selector can otherwise express.
        nameField.addClass("__copy-name-field__");
        selector.addClass("__copy-target-selector__");

        var dialog = new Dialog().setTitle("ldlib.gui.editor.menu.copy_to");
        dialog.addContent(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightAuto();
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        }).addChildren(
                new Label().setText(Component.translatable("editor.copy.target")),
                selector,
                new Label().setText(Component.translatable("editor.copy.name")),
                nameField));
        // The selector's dropdown is anchored on the root element, so a click in it is a click outside
        // the dialog unless the dialog is told otherwise.
        dialog.addExternalElement(selector.dialog);
        dialog.addButton(new Button().setOnClick(e -> {
            var target = selector.getValue();
            var name = nameField.getText().trim();
            dialog.close();
            if (target != null && !name.isEmpty()) {
                onConfirm.accept(target, name);
            }
        }).setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        dialog.addButton(new Button().setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel").addClass("__cancel-button__"));
        dialog.show(this);
    }

    /**
     * Writes {@code value} into {@code target} under {@code name}, suffixed until the name is free.
     *
     * <p>Undoable, like every other resource write here. Only a copy landing in <em>this</em> container
     * touches the cell grid; one sent elsewhere is picked up by that provider's own container when it
     * is next shown, which is also how a resource added to a watched folder from outside the game
     * appears.</p>
     */
    protected void copyResourceTo(T value, IResourceProvider<T> target, String name) {
        var key = target.createSubPath(name);
        var count = 1;
        while (target.hasResource(key)) {
            key = target.createSubPath(name + "_" + count);
            count++;
        }
        var finalKey = key;
        var isHere = target == resourceProvider;
        editor.historyView.pushHistory(Component.translatable("editor.copy_resource"), EditAction.of(
                () -> {
                    if (isHere) {
                        addResourceInternal(value, finalKey);
                    } else {
                        target.addResource(finalKey, value);
                    }
                },
                () -> {
                    if (isHere) {
                        removeResourceInternal(finalKey);
                    } else {
                        target.removeResource(finalKey);
                    }
                }));
    }

    public void removeResource(IResourcePath key, boolean confirm) {
        if (key != null && canRemove.test(key)) {
            var value = resourceProvider.getResource(key);
            if (value == null) return;
            if (confirm) {
                Dialog.showCheckBox("ldlib.gui.editor.menu.remove", "editor.remove.confirm", result -> {
                    if (result) {
                        editor.historyView.pushHistory(Component.translatable("editor.remove_resource"), EditAction.of(
                                () -> removeResourceInternal(key),
                                () -> addResourceInternal(value, key)));
                    }
                }).show(editor);
            } else {
                editor.historyView.pushHistory(Component.translatable("editor.remove_resource"), EditAction.of(
                        () -> removeResourceInternal(key),
                        () -> addResourceInternal(value, key)));
            }
        }
    }

    private void addResourceInternal(T value, IResourcePath finalKey) {
        resourceProvider.addResource(finalKey, value);
        appendResourceUI(finalKey);
        selectResource(finalKey);
    }

    private void removeResourceInternal(IResourcePath key) {
        resourceProvider.removeResource(key);
        var ui = resourceUIs.remove(key);
        if (ui != null) {
            scrollerView.removeScrollViewChild(ui);
        }
        if (key != null && key.equals(selected)) {
            selected = null;
        }
        notifyInvalidated(key);
    }

    /**
     * Opens a resource — for editing when the provider allows it, read-only otherwise. Whether the
     * opened view is editable is the {@link #onEdit} handler's call, made from the same
     * {@link IResourceProvider#canEdit} answer this class uses for its menu.
     */
    public void editResource(IResourcePath key) {
        if (key != null && canOpen.test(key) && onEdit != null) {
            onEdit.accept(this, key);
        }
    }

    /**
     * Turns the name label of a cell built by {@link #createResourceCell} into a text field, the way
     * renaming works everywhere in the resource panel. Enter or losing focus commits, Escape cancels.
     *
     * @param cell          the cell whose LAST child is the name label.
     * @param initial       the name to start editing from.
     * @param charValidator which characters may be typed.
     * @param onCommit      receives the trimmed new name. The label has already been restored, so a
     *                      caller that decides not to rename does not have to undo anything.
     */
    public static void startInlineRename(UIElement cell, String initial,
                                         Predicate<Character> charValidator, Consumer<String> onCommit) {
        if (cell == null || cell.getChildren().isEmpty()) return;
        if (!(cell.getChildren().getLast() instanceof Label label)) return;
        var textField = new TextField().setText(initial).setCharValidator(charValidator);
        var cancelled = new boolean[1];
        textField.addEventListener(UIEvents.BLUR, e -> {
            var newName = textField.getText().trim();
            // Verbatim, like the label this replaced: restoring it through a translation lookup would
            // leave a folder named after a key showing that key's translation until the next refresh.
            label.setText(initial, false);
            label.removeChild(textField);
            if (!cancelled[0]) {
                onCommit.accept(newName);
            }
        });
        textField.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ENTER) {
                textField.blur();
            } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelled[0] = true;
                textField.blur();
            }
        });
        label.setText("");
        label.addChild(textField);
        textField.focus();
    }

    public void renameResource(IResourcePath key) {
        if (key == null || !canRename.test(key)) return;
        startInlineRename(resourceUIs.get(key), nameSupplier.apply(key),
                Identifier::isAllowedInIdentifier,
                newName -> renameResourceTo(key, newName));
    }

    /**
     * Renames a resource to the given (extension-less) name, reusing this container's rename pipeline:
     * the unique-name search, the history entry and the {@link #onRename} hook. It is the counterpart of
     * {@link #renameResource(IResourcePath)} for callers that supply the new name themselves instead of
     * showing the inline text field.
     *
     * @param key     the resource to rename.
     * @param newName the new name, without the resource file extension.
     * @return the new path, {@code key} itself when the name did not change, or null if it was rejected.
     */
    @Nullable
    public IResourcePath renameResourceTo(IResourcePath key, String newName) {
        if (key == null || newName == null || newName.isBlank() || !canRename.test(key)) return null;
        var trimmed = newName.trim();
        var newPath = resourceProvider.createSubPath(trimmed);
        if (newPath.equals(key)) return key;
        var count = 0;
        while (resourceProvider.hasResource(newPath)) {
            count++;
            newPath = resourceProvider.createSubPath(trimmed + " (" + count + ")");
        }
        onRename(key, newPath);
        return newPath;
    }

    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        editor.historyView.pushHistory(Component.translatable("editor.rename_resource"), EditAction.of(() -> {
            resourceProvider.addResource(newPath, resourceProvider.getResource(oldPath));
            resourceProvider.removeResource(oldPath);
            removeResource(oldPath, false);
            appendResourceUI(newPath);
            selectResource(newPath);
        }, () -> {
            resourceProvider.addResource(oldPath, resourceProvider.getResource(newPath));
            resourceProvider.removeResource(newPath);
            removeResource(newPath, false);
            appendResourceUI(oldPath);
            selectResource(oldPath);
        }));
    }

}
