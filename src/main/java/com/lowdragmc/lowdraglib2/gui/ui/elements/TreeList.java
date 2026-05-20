package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.google.common.base.Predicates;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.*;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * TreeList represents a hierarchical UI element structure, where each node in the hierarchy can contain UI elements and may have a parent node.
 * This class is designed to display and interact with tree-structured data.
 */
@Accessors(chain = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "tree-list", group = "misc", registry = "ldlib2:ui_element")
public class TreeList<NODE extends ITreeNode<?, ?>> extends UIElement {
    @Getter @Setter
    @Configurable(name = "TreeListStyle")
    public class TreeListStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.NODE_BACKGROUND,
                PropertyRegistry.NODE_HOVER_BACKGROUND,
                PropertyRegistry.COLLAPSE_ICON,
                PropertyRegistry.EXPAND_ICON,
        };

        public TreeListStyle() {
            super(TreeList.this);
            setDefault(PropertyRegistry.NODE_HOVER_BACKGROUND, ColorPattern.BLUE.rectTexture());
            setDefault(PropertyRegistry.COLLAPSE_ICON, Icons.RIGHT_ARROW_NO_BAR_S_WHITE);
            setDefault(PropertyRegistry.EXPAND_ICON, Icons.DOWN_ARROW_NO_BAR_S_WHITE);
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture nodeTexture() {
            return getValueSave(PropertyRegistry.NODE_BACKGROUND);
        }

        public TreeListStyle nodeTexture(IGuiTexture texture) {
            set(PropertyRegistry.NODE_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture hoverTexture() {
            return getValueSave(PropertyRegistry.NODE_HOVER_BACKGROUND);
        }

        public TreeListStyle hoverTexture(IGuiTexture texture) {
            set(PropertyRegistry.NODE_HOVER_BACKGROUND, texture);
            return this;
        }

        public IGuiTexture expandIcon() {
            return getValueSave(PropertyRegistry.EXPAND_ICON);
        }

        public TreeListStyle expandIcon(IGuiTexture texture) {
            set(PropertyRegistry.EXPAND_ICON, texture);
            return this;
        }

        public IGuiTexture collapseIcon() {
            return getValueSave(PropertyRegistry.COLLAPSE_ICON);
        }

        public TreeListStyle collapseIcon(IGuiTexture texture) {
            set(PropertyRegistry.COLLAPSE_ICON, texture);
            return this;
        }
    }

    @Getter
    private final TreeListStyle treeListStyle = new TreeListStyle();
    protected UIElementProvider<NODE> nodeUISupplier = textTemplate(value -> Component.translatable(value.toString()));
    protected BiConsumer<NODE, UIElement> onNodeUICreated = (node, ui) -> {};
    @Setter
    protected Consumer<Set<NODE>> onSelectedChanged = Consumers.nop();
    @Setter
    protected Consumer<NODE> onDoubleClickNode = Consumers.nop();
    @Setter
    protected Predicate<NODE> selectableNodeFilter = Predicates.alwaysTrue();
    @Setter
    protected boolean doubleClickToExpand = true;
    @Setter
    protected boolean clickToExpand = false;
    @Setter
    protected boolean supportMultipleSelection = false;
    @Setter
    protected boolean staticTree = false;
    protected boolean flattenRoot = false;
    /**
     * When true, the TreeList and each row size to their content's max width instead of stretching
     * to the parent. Useful when embedded in a {@link ScrollerView} with horizontal scrolling so
     * that deeply-indented rows or long node labels can push the scroll container wider.
     * <p>Default {@code false} preserves the previous full-width behavior.
     */
    @Getter
    protected boolean widthFitsContent = false;

    // runtime
    @Nullable
    @Getter
    protected NODE root;
    @Getter
    protected final BiMap<NODE, UIElement> nodeUIs = HashBiMap.create();
    protected final Set<NODE> selectedNodes = new LinkedHashSet<>();
    @Getter @Nullable
    protected NODE hoveredNode = null;
    @Getter
    protected final Set<NODE> expandedNodes = new HashSet<>();
    protected final Map<NODE, List<NODE>> displayedChildren = new HashMap<>();

    public TreeList() {
        getLayout().widthPercent(100);
        getLayout().gapAll(1);
        internalSetup();
    }

    public TreeList(NODE root) {
        this();
        setRoot(root);
    }

    public TreeList(NODE root, boolean staticTree) {
        this();
        setStaticTree(staticTree);
        setRoot(root);
    }

    public TreeList<NODE> menuStyle(Consumer<TreeListStyle> treeListStyle) {
        treeListStyle.accept(this.treeListStyle);
        return this;
    }

    public TreeList<NODE> setNodeUISupplier(UIElementProvider<NODE> nodeUISupplier) {
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

    public TreeList<NODE> setFlattenRoot(boolean flattenRoot) {
        this.flattenRoot = flattenRoot;
        reloadList();
        return this;
    }

    /**
     * Toggle whether the TreeList sizes to its content (and each row sizes to its own content)
     * instead of stretching to the parent's width. See {@link #widthFitsContent} for details.
     * <p>Callers' node UIs should also let their text label use {@code adaptiveWidth(true)} on
     * its text style so the label reports its natural width to the layout engine.
     */
    public TreeList<NODE> setWidthFitsContent(boolean v) {
        this.widthFitsContent = v;
        if (v) {
            // Auto width + min-width 100% of parent: TreeList fills the parent when content is
            // narrow, but grows beyond it when the widest row is wider (horizontal scroll case).
            // alignSelf FLEX_START prevents the parent's stretch from short-circuiting auto-width;
            // alignItems FLEX_START prevents row children from auto-stretching to TreeList width
            // (rows manage their own minWidthPercent(100) — see createNodeUI).
            getLayout().widthAuto();
            getLayout().minWidthPercent(100);
            getLayout().alignSelf(AlignItems.FLEX_START);
            getLayout().alignItems(AlignItems.FLEX_START);
        } else {
            getLayout().widthPercent(100);
        }
        reloadList();
        return this;
    }

    public Set<NODE> getSelected() {
        return Collections.unmodifiableSet(selectedNodes);
    }

    public TreeList<NODE> setSelected(Collection<NODE> selected, boolean notify) {
        if (selectedNodes.equals(selected)) return this;
        selectedNodes.clear();
        selectedNodes.addAll(selected);
        if (notify) {
            onSelectedChanged.accept(selectedNodes);
        }
        return this;
    }

    public TreeList<NODE> addSelected(NODE node, boolean notify) {
        if (selectedNodes.add(node)) {
            if (notify) {
                onSelectedChanged.accept(getSelected());
            }
        }
        return this;
    }

    public TreeList<NODE> removeSelected(NODE node, boolean notify) {
        if (selectedNodes.remove(node)) {
            if (notify) {
                onSelectedChanged.accept(getSelected());
            }
        }
        return this;
    }

    public void expandNodeAlongPath(@Nullable NODE node) {
        if (node == null) return;
        var stack = new Stack<NODE>();
        var parent = node.getParent();
        while (parent != null) {
            stack.push((NODE) parent);
            parent = parent.getParent();
        }
        while (!stack.isEmpty()) {
            expandNode(stack.pop());
        }
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
     * Expands or collapses all nodes in the tree starting from the specified root node,
     * based on the evaluation of a provided predicate function. Nodes for which the
     * predicate evaluates to {@code true} are expanded, while others are collapsed.
     *
     * This method recursively processes each node and its children, ensuring that
     * the expand or collapse operation is applied consistently throughout the subtree.
     * It utilizes {@link #expandNode(NODE)} to expand nodes and {@link #collapseNode(NODE)}
     * to collapse nodes.
     *
     * @param root the starting node of the tree or subtree to process
     * @param predicate a {@code Predicate} applied to each node to determine if it
     *        should be expanded ({@code true}) or collapsed ({@code false})
     */
    public void expandAllNodesIf(NODE root, Predicate<NODE> predicate) {
        if (predicate.test(root)) {
            expandNode(root);
            for (var child : root.getChildren()) {
                expandAllNodesIf((NODE) child, predicate);
            }
        } else {
            collapseNode(root);
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
        var selected = getSelected();
        for (var child : node.getChildren()) {
            removeNodeUI((NODE) child);
        }
        if (!selected.equals(getSelected())) {
            onSelectedChanged.accept(getSelected());
        }
        expandedNodes.remove(node);
        displayedChildren.remove(node);
    }

    /**
     * Reloads the list of nodes and rebuilds the UI elements.
     */
    public TreeList<NODE> reloadList() {
        nodeUIs.clear();
        setSelected(Collections.emptySet(), true);
        expandedNodes.clear();
        displayedChildren.clear();
        clearAllChildren();
        if (root == null) return this;
        if (flattenRoot) {
            var index = 0;
            for (var child : root.getChildren()) {
                addNodeUI((NODE) child, index++);
            }
        } else {
            addNodeUI(root, 0);
        }
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
        if (isNodeExpanded(node) && nodeUIs.containsKey(node)) {
            var currentChildren = (List<NODE>) node.getChildren();
            var displayedChildren = this.displayedChildren.getOrDefault(node, Collections.emptyList());
            if (currentChildren.equals(displayedChildren)) return;
            var removed = new ArrayList<>(displayedChildren);
            removed.removeAll(currentChildren);
            var selected = getSelected();
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
            if (!selected.equals(getSelected())) {
                onSelectedChanged.accept(getSelected());
            }
        }
    }

    protected void removeNodeUI(NODE node) {
        removeNodeUI(node, true);
    }

    protected void removeNodeUI(NODE node, boolean removeExpanded) {
        var ui = nodeUIs.remove(node);
        removeSelected(node, true);
        if (ui != null) {
            removeChild(ui);
        }
        if (displayedChildren.containsKey(node)) {
            var children = displayedChildren.get(node);
            for (var child : children) {
                removeNodeUI(child, removeExpanded);
            }
        }
        if (removeExpanded) {
            expandedNodes.remove(node);
        }
        displayedChildren.remove(node);
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
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            if (widthFitsContent) {
                // Row sizes to its own content, but at least the full TreeList width — which
                // itself auto-sizes to the widest row's content. Net effect: every row ends up
                // as wide as the widest row, so the selection/hover background spans full width
                // and horizontal scrolling exposes the whole tree.
                layout.minWidthPercent(100);
            } else {
                layout.widthPercent(100);
            }
            layout.gapAll(2);
        }).style(style -> {
            style.backgroundTexture(DynamicTexture.of(() -> isNodeSelected(node) ? treeListStyle.hoverTexture() : treeListStyle.nodeTexture()));
        });
        var arrow = new UIElement().layout(layout -> {
            layout.marginLeft(5 * (node.getDimension() - (flattenRoot ? 1 : 0)));
            layout.width(7);
            layout.height(7);
        }).style(style -> style.backgroundTexture(DynamicTexture.of(() -> node.isBranch() ?
                (isNodeExpanded(node) ? treeListStyle.expandIcon() : treeListStyle.collapseIcon()) :
                IGuiTexture.EMPTY
        ))).addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                if (node.isBranch() && !clickToExpand) {
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
        container.addEventListener(UIEvents.CLICK, e -> onNodeClicked(e, node));
        container.addEventListener(UIEvents.DOUBLE_CLICK, e -> onNodeDoubleClicked(e, node));
        container.addEventListener(UIEvents.MOUSE_ENTER, e -> hoveredNode = node, true);
        container.addEventListener(UIEvents.MOUSE_LEAVE, e -> hoveredNode = null, true);
        onNodeUICreated.accept(node, container);
        return container;
    }

    protected void onNodeClicked(UIEvent event, NODE node) {
        if (event.button == 0) {
            if (node.isBranch() && clickToExpand) {
                if (isNodeExpanded(node)) {
                    collapseNode(node);
                } else {
                    expandNode(node);
                }
            }
            if (!selectableNodeFilter.test(node)) return;
            // shift
            if (supportMultipleSelection && event.isShiftDown()) {
                if (!selectedNodes.isEmpty()) {
                    var first = selectedNodes.iterator().next();
                    if (node.getDimension() == first.getDimension() && first.getParent() != null) {
                        selectedNodes.removeIf(n -> n != first);
                        var allSibling = first.getParent().getChildren();
                        var currentIndex = first.getSiblingIndex();
                        var targetIndex = node.getSiblingIndex();
                        for (int i = Math.min(currentIndex, targetIndex); i <= Math.max(currentIndex, targetIndex); i++) {
                            if (i < 0 || i >= allSibling.size() || allSibling.get(i) == node) continue;
                            var sibling = (NODE) allSibling.get(i);
                            selectedNodes.add(sibling);
                        }
                        onSelectedChanged.accept(getSelected());
                        return;
                    }
                }
            }
            // ctrl
            if (!supportMultipleSelection || !event.isCtrlDown()) {
                selectedNodes.clear();
            }
            if (selectedNodes.contains(node)) {
                selectedNodes.remove(node);
            } else {
                selectedNodes.add(node);
            }
            onSelectedChanged.accept(getSelected());
        }
    }

    protected void onNodeDoubleClicked(UIEvent event, NODE node) {
        if (event.button == 0) {
            if (node.isBranch() && doubleClickToExpand) {
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
    public static <NODE extends ITreeNode<?, ?>> UIElementProvider<NODE> iconTextTemplate(
            Function<NODE, IGuiTexture> iconMapper,
            Function<NODE, Component> textMapper) {
        var provider = UIElementProvider.iconText(iconMapper, textMapper);
        return node -> provider.apply(node).layout(layout -> layout.flex(1));
    }

    public static <NODE extends ITreeNode<?, ?>> UIElementProvider<NODE> optionalIconTextTemplate(
            Function<NODE, IGuiTexture> iconMapper,
            Function<NODE, Component> textMapper) {
        var provider = UIElementProvider.optionalIconText(iconMapper, textMapper);
        return node -> provider.apply(node).layout(layout -> layout.flex(1));
    }

    public static <NODE extends ITreeNode<?, ?>> UIElementProvider<NODE> textTemplate(
            Function<NODE, Component> textMapper) {
        var provider = UIElementProvider.text(textMapper);
        return node -> provider.apply(node).layout(layout -> layout.flex(1));
    }

    public static boolean isMouseOverNodeAbove(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return ui.isMouseOver(x, y, width, height / 3, event.x, event.y);
    }

    public static boolean isMouseOverNodeCenter(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return ui.isMouseOver(x, y + height / 3, width, height / 3, event.x, event.y);
    }

    public static boolean isMouseOverNodeBelow(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return ui.isMouseOver(x, y + height * 2 / 3, width, height / 3, event.x, event.y);
    }

    @OnlyIn(Dist.CLIENT)
    public static IGuiTexture createDraggingOverlay(int mode) {
        if (mode == 0) {
            return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
                DrawerHelper.drawSolidRect(graphics, x, y - 1, width, 1, ColorPattern.T_WHITE.color);
            };
        } else if (mode == 1) {
            return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
                DrawerHelper.drawSolidRect(graphics, x, y, width, height, ColorPattern.T_WHITE.color);
            };
        } else if (mode == 2) {
            return (graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
                DrawerHelper.drawSolidRect(graphics, x, y + height, width, 1, ColorPattern.T_WHITE.color);
            };
        }
        return IGuiTexture.EMPTY;
    }
}
