package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
    public final TreeList<TreeNode<ItemLibraryItem, Void>> nodeTree = new TreeList<>();
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
    protected Consumer<@Nullable ItemLibraryItem> onFinished;
    @Nullable
    protected List<ItemLibraryItem> searchCandidates;

    public ItemLibrary(GraphView graphView) {
        this.graphView = graphView;
        getLayout().positionType(TaffyPosition.ABSOLUTE)
                .gapAll(2)
                .paddingAll(5)
                .width(150)
                .height(200);
        getStyle().background(Sprites.BORDER1_RT1);

        headBar.getLayout().flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2);
        title.getTextStyle().textWrap(TextWrap.HOVER_ROLL);
        title.setOverflowVisible(false);
        title.getLayout().flex(1);

        resultContainer.getLayout().flex(1);

        searchField.setTextResponder(this::onSearchWordChanged);
        searchTree.setDisplay(false);
        searchTree.setFlattenRoot(true);
        initTreeList(searchTree, null);

        recommendationTree.setDisplay(false);
        initTreeList(recommendationTree, treeContainer);
        initTreeList(nodeTree, treeContainer);

        resultContainer.addScrollViewChildren(searchTree, treeContainer);

        tailBar.getLayout().flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2);
        tailLabel.setText("Double click to add a node");
        tailLabel.getTextStyle().textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER).fontSize(4.5f);
        tailLabel.setOverflowVisible(false);
        tailLabel.getLayout().flex(1);
        resizeButton.getLayout().width(9).height(9);
        resizeButton.getStyle().background(DynamicTexture.of(() -> resizeButton.isHover() ?
                Icons.RESIZE_BOTTOM_RIGHT : Icons.RESIZE_BOTTOM_RIGHT.copy().setColor(ColorPattern.LIGHT_GRAY.color)));

        addChildren(
                headBar.addChildren(title),
                searchField,
                resultContainer,
                tailBar.addChildren(tailLabel, resizeButton)
        );
        setFocusable(true);
        setEnforceFocus(e -> this.hide());
        addEventListener(UIEvents.LAYOUT_CHANGED, e -> adaptPositionToScreen());
        internalSetup();

        // drag
        headBar.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.bubbleListeners.size() > 1) return;
            var icon = Icons.MOVE;
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            headBar.startDrag(new DragMove(new Vector2f(this.getLayoutX(), this.getLayoutY())), Icons.MOVE)
                    .setDragTexture(- width / 2f, -height / 2f, width, height);
        });
        headBar.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (e.dragHandler.draggingObject instanceof DragMove(var oPos)) {
                var normalPosOffset = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
                this.getLayout()
                        .left(oPos.x + normalPosOffset.x)
                        .top(oPos.y + normalPosOffset.y);
            }
        });

        // resize
        resizeButton.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            var icon = Icons.MOVE;
            var width = icon.spriteSize.width;
            var height = icon.spriteSize.height;
            resizeButton.startDrag(new DragResize(new Vector2f(this.getSizeWidth(), this.getSizeHeight())), Icons.MOVE)
                    .setDragTexture(- width / 2f, -height / 2f, width, height);
        });
        resizeButton.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (e.dragHandler.draggingObject instanceof DragResize(var oSize)) {
                var normalSizeOffset = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
                this.getLayout()
                        .width(Math.max(oSize.x + normalSizeOffset.x, 50))
                        .height(Math.max(oSize.y + normalSizeOffset.y, 70));
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
            onSelectedChanged(treeList, selected.iterator().next().getKey());
        });
        treeList.setDoubleClickToExpand(false);
        treeList.setClickToExpand(true);
        treeList.setSelectableNodeFilter(ITreeNode::isLeaf);
        if (container == null) return;
        container.addChild(treeList);
    }

    public void onLoadGraph(GraphModel graphModel) {
        this.graphModel = graphModel;
        // tree
        var nodesBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setIcon(Icons.NODE)
                .setDisplayName(Component.translatable("graph.library.nodes")));
        for (var nodeType : graphModel.getSupportNodes()) {
            nodesBuilder.leaf(new NodeModelLibraryItem(nodeType.getSimpleName(),
                    data -> CustomGraphModelImpl.createNodeFromData(data, nodeType)), null);
        }
        nodeTree.setRoot(nodesBuilder.build());
    }

    protected void onSelectedChanged(TreeList<TreeNode<ItemLibraryItem, Void>> tree, ItemLibraryItem newSelected) {
        if (selectedTree != tree) {
            if (selectedTree != null) {
                selectedTree.setSelected(Collections.emptySet(), false);
            }
            selectedTree = tree;
        }
        clearSelectedItemData(this.selectedItem);
        this.selectedItem = newSelected;
        prepareSelectedItemData(newSelected);
    }

    protected void onSearchWordChanged(String word) {
        if (word.isBlank()) {
            clearSearchResult();
            return;
        }
        var lowerWorld = word.toLowerCase();
        var candidates = nodeTree.getRoot() == null
                ? Collections.<ItemLibraryItem>emptyList()
                : nodeTree.getRoot().flatten().stream()
                .filter(ITreeNode::isLeaf)
                .map(ITreeNode::getKey)
                .filter(item -> item.getSearchableName().toLowerCase().contains(lowerWorld)).toList();
        var builder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem());
        for (ItemLibraryItem item : candidates) {
            builder.leaf(item, null);
        }
        searchTree.setDisplay(true);
        searchTree.setRoot(builder.build());
        treeContainer.setDisplay(false);
    }

    protected void clearSearchResult() {
        searchTree.setDisplay(false);
        searchTree.setRoot(null);
        treeContainer.setDisplay(true);
        searchCandidates = null;
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
        var parent = getParent();
        var localMouse = getLocalMouse(mouseX, mouseY);
        var offset = new Vector2f(
                localMouse.x - (parent == null ? 0 : parent.getPositionX()),
                localMouse.y - (parent == null ? 0 : parent.getPositionY())
        );
        this.getLayout()
                .left(offset.x)
                .top(offset.y);
        setDisplay(true);
        focus();
        this.onFinished = onFinished;
    }

    public void setRecommendation(Consumer<TreeBuilder<ItemLibraryItem, Void>> builderConsumer) {
        var recommendationBuilder = TreeBuilder.<ItemLibraryItem, Void>start(new ItemLibraryItem()
                .setDisplayName(Component.translatable("graph.library.recommendation")));
        builderConsumer.accept(recommendationBuilder);
        if (recommendationBuilder.isEmpty()) return;
        recommendationTree.setRoot(recommendationBuilder.build());
        recommendationTree.expandNode(recommendationTree.getRoot());
        recommendationTree.setDisplay(true);
    }

    public void setPortRecommendation(PortModel sourcePort) {
        if (this.graphModel == null) return;
        var testData = GraphNodeCreationData.ofOrphan(this.graphModel);
        setRecommendation(builder -> {
            if (nodeTree.getRoot() != null) {
                for (var node : nodeTree.getRoot().flatten()) {
                    var item = node.getKey();
                    if (item instanceof NodeModelLibraryItem nodeItem) {
                        if (nodeItem.createNode(testData) instanceof PortNodeModel portNodeModel) {
                            if (portNodeModel.getPortFitToConnectTo(sourcePort) != null) {
                                builder.leaf(nodeItem, null);
                            }
                        }
                    }
                }
            }
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
        if (this.selectedTree != null)
            this.selectedTree.setSelected(Collections.emptySet(), false);
        this.searchField.setText("", false);
        this.selectedTree = null;
        this.selectedItem = null;
        this.portModels = null;
        this.onFinished = null;
        this.recommendationTree.setRoot(null);
        this.recommendationTree.setDisplay(false);
        setDisplay(false);
        blur();
    }

    protected void onNodeDecided(ItemLibraryItem itemLibraryItem) {
        if (onFinished != null) {
            onFinished.accept(itemLibraryItem);
            onFinished = null;
        }
        hide();
    }
}
