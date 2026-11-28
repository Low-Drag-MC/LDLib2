package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.LDLibFonts;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceImportContext;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceProvider;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
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
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collectors;

@Accessors(chain = true)
public class ResourceProviderContainer<T> extends UIElement {
    /** The smallest and largest preview size the bottom bar's slider offers. */
    public static final int MIN_UI_WIDTH = 10;
    public static final int MAX_UI_WIDTH = 100;
    public static final int INLINE_RENAME_MIN_WIDTH = 90;
    public static final String CLASS_RENAME_FIELD = "__resource-rename-field__";
    /** Ticks between scans of the provider for changes made outside the editor. */
    private static final int POLL_INTERVAL = 20;
    // staggered, so the containers of one folder (one per resource type) do not all list it on the same tick
    private static int nextPollPhase;

    public final UIElement toolbar = new UIElement();
    public final TextField searchField = new TextField();
    public final ScrollerView scrollerView = new ScrollerView();
    public final ResourceBottomBar bottomBar = new ResourceBottomBar(MIN_UI_WIDTH, MAX_UI_WIDTH);
    public final IResourceProvider<T> resourceProvider;
    private final Map<IResourcePath, UIElement> resourceUIs = new HashMap<>();
    private int pollCountdown = Math.floorMod(nextPollPhase++, POLL_INTERVAL);
    /** Lower-cased; empty shows everything. */
    private String searchFilter = "";
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
    protected final LinkedHashSet<IResourcePath> selectedResources = new LinkedHashSet<>();
    @Nullable
    protected IResourcePath selectionAnchor = null;
    @Nullable
    private IResourcePath pendingCollapsePath = null;
    private final GridMarqueeSelection<IResourcePath> marqueeSelection;
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
        setupToolbar();

        addChildren(toolbar, scrollerView, bottomBar);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.FILE_DROP, this::onFilesDropped);

        var viewPort = scrollerView.viewPort;
        viewPort.setFocusable(true);
        viewPort.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0 || event.button == 1) {
                viewPort.focus();
            }
        }, true);
        viewPort.addEventListener(UIEvents.KEY_DOWN, this::onGridKeyDown);
        viewPort.addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        viewPort.addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
        this.marqueeSelection = GridMarqueeSelection.attach(
                scrollerView,
                () -> resourceUIs,
                this::getSelectedResources,
                paths -> setSelectedResources(paths, false));
    }

    protected void onMouseDown(UIEvent event) {
        if (event.button == 1 && editor != null) {
            // Right-clicking empty space inside the grid clears any stale selection before opening the menu.
            if (event.target != null && event.target.getStructurePath().contains(scrollerView.viewPort)
                    && marqueeSelection.keyAt(event.target) == null) {
                selectResource(null, false);
            }
            editor.openMenu(this, event.x, event.y, getMenu());
        }
    }

    protected void onGridKeyDown(UIEvent event) {
        if (event.keyCode == GLFW.GLFW_KEY_DELETE && !selectedResources.isEmpty()) {
            var removable = selectedResources.stream().filter(canRemove).toList();
            if (!removable.isEmpty()) {
                event.stopPropagation();
                removeResources(removable, true);
            }
        } else if (event.keyCode == GLFW.GLFW_KEY_F2 && selectedResources.size() == 1
                && selected != null && canRename.test(selected)) {
            event.stopPropagation();
            renameResource(selected);
        } else if (event.keyCode == GLFW.GLFW_KEY_ESCAPE && !selectedResources.isEmpty()) {
            event.stopPropagation();
            selectResource(null, false);
        }
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.SELECT_ALL.equals(event.command)) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.SELECT_ALL.equals(event.command)) {
            event.stopPropagation();
            selectAll();
        }
    }

    private void setupToolbar() {
        searchField.setTextResponder(text -> {
            searchFilter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            applySearchFilter();
        });
        searchField.layout(layout -> {
            layout.height(14);
            layout.flex(1);
        }).style(style -> style.tooltips("editor.resource.search"))
                .addClass("__resource-toolbar_search-field__").moveInlineAsDefault();

        var sortButton = createToolbarButton(Icons.SORT, "editor.assets.sort", () -> {});
        sortButton.setOnClick(e -> {
            e.stopPropagation();
            if (editor != null) {
                editor.openMenu(sortButton, sortButton.getPositionX(),
                        sortButton.getPositionY() + sortButton.getSizeHeight(), createSortMenu());
            }
        });
        sortButton.addClass("__resource-toolbar_button__");

        toolbar.layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            layout.paddingHorizontal(2);
        }).addChildren(sortButton, searchField).addClass("__resource-toolbar__").moveInlineAsDefault();
    }

    public static Button createToolbarButton(IGuiTexture icon, String tooltip, Runnable onClick) {
        var button = new Button().setOnClick(e -> {
            e.stopPropagation();
            onClick.run();
        }).noText().buttonStyle(style -> {
            style.baseTexture(icon);
            style.hoverTexture(icon.copy().setColor(ColorPattern.GRAY.color));
            style.pressedTexture(icon);
        });
        button.layout(layout -> {
            layout.width(10);
            layout.height(10);
            layout.paddingAll(0);
        });
        button.style(style -> style.tooltips(tooltip));
        return button;
    }

    protected TreeBuilder.Menu createSortMenu() {
        var instance = resourceProvider.getResourceInstance();
        var menu = TreeBuilder.Menu.start();
        for (var mode : Resource.SortMode.values()) {
            if (mode.fileBased && !(resourceProvider instanceof FileResourceProvider<?>)) continue;
            menu.leaf(instance.getSortMode() == mode ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY,
                    "editor.resource.sort_" + mode.name().toLowerCase(Locale.ROOT), () -> setSortMode(mode));
        }
        menu.crossLine();
        menu.leaf(instance.isSortAscending() ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY,
                "editor.assets.sort_ascending", () -> setSortAscending(true));
        menu.leaf(instance.isSortAscending() ? IGuiTexture.EMPTY : Icons.CHECK_SPRITE,
                "editor.assets.sort_descending", () -> setSortAscending(false));
        return menu;
    }

    public void setSortMode(Resource.SortMode sortMode) {
        var instance = resourceProvider.getResourceInstance();
        if (instance.getSortMode() == sortMode) return;
        instance.setSortMode(sortMode);
        reloadResourceContainer();
    }

    public void setSortAscending(boolean sortAscending) {
        var instance = resourceProvider.getResourceInstance();
        if (instance.isSortAscending() == sortAscending) return;
        instance.setSortAscending(sortAscending);
        reloadResourceContainer();
    }

    public String getSearchText() {
        return searchField.getText();
    }

    public void setSearchText(String text) {
        searchField.setText(text, true);
    }

    public boolean matchesSearch(IResourcePath path) {
        return searchFilter.isEmpty() || nameSupplier.apply(path).toLowerCase(Locale.ROOT).contains(searchFilter);
    }

    /** Hides cells instead of rebuilding them: some thumbnails (renderers) are expensive to build. */
    protected void applySearchFilter() {
        for (var entry : resourceUIs.entrySet()) {
            entry.getValue().setDisplay(matchesSearch(entry.getKey()));
        }
        // the menu would otherwise act on a selection nobody can see
        var remaining = selectedResources.stream().filter(this::matchesSearch).toList();
        if (remaining.size() != selectedResources.size()) {
            setSelectedResources(remaining, false);
        }
    }

    private void revealIfFiltered(IResourcePath path) {
        if (!matchesSearch(path)) {
            setSearchText("");
        }
    }

    protected void sortPaths(List<IResourcePath> paths) {
        var instance = resourceProvider.getResourceInstance();
        var comparator = pathComparator(instance.getSortMode());
        if (comparator != null) {
            paths.sort(comparator);
        }
        if (!instance.isSortAscending()) {
            Collections.reverse(paths);
        }
    }

    /** Ascending order of a mode, or null for {@link Resource.SortMode#DEFAULT}: the provider's own order. */
    @Nullable
    protected Comparator<IResourcePath> pathComparator(Resource.SortMode mode) {
        return switch (mode) {
            case DEFAULT -> null;
            case NAME -> Comparator.comparing(nameSupplier, String.CASE_INSENSITIVE_ORDER);
            case MODIFIED -> byFileKey(File::lastModified);
            case SIZE -> byFileKey(File::length);
        };
    }

    /** Resolves the key once per path: it is a stat call, and a comparator asks once per comparison. */
    private static Comparator<IResourcePath> byFileKey(ToLongFunction<File> key) {
        var keys = new HashMap<IResourcePath, Long>();
        return Comparator.comparingLong(path -> keys.computeIfAbsent(path,
                p -> p instanceof FilePath filePath ? key.applyAsLong(filePath.file) : 0L));
    }

    private int insertionIndex(IResourcePath path) {
        var instance = resourceProvider.getResourceInstance();
        var ascending = instance.isSortAscending();
        var children = scrollerView.viewContainer.getChildren();
        var comparator = pathComparator(instance.getSortMode());
        // a new resource is last in the provider's own order
        if (comparator == null) return ascending ? children.size() : 0;
        var pathOfCell = new IdentityHashMap<UIElement, IResourcePath>(resourceUIs.size());
        resourceUIs.forEach((key, ui) -> pathOfCell.put(ui, key));
        for (var i = 0; i < children.size(); i++) {
            var other = pathOfCell.get(children.get(i));
            if (other == null) continue;
            var order = comparator.compare(path, other);
            if (ascending ? order < 0 : order > 0) return i;
        }
        return children.size();
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
        var expiry = Util.getMillis() + 400;
        flash.addEventListener(UIEvents.TICK, e -> {
            if (Util.getMillis() > expiry) {
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
                .addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) {
                        lastClickPath = key;
                        pendingCollapsePath = null;
                        if (e.isShiftDown()) {
                            selectRangeTo(key, e.isCtrlDown(), true);
                        } else if (e.isCtrlDown()) {
                            toggleSelectResource(key, true);
                        } else if (selectedResources.size() > 1 && selectedResources.contains(key)) {
                            // Defer collapsing a multi-selection until mouse-up so dragging stays possible.
                            pendingCollapsePath = key;
                        } else {
                            selectResource(key);
                        }
                    } else if (e.button == 1) {
                        if (!selectedResources.contains(key)) {
                            selectResource(key);
                        }
                    }
                })
                .addEventListener(UIEvents.DOUBLE_CLICK, e -> {
                    if (!e.isCtrlDown() && !e.isShiftDown()) {
                        editResource(key);
                    }
                })
                .addEventListener(UIEvents.MOUSE_UP, e -> {
                    if (e.button == 0 && pendingCollapsePath != null && pendingCollapsePath.equals(key)) {
                        selectResource(key);
                    }
                    pendingCollapsePath = null;
                    lastClickPath = null;
                }).addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                    if (lastClickPath == key && isMouseDown(0)) {
                        pendingCollapsePath = null;
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
        pendingCollapsePath = null;
        if (headless) {
            notifyInvalidated(null);
            return;
        }
        scrollerView.clearAllScrollViewChildren();
        var paths = new ArrayList<IResourcePath>();
        resourceProvider.forEach(entry -> paths.add(entry.getKey()));
        sortPaths(paths);
        for (var path : paths) {
            addResourceUI(path, scrollerView.viewContainer.getChildren().size());
        }
        // the cells are new, so the highlight has to be put back on the ones still selected,
        // and dropped for any resource that is no longer here
        var previous = new ArrayList<>(selectedResources);
        selectedResources.clear();
        selected = null;
        setSelectedResources(previous, false);
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
        ui.setDisplay(matchesSearch(path));
        var index = scrollerView.viewContainer.getChildren().indexOf(resourceUIs.get(path));
        scrollerView.removeScrollViewChild(resourceUIs.get(path));
        scrollerView.addScrollViewChildAt(ui, index);
        resourceUIs.put(path, ui);
        if (selectedResources.contains(path)) {
            applySelectionHighlight(ui, true);
        }
    }

    public void appendResourceUI(IResourcePath resourcePath) {
        if (resourcePath == null || resourceUIs.containsKey(resourcePath) || !resourceProvider.hasResource(resourcePath)) return;
        if (headless) {
            notifyInvalidated(resourcePath);
            return;
        }
        addResourceUI(resourcePath, insertionIndex(resourcePath));
    }

    private void addResourceUI(IResourcePath resourcePath, int index) {
        if (resourceUIs.containsKey(resourcePath) || !resourceProvider.hasResource(resourcePath)) return;
        var ui = createResourceUI(resourcePath);
        ui.setDisplay(matchesSearch(resourcePath));
        resourceUIs.put(resourcePath, ui);
        scrollerView.addScrollViewChildAt(ui, index);
    }

    public Set<IResourcePath> getSelectedResources() {
        return Collections.unmodifiableSet(selectedResources);
    }

    public boolean isSelected(@Nullable IResourcePath path) {
        return path != null && selectedResources.contains(path);
    }

    public void selectResource(@Nullable IResourcePath resourcePath) {
        selectResource(resourcePath, true);
    }

    /**
     * Selects the given resource (replacing any previous multi-selection).
     *
     * @param resourcePath the resource path to select, or null to clear selection.
     * @param notify whether the {@link #onResourceSelect} callback should be notified.
     */
    public void selectResource(@Nullable IResourcePath resourcePath, boolean notify) {
        selectionAnchor = resourcePath;
        setSelectedResources(resourcePath == null ? List.of() : List.of(resourcePath), notify);
    }

    /**
     * Replaces the current selection with {@code paths}.
     *
     * @param paths  the resource paths to select.
     * @param notify whether {@link #onResourceSelect} should be notified for the primary selected resource.
     */
    public void setSelectedResources(Collection<IResourcePath> paths, boolean notify) {
        if (headless) {
            selectedResources.clear();
            IResourcePath primary = null;
            for (var path : paths) {
                if (path != null) {
                    selectedResources.add(path);
                    primary = path;
                }
            }
            selected = primary;
            if (notify && onResourceSelect != null && primary != null) {
                var res = resourceProvider.getResource(primary);
                if (res != null) {
                    onResourceSelect.accept(res);
                }
            }
            return;
        }
        var next = new LinkedHashSet<IResourcePath>();
        IResourcePath primary = null;
        for (var path : paths) {
            if (path != null && resourceUIs.containsKey(path)) {
                next.add(path);
                primary = path;
            }
        }
        for (var oldPath : selectedResources) {
            if (!next.contains(oldPath)) {
                applySelectionHighlight(resourceUIs.get(oldPath), false);
            }
        }
        for (var newPath : next) {
            applySelectionHighlight(resourceUIs.get(newPath), true);
        }
        selectedResources.clear();
        selectedResources.addAll(next);
        selected = primary;
        if (selected == null) {
            selectionAnchor = null;
        } else if (selectionAnchor == null || !selectedResources.contains(selectionAnchor)) {
            selectionAnchor = selected;
        }
        if (notify && onResourceSelect != null && primary != null) {
            var res = resourceProvider.getResource(primary);
            if (res != null) {
                onResourceSelect.accept(res);
            }
        }
        updateBottomBar();
    }

    public void toggleSelectResource(IResourcePath path, boolean notify) {
        if (path == null) return;
        var next = new LinkedHashSet<>(selectedResources);
        boolean added;
        if (next.contains(path)) {
            next.remove(path);
            added = false;
        } else {
            next.add(path);
            added = true;
            selectionAnchor = path;
        }
        setSelectedResources(next, notify && added);
    }

    public void selectRangeTo(IResourcePath target, boolean additive, boolean notify) {
        if (target == null) return;
        var ordered = visiblePathsInOrder();
        int toIdx = ordered.indexOf(target);
        if (toIdx < 0) return;
        var anchor = selectionAnchor != null && ordered.contains(selectionAnchor)
                ? selectionAnchor
                : (selected != null && ordered.contains(selected) ? selected : target);
        int fromIdx = ordered.indexOf(anchor);
        int start = Math.min(fromIdx, toIdx);
        int end = Math.max(fromIdx, toIdx);
        var next = additive ? new LinkedHashSet<>(selectedResources) : new LinkedHashSet<IResourcePath>();
        for (int i = start; i <= end; i++) {
            next.add(ordered.get(i));
        }
        selectionAnchor = anchor;
        setSelectedResources(next, notify);
    }

    public void selectAll() {
        setSelectedResources(visiblePathsInOrder(), false);
    }

    private List<IResourcePath> visiblePathsInOrder() {
        var cellToKey = new IdentityHashMap<UIElement, IResourcePath>(resourceUIs.size());
        resourceUIs.forEach((key, ui) -> cellToKey.put(ui, key));
        var ordered = new ArrayList<IResourcePath>(resourceUIs.size());
        for (var child : scrollerView.viewContainer.getChildren()) {
            if (!child.isDisplayed()) continue;
            var key = cellToKey.get(child);
            if (key != null) ordered.add(key);
        }
        return ordered;
    }

    private void applySelectionHighlight(@Nullable UIElement ui, boolean isSelected) {
        if (ui == null) return;
        if (isSelected) {
            ui.style(style -> style.overlayTexture(selectedTexture));
            ui.addClass("__selected__");
        } else {
            ui.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            ui.removeClass("__selected__");
        }
    }

    protected void updateBottomBar() {
        if (selectedResources.size() > 1) {
            bottomBar.setText(Component.translatable("editor.resource.selected_count", selectedResources.size()).getString());
        } else {
            bottomBar.setText(selected == null ? "" : selected.getPathWithType());
        }
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
        if (--pollCountdown < 0) {
            pollCountdown = POLL_INTERVAL - 1;
            if (resourceProvider.checkAndUpdateResourceProvider()) {
                reloadResourceContainer();
                dirtyResources.clear();
            }
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
        boolean multi = selectedResources.size() > 1;
        if (multi) {
            var selectionSnapshot = List.copyOf(selectedResources);
            menu.leaf("ldlib.gui.editor.menu.copy_path", () ->
                    ClipboardManager.INSTANCE.copyDirect(selectionSnapshot.stream()
                            .map(IResourcePath::getPathWithType)
                            .collect(Collectors.joining("\n")))
            );
            menu.crossLine();
            var copyable = selectionSnapshot.stream().filter(canCopy).toList();
            if (!copyable.isEmpty()) {
                menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyResources(copyable));
            }
            if (supportAdd.getAsBoolean() && addDefault != null) {
                menu.leaf(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", this::addNewResource);
            }
            if (onCreateMenu != null) {
                onCreateMenu.accept(this, menu);
            }
            var removable = selectionSnapshot.stream().filter(canRemove).toList();
            if (!removable.isEmpty()) {
                menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> removeResources(removable, true));
            }
        } else {
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
        copyResourceTo(copied, resourceProvider, copyNameOf(key));
    }

    /** The name a copy starts with: a builtin's name may hold a {@code :}, which no file name can. */
    protected String copyNameOf(IResourcePath key) {
        return toValidName(resourceProvider.getResourceName(key)) + "_copy";
    }

    /** Whether a character may be typed into a resource name: one a resource location and a file name both take. */
    public static boolean isValidNameChar(char character) {
        return ResourceLocation.isAllowedInResourceLocation(character) && FileOps.isValidNameChar(character);
    }

    private static String toValidName(String name) {
        return FileOps.INVALID_NAME.matcher(name).replaceAll("_");
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
        showCopyDialog(targets, defaultTarget, copyNameOf(key),
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
                .setCharValidator(ResourceProviderContainer::isValidNameChar);
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
        var validName = toValidName(name);
        var key = target.createSubPath(validName);
        var count = 1;
        while (target.hasResource(key)) {
            key = target.createSubPath(validName + "_" + count);
            count++;
        }
        var finalKey = key;
        var isHere = target == resourceProvider;
        editor.historyView.pushHistory(Component.translatable("editor.copy_resource"), EditAction.of(
                () -> {
                    if (isHere) {
                        addResourceInternal(value, finalKey);
                    } else if (!target.addResource(finalKey, value)) {
                        Dialog.showNotification("ldlib.gui.editor.menu.copy", "editor.copy.failed", null).show(this);
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

    /**
     * Asks where copies of multiple resources should go, then writes them all in one undoable action.
     */
    public void copyResources(Collection<IResourcePath> keys) {
        var validKeys = keys.stream().filter(k -> k != null && canCopy.test(k)).distinct().toList();
        if (validKeys.isEmpty()) return;
        if (validKeys.size() == 1) {
            copyResource(validKeys.getFirst());
            return;
        }
        var instance = resourceProvider.getResourceInstance();
        var targets = instance.listWritableProviders();
        if (targets.isEmpty()) {
            Dialog.showNotification("ldlib.gui.editor.menu.copy", "editor.copy.no_target", null).show(this);
            return;
        }
        var copiedEntries = new ArrayList<Tuple<IResourcePath, T>>();
        for (var key : validKeys) {
            var copied = detachedCopy(key);
            if (copied != null) {
                copiedEntries.add(new Tuple<>(key, copied));
            }
        }
        if (copiedEntries.isEmpty()) return;
        var defaultTarget = targets.contains(resourceProvider) ? resourceProvider : targets.getFirst();
        showBatchCopyDialog(targets, defaultTarget, copiedEntries.size(),
                target -> copyResourcesTo(copiedEntries, target));
    }

    protected void showBatchCopyDialog(List<IResourceProvider<T>> targets, IResourceProvider<T> defaultTarget,
                                       int count, Consumer<IResourceProvider<T>> onConfirm) {
        var selector = new Selector<IResourceProvider<T>>()
                .setCandidateUIProvider(UIElementProvider.iconText(
                        provider -> provider == null ? IGuiTexture.EMPTY : provider.getType().getIcon(),
                        provider -> Component.literal(provider == null ? "" : provider.getName())))
                .setCandidates(targets)
                .setSelected(defaultTarget, false);
        selector.getLayout().widthPercent(100);
        selector.addClass("__copy-target-selector__");

        var dialog = new Dialog().setTitle("ldlib.gui.editor.menu.copy_to");
        dialog.addContent(new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightAuto();
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        }).addChildren(
                new Label().setText(Component.translatable("editor.copy.target_multiple", count)),
                selector));
        dialog.addExternalElement(selector.dialog);
        dialog.addButton(new Button().setOnClick(e -> {
            var target = selector.getValue();
            dialog.close();
            if (target != null) {
                onConfirm.accept(target);
            }
        }).setText("ldlib.gui.tips.confirm").addClass("__confirm-button__"));
        dialog.addButton(new Button().setOnClick(e -> dialog.close())
                .setText("ldlib.gui.tips.cancel").addClass("__cancel-button__"));
        dialog.show(this);
    }

    protected void copyResourcesTo(List<Tuple<IResourcePath, T>> entries, IResourceProvider<T> target) {
        var isHere = target == resourceProvider;
        var reserved = new HashSet<IResourcePath>();
        var planned = new ArrayList<Tuple<IResourcePath, T>>(entries.size());
        for (var entry : entries) {
            var baseName = isHere
                    ? copyNameOf(entry.getA())
                    : toValidName(resourceProvider.getResourceName(entry.getA()));
            var key = target.createSubPath(baseName);
            var count = 1;
            while (target.hasResource(key) || reserved.contains(key)) {
                key = target.createSubPath(baseName + "_" + count);
                count++;
            }
            reserved.add(key);
            planned.add(new Tuple<>(key, entry.getB()));
        }
        editor.historyView.pushHistory(Component.translatable("editor.copy_resource"), EditAction.of(
                () -> {
                    var addedHere = new ArrayList<IResourcePath>();
                    boolean anyFailed = false;
                    for (var item : planned) {
                        if (isHere) {
                            resourceProvider.addResource(item.getA(), item.getB());
                            appendResourceUI(item.getA());
                            revealIfFiltered(item.getA());
                            addedHere.add(item.getA());
                        } else if (!target.addResource(item.getA(), item.getB())) {
                            anyFailed = true;
                        }
                    }
                    if (isHere && !addedHere.isEmpty()) {
                        setSelectedResources(addedHere, false);
                    }
                    if (anyFailed) {
                        Dialog.showNotification("ldlib.gui.editor.menu.copy", "editor.copy.failed", null).show(this);
                    }
                },
                () -> {
                    for (var item : planned) {
                        if (isHere) {
                            removeResourceInternal(item.getA());
                        } else {
                            target.removeResource(item.getA());
                        }
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

    public void removeResources(Collection<IResourcePath> keys, boolean confirm) {
        var toRemove = new ArrayList<Tuple<IResourcePath, T>>();
        for (var key : new LinkedHashSet<>(keys)) {
            if (key == null || !canRemove.test(key)) continue;
            var value = resourceProvider.getResource(key);
            if (value != null) {
                toRemove.add(new Tuple<>(key, value));
            }
        }
        if (toRemove.isEmpty()) return;
        if (toRemove.size() == 1) {
            removeResource(toRemove.getFirst().getA(), confirm);
            return;
        }
        Runnable perform = () -> editor.historyView.pushHistory(
                Component.translatable("editor.remove_resource"),
                EditAction.of(
                        () -> {
                            for (var item : toRemove) {
                                removeResourceInternal(item.getA());
                            }
                        },
                        () -> {
                            var restored = new ArrayList<IResourcePath>(toRemove.size());
                            for (var item : toRemove) {
                                resourceProvider.addResource(item.getA(), item.getB());
                                appendResourceUI(item.getA());
                                revealIfFiltered(item.getA());
                                restored.add(item.getA());
                            }
                            setSelectedResources(restored, false);
                        }));
        if (confirm) {
            Dialog.showCheckBox("ldlib.gui.editor.menu.remove",
                    Component.translatable("editor.remove.confirm_multiple", toRemove.size()),
                    result -> {
                        if (result) {
                            perform.run();
                        }
                    }).show(editor);
        } else {
            perform.run();
        }
    }

    private void addResourceInternal(T value, IResourcePath finalKey) {
        resourceProvider.addResource(finalKey, value);
        appendResourceUI(finalKey);
        revealIfFiltered(finalKey);
        selectResource(finalKey);
    }

    private void removeResourceInternal(IResourcePath key) {
        resourceProvider.removeResource(key);
        var ui = resourceUIs.remove(key);
        if (ui != null) {
            scrollerView.removeScrollViewChild(ui);
        }
        if (key != null && selectedResources.remove(key)) {
            if (key.equals(selected)) {
                selected = selectedResources.isEmpty() ? null : selectedResources.getLast();
            }
            if (key.equals(selectionAnchor)) {
                selectionAnchor = selected;
            }
            updateBottomBar();
        } else if (key != null && key.equals(selected)) {
            selected = null;
            updateBottomBar();
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
     * <p>The text field is mounted as an overlay on the UI root at least {@link #INLINE_RENAME_MIN_WIDTH}
     * wide, so narrow grid cells (e.g. small icon sizes) do not squeeze the input into an unusable sliver.
     * Once editing ends, the overlay is removed and the label returns to its normal cell-bounded width.</p>
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
        var initialText = initial == null ? "" : initial;
        var textField = new TextField().setText(initialText).setCharValidator(charValidator);
        textField.addClass(CLASS_RENAME_FIELD);
        var cancelled = new boolean[1];
        var finished = new boolean[1];
        var removeListener = new UIEventListener[1];
        var mui = cell.getModularUI();
        Runnable cleanup = () -> {
            if (finished[0]) return;
            finished[0] = true;
            if (removeListener[0] != null) {
                cell.removeEventListener(UIEvents.REMOVED, removeListener[0]);
            }
            var newName = textField.getText().trim();
            // Verbatim, like the label this replaced: restoring it through a translation lookup would
            // leave a folder named after a key showing that key's translation until the next refresh.
            label.setText(initialText, false);
            textField.removeSelf();
            if (!cancelled[0]) {
                onCommit.accept(newName);
            }
        };
        removeListener[0] = e -> {
            if (!finished[0]) {
                cancelled[0] = true;
                cleanup.run();
            }
        };
        cell.addEventListener(UIEvents.REMOVED, removeListener[0]);
        textField.addEventListener(UIEvents.BLUR, e -> cleanup.run());
        textField.addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == GLFW.GLFW_KEY_ENTER || e.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                e.stopPropagation();
                textField.blur();
            } else if (e.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                e.stopPropagation();
                cancelled[0] = true;
                textField.blur();
            }
        });

        label.setText("", false);
        if (mui != null) {
            var root = mui.ui.rootElement;
            float labelW = label.getSizeWidth();
            float labelH = label.getSizeHeight();
            float rawW = Math.max(labelW, INLINE_RENAME_MIN_WIDTH);
            float targetW = root.getSizeWidth() > 0 ? Math.min(rawW, root.getSizeWidth()) : rawW;
            float targetH = Math.max(14f, Math.min(labelH, 16f));
            boolean isList = cell.hasClass("__resource-cell_list__");
            float desiredX = isList ? label.getPositionX() : label.getPositionX() + (labelW - targetW) / 2f;
            float desiredY = label.getPositionY() + (labelH - targetH) / 2f;
            var localOffset = root.worldToLocalLayoutOffset(new Vector2f(desiredX, desiredY));
            float clampedLeft = Math.max(0f, Math.min(localOffset.x, Math.max(0f, root.getSizeWidth() - targetW)));
            float clampedTop = Math.max(0f, Math.min(localOffset.y, Math.max(0f, root.getSizeHeight() - targetH)));
            Style.importantPipeline(textField.getLayout(), l -> l
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(clampedLeft)
                    .top(clampedTop)
                    .width(targetW)
                    .height(targetH));
            root.addChild(textField);
        } else {
            label.addChild(textField);
        }
        textField.focus();
        textField.setCursor(initialText.length());
        textField.setSelection(0, initialText.length());
    }

    public void renameResource(IResourcePath key) {
        if (key == null || !canRename.test(key)) return;
        startInlineRename(resourceUIs.get(key), nameSupplier.apply(key),
                ResourceProviderContainer::isValidNameChar,
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
        if (!resourceProvider.hasResource(key)) return null;
        var trimmed = newName.trim();
        var newPath = resourceProvider.createSubPath(trimmed);
        if (newPath.equals(key)) return key;
        // a name the provider cannot store, e.g. "a/b" for a file provider, which lists one folder
        if (resourceProvider instanceof ResourceProvider<T> provider && !provider.supportResourcePath(newPath)) return null;
        var count = 0;
        while (resourceProvider.hasResource(newPath)) {
            count++;
            newPath = resourceProvider.createSubPath(trimmed + " (" + count + ")");
        }
        onRename(key, newPath);
        return newPath;
    }

    protected void onRename(IResourcePath oldPath, IResourcePath newPath) {
        editor.historyView.pushHistory(Component.translatable("editor.rename_resource"), EditAction.of(
                () -> moveResource(oldPath, newPath),
                () -> moveResource(newPath, oldPath)));
    }

    /**
     * Stores the resource at {@code from} under {@code to} and drops the old one, its cell with it.
     */
    private void moveResource(IResourcePath from, IResourcePath to) {
        var value = resourceProvider.getResource(from);
        // the old one is removed only once the new one is written
        if (value == null || !resourceProvider.addResource(to, value)) return;
        // told before the old one goes, so whatever is kept beside the old path can still be moved
        onResourceMoved(from, to);
        removeResourceInternal(from);
        appendResourceUI(to);
        revealIfFiltered(to);
        selectResource(to);
    }

    /**
     * Called when a resource moves from {@code from} to {@code to} — by a rename, and by its undo and redo, which
     * {@link #onRename} does not see — once it is stored at {@code to} and before {@code from} is removed. Where an
     * editor open on the resource follows it.
     */
    protected void onResourceMoved(IResourcePath from, IResourcePath to) {
    }

    /** @return false if none is open on {@code path} in this container's editor; one in another editor, a minimized one say, does not count */
    protected boolean selectOpenedView(Collection<? extends Tuple<IResourcePath, ? extends View>> opened, IResourcePath path) {
        for (var entry : opened) {
            var view = entry.getB();
            if (!entry.getA().equals(path) || view.getEditor() != editor) continue;
            bringToFront(view);
            return true;
        }
        return false;
    }

    /** Selects {@code view}'s tab: adding a view to a container leaves the tab already selected there in front. */
    protected static void bringToFront(View view) {
        var container = view.getViewContainer();
        if (container != null) {
            container.selectView(view);
        }
    }

}
