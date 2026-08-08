package com.lowdragmc.lowdraglib2.editor.ui.browser;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
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
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
                    attachDropTarget(ui, node.getKey());
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
        searchField.setTextResponder(text -> {
            searchFilter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            requestGridRebuild();
        });
        searchField.layout(layout -> {
            layout.height(12);
            layout.flex(1);
        }).style(style -> style.tooltips("editor.assets.search"))
                .addClass("__asset-browser_search-field__").moveInlineAsDefault();

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
                openMenu(event, hovered, hovered == null || hovered.isDirectory());
            } else if (event.button == 0 && hovered == null) {
                selectEntry(null);
            }
        });
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
        label.textStyle(style -> style.adaptiveWidth(true).textAlignVertical(Vertical.CENTER))
                .setText(node.getKey().getName())
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
                editor.openMenu(button.getPositionX(), button.getPositionY() + button.getSizeHeight(),
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
        button.addClass("__asset-browser_toolbar-button__");
        return button;
    }

    // ------------------------------------------------------------------------------- navigation

    public AssetBrowser setRoot(File root) {
        this.root = root;
        if (!root.exists()) {
            root.mkdirs();
        }
        tree.setRoot(new FileNode(root).setValid(node -> node.getKey().isDirectory()));
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
        this.currentDirectory = directory;
        this.selected = null;
        this.hovered = null;
        behaviors.setDirectory(directory);
        rebuildBreadcrumb();
        this.directoryStamp = stampOf(directory);
        requestGridRebuild();
    }

    private void rebuildBreadcrumb() {
        breadcrumb.clearAllChildren();
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

    /** An entry that survived filtering, with its resource type already resolved. */
    private record GridEntry(File file, @Nullable ResourceBehaviorCache.Behavior<?> behavior) {}

    protected void rebuildGrid() {
        gridScroller.clearAllScrollViewChildren();
        entryUIs.clear();
        hovered = null;
        var directory = currentDirectory;
        if (directory == null) return;
        var files = directory.listFiles();
        if (files == null) return;

        var entries = new ArrayList<GridEntry>();
        for (var file : files) {
            var isDirectory = file.isDirectory();
            // resolved once per entry: every lookup scans all loaded resource types
            var behavior = isDirectory ? null : behaviors.forFile(file);
            if (!isDirectory && !accepts(file, behavior)) continue;
            entries.add(new GridEntry(file, behavior));
        }
        entries.sort(entryComparator());

        var liveThumbnails = 0;
        for (var entry : entries) {
            var behavior = entry.behavior();
            var ui = createEntryUI(entry.file(), behavior, behavior != null && liveThumbnails < LIVE_THUMBNAIL_LIMIT);
            if (behavior != null) liveThumbnails++;
            entryUIs.put(entry.file(), ui);
            gridScroller.addScrollViewChild(ui);
        }
        // re-applies the highlight on the new cell, and drops the selection if it is gone
        var previous = selected;
        selected = null;
        selectEntry(previous);
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

    /** Folders always come first, whichever way the rest is sorted. */
    private Comparator<GridEntry> entryComparator() {
        Comparator<GridEntry> comparator = switch (sortMode) {
            case NAME -> Comparator.comparing(this::displayNameOf, String.CASE_INSENSITIVE_ORDER);
            case TYPE -> Comparator.comparing(this::typeNameOf, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(this::displayNameOf, String.CASE_INSENSITIVE_ORDER);
            case SIZE -> Comparator.comparingLong(entry -> entry.file().length());
            case MODIFIED -> Comparator.comparingLong(entry -> entry.file().lastModified());
        };
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        return Comparator.<GridEntry, Boolean>comparing(entry -> !entry.file().isDirectory())
                .thenComparing(comparator);
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
                                      boolean liveThumbnail) {
        // the same cell the resource panel builds, so both stay in step
        var entry = ResourceProviderContainer.createResourceCell(displayMode, uiWidth,
                createThumbnail(file, behavior, liveThumbnail), displayNameOf(file, behavior));

        entry.addEventListener(UIEvents.MOUSE_ENTER, e -> hovered = file, true);
        entry.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (hovered == file) hovered = null;
        }, true);
        entry.addEventListener(UIEvents.MOUSE_DOWN, e -> selectEntry(file));
        entry.addEventListener(UIEvents.DOUBLE_CLICK, e -> activate(file));
        attachDragSource(entry, file, behavior);
        if (file.isDirectory()) {
            attachDropTarget(entry, file);
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
                                        boolean liveThumbnail) {
        IGuiTexture icon;
        if (file.isDirectory()) {
            icon = Icons.FOLDER;
        } else if (behavior != null) {
            var path = behaviors.pathOf(file);
            if (behavior.isLoaded(path)) {
                if (liveThumbnail) {
                    return behavior.container().getUiSupplier().apply(path);
                }
                icon = behavior.resource().getIcon();
            } else {
                // a resource file that failed to load, tinted so it stands out but stays manageable
                icon = behavior.resource().getIcon().copy().setColor(ColorPattern.RED.color);
            }
        } else {
            var projectType = projectTypeOf(file);
            if (projectType != null) {
                icon = projectType.icon;
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
                        new TextTexture(displayNameOf(file, behavior)));
            }
            lastClickFile = null;
        }, true);
    }

    private void attachDropTarget(UIElement ui, File directory) {
        ui.addEventListener(UIEvents.DRAG_ENTER, e -> {
            if (acceptsDrop(e, directory)) {
                ui.style(style -> style.overlayTexture(ColorPattern.T_GREEN.rectTexture()));
            }
        }, true);
        ui.addEventListener(UIEvents.DRAG_LEAVE, e -> clearDropHighlight(ui, directory), true);
        ui.addEventListener(UIEvents.DRAG_PERFORM, e -> {
            clearDropHighlight(ui, directory);
            if (!acceptsDrop(e, directory)) return;
            DraggedAssets assets = e.dragHandler.getDraggingObject();
            moveFiles(assets.files(), directory);
        });
    }

    /** Drops the drop highlight without losing the selection highlight of the same element. */
    private void clearDropHighlight(UIElement ui, File file) {
        var overlay = file.equals(selected) ? selectedTexture : IGuiTexture.EMPTY;
        ui.style(style -> style.overlayTexture(overlay));
    }

    private boolean acceptsDrop(UIEvent event, File directory) {
        if (event.dragHandler == null || !directory.isDirectory()) return false;
        if (!(event.dragHandler.getDraggingObject() instanceof DraggedAssets assets)) return false;
        return assets.files().stream().anyMatch(file -> canMove(file, directory));
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
        editor.openMenu(event.x, event.y, createDropMenu(files, target));
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
        editor.openMenu(event.x, event.y, menu);
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
        if (container != null && container.getOnEdit() != null && container.getCanEdit().test(path)) {
            menu.leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", () -> activate(target));
        } else if (projectTypeOf(target) != null) {
            menu.leaf(Icons.OPEN_FILE, "ldlib.gui.editor.menu.open", () -> activate(target));
        }
        menu.leaf("ldlib.gui.editor.menu.rename", () -> renameEntry(target));
        menu.crossLine();
        menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> copyToClipboard(target, false));
        menu.leaf(Icons.CUT, "editor.assets.cut", () -> copyToClipboard(target, true));
        if (container != null && container.getCanCopy().test(path)) {
            menu.leaf("editor.assets.duplicate", () -> {
                container.copyResource(path);
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
                    Identifier::isAllowedInIdentifier,
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
     *     <li>the tree diffs its children every tick, because {@link FileNode} re-reads the directory
     *     on each {@code getChildren()} call — the same mechanism the file dialog relies on;</li>
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

    /** A cheap fingerprint of a directory: entry count mixed with the newest modification time. */
    private static long stampOf(File directory) {
        var files = directory.listFiles();
        if (files == null) return -1;
        var newest = directory.lastModified();
        for (var file : files) {
            newest = Math.max(newest, file.lastModified());
        }
        return newest * 31 + files.length;
    }

    @Nullable
    public File getSelectedFile() {
        return selected;
    }
}
