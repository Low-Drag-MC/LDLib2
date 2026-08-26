package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemLibraryPanel;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ICustomNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.InputOutputPortsNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The graph's pick-a-node popup: an {@link ItemLibraryPanel} whose entries are node classes, the
 * constants of the graph's type system, a context's blocks, and the ports of any of those.
 *
 * <p>Everything about <i>being</i> a searchable popup — where it opens, the arrow keys, the search,
 * the description panel, the drag and the resize — is the panel's, and was moved there so that
 * anything else needing this window does not have to grow a second one that looks almost like it.
 * What is left here is the half that knows what a node is: which trees exist, how a node type becomes
 * an entry, the throwaway models used to read a node's ports and description, and the port sub-rows a
 * wire drag hangs under each entry.
 */
public class ItemLibrary extends ItemLibraryPanel<ItemLibraryItem> {

    public final GraphView graphView;

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
    /** True while the library is in "pick a block for context X" mode (see {@link #showBlocksForContext}). */
    protected boolean blockOnlyMode = false;
    /**
     * Throwaway models built to inspect a node type (ports, description). Cached for the lifetime of a
     * single show, so the recommendation pass, the port sub-items and the description panel share one
     * model per item instead of creating one each.
     */
    protected final Map<ItemLibraryItem, GraphElementModel> testModels = new HashMap<>();

    public ItemLibrary(GraphView graphView) {
        super(graphView, ItemLibraryItem::new);
        this.graphView = graphView;

        constantTree.addClass("__item-library_constant-tree__");
        contextTree.addClass("__item-library_context-tree__");
        nodeTree.addClass("__item-library_node-tree__");
        blockTree.addClass("__item-library_block-tree__");

        addContentTree(constantTree);
        addContentTree(contextTree);
        addContentTree(nodeTree);
        // Block tree shares the same container slot; only one of {nodeTree+constantTree+contextTree}
        // and {blockTree} is visible at a time — see applyTreeVisibility.
        Style.importantPipeline(blockTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
        addContentTree(blockTree);
    }

    // ---- the graph's entries ----

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

    @Override
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

    @Override
    protected List<TreeList<TreeNode<ItemLibraryItem, Void>>> getKeyboardNavigationTrees() {
        if (searchTree.getRoot() != null) {
            return List.of(searchTree);
        }
        if (blockOnlyMode) {
            return List.of(blockTree);
        }
        return List.of(recommendationTree, constantTree, contextTree, nodeTree);
    }

    // ---- what the panel asks about an entry ----

    /**
     * A node is decidable (selectable + confirmable) when it holds a node item — even if it grew port
     * children — or when it is a leaf, i.e. a constant / block / port item. Group folders are not.
     */
    protected static boolean isDecidableNode(ITreeNode<ItemLibraryItem, ?> node) {
        return node.getKey() instanceof NodeModelLibraryItem || node.isLeaf();
    }

    @Override
    protected boolean isDecidable(ITreeNode<ItemLibraryItem, ?> node) {
        return isDecidableNode(node);
    }

    /** A node item stays an entry once it grew port children; the port children themselves are not. */
    @Override
    protected boolean isEntryItem(ItemLibraryItem item) {
        return !(item instanceof PortLibraryItem);
    }

    /**
     * A node item stays selectable/confirmable once it grows port children; clicking it only selects
     * it, expanding its ports is done with a right click (or the arrow).
     */
    @Override
    protected boolean isClickToExpand(ITreeNode<ItemLibraryItem, ?> node) {
        return !(node.getKey() instanceof NodeModelLibraryItem);
    }

    @Override
    protected void onRowBuilt(TreeList<TreeNode<ItemLibraryItem, Void>> tree,
                              TreeNode<ItemLibraryItem, Void> node, UIElement ui) {
        ensurePortChildren(node);
    }

    /** The selected item's node description — see {@link Node#createDescriptionUI()}. */
    @Nullable
    @Override
    protected UIElement createDescriptionUI(ItemLibraryItem item) {
        var node = resolveNode(item);
        return node == null ? null : node.createDescriptionUI();
    }

    /** Resolves the {@link Node} behind a library item, or null if the item isn't backed by one. */
    @Nullable
    protected Node resolveNode(ItemLibraryItem rawItem) {
        var item = ownerItem(rawItem);
        if (item instanceof BlockLibraryItem blockItem) {
            try {
                return blockItem.getBlockClass().getConstructor().newInstance();
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to create block node {} for its description", blockItem.getBlockClass(), e);
                return null;
            }
        }
        // constants have no custom node behind them, hence no description
        return getTestModel(item) instanceof ICustomNodeModel customNodeModel ? customNodeModel.getNode() : null;
    }

    /** Sets the width of the description side panel. */
    @Override
    public ItemLibrary setDescriptionWidth(float descriptionWidth) {
        super.setDescriptionWidth(descriptionWidth);
        return this;
    }

    // ---- test models and ports ----

    /**
     * Gets the throwaway model used to inspect the item's node type, creating it on first use.
     * Returns null for items that don't create a node (e.g. blocks) or when no graph is loaded.
     */
    @Nullable
    protected GraphElementModel getTestModel(ItemLibraryItem item) {
        if (this.graphModel == null || !(item instanceof NodeModelLibraryItem nodeItem)) return null;
        return testModels.computeIfAbsent(item, key -> nodeItem.createNode(GraphNodeCreationData.ofOrphan(this.graphModel)));
    }

    /**
     * The ports of the item's node that the dragged wire can be connected to, in display order.
     * Empty when the library wasn't opened from a wire drag.
     */
    protected List<PortModel> getCompatiblePorts(ItemLibraryItem item) {
        if (portModels == null || portModels.isEmpty()) return List.of();
        return getCompatiblePorts(item, portModels.getFirst());
    }

    /** As {@link #getCompatiblePorts(ItemLibraryItem)}, for an explicit source port. */
    protected List<PortModel> getCompatiblePorts(ItemLibraryItem item, PortModel sourcePort) {
        return getTestModel(item) instanceof InputOutputPortsNodeModel nodeModel
                ? nodeModel.getPortsFitToConnectTo(sourcePort)
                : List.of();
    }

    /**
     * Attaches one child per connectable port to a node item's tree node, so the user can expand it and
     * pick the port to wire to instead of taking the first compatible one. Does nothing when the
     * library wasn't opened from a wire drag, or when the children already exist.
     */
    protected void ensurePortChildren(TreeNode<ItemLibraryItem, Void> node) {
        // getCompatiblePorts is empty unless a wire drag is in progress, so this self-disables
        if (!(node.getKey() instanceof NodeModelLibraryItem nodeItem) || node.isBranch()) return;
        for (var port : getCompatiblePorts(nodeItem)) {
            node.createChild(new PortLibraryItem(nodeItem, port));
        }
    }

    protected void removePortChildren(TreeNode<ItemLibraryItem, Void> node) {
        for (var child : List.copyOf(node.getChildren())) {
            if (child.getKey() instanceof PortLibraryItem) {
                node.removeChild(child);
            }
        }
    }

    /**
     * Attaches the port sub-items to the rows that already exist. Needed because trees (and their
     * rows) survive between shows, so a row built before this wire drag would never get them —
     * rows built from now on are covered by the {@code onRowBuilt} hook. With
     * {@code staticTree = false} the {@link TreeList} picks the new children up on its next tick.
     */
    protected void attachPortChildren() {
        for (var tree : getAllTrees()) {
            // only materialised rows: entries inside collapsed folders are handled when they are built
            for (var node : List.copyOf(tree.getNodeUIs().keySet())) {
                ensurePortChildren(node);
            }
        }
    }

    /** Strips every port sub-item, whether its row is currently built or not. */
    protected void detachPortChildren() {
        for (var tree : getAllTrees()) {
            var root = tree.getRoot();
            if (root == null) continue;
            for (var node : root.flatten()) {
                if (!(node instanceof TreeNode<ItemLibraryItem, Void> treeNode)) continue;
                // collapse first: it drops the port rows right away instead of leaving them on screen
                // until the tree's next tick notices the children are gone.
                if (treeNode.getKey() instanceof NodeModelLibraryItem && tree.isNodeExpanded(treeNode)) {
                    tree.collapseNode(treeNode);
                }
                removePortChildren(treeNode);
            }
        }
    }

    /** The entry an item belongs to: a port sub-item stands for its owner, anything else for itself. */
    protected static ItemLibraryItem ownerItem(ItemLibraryItem item) {
        return item instanceof PortLibraryItem portItem ? portItem.getOwner() : item;
    }

    /**
     * A port sub-item pins its port on the owner and hands the owner back: the create-node commands
     * work with a {@link NodeModelLibraryItem} and read the port from its data.
     */
    @Override
    protected ItemLibraryItem decidedItem(ItemLibraryItem item) {
        if (item instanceof PortLibraryItem) {
            prepareSelectedItemData(item);
        }
        return ownerItem(item);
    }

    @Override
    protected void prepareSelectedItemData(@Nullable ItemLibraryItem item) {
        if (item == null || portModels == null || portModels.isEmpty()) return;
        NodeModelLibraryItem owner;
        PortModel portToConnect;
        if (item instanceof PortLibraryItem portItem) {
            // an explicitly picked port wins over the default first-compatible-port behaviour
            owner = portItem.getOwner();
            portToConnect = portItem.getPort();
        } else if (item instanceof NodeModelLibraryItem nodeItem) {
            var ports = getCompatiblePorts(nodeItem);
            if (ports.isEmpty()) return;
            owner = nodeItem;
            portToConnect = ports.getFirst();
        } else return;
        var model = getTestModel(owner);
        if (model != null) {
            owner.setData(new NodeItemLibraryData(model.getClass(), portToConnect));
        }
    }

    @Override
    protected void clearSelectedItemData(@Nullable ItemLibraryItem item) {
        if (item == null) return;
        ownerItem(item).setData(null);
    }

    // ---- show / hide ----

    @Override
    public void show(float mouseX, float mouseY, Consumer<@Nullable ItemLibraryItem> onFinished) {
        title.setText("graph.commands.add_node");
        tailLabel.setText("graph.double_click_add");
        this.blockOnlyMode = false;
        applyTreeVisibility();
        super.show(mouseX, mouseY, onFinished);
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
        for (var blockType : context.getSupportBlockClasses()) {
            var annotation = blockType.getAnnotation(NodeAttribute.class);
            var name = annotation == null ? blockType.getSimpleName() : annotation.name();
            builder.leaf(new BlockLibraryItem(name, blockType), null);
        }
        var root = builder.build();
        blockTree.setRoot(root);
        blockTree.expandNode(root);
        clearKeyboardSelection();

        applyTreeVisibility();
        // not show(): that one resets block-only mode, which is the whole point of this entry point
        super.show(mouseX, mouseY, onFinished);
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

    public void setRecommendation(Consumer<TreeBuilder<ItemLibraryItem, Void>> builderConsumer) {
        var recommendationBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setDisplayName(Component.translatable("graph.library.recommendation")));
        builderConsumer.accept(recommendationBuilder);
        if (recommendationBuilder.isEmpty()) return;
        setRecommendationRoot(recommendationBuilder.build());
    }

    public void setPortRecommendation(PortModel sourcePort) {
        if (this.graphModel == null) return;
        setRecommendation(builder -> getAllItems().forEach(item -> {
            // same lookup the port sub-items use, so it hits the shared test model cache
            if (item instanceof NodeModelLibraryItem nodeItem && !getCompatiblePorts(nodeItem, sourcePort).isEmpty()) {
                builder.leaf(nodeItem, null);
            }
        }));
    }

    public void showWithNodesFitPort(float mouseX, float mouseY, List<PortModel> portModels, Consumer<@Nullable ItemLibraryItem> onFinished) {
        if (portModels.isEmpty()) return;
        testModels.clear();
        this.portModels = portModels;
        attachPortChildren();
        setPortRecommendation(portModels.getFirst());
        show(mouseX, mouseY, onFinished);
        // AFTER show(), which puts the generic "add a node" title up. This one names the type the
        // dragged wire carries, and setting it first meant it was overwritten before anyone saw it.
        title.setText(Component.translatable("graph.library.choose",
                Component.translatable(portModels.getFirst().getDataTypeHandle().getFriendlyName())));
    }

    @Override
    protected void onHide() {
        // strip the port sub-items: the trees outlive this popup and the ports they point at don't
        detachPortChildren();
        testModels.clear();
        this.portModels = null;
        // Clear block-mode state so the next show() starts fresh in default-tree mode.
        this.blockOnlyMode = false;
        this.blockTree.setRoot(null);
        Style.importantPipeline(this.blockTree.getLayout(), l -> l.display(TaffyDisplay.NONE));
    }
}
