package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ItemLibrary extends UIElement {
    public record DragMove(Vector2f originalPos) {}
    public record DragResize(Vector2f originalSize) {}

    public final GraphView graphView;

    public final UIElement headBar = new UIElement();
    public final Label title = new Label();
    public final TextField searchField = new TextField();
    public final ScrollerView resultContainer = new ScrollerView();
    public final UIElement tailBar = new UIElement();
    public final Label tailLabel = new Label();
    public final UIElement resizeButton = new UIElement();

    public final UIElement treeContainer = new UIElement();
    public final TreeList<TreeNode<ItemLibraryItem, Void>> searchTree = new TreeList<>();
    public final TreeList<TreeNode<ItemLibraryItem, Void>> recommendationTree = new TreeList<>();
    public final TreeList<TreeNode<ItemLibraryItem, Void>> constantTree = new TreeList<>();
    public final TreeList<TreeNode<ItemLibraryItem, Void>> contextTree = new TreeList<>();
    public final TreeList<TreeNode<ItemLibraryItem, Void>> nodeTree = new TreeList<>();
    /** Hidden by default. Populated and shown only when {@link #showBlocksForContext} is called. */
    public final TreeList<TreeNode<ItemLibraryItem, Void>> blockTree = new TreeList<>();
    // runtime
    @Nullable
    protected GraphModel graphModel;
    @Nullable
    protected List<PortModel> portModels;
    @Nullable
    protected TreeList<?> selectedTree;
    @Nullable
    protected ItemLibraryItem selectedItem;
    @Nullable
    protected TreeNode<ItemLibraryItem, Void> selectedNode;
    @Nullable
    protected Consumer<@Nullable ItemLibraryItem> onFinished;
    @Nullable
    protected List<ItemLibraryItem> searchCandidates;
    /** True while the library is in "pick a block for context X" mode (see {@link #showBlocksForContext}). */
    protected boolean blockOnlyMode = false;

    public ItemLibrary(GraphView graphView) {
        this.graphView = graphView;
        addClass("__item-library__");
        // ABSOLUTE positioning + width/height are popup-driven (resize, show-at-mouse) — pin via IMPORTANT.
        Style.importantPipeline(getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .width(150)
                .height(200));
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

        searchTree.setStaticTree(false);
        recommendationTree.setStaticTree(false);
        constantTree.setStaticTree(false);
        contextTree.setStaticTree(false);
        nodeTree.setStaticTree(false);
        blockTree.setStaticTree(false);

        searchTree.addClass("__item-library_search-tree__");
        recommendationTree.addClass("__item-library_recommend-tree__");
        constantTree.addClass("__item-library_constant-tree__");
        contextTree.addClass("__item-library_context-tree__");
        nodeTree.addClass("__item-library_node-tree__");
        blockTree.addClass("__item-library_block-tree__");
        treeContainer.addClass("__item-library_tree-container__");

        searchField.setTextResponder(this::onSearchWordChanged);
        // Initial tree visibility is mode-driven — pin via IMPORTANT.
        Style.importantPipeline(searchTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        searchTree.setFlattenRoot(true);
        initTreeList(searchTree, null);

        Style.importantPipeline(recommendationTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        initTreeList(recommendationTree, treeContainer);
        initTreeList(constantTree, treeContainer);
        initTreeList(contextTree, treeContainer);
        initTreeList(nodeTree, treeContainer);
        // Block tree shares the same container slot; only one of {nodeTree+constantTree+contextTree}
        // and {blockTree} is visible at a time — see applyTreeVisibility.
        Style.importantPipeline(blockTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        initTreeList(blockTree, treeContainer);

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

        addChildren(
                headBar.addChildren(title),
                searchField,
                resultContainer,
                tailBar.addChildren(tailLabel, resizeButton)
        );
        setFocusable(true);
        setEnforceFocus(e -> this.hide());
        addEventListener(UIEvents.LAYOUT_CHANGED, e -> adaptPositionToScreen());
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

    protected void initTreeList(TreeList<TreeNode<ItemLibraryItem, Void>> treeList, @Nullable UIElement container) {
        treeList.setNodeUISupplier(TreeList.iconTextTemplate(
                node -> node.getKey().getIcon(),
                node -> node.getKey().getDisplayName())
        );
        treeList.setOnDoubleClickNode(node -> {
            if (node.isBranch()) return;
            onNodeDecided(node.getKey());
        });
        treeList.setOnSelectedChanged(selected -> {
            if (selected.isEmpty()) return;
            var node = selected.iterator().next();
            onSelectedChanged(treeList, node, node.getKey());
        });
        treeList.setDoubleClickToExpand(false);
        treeList.setClickToExpand(true);
        treeList.setSelectableNodeFilter(ITreeNode::isLeaf);
        if (container == null) return;
        container.addChild(treeList);
    }

    public void onLoadGraph(GraphModel graphModel) {
        this.graphModel = graphModel;
        // Regular nodes go to nodeTree, context nodes to contextTree, blocks are excluded —
        // block-only mode populates blockTree on demand per parent context.
        var nodesBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.nodes")));
        var contextsBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.contexts")));
        var nodeGroupItems = new HashMap<String, ItemLibraryItem>();
        var contextGroupItems = new HashMap<String, ItemLibraryItem>();
        for (var nodeType : graphModel.getLibrarySupportNodes()) {
            if (BlockNode.class.isAssignableFrom(nodeType)) continue;
            if (ContextNode.class.isAssignableFrom(nodeType)) {
                addNodeLibraryItem(contextsBuilder, contextGroupItems, nodeType);
            } else {
                addNodeLibraryItem(nodesBuilder, nodeGroupItems, nodeType);
            }
        }
        nodeTree.setRoot(nodesBuilder.build());
        contextTree.setRoot(contextsBuilder.build());
        // load constants
        var constantsBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.constants")));
        for (var typeHandle : graphModel.getLibrarySupportTypes()) {
            constantsBuilder.leaf(new NodeModelLibraryItem(typeHandle.getName(),
                    data -> data.createConstantNode(typeHandle.getName(), typeHandle))
                    .setDisplayName(Component.translatable(typeHandle.getFriendlyName())), null);
        }
        constantTree.setRoot(constantsBuilder.build());
    }

    static void addNodeLibraryItem(TreeBuilder<ItemLibraryItem, Void> builder,
                                   Map<String, ItemLibraryItem> groupItems,
                                   Class<? extends Node> nodeType) {
        var annotation = nodeType.getAnnotation(NodeAttribute.class);
        var name = annotation == null ? nodeType.getSimpleName() : annotation.name();
        var item = new NodeModelLibraryItem(name,
                data -> CustomGraphModelImpl.createNodeFromData(data, nodeType));
        if (annotation == null || annotation.group().isBlank()) {
            builder.leaf(item, null);
            return;
        }

        var groupPath = getNodeGroupPath(annotation.group(), groupItems);
        if (groupPath.isEmpty()) {
            builder.leaf(item, null);
        } else {
            builder.diveBranch(groupPath, b -> b.leaf(item, null));
        }
    }

    private static List<ItemLibraryItem> getNodeGroupPath(String group, Map<String, ItemLibraryItem> groupItems) {
        var groupPath = new ArrayList<ItemLibraryItem>();
        var fullPath = new StringBuilder();
        for (var part : group.split("/")) {
            var groupName = part.trim();
            if (groupName.isEmpty()) continue;
            if (!fullPath.isEmpty()) {
                fullPath.append('/');
            }
            fullPath.append(groupName);
            var path = fullPath.toString();
            groupPath.add(groupItems.computeIfAbsent(path, ignored -> new ItemLibraryItem()
                    .setPath(path)
                    .setIcon(Icons.FOLDER)
                    .setDisplayName(Component.translatable(groupName))
                    .setSearchableName(groupName)));
        }
        return groupPath;
    }

    public Stream<ItemLibraryItem> getAllItems() {
        // Search is scoped to whatever's currently visible. In block-only mode that's only the
        // compatible blocks; otherwise it's the regular nodes + contexts + constants.
        if (blockOnlyMode) {
            return getTreeItems(blockTree);
        }
        return Stream.concat(
                Stream.concat(getTreeItems(constantTree), getTreeItems(nodeTree)),
                getTreeItems(contextTree));
    }

    protected Stream<ItemLibraryItem> getTreeItems(TreeList<TreeNode<ItemLibraryItem, Void>> tree) {
        return Optional.ofNullable(tree.getRoot())
                .map(node -> node.flatten().stream()
                        .filter(ITreeNode::isLeaf)
                        .filter(n -> n.getParent() != null) // not root
                        .map(ITreeNode::getKey)
                )
                .orElseGet(Stream::empty);
    }

    protected void onSelectedChanged(TreeList<TreeNode<ItemLibraryItem, Void>> tree, TreeNode<ItemLibraryItem, Void> node, ItemLibraryItem newSelected) {
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
    }

    protected void onKeyDown(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent event) {
        switch (event.keyCode) {
            case GLFW.GLFW_KEY_UP -> {
                moveKeyboardSelection(-1);
                event.stopPropagation();
            }
            case GLFW.GLFW_KEY_DOWN -> {
                moveKeyboardSelection(1);
                event.stopPropagation();
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (selectedNode != null && selectedNode.isLeaf() && selectedItem != null) {
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

    protected List<TreeNavigationEntry> getKeyboardNavigationEntries() {
        var entries = new ArrayList<TreeNavigationEntry>();
        for (var tree : getKeyboardNavigationTrees()) {
            addVisibleNodes(entries, tree);
        }
        return entries;
    }

    protected List<TreeList<TreeNode<ItemLibraryItem, Void>>> getKeyboardNavigationTrees() {
        if (searchTree.getRoot() != null) {
            return List.of(searchTree);
        }
        if (blockOnlyMode) {
            return List.of(blockTree);
        }
        return List.of(recommendationTree, constantTree, contextTree, nodeTree);
    }

    protected void addVisibleNodes(List<TreeNavigationEntry> entries, TreeList<TreeNode<ItemLibraryItem, Void>> tree) {
        var root = tree.getRoot();
        if (root == null || !tree.isDisplayed()) return;
        if (tree == searchTree) {
            for (var child : root.getChildren()) {
                addVisibleNode(entries, tree, child);
            }
        } else {
            addVisibleNode(entries, tree, root);
        }
    }

    protected void addVisibleNode(List<TreeNavigationEntry> entries,
                                  TreeList<TreeNode<ItemLibraryItem, Void>> tree,
                                  ITreeNode<ItemLibraryItem, Void> rawNode) {
        var node = (TreeNode<ItemLibraryItem, Void>) rawNode;
        entries.add(new TreeNavigationEntry(tree, node));
        if (node.isBranch() && tree.isNodeExpanded(node)) {
            for (var child : node.getChildren()) {
                addVisibleNode(entries, tree, child);
            }
        }
    }

    protected void selectKeyboardEntry(TreeNavigationEntry entry) {
        if (entry.tree() != selectedTree && selectedTree != null) {
            selectedTree.setSelected(Collections.emptySet(), false);
        }
        entry.tree().setSelected(List.of(entry.node()), true);
        selectedTree = entry.tree();
        selectedNode = entry.node();
    }

    protected void onSearchWordChanged(String word) {
        if (word.isBlank()) {
            clearSearchResult();
            return;
        }
        clearKeyboardSelection();
        var lowerWorld = word.toLowerCase();
        var builder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem());
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
        searchCandidates = null;
        clearKeyboardSelection();
    }

    protected void prepareSelectedItemData(ItemLibraryItem item) {
        if (item == null || this.graphModel == null) return;
        if (portModels == null || portModels.isEmpty()) return;
        var sourcePort = portModels.getFirst();
        var testData = GraphNodeCreationData.ofOrphan(this.graphModel);
        if (item instanceof NodeModelLibraryItem nodeItem) {
            if (nodeItem.createNode(testData) instanceof NodeModel nodeModel) {
                var ports = sourcePort.getDirection() == PortDirection.INPUT ?
                    nodeModel.getOutputsByDisplayOrder() : nodeModel.getInputsByDisplayOrder();
                var compatiblePorts = graphModel.getCompatiblePorts(ports, sourcePort);
                if (compatiblePorts.isEmpty()) return;
                nodeItem.setData(new NodeItemLibraryData(nodeModel.getClass(), compatiblePorts.getFirst()));
                for (var portToAdd : compatiblePorts) {
                    // todo sub port items
                }
            }
        }
    }

    protected void clearSelectedItemData(ItemLibraryItem item) {
        if (item == null) return;
        item.setData(null);
    }

    public void show(float mouseX, float mouseY, Consumer<@Nullable ItemLibraryItem> onFinished) {
        title.setText("graph.commands.add_node");
        tailLabel.setText("graph.double_click_add");
        this.blockOnlyMode = false;
        applyTreeVisibility();
        positionAndShow(mouseX, mouseY, onFinished);
    }

    /**
     * Opens the library in block-only mode for the given context. The block tree is rebuilt
     * from {@code context.getSupportBlockClasses()}; all other trees are hidden.
     *
     * <p>The {@code onFinished} consumer receives a {@link BlockLibraryItem} on selection (or
     * {@code null} on dismiss). Callers should dispatch
     * {@code BlockCommands.InsertBlockCommand} using {@code item.getBlockClass()}.</p>
     */
    public void showBlocksForContext(float mouseX, float mouseY, ContextNodeModel context,
                                     Consumer<@Nullable ItemLibraryItem> onFinished) {
        if (context == null) return;
        title.setText("graph.add_block");
        tailLabel.setText("graph.double_click_add");
        this.blockOnlyMode = true;

        var builder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.blocks")));
        for (var blockClass : context.getSupportBlockClasses()) {
            builder.leaf(new BlockLibraryItem(blockClass), null);
        }
        var root = builder.build();
        blockTree.setRoot(root);
        blockTree.expandNode(root);
        clearKeyboardSelection();

        applyTreeVisibility();
        positionAndShow(mouseX, mouseY, onFinished);
    }

    /** Toggles tree visibility based on {@link #blockOnlyMode}. State-driven, pin via IMPORTANT. */
    protected void applyTreeVisibility() {
        var normalDisplay = blockOnlyMode ? TaffyDisplay.NONE : TaffyDisplay.FLEX;
        var blockDisplay = blockOnlyMode ? TaffyDisplay.FLEX : TaffyDisplay.NONE;
        Style.importantPipeline(constantTree.getLayout(), l -> l.display(normalDisplay));
        Style.importantPipeline(contextTree.getLayout(), l -> l.display(normalDisplay));
        Style.importantPipeline(nodeTree.getLayout(), l -> l.display(normalDisplay));
        Style.importantPipeline(blockTree.getLayout(), l -> l.display(blockDisplay));
    }

    /** Shared positioning + focus path for both show variants. */
    private void positionAndShow(float mouseX, float mouseY, Consumer<@Nullable ItemLibraryItem> onFinished) {
        var mui = graphView.getModularUI();
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

    public void setRecommendation(Consumer<TreeBuilder<ItemLibraryItem, Void>> builderConsumer) {
        var recommendationBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setDisplayName(Component.translatable("graph.library.recommendation")));
        builderConsumer.accept(recommendationBuilder);
        if (recommendationBuilder.isEmpty()) return;
        recommendationTree.setRoot(recommendationBuilder.build());
        recommendationTree.expandNode(recommendationTree.getRoot());
        Style.importantPipeline(recommendationTree.getLayout(), l -> l.display(TaffyDisplay.FLEX));
    }

    public void setPortRecommendation(PortModel sourcePort) {
        if (this.graphModel == null) return;
        var testData = GraphNodeCreationData.ofOrphan(this.graphModel);
        setRecommendation(builder -> {
            getAllItems().forEach(item -> {
                if (item instanceof NodeModelLibraryItem nodeItem) {
                    if (nodeItem.createNode(testData) instanceof PortNodeModel portNodeModel) {
                        if (portNodeModel.getPortFitToConnectTo(sourcePort) != null) {
                            builder.leaf(nodeItem, null);
                        }
                    }
                }
            });
        });
    }

    public void showWithNodesFitPort(float mouseX, float mouseY, List<PortModel> portModels, Consumer<@Nullable ItemLibraryItem> onFinished) {
        if (portModels.isEmpty()) return;
        this.portModels = portModels;
        title.setText(Component.translatable("graph.library.choose", Component.translatable(portModels.getFirst().getDataTypeHandle().getFriendlyName())));
        setPortRecommendation(portModels.getFirst());
        show(mouseX, mouseY, onFinished);
    }

    public void hide() {
        if (this.onFinished != null) {
            this.onFinished.accept(null);
        }
        clearSelectedItemData(this.selectedItem);
        clearSearchResult();
        clearKeyboardSelection();
        this.searchField.setText("", false);
        this.selectedTree = null;
        this.selectedItem = null;
        this.selectedNode = null;
        this.portModels = null;
        this.onFinished = null;
        this.recommendationTree.setRoot(null);
        Style.importantPipeline(this.recommendationTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        // Clear block-mode state so the next show() starts fresh in default-tree mode.
        this.blockOnlyMode = false;
        this.blockTree.setRoot(null);
        Style.importantPipeline(this.blockTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        Style.importantPipeline(getLayout(), l -> l.display(TaffyDisplay.NONE));
        blur();
        removeSelf();
    }

    protected void onNodeDecided(ItemLibraryItem itemLibraryItem) {
        if (onFinished != null) {
            onFinished.accept(itemLibraryItem);
            onFinished = null;
        }
        hide();
    }

    protected record TreeNavigationEntry(TreeList<TreeNode<ItemLibraryItem, Void>> tree,
                                         TreeNode<ItemLibraryItem, Void> node) {}

    protected void clearKeyboardSelection() {
        clearSelectedItemData(this.selectedItem);
        if (this.selectedTree != null) {
            this.selectedTree.setSelected(Collections.emptySet(), false);
        }
        this.selectedTree = null;
        this.selectedNode = null;
        this.selectedItem = null;
    }
}
