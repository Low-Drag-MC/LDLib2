package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <b>A pick-one-thing popup</b>: a draggable, resizable window that opens at the mouse, with a search
 * field over one or more trees of entries, keyboard navigation, and an optional description panel
 * beside it.
 *
 * <p>This is the node graph's item library with the node graph taken out of it. Everything here is
 * about <i>picking</i> — where the popup sits, what the arrow keys do, which rows the typed text
 * matches, when the description panel flips to the other side of the screen. What an entry stands for
 * is the caller's, through {@link ILibraryItem} and the hooks below, so anything that needs "let the
 * user search a categorised list and choose one" can have this window rather than a second one that
 * looks almost like it.
 *
 * <h2>Using it directly</h2>
 * <pre>{@code
 * var picker = new ItemLibraryPanel<MyItem>(someElementOnScreen, MyItem::new);
 * picker.addContentTree().setRoot(treeBuilder.build());
 * picker.show(mouseX, mouseY, picked -> { if (picked != null) ... });   // null = dismissed
 * }</pre>
 *
 * <h2>Or subclassing it</h2>
 * The hooks are {@link #isDecidable}, {@link #isEntryItem}, {@link #isClickToExpand},
 * {@link #onRowBuilt}, {@link #createDescriptionUI}, {@link #decidedItem},
 * {@link #prepareSelectedItemData}, {@link #clearSelectedItemData} and {@link #onHide} — see
 * {@code ItemLibrary} in the node graph toolkit, which is exactly this class plus nodes, ports,
 * constants and blocks.
 *
 * <h2>Styling</h2>
 * The class names are the ones the built-in stylesheets already carry
 * ({@code __item-library__}, {@code __item-library_description-panel__},
 * {@code __item-library_resize-button__}, …) and they are declared here rather than in the subclass
 * on purpose: the look belongs to the window, not to what is listed in it, so every theme styles
 * every library the same and none of them needed a new rule when this was split out.
 */
public class ItemLibraryPanel<T extends ILibraryItem> extends UIElement {
    public record DragMove(Vector2f originalPos) {}
    public record DragResize(Vector2f originalSize) {}

    /**
     * The element this popup borrows a screen from. It has no parent between shows — it adds itself to
     * the root on {@link #show} and takes itself off again on {@link #hide} — so it cannot find the
     * {@code ModularUI} through itself at the moment it needs to.
     */
    protected final UIElement host;
    /** Makes the throwaway item the search results hang from; called at most once. */
    private final Supplier<T> groupItemFactory;
    @Nullable
    private T searchRootItem;

    public final UIElement headBar = new UIElement();
    public final Label title = new Label();
    public final TextField searchField = new TextField();
    public final ScrollerView resultContainer = new ScrollerView();
    public final UIElement tailBar = new UIElement();
    public final Label tailLabel = new Label();
    public final UIElement resizeButton = new UIElement();

    /** Side panel showing {@link #createDescriptionUI} for the selected entry. Collapsed by default. */
    public final UIElement descriptionPanel = new UIElement();
    public final ScrollerView descriptionView = new ScrollerView();

    public final UIElement treeContainer = new UIElement();
    public final TreeList<TreeNode<T, Void>> searchTree = new TreeList<>();
    /** A second tree above the content, for whatever the opener wants to put first. Hidden until set. */
    public final TreeList<TreeNode<T, Void>> recommendationTree = new TreeList<>();
    /** The trees the entries live in, in display order — see {@link #addContentTree()}. */
    protected final List<TreeList<TreeNode<T, Void>>> contentTrees = new ArrayList<>();

    /** Width of the {@link #descriptionPanel}, the library's own default width by default. */
    protected float descriptionWidth = 150;

    // runtime
    @Nullable
    protected TreeList<TreeNode<T, Void>> selectedTree;
    @Nullable
    protected T selectedItem;
    @Nullable
    protected TreeNode<T, Void> selectedNode;
    @Nullable
    protected Consumer<@Nullable T> onFinished;

    public ItemLibraryPanel(UIElement host, Supplier<T> groupItemFactory) {
        this.host = host;
        this.groupItemFactory = groupItemFactory;
        addClass("__item-library__");
        // ABSOLUTE positioning + width/height are popup-driven (resize, show-at-mouse) — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
        setPopupSize(150, 200);
        Style.defaultPipeline(getLayout(), l -> l.gapAll(2).paddingAll(5));
        Style.defaultPipeline(getStyle(), s -> s.background(Sprites.BORDER1_RT1));

        headBar.addClass("__item-library_head-bar__");
        Style.defaultPipeline(headBar.getLayout(), l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));
        title.addClass("__item-library_title__");
        Style.defaultPipeline(title.getTextStyle(), s -> s.textWrap(TextWrap.HOVER_ROLL));
        Style.defaultPipeline(title.getStyle(), s -> s.overflowVisible(false));
        Style.defaultPipeline(title.getLayout(), l -> l.flex(1));

        searchField.addClass("__item-library_search-field__");
        resultContainer.addClass("__item-library_result-container__");
        Style.defaultPipeline(resultContainer.getLayout(), l -> l.flex(1));

        searchTree.addClass("__item-library_search-tree__");
        recommendationTree.addClass("__item-library_recommend-tree__");
        treeContainer.addClass("__item-library_tree-container__");

        searchField.setTextResponder(this::onSearchWordChanged);
        // Initial tree visibility is mode-driven — pin via IMPORTANT.
        Style.importantPipeline(searchTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        searchTree.setFlattenRoot(true);
        initTreeList(searchTree, null);

        Style.importantPipeline(recommendationTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        initTreeList(recommendationTree, treeContainer);

        resultContainer.addScrollViewChildren(searchTree, treeContainer);

        tailBar.addClass("__item-library_tail-bar__");
        Style.defaultPipeline(tailBar.getLayout(), l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));
        tailLabel.addClass("__item-library_tail-label__");
        tailLabel.setText("Double click to add a node");
        Style.defaultPipeline(tailLabel.getTextStyle(), s -> s.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER).fontSize(4.5f));
        Style.defaultPipeline(tailLabel.getStyle(), s -> s.overflowVisible(false));
        Style.defaultPipeline(tailLabel.getLayout(), l -> l.flex(1));
        resizeButton.addClass("__item-library_resize-button__");
        Style.defaultPipeline(resizeButton.getLayout(), l -> l.width(9).height(9));
        Style.defaultPipeline(resizeButton.getStyle(), s -> s.background(DynamicTexture.of(() -> resizeButton.isHover() ?
                Icons.RESIZE_BOTTOM_RIGHT : Icons.RESIZE_BOTTOM_RIGHT.copy().setColor(ColorPattern.LIGHT_GRAY.color))));

        // Description panel: an absolutely positioned child, so it doesn't take part in the column
        // layout and — being a descendant — hovering/scrolling it doesn't trip setEnforceFocus.
        descriptionPanel.addClass("__item-library_description-panel__");
        Style.importantPipeline(descriptionPanel.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .display(TaffyDisplay.NONE));
        Style.defaultPipeline(descriptionPanel.getLayout(), l -> l.paddingAll(5));
        Style.defaultPipeline(descriptionPanel.getStyle(), s -> s.background(Sprites.BORDER1_RT1));
        descriptionView.addClass("__item-library_description-view__");
        descriptionView.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        Style.defaultPipeline(descriptionView.getLayout(), l -> l.widthPercent(100).flex(1));
        descriptionPanel.addChild(descriptionView);

        addChildren(
                headBar.addChildren(title),
                searchField,
                resultContainer,
                tailBar.addChildren(tailLabel, resizeButton),
                descriptionPanel
        );
        setFocusable(true);
        setEnforceFocus(e -> this.hide());
        addEventListener(UIEvents.LAYOUT_CHANGED, e -> {
            adaptPositionToScreen();
            // only while it's on screen: this fires every frame the popup is dragged or resized
            if (descriptionPanel.isDisplayed()) {
                updateDescriptionPanelBounds();
            }
        });
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);

        // drag
        WindowDragHelper.setDragMove(headBar, this, null, null);

        // resize
        resizeButton.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            var width = 12;
            var height = 12;
            resizeButton.startDrag(new DragResize(new Vector2f(this.getSizeWidth(), this.getSizeHeight())), Icons.MOVE)
                    .setDragTexture(- width / 2f, -height / 2f, width, height);
        });
        resizeButton.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (e.dragHandler.draggingObject instanceof DragResize(var oSize)) {
                var normalSizeOffset = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
                // Live resize — width/height are data-driven and must outrank stylesheet defaults.
                Style.importantPipeline(getLayout(), l -> l
                        .width(Math.max(oSize.x + normalSizeOffset.x, 50))
                        .height(Math.max(oSize.y + normalSizeOffset.y, 70)));
            }
        });
    }

    // ---- trees ----

    protected void initTreeList(TreeList<TreeNode<T, Void>> treeList, @Nullable UIElement container) {
        treeList.setStaticTree(false);
        treeList.setNodeUISupplier(TreeList.iconTextTemplate(
                node -> node.getKey().getIcon(),
                node -> node.getKey().getDisplayName())
        );
        treeList.setOnDoubleClickNode(node -> {
            if (!isDecidable(node)) return;
            onNodeDecided(node.getKey());
        });
        treeList.setOnSelectedChanged(selected -> {
            if (selected.isEmpty()) return;
            var node = selected.iterator().next();
            onSelectedChanged(treeList, node, node.getKey());
        });
        // Called as a row's UI is built, which covers every tree — including the search tree that is
        // rebuilt on each keystroke — and only for the rows actually displayed.
        treeList.setOnNodeUICreated((node, ui) -> onRowBuilt(treeList, node, ui));
        treeList.setDoubleClickToExpand(false);
        treeList.setClickToExpand(true);
        treeList.setSelectableNodeFilter(this::isDecidable);
        treeList.setClickToExpandFilter(this::isClickToExpand);
        treeList.setRightClickToExpand(true);
        if (container == null) return;
        container.addChild(treeList);
    }

    /**
     * Registers a tree of entries: initialises it, appends it to the tree container, and makes the
     * search look inside it. Call it from a subclass constructor for a tree held in a field.
     */
    protected <L extends TreeList<TreeNode<T, Void>>> L addContentTree(L tree) {
        initTreeList(tree, treeContainer);
        contentTrees.add(tree);
        return tree;
    }

    /** Creates and registers a content tree — the single-tree case, which needs no subclass. */
    public TreeList<TreeNode<T, Void>> addContentTree() {
        return addContentTree(new TreeList<>());
    }

    /** All trees the panel owns, whether currently visible or not. */
    protected List<TreeList<TreeNode<T, Void>>> getAllTrees() {
        var trees = new ArrayList<TreeList<TreeNode<T, Void>>>(contentTrees.size() + 2);
        trees.add(searchTree);
        trees.add(recommendationTree);
        trees.addAll(contentTrees);
        return trees;
    }

    /** Every entry the search may match, i.e. everything in the content trees. */
    public Stream<T> getAllItems() {
        return contentTrees.stream().flatMap(this::getTreeItems);
    }

    protected Stream<T> getTreeItems(TreeList<TreeNode<T, Void>> tree) {
        var root = tree.getRoot();
        if (root == null) return Stream.empty();
        return root.flatten().stream()
                // entries, not group folders, and not whatever a subclass hangs under an entry
                .filter(this::isDecidable)
                .filter(n -> n.getParent() != null) // not root
                .map(ITreeNode::getKey)
                .filter(this::isEntryItem);
    }

    /**
     * Puts a root on {@link #recommendationTree} and shows it, or hides it again for {@code null}.
     * Expanded on the way in, because a collapsed recommendation recommends nothing.
     */
    public void setRecommendationRoot(@Nullable TreeNode<T, Void> root) {
        recommendationTree.setRoot(root);
        if (root != null) {
            recommendationTree.expandNode(root);
        }
        Style.importantPipeline(recommendationTree.getLayout(),
                l -> l.display(root == null ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
    }

    // ---- selection ----

    protected void onSelectedChanged(TreeList<TreeNode<T, Void>> tree, TreeNode<T, Void> node, T newSelected) {
        if (selectedTree != tree) {
            if (selectedTree != null) {
                selectedTree.setSelected(Collections.emptySet(), false);
            }
            selectedTree = tree;
        }
        clearSelectedItemData(this.selectedItem);
        this.selectedNode = node;
        this.selectedItem = newSelected;
        prepareSelectedItemData(newSelected);
        updateDescription(newSelected);
    }

    /** Shows the selected entry's description in the side panel, or collapses the panel when it has none. */
    protected void updateDescription(@Nullable T item) {
        descriptionView.clearAllScrollViewChildren();
        var description = item == null ? null : createDescriptionUI(item);
        Style.importantPipeline(descriptionPanel.getLayout(),
                l -> l.display(description == null ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        if (description != null) {
            descriptionView.addScrollViewChild(description);
            updateDescriptionPanelBounds();
        }
    }

    /**
     * Places the description panel next to the library, flipping it to the left side when it wouldn't
     * fit on the right of the screen. Position and size are data-driven, so they are pinned IMPORTANT.
     */
    protected void updateDescriptionPanelBounds() {
        var mui = getModularUI();
        if (mui == null) return;
        var onRight = getPositionX() + getSizeWidth() + descriptionWidth <= mui.getScreenWidth();
        var anchorX = onRight ? getPositionX() + getSizeWidth() : getPositionX() - descriptionWidth;
        var offset = worldToLocalLayoutOffset(new Vector2f(anchorX, getPositionY()));
        var height = getSizeHeight();
        Style.importantPipeline(descriptionPanel.getLayout(), l -> l
                .left(offset.x)
                .top(offset.y)
                .width(descriptionWidth)
                .height(height));
    }

    /**
     * The size the popup opens at, before anyone drags its corner. Data-driven rather than a
     * stylesheet default — the resize drag writes the same two properties — so it is pinned IMPORTANT.
     */
    public ItemLibraryPanel<T> setPopupSize(float width, float height) {
        Style.importantPipeline(getLayout(), l -> l.width(width).height(height));
        return this;
    }

    /** Sets the width of the description side panel. */
    public ItemLibraryPanel<T> setDescriptionWidth(float descriptionWidth) {
        this.descriptionWidth = descriptionWidth;
        updateDescriptionPanelBounds();
        return this;
    }

    // ---- keyboard ----

    protected void onKeyDown(UIEvent event) {
        switch (event.keyCode) {
            case GLFW.GLFW_KEY_UP -> {
                moveKeyboardSelection(-1);
                event.stopPropagation();
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveKeyboardSelection(1);
                event.stopPropagation();
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT -> {
                if (selectedTree != null && selectedNode != null && selectedNode.isBranch()) {
                    if (event.keyCode == GLFW.GLFW_KEY_RIGHT) {
                        selectedTree.expandNode(selectedNode);
                    } else {
                        selectedTree.collapseNode(selectedNode);
                    }
                    event.stopPropagation();
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (selectedNode != null && selectedItem != null && isDecidable(selectedNode)) {
                    onNodeDecided(selectedItem);
                    event.stopPropagation();
                }
            }
        }
    }

    protected void moveKeyboardSelection(int direction) {
        var entries = getKeyboardNavigationEntries();
        if (entries.isEmpty()) return;

        var currentIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            if (entry.tree() == selectedTree && entry.node() == selectedNode) {
                currentIndex = i;
                break;
            }
        }

        var nextIndex = currentIndex < 0
                ? (direction > 0 ? 0 : entries.size() - 1)
                : Math.max(0, Math.min(entries.size() - 1, currentIndex + direction));
        selectKeyboardEntry(entries.get(nextIndex));
    }

    protected List<TreeNavigationEntry<T>> getKeyboardNavigationEntries() {
        var entries = new ArrayList<TreeNavigationEntry<T>>();
        for (var tree : getKeyboardNavigationTrees()) {
            addVisibleNodes(entries, tree);
        }
        return entries;
    }

    protected List<TreeList<TreeNode<T, Void>>> getKeyboardNavigationTrees() {
        if (searchTree.getRoot() != null) {
            return List.of(searchTree);
        }
        var trees = new ArrayList<TreeList<TreeNode<T, Void>>>(contentTrees.size() + 1);
        trees.add(recommendationTree);
        trees.addAll(contentTrees);
        return trees;
    }

    protected void addVisibleNodes(List<TreeNavigationEntry<T>> entries, TreeList<TreeNode<T, Void>> tree) {
        var root = tree.getRoot();
        if (root == null || !tree.isDisplayed()) return;
        // a flattened root has no row of its own, so stepping onto it would be a keypress that
        // selects nothing and looks like the arrow key being ignored
        if (tree.isFlattenRoot()) {
            for (var child : root.getChildren()) {
                addVisibleNode(entries, tree, child);
            }
        } else {
            addVisibleNode(entries, tree, root);
        }
    }

    protected void addVisibleNode(List<TreeNavigationEntry<T>> entries,
                                  TreeList<TreeNode<T, Void>> tree,
                                  ITreeNode<T, Void> rawNode) {
        var node = (TreeNode<T, Void>) rawNode;
        entries.add(new TreeNavigationEntry<>(tree, node));
        if (node.isBranch() && tree.isNodeExpanded(node)) {
            for (var child : node.getChildren()) {
                addVisibleNode(entries, tree, child);
            }
        }
    }

    protected void selectKeyboardEntry(TreeNavigationEntry<T> entry) {
        if (entry.tree() != selectedTree && selectedTree != null) {
            selectedTree.setSelected(Collections.emptySet(), false);
        }
        entry.tree().setSelected(List.of(entry.node()), true);
        selectedTree = entry.tree();
        selectedNode = entry.node();
    }

    protected void clearKeyboardSelection() {
        clearSelectedItemData(this.selectedItem);
        if (this.selectedTree != null) {
            this.selectedTree.setSelected(Collections.emptySet(), false);
        }
        this.selectedTree = null;
        this.selectedNode = null;
        this.selectedItem = null;
        updateDescription(null);
    }

    public record TreeNavigationEntry<T extends ILibraryItem>(TreeList<TreeNode<T, Void>> tree,
                                                              TreeNode<T, Void> node) {}

    // ---- search ----

    protected void onSearchWordChanged(String word) {
        if (word.isBlank()) {
            clearSearchResult();
            return;
        }
        clearKeyboardSelection();
        var lowerWorld = word.toLowerCase();
        var builder = TreeBuilder.<T, Void>start(searchRootItem());
        getAllItems().filter(item -> {
                    if (item.getSearchableName().toLowerCase().contains(lowerWorld)) {
                        return true;
                    }
                    if (item.getDisplayName().getString().toLowerCase().contains(lowerWorld)) {
                        return true;
                    }
                    return LocalizationUtils.format(item.getDisplayName().getString()).toLowerCase().contains(lowerWorld);
                })
                .forEach(item -> {
                    builder.leaf(item, null);
                });
        Style.importantPipeline(searchTree.getLayout(), l -> l.display(TaffyDisplay.FLEX));
        searchTree.setRoot(builder.build());
        Style.importantPipeline(treeContainer.getLayout(), l -> l.display(TaffyDisplay.NONE));
    }

    protected void clearSearchResult() {
        Style.importantPipeline(searchTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        searchTree.setRoot(null);
        Style.importantPipeline(treeContainer.getLayout(), l -> l.display(TaffyDisplay.FLEX));
        clearKeyboardSelection();
    }

    /** The search tree is flattened, so its root is never drawn — but it still needs to exist. */
    private T searchRootItem() {
        if (searchRootItem == null) {
            searchRootItem = groupItemFactory.get();
        }
        return searchRootItem;
    }

    // ---- show / hide / decide ----

    /**
     * Opens the popup at the given screen position. {@code onFinished} is handed the chosen entry, or
     * {@code null} when the popup is dismissed without one.
     */
    public void show(float mouseX, float mouseY, Consumer<@Nullable T> onFinished) {
        var mui = host.getModularUI();
        if (mui == null) return;

        var root = mui.ui.rootElement;
        if (getParent() != null) {
            removeSelf();
        }
        root.addChild(this);

        var offset = root.worldToLocalLayoutOffset(new Vector2f(mouseX, mouseY));
        this.getLayout()
                .left(offset.x)
                .top(offset.y);
        Style.importantPipeline(getLayout(), l -> l.display(TaffyDisplay.FLEX));
        searchField.focus();
        this.onFinished = onFinished;
    }

    public void hide() {
        if (this.onFinished != null) {
            this.onFinished.accept(null);
        }
        clearSelectedItemData(this.selectedItem);
        clearSearchResult();
        clearKeyboardSelection();
        onHide();
        this.searchField.setText("", false);
        this.selectedTree = null;
        this.selectedItem = null;
        this.selectedNode = null;
        this.onFinished = null;
        setRecommendationRoot(null);
        Style.importantPipeline(getLayout(), l -> l.display(TaffyDisplay.NONE));
        blur();
        removeSelf();
    }

    protected void onNodeDecided(T item) {
        var decided = decidedItem(item);
        if (onFinished != null) {
            onFinished.accept(decided);
            onFinished = null;
        }
        hide();
    }

    // ---- hooks ----

    /**
     * Whether a row can be selected and confirmed. Leaves only, by default: a folder is a way through
     * the list, not an answer to it.
     */
    protected boolean isDecidable(ITreeNode<T, ?> node) {
        return node.isLeaf();
    }

    /** Whether an item is one of the panel's own entries, i.e. something the search should offer. */
    protected boolean isEntryItem(T item) {
        return true;
    }

    /** Whether a left click on this row expands it, or only selects it. */
    protected boolean isClickToExpand(ITreeNode<T, ?> node) {
        return true;
    }

    /**
     * Called as a row's UI is built — the place to grow children a row only needs once shown, or to
     * put an id on the row so a UI test can press the actual thing.
     */
    protected void onRowBuilt(TreeList<TreeNode<T, Void>> tree, TreeNode<T, Void> node, UIElement ui) {
    }

    /** The description to show beside the selected entry, or null for none. */
    @Nullable
    protected UIElement createDescriptionUI(T item) {
        return null;
    }

    /** What choosing {@code item} actually returns — a sub-row may stand for its parent entry. */
    protected T decidedItem(T item) {
        return item;
    }

    /** Called when an entry becomes the selected one. */
    protected void prepareSelectedItemData(@Nullable T item) {
    }

    /** Called when an entry stops being the selected one, and once more on {@link #hide}. */
    protected void clearSelectedItemData(@Nullable T item) {
    }

    /** Subclass cleanup, run while {@link #hide} is tearing the popup's state down. */
    protected void onHide() {
    }
}
