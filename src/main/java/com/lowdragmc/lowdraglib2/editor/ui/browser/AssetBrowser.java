package com.lowdragmc.lowdraglib2.editor.ui.browser;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceBottomBar;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.utils.FileUtility;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A file system browser over the {@code ldlib2} folder, in the spirit of Unity's Project window: a
 * directory tree on the left, the contents of the selected directory as a grid on the right.
 * <p>
 * Files that a loaded {@link Resource} type can read are shown with that type's own thumbnail and open
 * in that type's own editor on double click; everything else is an ordinary file that can be renamed,
 * copied, moved or deleted. All of the per-type behavior is borrowed from the resource system through
 * {@link ResourceBehaviorCache} rather than reimplemented here.
 */
public class AssetBrowser extends UIElement {
    /** What the grid orders its entries by. Folders are always listed before files regardless. */
    public enum SortMode {
        NAME, TYPE, SIZE, MODIFIED
    }

    /** Payload of a drag started in the browser, so drop targets can recognise plain files. */
    public record DraggedAssets(List<File> files) {}

    /** Clipboard payload of a copy/cut in the browser. */
    public record ClipboardAssets(List<File> files, boolean cut) {}

    /** How often the current directory is checked for external changes, in ticks. */
    private static final int POLL_INTERVAL = 10;
    /**
     * Beyond this many entries the live thumbnails are replaced with the plain resource type icon:
     * some types (renderers) build a whole scene renderer per thumbnail.
     */
    private static final int LIVE_THUMBNAIL_LIMIT = 200;
    /** A global search stops walking at this many matches, and shows at most {@link #SEARCH_SHOWN_LIMIT}. */
    private static final int SEARCH_LIMIT = 2000;
    private static final int SEARCH_SHOWN_LIMIT = 500;
    /** Results added to the grid per tick while a search streams in. */
    private static final int SEARCH_BATCH = 100;
    private static final long SEARCH_DEBOUNCE_MS = 150;

    public final Editor editor;
    public final SplitView.Horizontal splitView = new SplitView.Horizontal();
    public final UIElement treePane = new UIElement();
    public final ScrollerView treeScroller = new ScrollerView();
    public final TreeList<FileNode> tree = new TreeList<>();
    /** Keeps the tree's scroll bar clear of the split handle, and draws the pane's right edge. */
    public final UIElement treeSeparator = new UIElement();
    public final UIElement contentPane = new UIElement();
    public final UIElement toolbar = new UIElement();
    public final TextField searchField = new TextField();
    public final UIElement breadcrumb = new UIElement();
    /** Replaces the breadcrumb during a global search. */
    public final Label searchStatus = new Label();
    public final ScrollerView gridScroller = new ScrollerView();
    public final ResourceBottomBar bottomBar = new ResourceBottomBar(
            ResourceProviderContainer.MIN_UI_WIDTH, ResourceProviderContainer.MAX_UI_WIDTH);

    private final ResourceBehaviorCache behaviors;

    @Getter
    private File root;
    @Getter
    @Nullable
    private File currentDirectory;
    /** Whether files no loaded resource type can read are listed too. */
    @Getter
    private boolean showAllFiles = false;
    /** Lower-cased name filter from the search field; empty means everything is shown. */
    private String searchFilter = "";
    @Getter
    private boolean globalSearch = false;
    /** What the grid is ordered by, within the folders-first rule. */
    @Getter
    private SortMode sortMode = SortMode.NAME;
    @Getter
    private boolean sortAscending = true;
    /** The resource types to show. Empty means no filtering, i.e. every type. */
    private final Set<Resource<?>> typeFilter = new LinkedHashSet<>();
    /** Cell size, local to the browser so it never writes a resource type's saved display settings. */
    @Getter
    private int uiWidth = 30;
    @Getter
    private Resource.DisplayMode displayMode = Resource.DisplayMode.GRID;
    /** @see ResourceProviderContainer#getSelectedTexture() */
    @Getter @Setter
    private IGuiTexture selectedTexture = ResourceProviderContainer.defaultSelectedTexture();

    /** What else may be dropped on a folder besides asset files, see {@link #addDropHandler}. */
    private final List<DropHandler> dropHandlers = new ArrayList<>();

    // runtime
    private final Map<File, UIElement> entryUIs = new LinkedHashMap<>();
    @Nullable
    private File selected;
    @Nullable
    private File hovered;
    @Nullable
    private File lastClickFile;
    private boolean gridDirty;
    private int pollCounter;
    private long directoryStamp;
    /** Selected and scrolled to once the grid it belongs in is built. */
    @Nullable
    private File pendingSelection;

    // global search runtime
    @Nullable
    private AssetSearch search;
    /** The running search re-runs the query on show: swap its results in when done instead of streaming. */
    private boolean searchReplacesGrid;
    /** 0 when no walk is pending. */
    private long searchDueAt;
    private final List<AssetSearch.Hit> searchHits = new ArrayList<>();
    private int searchRendered;
    /** In grid order, for binary search insertion. */
    private final List<GridEntry> searchEntries = new ArrayList<>();
    private Comparator<GridEntry> searchComparator = (a, b) -> 0;
    @Nullable
    private Component searchStatusText;

    public AssetBrowser(Editor editor) {
        this(editor, new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID));
    }

    public AssetBrowser(Editor editor, File root) {
        this.editor = editor;
        this.root = root;
        this.behaviors = new ResourceBehaviorCache(this, editor);
        this.behaviors.setOnResourceInvalidated(path -> requestGridRebuild());

        // same shape as ResourceContainer, the other thing a resource tab can hold
        getLayout().flex(1);
        getLayout().heightPercent(100);
        getLayout().flexDirection(FlexDirection.ROW);

        setupTree();
        setupToolbar();
        setupGrid();

        treePane.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
        }).addChildren(treeScroller, treeSeparator).addClass("__asset-browser_tree-pane__").moveInlineAsDefault();

        contentPane.layout(layout -> {
            layout.heightPercent(100);
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
        }).addChildren(toolbar, breadcrumb, gridScroller, bottomBar)
                .addClass("__asset-browser_content-pane__").moveInlineAsDefault();

        splitView.left(treePane).right(contentPane).setPercentage(20);
        splitView.addClass("__asset-browser_split-view__");

        bottomBar.setOnValueChanged(this::setUiWidth);
        addChild(splitView);
        addEventListener(UIEvents.FILE_DROP, this::onFilesDropped);

        addClass("__asset-browser__");
        moveInlineAsDefault();

        setRoot(root);
    }

    // ------------------------------------------------------------------------------------ setup

    private void setupTree() {
        treeScroller.scrollerStyle(style -> style.mode(ScrollerMode.BOTH)).layout(layout -> {
            layout.heightPercent(100);
            layout.flex(1);
        }).addClass("__asset-browser_tree-scroller__").moveInlineAsDefault();
        // the split handle is the last couple of pixels of the left pane. Without this gap the tree's
        // scroll bar sits right on top of it and swallows the drag.
        treeSeparator.layout(layout -> {
            layout.width(1);
            layout.heightPercent(100);
            layout.marginHorizontal(1);
        }).style(style -> style.backgroundTexture(ColorPattern.T_WHITE.rectTexture()))
                .addClass("__asset-browser_tree-separator__").moveInlineAsDefault();
        // rows size to their own content, so a long name scrolls horizontally instead of being clipped
        tree.setWidthFitsContent(true)
                .setNodeUISupplier(AssetBrowser::createTreeNodeUI)
                .setOnSelectedChanged(selection -> selection.stream().findFirst()
                        .ifPresent(node -> openDirectory(node.getKey())))
                .setOnNodeUICreated((node, ui) -> {
                    attachDragSource(ui, node.getKey(), null);
                    attachDropTarget(ui, node::getKey);
                })
                .addClass("__asset-browser_tree__");
        treeScroller.addScrollViewChild(tree);
        treeScroller.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1) {
                var node = tree.getHoveredNode();
                openMenu(event, node == null ? currentDirectory : node.getKey(), true);
            }
        });
    }

    private void setupToolbar() {
        searchField.setTextResponder(text ->
                onSearchTextChanged(text == null ? "" : text.trim().toLowerCase(Locale.ROOT)));
        searchField.layout(layout -> {
            layout.height(14);
            layout.flex(1);
        }).style(style -> style.tooltips("editor.assets.search"))
                .addClass("__asset-browser_search-field__").moveInlineAsDefault();
        var searchScopeButton = iconButton(DynamicTexture.of(() -> globalSearch ? Icons.GLOBAL : Icons.LOCAL),
                "editor.assets.search_scope", () -> setGlobalSearch(!globalSearch));
        searchScopeButton.addClass("__asset-browser_search-scope__");

        toolbar.layout(layout -> {
            layout.widthPercent(100);
            layout.height(14);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(2);
            layout.paddingHorizontal(2);
        }).addChildren(
                iconButton(Icons.FOLDER, "ldlib.gui.tips.open_folder", () -> {
                    var directory = currentDirectory;
                    if (directory == null) return;
                    if (!directory.exists()) directory.mkdirs();
                    Util.getPlatform().openFile(directory);
                }),
                iconButton(DynamicTexture.of(() -> showAllFiles ? Icons.EYE : Icons.EYE_OFF),
                        "editor.assets.show_all_files", () -> setShowAllFiles(!showAllFiles)),
                menuButton(Icons.SORT, "editor.assets.sort", this::createSortMenu),
                menuButton(DynamicTexture.of(() -> typeFilter.isEmpty() ? Icons.FILTER : Icons.FILTER_CHECK),
                        "editor.assets.filter", this::createFilterMenu, false),
                searchScopeButton,
                searchField
        ).addClass("__asset-browser_toolbar__").moveInlineAsDefault();
    }

    private void setupGrid() {
        breadcrumb.layout(layout -> {
            layout.widthPercent(100);
            layout.height(11);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingHorizontal(2);
        }).addClass("__asset-browser_breadcrumb__").moveInlineAsDefault();
        breadcrumb.setOverflowVisible(false);
        searchStatus.textStyle(style -> style.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL).fontSize(7))
                .setText("", false)
                .setOverflowVisible(false)
                .layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                });
        searchStatus.addClass("__asset-browser_search-status__").moveInlineAsDefault();

        gridScroller.scrollerStyle(style ->
                style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.ALWAYS)
        ).layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
        }).addClass("__asset-browser_grid__").moveInlineAsDefault();
        gridScroller.viewContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        gridScroller.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 1) {
                if (isGlobalSearchActive()) {
                    if (editor != null) {
                        editor.openMenu(this, event.x, event.y, createSearchResultMenu(hovered));
                    }
                } else {
                    openMenu(event, hovered, hovered == null || hovered.isDirectory());
                }
            } else if (event.button == 0 && hovered == null) {
                selectEntry(null);
            }
        });
        // the empty part of the grid is the folder on show
        attachDropTarget(gridScroller, () -> isGlobalSearchActive() ? null : currentDirectory);
    }

    /**
     * A folder row. Unlike {@link TreeList#iconTextTemplate}, the label reports its natural width
     * instead of taking {@code flex(1)} — that is what lets {@link TreeList#setWidthFitsContent} give a
     * deep tree a horizontal scroll bar rather than clipping the names.
     */
    private static UIElement createTreeNodeUI(FileNode node) {
        var icon = new UIElement().layout(layout -> {
            layout.setAspectRatio(1);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(Icons.FOLDER));
        icon.addClass("__asset-browser_tree-node-icon__").moveInlineAsDefault();

        var label = new Label();
        // Verbatim: a folder is named by whoever made it, not by a lang file.
        label.textStyle(style -> style.adaptiveWidth(true).textAlignVertical(Vertical.CENTER))
                .setText(node.getKey().getName(), false)
                .layout(layout -> layout.heightPercent(100));
        label.addClass("__asset-browser_tree-node-label__").moveInlineAsDefault();

        var row = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(2);
            layout.height(10);
        }).addChildren(icon, label);
        row.addClass("__asset-browser_tree-node__").moveInlineAsDefault();
        return row;
    }

    /** A toolbar button that drops its menu right underneath itself. */
    private Button menuButton(IGuiTexture icon, String tooltip, Supplier<TreeBuilder.Menu> menu) {
        return menuButton(icon, tooltip, menu, true);
    }

    /** @param closeOnClick false for a menu of toggles, so it survives picking an entry. */
    private Button menuButton(IGuiTexture icon, String tooltip, Supplier<TreeBuilder.Menu> menu,
                              boolean closeOnClick) {
        var button = iconButton(icon, tooltip, () -> {});
        button.setOnClick(e -> {
            e.stopPropagation();
            if (editor != null) {
                editor.openMenu(button, button.getPositionX(), button.getPositionY() + button.getSizeHeight(),
                        menu.get(), closeOnClick);
            }
        });
        return button;
    }

    protected TreeBuilder.Menu createSortMenu() {
        var menu = TreeBuilder.Menu.start();
        for (var mode : SortMode.values()) {
            menu.leaf(sortMode == mode ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY,
                    "editor.assets.sort_" + mode.name().toLowerCase(Locale.ROOT), () -> setSortMode(mode));
        }
        menu.crossLine();
        menu.leaf(sortAscending ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY,
                "editor.assets.sort_ascending", () -> setSortAscending(true));
        menu.leaf(sortAscending ? IGuiTexture.EMPTY : Icons.CHECK_SPRITE,
                "editor.assets.sort_descending", () -> setSortAscending(false));
        return menu;
    }

    /**
     * The type filter is a set of toggles, so this menu stays open while they are being picked. Its
     * icons are read every frame rather than captured, because the menu is built once and clicking an
     * entry no longer rebuilds it.
     */
    protected TreeBuilder.Menu createFilterMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.leaf(DynamicTexture.of(() -> typeFilter.isEmpty() ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY),
                Component.translatable("editor.assets.filter_all"), this::clearTypeFilter);
        menu.crossLine();
        // an empty filter means everything is shown, so every entry reads as ticked in that state
        for (var resource : List.copyOf(behaviors.availableResources())) {
            menu.leaf(DynamicTexture.of(() -> typeFilter.isEmpty() || typeFilter.contains(resource)
                            ? Icons.CHECKBOX_MARKED : Icons.CHECKBOX_BLANK),
                    resource.getDisplayName(), () -> toggleTypeFilter(resource));
        }
        return menu;
    }

    private Button iconButton(IGuiTexture icon, String tooltip, Runnable onClick) {
        var button = ResourceProviderContainer.createToolbarButton(icon, tooltip, onClick);
        button.addClass("__asset-browser_toolbar-button__");
        return button;
    }

    // ------------------------------------------------------------------------------- navigation

    public AssetBrowser setRoot(File root) {
        this.root = root;
        if (!root.exists()) {
            root.mkdirs();
        }
        tree.setRoot(new FileNode(root).setValid(FileNode::isDirectory));
        openDirectory(root);
        return this;
    }

    /** Re-reads the root, dropping every cached behavior. Called when the loaded resource types change. */
    public void reset() {
        behaviors.setDirectory(null);
        setRoot(root);
    }

    public void openDirectory(@Nullable File directory) {
        if (directory == null || !directory.isDirectory()) return;
        directory = spelledFromRoot(directory);
        if (isGlobalSearchActive()) {
            exitGlobalSearch();
        }
        this.currentDirectory = directory;
        this.selected = null;
        this.hovered = null;
        this.pendingSelection = null;
        behaviors.setDirectory(directory);
        rebuildBreadcrumb();
        this.directoryStamp = stampOf(directory);
        syncTree(directory);
        requestGridRebuild();
    }

    /** Opens the folder a file is in and selects it there. */
    public void revealFile(File file) {
        file = spelledFromRoot(file);
        var parent = file.getParentFile();
        if (parent == null || !parent.isDirectory()) return;
        openDirectory(parent);
        pendingSelection = file;
    }

    /**
     * A file inside the root spelled from the root, as the tree, the breadcrumb and the providers' folders are: they
     * compare files by path, so the same folder given absolute or normalized would match none of them.
     */
    private File spelledFromRoot(File file) {
        if (root == null) return file;
        var rootPath = root.toPath().toAbsolutePath().normalize();
        var path = file.toPath().toAbsolutePath().normalize();
        if (!path.startsWith(rootPath)) return file;
        return path.equals(rootPath) ? root : new File(root, rootPath.relativize(path).toString());
    }

    /** Expands the tree down to the folder on show and selects it. */
    private void syncTree(File directory) {
        var node = tree.getRoot();
        if (node == null) return;
        var chain = new ArrayList<File>();
        var file = directory;
        while (file != null && !file.equals(node.getKey())) {
            chain.add(0, file);
            file = file.getParentFile();
        }
        if (file == null) return;
        for (var step : chain) {
            tree.expandNode(node);
            FileNode next = null;
            for (var child : node.getChildren()) {
                if (child.getKey().equals(step)) {
                    next = child;
                    break;
                }
            }
            if (next == null) return;
            node = next;
        }
        // not notified, openDirectory is what called this
        tree.setSelected(Set.of(node), false);
    }

    private void rebuildBreadcrumb() {
        breadcrumb.clearAllChildren();
        if (isGlobalSearchActive()) {
            breadcrumb.addChild(searchStatus);
            updateSearchStatus();
            return;
        }
        var segments = new ArrayList<File>();
        for (var file = currentDirectory; file != null; file = file.getParentFile()) {
            segments.add(0, file);
            if (file.equals(root)) break;
        }
        for (var i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            if (i > 0) {
                var slash = new Label();
                slash.textStyle(style -> style.adaptiveWidth(true).textAlignVertical(Vertical.CENTER).fontSize(7))
                        .setText("/")
                        .layout(layout -> layout.heightPercent(100));
                slash.addClass("__asset-browser_breadcrumb-separator__").moveInlineAsDefault();
                breadcrumb.addChild(slash);
            }
            var button = new Button()
                    .setOnClick(e -> openDirectory(segment))
                    .setText(segment.getName(), false)
                    .buttonStyle(style -> {
                        style.baseTexture(IGuiTexture.EMPTY);
                        style.hoverTexture(ColorPattern.T_WHITE.rectTexture());
                        style.pressedTexture(ColorPattern.T_LIGHT_GRAY.rectTexture());
                    });
            button.text.textStyle(style -> style.fontSize(7));
            button.layout(layout -> {
                layout.heightPercent(100);
                layout.paddingHorizontal(2);
                layout.paddingVertical(0);
            });
            button.addClass("__asset-browser_breadcrumb-segment__");
            breadcrumb.addChild(button);
        }
    }

    // ------------------------------------------------------------------------------------- grid

    public AssetBrowser setShowAllFiles(boolean showAllFiles) {
        if (this.showAllFiles == showAllFiles) return this;
        this.showAllFiles = showAllFiles;
        requestGridRebuild();
        return this;
    }

    public AssetBrowser setUiWidth(int uiWidth) {
        if (this.uiWidth == uiWidth || uiWidth <= 0) return this;
        this.uiWidth = uiWidth;
        bottomBar.setValue(uiWidth);
        requestGridRebuild();
        return this;
    }

    public AssetBrowser setSortMode(SortMode sortMode) {
        if (this.sortMode == sortMode) return this;
        this.sortMode = sortMode;
        requestGridRebuild();
        return this;
    }

    public AssetBrowser setSortAscending(boolean sortAscending) {
        if (this.sortAscending == sortAscending) return this;
        this.sortAscending = sortAscending;
        requestGridRebuild();
        return this;
    }

    /** The resource types currently shown, or an empty set when nothing is filtered out. */
    public Set<Resource<?>> getTypeFilter() {
        return Set.copyOf(typeFilter);
    }

    public AssetBrowser toggleTypeFilter(Resource<?> resource) {
        if (!typeFilter.remove(resource)) {
            typeFilter.add(resource);
        }
        requestGridRebuild();
        return this;
    }

    public AssetBrowser clearTypeFilter() {
        if (typeFilter.isEmpty()) return this;
        typeFilter.clear();
        requestGridRebuild();
        return this;
    }

    public AssetBrowser setDisplayMode(Resource.DisplayMode displayMode) {
        if (this.displayMode == displayMode) return this;
        this.displayMode = displayMode;
        requestGridRebuild();
        return this;
    }

    /**
     * Marks the grid for a rebuild on the next tick. Deferred on purpose: this is reached from event
     * listeners and from resource callbacks, and rebuilding there would mutate the child list of an
     * element that is currently dispatching.
     */
    public void requestGridRebuild() {
        gridDirty = true;
    }

    /**
     * An entry that survived filtering, with its resource type resolved and the attributes the listing
     * already carried. Holding the attributes is what keeps {@link #sortEntries} off the file system:
     * a comparator's key extractor runs once per comparison, so an {@code isDirectory()} in there is
     * tens of thousands of stat syscalls on a directory of any size.
     */
    private record GridEntry(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior,
                             boolean directory, long size, long lastModified) {}

    protected void rebuildGrid() {
        if (isGlobalSearchActive()) {
            // walk again, keeping the old results up until the walk is done
            if (searchDueAt == 0) {
                startSearch(search != null);
            }
            return;
        }
        gridScroller.clearAllScrollViewChildren();
        entryUIs.clear();
        hovered = null;
        var directory = currentDirectory;
        if (directory == null) return;
        // taken before listing: this listing is what the poll would rebuild for, and a cell rebuilt twice drops a
        // drag pressed on it in between
        directoryStamp = stampOf(directory);

        var entries = new ArrayList<GridEntry>();
        for (var listed : FileUtility.listDirectory(directory)) {
            var file = listed.file();
            var isDirectory = listed.directory();
            // resolved once per entry: every lookup scans all loaded resource types
            var behavior = isDirectory ? null : behaviors.forNonDirectory(file);
            if (!isDirectory && !accepts(file, behavior)) continue;
            entries.add(new GridEntry(file, behavior, isDirectory, listed.size(), listed.lastModified()));
        }
        sortEntries(entries);

        var liveThumbnails = 0;
        for (var entry : entries) {
            var behavior = entry.behavior();
            var ui = createEntryUI(entry.file(), behavior, entry.directory(),
                    behavior != null && liveThumbnails < LIVE_THUMBNAIL_LIMIT);
            if (behavior != null) liveThumbnails++;
            entryUIs.put(entry.file(), ui);
            gridScroller.addScrollViewChild(ui);
        }
        // re-applies the highlight on the new cell, and drops the selection if it is gone
        var revealed = pendingSelection;
        var previous = revealed != null ? revealed : selected;
        pendingSelection = null;
        selected = null;
        selectEntry(previous);
        if (revealed != null && selected != null) {
            gridScroller.scrollToChildDelayed(entryUIs.get(selected));
        }
    }

    /** Whether a file passes the "show all files" toggle, the type filter and the search box. */
    private boolean accepts(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior) {
        if (behavior == null) {
            // neither a plain file nor a project belongs to a resource type, so a type filter hides both
            if (!typeFilter.isEmpty()) return false;
            // a project is editor content like a resource is, so it shows without "show all files"
            if (!showAllFiles && projectTypeOf(file) == null) return false;
        } else if (!typeFilter.isEmpty() && !typeFilter.contains(behavior.resource())) {
            return false;
        }
        return searchFilter.isEmpty() ||
                displayNameOf(file, behavior).toLowerCase(Locale.ROOT).contains(searchFilter);
    }

    /**
     * The project type that can open the file, or null when it is not a project of a known type.
     * <p>
     * The file menu is built after the resource view is, so it can still be missing while the browser
     * is being constructed.
     */
    @Nullable
    protected ProjectType projectTypeOf(File file) {
        if (editor == null || editor.fileMenu == null) return null;
        return editor.fileMenu.getProjectType(file);
    }

    private void sortEntries(List<GridEntry> entries) {
        entries.sort(entryComparator());
    }

    /** Folders always come first. The TYPE key cache lives in the instance, so reuse it for one set only. */
    private Comparator<GridEntry> entryComparator() {
        Comparator<GridEntry> comparator = switch (sortMode) {
            case NAME -> Comparator.comparing(this::displayNameOf, String.CASE_INSENSITIVE_ORDER);
            case TYPE -> byResolvedKey(this::typeNameOf)
                    .thenComparing(this::displayNameOf, String.CASE_INSENSITIVE_ORDER);
            case SIZE -> Comparator.comparingLong(GridEntry::size);
            case MODIFIED -> Comparator.comparingLong(GridEntry::lastModified);
        };
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        return Comparator.<GridEntry, Boolean>comparing(entry -> !entry.directory()).thenComparing(comparator);
    }

    /**
     * Orders by a string key resolved once per entry instead of once per comparison — worth the map for
     * {@link #typeNameOf}, which reaches for the project type and so touches the file system.
     * {@link #displayNameOf} is string arithmetic and does not need it.
     */
    private Comparator<GridEntry> byResolvedKey(Function<GridEntry, String> key) {
        var resolved = new IdentityHashMap<GridEntry, String>();
        return Comparator.comparing(entry -> resolved.computeIfAbsent(entry, key), String.CASE_INSENSITIVE_ORDER);
    }

    private String displayNameOf(GridEntry entry) {
        return displayNameOf(entry.file(), entry.behavior());
    }

    /** What an entry sorts under by type: its resource or project type, else the file extension. */
    private String typeNameOf(GridEntry entry) {
        if (entry.behavior() != null) return entry.behavior().resource().getName();
        var projectType = projectTypeOf(entry.file());
        if (projectType != null) return projectType.name;
        var name = entry.file().getName();
        var dot = name.lastIndexOf('.');
        return dot == -1 ? "" : name.substring(dot + 1);
    }

    protected UIElement createEntryUI(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior,
                                      boolean isDirectory, boolean liveThumbnail) {
        // the same cell the resource panel builds, so both stay in step
        var entry = ResourceProviderContainer.createResourceCell(displayMode, uiWidth,
                createThumbnail(file, behavior, isDirectory, liveThumbnail), displayNameOf(file, behavior));

        entry.addEventListener(UIEvents.MOUSE_ENTER, e -> hovered = file, true);
        entry.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (hovered == file) hovered = null;
        }, true);
        entry.addEventListener(UIEvents.MOUSE_DOWN, e -> selectEntry(file));
        entry.addEventListener(UIEvents.DOUBLE_CLICK, e -> activate(file));
        attachDragSource(entry, file, behavior);
        if (isDirectory) {
            attachDropTarget(entry, () -> file);
            entry.addClass("__asset-browser_entry-directory__");
        } else {
            entry.addClass(behavior == null ? "__asset-browser_entry-file__" : "__asset-browser_entry-resource__");
        }
        entry.addClass("__asset-browser_entry__").moveInlineAsDefault();
        return entry;
    }

    /**
     * The thumbnail of an entry. Resource files reuse the very element their resource type builds for
     * the resource panel, so a texture/renderer/... looks the same in both places.
     */
    protected UIElement createThumbnail(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior,
                                        boolean isDirectory, boolean liveThumbnail) {
        IGuiTexture icon;
        if (isDirectory) {
            icon = Icons.FOLDER;
        } else if (behavior != null) {
            var path = behaviors.pathOf(file);
            if (behavior.isLoaded(path)) {
                if (liveThumbnail) {
                    return behavior.container().getUiSupplier().apply(path);
                }
                icon = behavior.resource().getIcon();
            } else if (!Objects.equals(file.getParentFile(), behaviors.getDirectory())) {
                // a search result the behaviors of the open folder cannot load
                icon = behavior.resource().getIcon();
            } else {
                // a resource file that failed to load, tinted so it stands out but stays manageable
                icon = behavior.resource().getIcon().copy().setColor(ColorPattern.RED.color);
            }
        } else {
            var projectType = projectTypeOf(file);
            if (projectType != null) {
                // resolved per file, so a type that brands its projects individually can say so
                icon = projectType.getIcon(file);
            } else {
                var name = file.getName();
                var dot = name.lastIndexOf('.');
                icon = dot == -1 ? Icons.FILE : Icons.getIcon(name.substring(dot + 1));
            }
        }
        IGuiTexture finalIcon = icon;
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }).style(style -> style.backgroundTexture(finalIcon));
    }

    /** The name shown under an entry: resource files drop their (compound) extension. */
    protected String displayNameOf(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior) {
        var name = file.getName();
        if (behavior == null) return name;
        return name.substring(0, name.length() - behavior.resource().getFileExtension().length());
    }

    /**
     * What a drag out of the browser carries. Resource files reuse their type's own payload, so every
     * existing drop target (a texture slot, a graph view, ...) accepts them exactly as it accepts a
     * drag from the resource panel.
     */
    @Nullable
    protected Object dragPayloadOf(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior) {
        if (behavior != null) {
            var path = behaviors.pathOf(file);
            if (behavior.isLoaded(path)) {
                return behavior.container().getOnDragProvider().apply(path);
            }
        }
        return new DraggedAssets(List.of(file));
    }

    public void selectEntry(@Nullable File file) {
        if (selected != null) {
            var previous = entryUIs.get(selected);
            if (previous != null) {
                previous.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                previous.removeClass("__selected__");
            }
        }
        selected = file != null && entryUIs.containsKey(file) ? file : null;
        if (selected != null) {
            var ui = entryUIs.get(selected);
            if (ui != null) {
                ui.style(style -> style.overlayTexture(selectedTexture));
                ui.addClass("__selected__");
            }
        }
        updateBottomBar();
    }

    protected void updateBottomBar() {
        bottomBar.setValue(uiWidth);
        if (selected == null) {
            bottomBar.setText("");
            return;
        }
        var behavior = selected.isDirectory() ? null : behaviors.forFile(selected);
        // a resource shows the reference you would paste into a project, anything else its plain path
        bottomBar.setText(behavior == null
                ? FilePath.toGameRelative(selected.getPath())
                : behaviors.pathOf(selected).getPathWithType());
    }

    /**
     * Double click: descend into folders, open resources in their editor, load projects into the editor
     * and hand anything else to the operating system.
     */
    public void activate(File file) {
        if (file.isDirectory()) {
            openDirectory(file);
            return;
        }
        if (isGlobalSearchActive()) {
            // behaviors are bound to the open folder, so open the result from its own
            revealFile(file);
        }
        var behavior = behaviors.forFile(file);
        if (behavior != null) {
            var path = behaviors.pathOf(file);
            if (behavior.isLoaded(path)) {
                behavior.container().selectResource(path, false);
                behavior.container().editResource(path);
                return;
            }
        }
        // the same route the File menu's open entry takes, prompt about the open project included
        if (editor != null && editor.fileMenu != null && editor.fileMenu.openProject(file)) return;
        Util.getPlatform().openFile(file);
    }

    // --------------------------------------------------------------------------- drag and drop

    /** Press, then leave the element to begin dragging — the same idiom the resource panel uses. */
    private void attachDragSource(UIElement ui, File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior) {
        ui.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) lastClickFile = file;
        });
        ui.addEventListener(UIEvents.MOUSE_UP, e -> lastClickFile = null);
        ui.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastClickFile == file && isMouseDown(0)) {
                e.currentElement.startDrag(dragPayloadOf(file, behavior),
                        TextTexture.raw(displayNameOf(file, behavior)));
            }
            lastClickFile = null;
        }, true);
    }

    private void attachDropTarget(UIElement ui, Supplier<File> directory) {
        ui.addEventListener(UIEvents.DRAG_ENTER, e -> {
            if (acceptsDrop(e, directory.get())) {
                ui.style(style -> style.overlayTexture(ColorPattern.T_GREEN.rectTexture()));
            }
        }, true);
        ui.addEventListener(UIEvents.DRAG_LEAVE, e -> clearDropHighlight(ui, directory.get()), true);
        ui.addEventListener(UIEvents.DRAG_PERFORM, e -> {
            var target = directory.get();
            clearDropHighlight(ui, target);
            if (!acceptsDrop(e, target)) return;
            // the innermost folder takes it, not the grid around it as well
            e.stopPropagation();
            var payload = e.dragHandler.getDraggingObject();
            if (payload instanceof DraggedAssets assets) {
                moveFiles(assets.files(), target);
                return;
            }
            for (var handler : dropHandlers) {
                if (handler.accepts(payload, target)) {
                    handler.drop(payload, target);
                    requestGridRebuild();
                    return;
                }
            }
        });
    }

    /** Drops the drop highlight without losing the selection highlight of the same element. */
    private void clearDropHighlight(UIElement ui, @Nullable File file) {
        var overlay = file != null && file.equals(selected) ? selectedTexture : IGuiTexture.EMPTY;
        ui.style(style -> style.overlayTexture(overlay));
    }

    private boolean acceptsDrop(UIEvent event, @Nullable File directory) {
        if (event.dragHandler == null || directory == null || !directory.isDirectory()) return false;
        var payload = event.dragHandler.getDraggingObject();
        if (payload instanceof DraggedAssets assets) {
            return assets.files().stream().anyMatch(file -> canMove(file, directory));
        }
        return payload != null && dropHandlers.stream().anyMatch(handler -> handler.accepts(payload, directory));
    }

    /**
     * Lets something other than asset files be dropped on a folder: an editor's own drag payload, which the
     * handler turns into files there (a scene object saved as an asset, say).
     */
    public AssetBrowser addDropHandler(DropHandler handler) {
        dropHandlers.add(handler);
        return this;
    }

    public AssetBrowser removeDropHandler(DropHandler handler) {
        dropHandlers.remove(handler);
        return this;
    }

    public interface DropHandler {
        boolean accepts(Object payload, File directory);

        void drop(Object payload, File directory);
    }

    private boolean canMove(File file, File directory) {
        return !file.equals(directory) &&
                !directory.equals(file.getParentFile()) &&
                !FileOps.isDescendant(directory, file);
    }

    private void moveFiles(List<File> files, File directory) {
        var moved = files.stream().filter(file -> canMove(file, directory)).toList();
        if (moved.isEmpty()) return;
        // where each file actually ended up, recorded while moving: the name gets uniquified when it
        // is already taken, so undo cannot derive the target from the source name.
        var performed = new ArrayList<File[]>();
        pushFileHistory("editor.assets.move", () -> {
            performed.clear();
            for (var file : moved) {
                var target = FileOps.moveInto(file, directory);
                if (target == null) {
                    LDLib2.LOGGER.warn("Failed to move {} into {}", file, directory);
                } else {
                    performed.add(new File[]{file, target});
                }
            }
        }, () -> {
            for (var move : performed) {
                var origin = move[0].getParentFile();
                if (origin != null) {
                    FileOps.moveInto(move[1], origin);
                }
            }
            performed.clear();
        });
    }

    // ------------------------------------------------------------------- external file drop

    /**
     * Files dropped onto the browser from outside the game. They can either be copied in as they are or
     * handed to a resource type that knows how to read them, so the choice is offered as a menu at the
     * point they landed on.
     * <p>
     * The cell under the cursor cannot be taken from {@link #hovered}: no mouse movement is delivered
     * while a drag from another application is in progress, so that field still holds whatever was
     * hovered before the drag started. The event's target comes from a fresh hit test instead.
     */
    protected void onFilesDropped(UIEvent event) {
        if (event.droppedFiles.isEmpty() || currentDirectory == null) return;
        event.stopPropagation();
        var files = event.droppedFiles.stream().filter(File::isFile).toList();
        if (files.isEmpty()) {
            Dialog.showNotification("editor.resource.import_failed", "editor.assets.drop_no_files", null).show(this);
            return;
        }
        var target = directoryOf(event.target);
        var cell = entryUIs.get(target);
        ResourceProviderContainer.flashDropTarget(cell == null ? gridScroller : cell);
        if (editor == null) {
            copyFilesInto(files, target);
            return;
        }
        editor.openMenu(this, event.x, event.y, createDropMenu(files, target));
    }

    /** The folder a drop landed on: the folder cell under the cursor, else the open directory. */
    private File directoryOf(@Nullable UIElement target) {
        if (target != null) {
            // the hit element is the thumbnail or the label inside a cell, so match on the whole path
            var path = target.getStructurePath();
            for (var entry : entryUIs.entrySet()) {
                if (entry.getKey().isDirectory() && path.contains(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }
        return currentDirectory;
    }

    protected TreeBuilder.Menu createDropMenu(List<File> files, File directory) {
        var menu = TreeBuilder.Menu.start();
        menu.leaf(Icons.COPY, Component.translatable("editor.assets.drop_copy", directory.getName()),
                () -> copyFilesInto(files, directory));
        // a resource can only be created in the directory the browser is showing, that is the one the
        // behaviour objects are bound to
        if (directory.equals(currentDirectory)) {
            for (var resource : List.copyOf(behaviors.availableResources())) {
                var importable = files.stream().filter(resource::canImportFile).toList();
                if (importable.isEmpty()) continue;
                var behavior = behaviors.get(resource);
                if (behavior == null || !behavior.provider().supportAdd()) continue;
                menu.leaf(resource.getIcon(),
                        Component.translatable("editor.assets.drop_import", resource.getDisplayName()),
                        () -> {
                            behavior.container().importFiles(importable, this);
                            requestGridRebuild();
                        });
            }
        }
        return menu;
    }

    protected void copyFilesInto(List<File> files, File directory) {
        for (var file : files) {
            if (FileOps.copyInto(file, directory) == null) {
                LDLib2.LOGGER.warn("Failed to copy {} into {}", file, directory);
            }
        }
        afterStructureChange();
    }

    // ------------------------------------------------------------------------------------ menus

    protected void openMenu(UIEvent event, @Nullable File target, boolean directoryContext) {
        if (editor == null) return;
        var menu = directoryContext ? createDirectoryMenu(target) : createFileMenu(target);
        editor.openMenu(this, event.x, event.y, menu);
    }

    /** Menu of the empty grid area, or of a folder: creating things and pasting into them. */
    protected TreeBuilder.Menu createDirectoryMenu(@Nullable File target) {
        var directory = target != null && target.isDirectory() ? target : currentDirectory;
        var menu = TreeBuilder.Menu.start();
        if (directory == null) return menu;
        // resources are only created into the directory the browser is showing: the behavior objects
        // that know how to create them are bound to it. Another folder gets an "open" entry instead.
        var isCurrent = directory.equals(currentDirectory);
        menu.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.new", branch -> {
            branch.leaf(Icons.FOLDER, "ldlib.gui.file_dialog.new_folder", () ->
                    FileOps.promptNewFolder(this, directory, this::requestGridRebuild));
            if (isCurrent) {
                branch.crossLine();
                buildCreateResourceMenu(branch);
            }
        });
        if (!isCurrent) {
            menu.leaf(Icons.OPEN_FILE, "editor.assets.open", () -> openDirectory(directory));
        }
        menu.crossLine();
        if (ClipboardManager.INSTANCE.getClipboardType() == ClipboardAssets.class) {
            menu.leaf(Icons.PASTE, "editor.assets.paste", () -> pasteInto(directory));
        }
        menu.leaf(Icons.FOLDER, "ldlib.gui.tips.open_folder", () -> Util.getPlatform().openFile(directory));
        menu.leaf("editor.assets.refresh", this::requestGridRebuild);
        appendAddProviderMenu(menu, directory);
        if (target != null && !target.equals(root) && target.isDirectory()) {
            menu.crossLine();
            menu.leaf("ldlib.gui.editor.menu.rename", () -> renameEntry(target));
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyToClipboard(target, false));
            menu.leaf(Icons.CUT, "editor.assets.cut", () -> copyToClipboard(target, true));
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () ->
                    FileOps.confirmDelete(this, target, this::afterStructureChange));
        }
        appendViewOptions(menu);
        return menu;
    }

    /** Menu of a single file. Resource files additionally get their own type's entries. */
    protected TreeBuilder.Menu createFileMenu(@Nullable File target) {
        if (target == null) return createDirectoryMenu(null);
        var menu = TreeBuilder.Menu.start();
        var behavior = behaviors.forFile(target);
        var path = behavior == null ? null : behaviors.pathOf(target);
        // only a resource file that actually loaded can be routed through its resource container
        var container = behavior != null && behavior.isLoaded(path) ? behavior.container() : null;

        menu.leaf("ldlib.gui.editor.menu.copy_path", () -> ClipboardManager.INSTANCE.copyDirect(
                path == null ? target.getAbsolutePath() : path.getPathWithType()));
        var projectType = projectTypeOf(target);
        if (container != null && container.getOnEdit() != null && container.getCanEdit().test(path)) {
            menu.leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", () -> activate(target));
        } else if (projectType != null) {
            menu.leaf(Icons.OPEN_FILE, "ldlib.gui.editor.menu.open", () -> activate(target));
            // the project type's own entries, beside its Open — see ProjectType#appendFileMenu
            projectType.appendFileMenu(editor, target, menu);
        }
        menu.leaf("ldlib.gui.editor.menu.rename", () -> renameEntry(target));
        menu.crossLine();
        menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyToClipboard(target, false));
        menu.leaf(Icons.CUT, "editor.assets.cut", () -> copyToClipboard(target, true));
        if (container != null && container.getCanCopy().test(path)) {
            menu.leaf("editor.assets.duplicate", () -> {
                // duplicateResource, not copyResource: this entry means "make one here" and rebuilds
                // the grid on the next line, so it needs the copy to have happened already.
                container.duplicateResource(path);
                requestGridRebuild();
            });
        }
        menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> {
            if (container != null && container.getCanRemove().test(path)) {
                // routed through the resource container so the deletion lands in the edit history
                container.removeResource(path, true);
                requestGridRebuild();
            } else {
                FileOps.confirmDelete(this, target, this::afterStructureChange);
            }
        });
        menu.crossLine();
        menu.leaf(Icons.OPEN_FILE, "ldlib.gui.tips.open_folder", () ->
                Util.getPlatform().openFile(target.getParentFile()));
        if (container != null) {
            // contextual entries of the resource type itself, e.g. "copy color", which read the
            // container's selection while the menu is being built
            container.selectResource(path, false);
            behavior.appendContextMenu(menu);
        }
        appendViewOptions(menu);
        return menu;
    }

    /** Leads to a result rather than editing it: that goes through the behaviors of its own folder. */
    protected TreeBuilder.Menu createSearchResultMenu(@Nullable File target) {
        var menu = TreeBuilder.Menu.start();
        if (target != null) {
            if (target.isDirectory()) {
                menu.leaf(Icons.OPEN_FILE, "editor.assets.open", () -> openDirectory(target));
            }
            menu.leaf(Icons.FOLDER, "editor.assets.show_in_folder", () -> revealFile(target));
            var resource = behaviors.resourceOf(target);
            menu.leaf("ldlib.gui.editor.menu.copy_path", () -> ClipboardManager.INSTANCE.copyDirect(
                    resource == null ? target.getAbsolutePath() : behaviors.pathOf(target).getPathWithType()));
            var parent = target.getParentFile();
            if (parent != null) {
                menu.leaf(Icons.OPEN_FILE, "ldlib.gui.tips.open_folder", () -> Util.getPlatform().openFile(parent));
            }
            if (target.isDirectory()) {
                appendAddProviderMenu(menu, target);
            }
            menu.crossLine();
        }
        menu.leaf("editor.assets.refresh", this::requestGridRebuild);
        appendViewOptions(menu);
        return menu;
    }

    /** Offers the folder as a file provider to every resource type it is not one of yet. */
    protected void appendAddProviderMenu(TreeBuilder.Menu menu, File directory) {
        var missing = new ArrayList<Resource<?>>();
        for (var resource : List.copyOf(behaviors.availableResources())) {
            if (ResourceBehaviorCache.findFileProvider(resource.getResourceInstance(), directory) == null) {
                missing.add(resource);
            }
        }
        if (missing.isEmpty()) return;
        menu.branch(Icons.RESOURCE, "editor.assets.add_provider", branch -> {
            for (var resource : missing) {
                branch.leaf(resource.getIcon(), resource.getDisplayName(),
                        () -> addFileProvider(resource.getResourceInstance(), directory));
            }
        });
    }

    /** @return false when the folder already is a file provider of that type. */
    public <T> boolean addFileProvider(ResourceInstance<T> instance, File directory) {
        if (!directory.isDirectory() || ResourceBehaviorCache.findFileProvider(instance, directory) != null) {
            return false;
        }
        instance.addCustomProvider(new FileResourceProvider<>(instance, directory));
        // rebuilt, the behavior shares the registered provider instead of running its own over the same files
        if (directory.equals(behaviors.getDirectory())) {
            behaviors.invalidate(instance.resource);
        }
        requestGridRebuild();
        return true;
    }

    /**
     * One branch per loaded resource type, holding that type's own "create" entries, so the browser
     * offers exactly what the resource panel offers for the same type.
     */
    protected void buildCreateResourceMenu(TreeBuilder.Menu menu) {
        for (var resource : List.copyOf(behaviors.availableResources())) {
            var behavior = behaviors.get(resource);
            if (behavior == null) continue;
            menu.branch(resource.getIcon(), resource.getDisplayName(), branch ->
                    behavior.appendCreateMenu(branch, this::requestGridRebuild));
        }
    }

    private void appendViewOptions(TreeBuilder.Menu menu) {
        menu.crossLine();
        var isList = displayMode == Resource.DisplayMode.LIST;
        menu.leaf(isList ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.list", () ->
                setDisplayMode(isList ? Resource.DisplayMode.GRID : Resource.DisplayMode.LIST));
        menu.branch("ldlib.gui.editor.group.size", m -> {
            m.leaf(uiWidth == 15 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.small", () -> setUiWidth(15));
            m.leaf(uiWidth == 30 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.medium", () -> setUiWidth(30));
            m.leaf(uiWidth == 50 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.large", () -> setUiWidth(50));
            m.leaf(uiWidth == 100 ? Icons.CHECK_SPRITE : IGuiTexture.EMPTY, "editor.extra_large", () -> setUiWidth(100));
        });
    }

    // ------------------------------------------------------------------------------- operations

    protected void copyToClipboard(File file, boolean cut) {
        ClipboardManager.INSTANCE.copy(() -> new ClipboardAssets(List.of(file), cut), ClipboardAssets.class);
    }

    protected void pasteInto(File directory) {
        ClipboardAssets assets = ClipboardManager.INSTANCE.paste();
        if (assets == null || assets.files().isEmpty()) return;
        for (var file : assets.files()) {
            if (!file.exists()) continue;
            if (assets.cut()) {
                FileOps.moveInto(file, directory);
            } else {
                FileOps.copyInto(file, directory);
            }
        }
        if (assets.cut()) {
            ClipboardManager.INSTANCE.clear();
        }
        afterStructureChange();
    }

    /**
     * Renames an entry in place. Resource files go through their resource container so the rename
     * lands in the edit history and any editor view opened on them follows the new name.
     */
    protected void renameEntry(File file) {
        var cell = entryUIs.get(file);
        if (cell == null) {
            // no cell to edit in place, e.g. a folder right-clicked in the tree
            FileOps.promptRename(this, file, this::afterStructureChange);
            return;
        }
        var behavior = behaviors.forFile(file);
        var path = behavior == null ? null : behaviors.pathOf(file);
        // a resource file renames through its container, so the change lands in the edit history and any
        // editor view opened on it follows the new name. Anything else is a plain file rename.
        if (behavior != null && behavior.isLoaded(path) && behavior.container().getCanRename().test(path)) {
            var container = behavior.container();
            ResourceProviderContainer.startInlineRename(cell, displayNameOf(file, behavior),
                    ResourceLocation::isAllowedInResourceLocation,
                    name -> {
                        container.renameResourceTo(path, name);
                        requestGridRebuild();
                    });
            return;
        }
        ResourceProviderContainer.startInlineRename(cell, file.getName(), FileOps::isValidNameChar,
                name -> {
                    if (!FileOps.isValidName(name)) return;
                    var renamed = new File(file.getParentFile(), name);
                    if (renamed.equals(file)) return;
                    if (renamed.exists() || !file.renameTo(renamed)) {
                        FileOps.showRenameError(this, renamed.exists());
                        return;
                    }
                    afterStructureChange();
                });
    }

    private void pushFileHistory(String translationKey, Runnable redo, Runnable undo) {
        Runnable doRedo = () -> {
            redo.run();
            afterStructureChange();
        };
        Runnable doUndo = () -> {
            undo.run();
            afterStructureChange();
        };
        if (editor == null) {
            doRedo.run();
            return;
        }
        editor.historyView.pushHistory(Component.translatable(translationKey), EditAction.of(doRedo, doUndo));
    }

    /** Files appeared/disappeared: refresh the grid and let the tree pick the change up on its own. */
    protected void afterStructureChange() {
        if (currentDirectory != null && !currentDirectory.isDirectory()) {
            openDirectory(root);
            return;
        }
        requestGridRebuild();
    }

    // ----------------------------------------------------------------------------- global search

    public boolean isGlobalSearchActive() {
        return globalSearch && !searchFilter.isEmpty();
    }

    public AssetBrowser setGlobalSearch(boolean globalSearch) {
        if (this.globalSearch == globalSearch) return this;
        var wasActive = isGlobalSearchActive();
        this.globalSearch = globalSearch;
        searchField.style(style -> style.tooltips(globalSearch ? "editor.assets.search_global" : "editor.assets.search"));
        if (isGlobalSearchActive()) {
            scheduleSearch(0);
            rebuildBreadcrumb();
        } else if (wasActive) {
            stopSearch();
            rebuildBreadcrumb();
            requestGridRebuild();
        }
        return this;
    }

    private void onSearchTextChanged(String filter) {
        var wasActive = isGlobalSearchActive();
        searchFilter = filter;
        if (isGlobalSearchActive()) {
            scheduleSearch(SEARCH_DEBOUNCE_MS);
            if (!wasActive) {
                rebuildBreadcrumb();
            }
            return;
        }
        if (wasActive) {
            // cleared: go to the picked result's folder, as Unity does
            var picked = selected != null ? selected : pendingSelection;
            stopSearch();
            if (picked != null && picked.exists()) {
                revealFile(picked);
                return;
            }
            pendingSelection = null;
            rebuildBreadcrumb();
        }
        requestGridRebuild();
    }

    private void scheduleSearch(long delayMs) {
        searchDueAt = System.currentTimeMillis() + delayMs;
    }

    /** @param replaceGrid keep the grid until the walk is over, instead of clearing it and streaming in. */
    private void startSearch(boolean replaceGrid) {
        searchDueAt = 0;
        if (search != null) {
            search.cancel();
        }
        var extensions = behaviors.availableResources().stream().map(Resource::getFileExtension).toList();
        search = AssetSearch.start(root, searchFilter, extensions, SEARCH_LIMIT);
        searchHits.clear();
        searchRendered = 0;
        searchReplacesGrid = replaceGrid;
        if (!replaceGrid) {
            pendingSelection = null;
            clearSearchGrid();
        }
        updateSearchStatus();
    }

    private void stopSearch() {
        if (search != null) {
            search.cancel();
            search = null;
        }
        searchDueAt = 0;
        searchReplacesGrid = false;
        searchHits.clear();
        searchEntries.clear();
        searchRendered = 0;
    }

    /** Clears the query without going to the picked result. */
    private void exitGlobalSearch() {
        stopSearch();
        searchFilter = "";
        searchField.setText("", false);
    }

    private void clearSearchGrid() {
        gridScroller.clearAllScrollViewChildren();
        entryUIs.clear();
        hovered = null;
        searchEntries.clear();
        searchComparator = entryComparator();
        if (selected != null) {
            selected = null;
            updateBottomBar();
        }
    }

    private void tickSearch() {
        if (searchDueAt != 0 && System.currentTimeMillis() >= searchDueAt) {
            startSearch(false);
        }
        var search = this.search;
        if (search == null) return;
        // read before draining, so a finished walk is drained completely
        var finished = search.isFinished();
        for (var hit = search.poll(); hit != null; hit = search.poll()) {
            searchHits.add(hit);
        }
        if (searchReplacesGrid) {
            if (!finished) return;
            searchReplacesGrid = false;
            if (selected != null) {
                pendingSelection = selected;
            }
            clearSearchGrid();
            renderSearchHits(Integer.MAX_VALUE);
            // not found again: deleted or renamed
            pendingSelection = null;
        } else {
            renderSearchHits(SEARCH_BATCH);
        }
        updateSearchStatus();
    }

    private void renderSearchHits(int budget) {
        while (budget-- > 0 && searchRendered < searchHits.size() && searchEntries.size() < SEARCH_SHOWN_LIMIT) {
            showSearchHit(searchHits.get(searchRendered++));
        }
    }

    /** Filtered like a folder listing, then inserted where the current sort puts it. */
    private void showSearchHit(AssetSearch.Hit hit) {
        var file = hit.file();
        var behavior = hit.directory() ? null : behaviors.forNonDirectory(file);
        if (!hit.directory() && !accepts(file, behavior)) return;
        var entry = new GridEntry(file, behavior, hit.directory(), hit.size(), hit.lastModified());
        var index = Collections.binarySearch(searchEntries, entry, searchComparator);
        if (index < 0) {
            index = -index - 1;
        }
        searchEntries.add(index, entry);
        // no live thumbnails, the behaviors only load the open folder
        var ui = createEntryUI(file, behavior, hit.directory(), false);
        ui.style(style -> style.tooltips(Component.literal(relativePathOf(file))));
        entryUIs.put(file, ui);
        gridScroller.addScrollViewChildAt(ui, index);
        if (file.equals(pendingSelection)) {
            pendingSelection = null;
            selectEntry(file);
        }
    }

    private String relativePathOf(File file) {
        try {
            return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException e) {
            return file.getPath();
        }
    }

    private void updateSearchStatus() {
        var search = this.search;
        var shown = searchEntries.size();
        var backlog = searchRendered < searchHits.size();
        String key;
        if (search == null || searchDueAt != 0 || !search.isFinished() || (backlog && shown < SEARCH_SHOWN_LIMIT)) {
            key = "editor.assets.search_running";
        } else if (search.isTruncated() || backlog) {
            key = "editor.assets.search_truncated";
        } else {
            key = "editor.assets.search_done";
        }
        var text = Component.translatable(key, root.getName(), shown);
        // setText relayouts even when the text is unchanged
        if (!text.equals(searchStatusText)) {
            searchStatusText = text;
            searchStatus.setText(text);
        }
    }

    // -------------------------------------------------------------------------------------- tick

    /**
     * Ticks the borrowed resource behaviors. They are hidden children the framework skips, and this has
     * to keep running while another tab is selected so dirty resources still flush to disk.
     */
    public void tickBehaviors() {
        behaviors.tick();
    }

    /**
     * The browser follows the file system on its own, so there is no refresh button:
     * <ul>
     *     <li>the tree diffs the children of its expanded nodes every tick, and {@link FileNode} keeps
     *     those in step with the directory — the same mechanism the file dialog relies on;</li>
     *     <li>the grid polls the open directory for added/removed/touched entries (below);</li>
     *     <li>edits to a resource's <em>content</em> arrive through the borrowed behavior containers,
     *     whose {@code checkAndUpdateResourceProvider} watches file modification times.</li>
     * </ul>
     * "Refresh" survives only as a context menu entry, the way Unity keeps Assets &gt; Refresh.
     */
    @Override
    public void screenTick() {
        super.screenTick();
        if (gridDirty) {
            gridDirty = false;
            rebuildGrid();
        }
        if (isGlobalSearchActive()) {
            // no folder to poll: results refresh on file operations made here and on "refresh"
            tickSearch();
            return;
        }
        if (++pollCounter >= POLL_INTERVAL) {
            pollCounter = 0;
            var directory = currentDirectory;
            if (directory != null) {
                var stamp = stampOf(directory);
                if (stamp != directoryStamp) {
                    directoryStamp = stamp;
                    requestGridRebuild();
                }
            }
        }
    }

    /**
     * A cheap fingerprint of a directory: its own modification time mixed with its entry count.
     * <p>
     * The count moves when an entry is added or removed, the timestamp on top of that when one is
     * renamed — which is all the grid polls for. An edit to a resource's <em>contents</em> reaches it
     * through the behavior containers instead, as {@link #screenTick()} describes. Reading each entry's
     * own timestamp would catch those too, but at one stat syscall per entry it is the poll itself that
     * would then cost the most in a large folder.
     */
    private static long stampOf(File directory) {
        var names = directory.list();
        if (names == null) return -1;
        return directory.lastModified() * 31 + names.length;
    }

    @Nullable
    public File getSelectedFile() {
        return selected;
    }
}
