package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TreeList represents a hierarchical UI element structure, where each node in the hierarchy can contain UI elements and may have a parent node.
 * This class is designed to display and interact with tree-structured data.
 */
@Accessors(chain = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TreeList<NODE extends ITreeNode<?, ?>> extends UIElement {
    @Accessors(chain = true, fluent = true)
    @Getter @Setter
    public static class TreeListStyle extends Style {
        private IGuiTexture nodeTexture = IGuiTexture.EMPTY;
        private IGuiTexture selectedTexture = ColorPattern.BLUE.rectTexture();
        private IGuiTexture collapseIcon = Icons.RIGHT_ARROW_NO_BAR_S_WHITE;
        private IGuiTexture expandIcon = Icons.DOWN_ARROW_NO_BAR_S_WHITE;

        public TreeListStyle(UIElement holder) {
            super(holder);
        }
    }

    @Getter
    private final TreeListStyle treeListStyle = new TreeListStyle(this);
    protected Function<NODE, UIElement> nodeUISupplier = iconTextTemplate(node -> IGuiTexture.EMPTY, Object::toString);
    protected BiConsumer<NODE, UIElement> onNodeUICreated = (node, ui) -> {};
    @Setter
    protected Consumer<Set<NODE>> onSelectedChanged = Consumers.nop();
    @Setter
    protected Consumer<NODE> onDoubleClickNode = Consumers.nop();
    @Setter
    protected boolean supportMultipleSelection = false;
    @Setter
    protected boolean staticTree = false;

    // runtime
    @Nullable
    @Getter
    protected NODE root;
    protected final BiMap<NODE, UIElement> nodeUIs = HashBiMap.create();
    protected final Set<NODE> selectedNodes = new HashSet<>();
    protected final Set<NODE> expandedNodes = new HashSet<>();
    protected final Map<NODE, List<NODE>> displayedChildren = new HashMap<>();

    public TreeList() {
        getLayout().setWidthPercent(100);
        getLayout().setGap(YogaGutter.ALL, 1);
    }

    public TreeList(NODE root) {
        this();
        setRoot(root);
    }

    public TreeList<NODE> menuStyle(Consumer<TreeListStyle> treeListStyle) {
        treeListStyle.accept(this.treeListStyle);
        onStyleChanged();
        return this;
    }

    public TreeList<NODE> setNodeUISupplier(Function<NODE, UIElement> nodeUISupplier) {
        this.nodeUISupplier = nodeUISupplier;
        reloadList();
        return this;
    }

    public TreeList<NODE> setOnNodeUICreated(BiConsumer<NODE, UIElement> onNodeUICreated) {
        this.onNodeUICreated = onNodeUICreated;
        reloadList();
        return this;
    }

    public TreeList<NODE> setRoot(@Nullable NODE root) {
        this.root = root;
        reloadList();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        treeListStyle.applyStyles(values);
    }

    public Set<NODE> getSelected() {
        return Collections.unmodifiableSet(selectedNodes);
    }

    /**
     * Determines if a given node in the tree is expanded.
     * A node is considered expanded if it is not present in the set of collapsed nodes.
     * @param node the {@code TreeNode} to check
     * @return {@code true} if the node is expanded, {@code false} otherwise
     */
    public boolean isNodeExpanded(NODE node) {
        return expandedNodes.contains(node);
    }

    /**
     * Checks if the specified node is currently selected in the tree.
     *
     * @param node the node to check
     * @return true if the node is selected, false otherwise
     */
    public boolean isNodeSelected(NODE node) {
        return selectedNodes.contains(node);
    }

    /**
     * Expands a given node in the tree by adding its child nodes to the UI representation
     * if the node is not already expanded and is not a leaf node. This method updates
     * the internal state of expanded nodes and manages the UI elements associated with the node.
     *
     * @param node the {@code TreeNode} to be expanded
     */
    public void expandNode(NODE node) {
        if (isNodeExpanded(node) || node.isLeaf()) return;
        expandedNodes.add(node);
        var nodeUI = nodeUIs.get(node);
        if (nodeUI != null) {
            var nodeIndex = getChildren().indexOf(nodeUI);
            if (nodeIndex >= 0) {
                var children = node.getChildren();
                for (int i = children.size() - 1; i >= 0; i--) {
                    var childNode = (NODE) children.get(i);
                    displayedChildren.computeIfAbsent(node, n -> new ArrayList<>()).addFirst(childNode);
                    addNodeUI(childNode, nodeIndex + 1);
                }
            }
        }
    }

    /**
     * Collapses a given node in the tree by removing its child nodes from the UI representation
     * if the node is expanded and not a leaf node. This method updates the internal state of
     * expanded nodes and manages the removal of the corresponding UI elements associated with
     * the node's children.
     *
     * @param node the {@code TreeNode} to be collapsed
     */
    public void collapseNode(NODE node) {
        if (!isNodeExpanded(node) || node.isLeaf()) return;
        for (var child : node.getChildren()) {
            removeNodeUI((NODE) child);
        }
        expandedNodes.remove(node);
        displayedChildren.remove(node);
    }

    /**
     * Reloads the list of nodes and rebuilds the UI elements.
     */
    public TreeList<NODE> reloadList() {
        nodeUIs.clear();
        selectedNodes.clear();
        expandedNodes.clear();
        displayedChildren.clear();
        clearAllChildren();
        if (root == null) return this;
        addNodeUI(root, 0);
        return this;
    }

    protected void addNodeUI(NODE node, int index) {
        var ui = createNodeUI(node);
        nodeUIs.put(node, ui);
        addChildAt(ui, index);
        if (node.isBranch() && isNodeExpanded(node)) {
            var children = node.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                var childNode = (NODE) children.get(i);
                displayedChildren.computeIfAbsent(node, n -> new ArrayList<>()).addFirst(childNode);
                addNodeUI(childNode, index + 1);
            }
        }
        if (!staticTree) {
            ui.addEventListener(UIEvents.TICK, e -> checkNodeChildrenValid(node));
        }
    }

    protected void checkNodeChildrenValid(NODE node) {
        if (isNodeExpanded(node)) {
            var currentChildren = (List<NODE>) node.getChildren();
            var displayedChildren = this.displayedChildren.getOrDefault(node, Collections.emptyList());
            if (currentChildren.equals(displayedChildren)) return;
            var removed = new ArrayList<>(displayedChildren);
            removed.removeAll(currentChildren);
            for (var displayed : new ArrayList<>(displayedChildren)) {
                removeNodeUI(displayed, removed.contains(displayed));
            }
            this.displayedChildren.remove(node);
            var index = getChildren().indexOf(nodeUIs.get(node));
            for (int i = currentChildren.size() - 1; i >= 0; i--) {
                var childNode = currentChildren.get(i);
                this.displayedChildren.computeIfAbsent(node, n -> new ArrayList<>()).addFirst(childNode);
                addNodeUI(childNode, index + 1);
            }
        }
    }

    protected void removeNodeUI(NODE node) {
        removeNodeUI(node, true);
    }

    protected void removeNodeUI(NODE node, boolean removeExpanded) {
        var ui = nodeUIs.remove(node);
        displayedChildren.remove(node);
        if (ui != null) {
            removeChild(ui);
        }
        if (node.isBranch() && isNodeExpanded(node)) {
            for (var child : node.getChildren()) {
                removeNodeUI((NODE) child, removeExpanded);
            }
            if (removeExpanded) {
                expandedNodes.remove(node);
                displayedChildren.remove(node);
            }
        }
    }

    /**
     * Creates a UI element representation for a given tree node.
     * The created UI element includes the node's UI, along with an arrow for expanding or collapsing the node.
     *
     * @param node the {@code TreeNode} for which the UI element is to be created
     * @return a {@code UIElement} representing the node's UI, including the expand/collapse arrow
     */
    public UIElement createNodeUI(NODE node) {
        var container = new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setWidthPercent(100);
            layout.setGap(YogaGutter.ALL, 2);
        }).style(style -> {
            style.backgroundTexture(DynamicTexture.of(() -> isNodeSelected(node) ? treeListStyle.selectedTexture : treeListStyle.nodeTexture));
        });
        var arrow = new UIElement().layout(layout -> {
            layout.setMargin(YogaEdge.LEFT, 5 * node.getDimension());
            layout.setWidth(7);
            layout.setHeight(7);
        }).style(style -> style.backgroundTexture(DynamicTexture.of(() -> node.isBranch() ?
                (isNodeExpanded(node) ? treeListStyle.expandIcon : treeListStyle.collapseIcon) :
                IGuiTexture.EMPTY
        ))).addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                if (node.isBranch()) {
                    if (isNodeExpanded(node)) {
                        collapseNode(node);
                    } else {
                        expandNode(node);
                    }
                }
            }
        });
        var ui = nodeUISupplier.apply(node);
        container.addChildren(arrow, ui);
        container.addEventListener(UIEvents.MOUSE_DOWN, e -> onNodeClicked(e, node));
        container.addEventListener(UIEvents.DOUBLE_CLICK, e -> onNodeDoubleClicked(e, node));
        onNodeUICreated.accept(node, ui);
        return container;
    }

    protected void onNodeClicked(UIEvent event, NODE node) {
        if (event.button == 0) {
            if (!supportMultipleSelection || !event.isCtrlDown()) {
                selectedNodes.clear();
            }
            selectedNodes.add(node);
            onSelectedChanged.accept(getSelected());
        }
    }

    protected void onNodeDoubleClicked(UIEvent event, NODE node) {
        if (event.button == 0) {
            if (node.isBranch()) {
                if (isNodeExpanded(node)) {
                    collapseNode(node);
                } else {
                    expandNode(node);
                }
            }
            onDoubleClickNode.accept(node);
        }
    }

    /// Template
    public static <NODE extends ITreeNode<?, ?>> Function<NODE, UIElement> iconTextTemplate(
            Function<NODE, IGuiTexture> iconMapper,
            Function<NODE, String> textMapper) {
        return node -> {
            var container = new UIElement().layout(layout -> {
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setGap(YogaGutter.ALL, 2);
                layout.setHeight(10);
                layout.setFlex(1);
            }).addChildren();
            var icon = new UIElement().layout(layout -> {
                layout.setAspectRatio(1);
                layout.setHeightPercent(100);
            }).style(style -> style.backgroundTexture(iconMapper.apply(node)));
            var label = new TextElement()
                    .textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                    .setText(textMapper.apply(node)).layout(layout -> {
                        layout.setHeightPercent(100);
                        layout.setFlex(1);
                    }).setOverflow(YogaOverflow.HIDDEN);
            return container.addChildren(icon, label);
        };
    }
}
