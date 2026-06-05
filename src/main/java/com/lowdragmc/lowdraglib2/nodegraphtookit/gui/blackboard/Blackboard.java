package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.IGraphTool;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.VariableDeclarationCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.NodeCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHintList;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.GroupModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.IGroupItemModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.group.SectionModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableCreationInfos;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;
import java.util.stream.Collectors;

public class Blackboard extends BlackboardElement implements IGraphTool {
    public record DraggingUINode(GroupItemTreeNode node) {}

    public final GraphView graphView;
    public final ScrollerView scrollerView = new ScrollerView();
    public final TreeList<GroupItemTreeNode> treeList = new TreeList<>();

    // runtime
    @Getter @Setter
    @Nullable
    private VariableCreationInfos lastVariableInfos;
    @Nullable
    @Getter
    private GroupItemTreeNode rootNode;
    private long lastClickTime = 0;
    @Getter
    private final Map<IGroupItemModel, UIElement> itemUIs = new HashMap<>();

    public Blackboard(GraphView graphView) {
        this.graphView = graphView;
        addClass("__blackboard__");
        Style.defaultPipeline(getLayout(), l -> l.widthPercent(100).heightPercent(100));

        scrollerView.addClass("__blackboard_scroller__");
        Style.defaultPipeline(scrollerView.viewPort.getLayout(), l -> l.paddingAll(0));
        scrollerView.viewPort.getStyle().background(IGuiTexture.EMPTY);
        Style.defaultPipeline(scrollerView.getLayout(), l -> l.widthPercent(100).heightPercent(100));

        treeList.setStaticTree(true);
        treeList.setFlattenRoot(true);
        treeList.setNodeUISupplier(n -> onItemUICreate(n.getKey()));
        treeList.setSelectableNodeFilter(Predicates.alwaysFalse());
        treeList.setOnNodeUICreated(this::onItemNodeCreated);
        scrollerView.addScrollViewChild(treeList);

        addChildren(scrollerView);
        this.addEventListener(UIEvents.MOUSE_UP, this::onBlackboardMouseUp);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("graph.blackboard");
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        updateFromModel();
    }

    public void clear() {
        treeList.setRoot(null);
        itemUIs.clear();
    }

    /**
     * Updates the treeview based on the model.
     */
    protected void updateFromModel() {
        if (graphView.getGraph() == null) return;
        var graphModel = graphView.getGraph().graphModel;
        var defaultSection = graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME);
        if (defaultSection == null) {
            rootNode = null;
        } else {
            rootNode = new GroupItemTreeNode(null, defaultSection);
        }
        treeList.setRoot(rootNode);
        updateItemUIs();
    }

    protected void updateItemUIs() {
        itemUIs.clear();
        for (var entry : treeList.getNodeUIs().entrySet()) {
            var node = entry.getKey();
            if (node != null) {
                var ui = entry.getValue();
                if (ui != null) {
                    // ui is container, we need its child
                    for (UIElement child : ui.getChildren()) {
                        if (child instanceof ModelElement modelElement) {
                            itemUIs.put(node.getKey(), modelElement);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateModelChanges(Map<UUID, ChangeHintList> changeset) {
        if (graphView.getGraph() == null) return;
        var graphModel = graphView.getGraph().graphModel;
        var shouldRebuildTreeView = false;

        // Adding/removing a variable/group should mark the parent group as changed.
        for (var entry : changeset.entrySet()) {
            var hints = entry.getValue();
            var model = graphModel.getModel(entry.getKey());
            if (hints == ChangeHintList.GROUPING) {
                // When there is a structural change (added, removed, reordered items), Rebuild should be called.
                shouldRebuildTreeView = true;
                break;
            } else if (model instanceof IGroupItemModel && hints == ChangeHintList.UNSPECIFIED) {
                shouldRebuildTreeView = true;
                break;
            }
        }

        if (shouldRebuildTreeView || changeset.containsKey(
                Objects.requireNonNull(graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME)).getUid()
        )) {
            var expandedNodes = treeList.getExpandedNodes().stream().map(ITreeNode::getKey).toList();
            treeList.reloadList();
            if (rootNode != null) {
                treeList.expandAllNodesIf(rootNode, n -> expandedNodes.contains(n.getKey()));
            }
            // update selection
            onSelectionChanged();
            updateItemUIs();
        }
    }

    protected UIElement onItemUICreate(IGroupItemModel itemModel) {
        UIElement element;
        // reuse first
        if (itemUIs.containsKey(itemModel)) {
            element = itemUIs.get(itemModel);
            element.removeSelf();
        } else {
            // create new
            if (itemModel instanceof GroupModelBase groupModel) {
                element = new BlackboardGroup(groupModel);
            } else if (itemModel instanceof VariableDeclarationModelBase variableModel) {
                element = new BlackboardVariableProperty(variableModel);
            } else {
                element = new UIElement();
            }
        }

        if (element instanceof BlackboardElement blackboardElement) {
            blackboardElement.setBlackboard(this);
        }
        if (element instanceof ModelElement modelElement) {
            modelElement.setGraphView(graphView);
            modelElement.doCompleteUpdate();
        }
        return element;
    }

    protected void onItemNodeCreated(GroupItemTreeNode node, UIElement nodeUI) {
        nodeUI.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                graphView.batchSelection(() -> {
                    if (node.getKey() instanceof Model model) {
                        if (!graphView.isSelected(model)) {
                            if (!graphView.getSelected().isEmpty()) {
                                graphView.clearAllSelected();
                            }
                            graphView.addSelected(model);
                        }
                    }
                });
                if (e.bubbleListeners.size() == 1 && e.captureListeners.isEmpty()) {
                    lastClickTime = System.currentTimeMillis();
                } else {
                    lastClickTime = 0;
                }
            }
        });
        nodeUI.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
            if (lastClickTime != 0 && isMouseDown(0) && treeList.getSelected().size() == 1
                    && node != rootNode) {
                nodeUI.startDrag(new DraggingUINode(node), new TextTexture(node.getKey().getName()));
            }
            lastClickTime = 0;
        }, true);
        nodeUI.addEventListener(UIEvents.MOUSE_UP, e -> {
            lastClickTime = 0;
        });
        nodeUI.addEventListener(UIEvents.DRAG_ENTER, e -> {
            if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                var mode = TreeList.isMouseOverNodeAbove(e) ? 0 : TreeList.isMouseOverNodeCenter(e) ? 1 : TreeList.isMouseOverNodeBelow(e) ? 2 : -1;
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(TreeList.createDraggingOverlay(mode)));
            }
        }, true);
        nodeUI.addEventListener(UIEvents.DRAG_LEAVE, e -> {
            Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
        }, true);
        nodeUI.addEventListener(UIEvents.DRAG_END, e -> {
            Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
            if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && graphView != null && graphView.graphView.isSelfOrChildHover()) {
                // drag into graph view
                if (dragged.getKey() instanceof VariableDeclarationModelBase variableModel) {
                    onDragVariablesIntoGraph(e, List.of(variableModel));
                }
            }
        });
        nodeUI.addEventListener(UIEvents.DRAG_UPDATE, e -> {
            if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                var mode = TreeList.isMouseOverNodeAbove(e) ? 0 : TreeList.isMouseOverNodeCenter(e) ? 1 : TreeList.isMouseOverNodeBelow(e) ? 2 : -1;
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(TreeList.createDraggingOverlay(mode)));
            } else {
                Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
            }
        });
        nodeUI.addEventListener(UIEvents.DRAG_PERFORM, e -> {
            Style.importantPipeline(e.currentElement.getStyle(), s -> s.overlayTexture(IGuiTexture.EMPTY));
            if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                performGroupItemDrop(dragged, node, e);
            }
        });
    }

    /**
     * Reorders or re-parents a Blackboard item in response to a successful drag-and-drop.
     * The hover region within the target row decides the semantics:
     * <ul>
     *   <li>top third &rarr; insert <em>before</em> target, into target's parent group</li>
     *   <li>middle third &rarr; insert <em>into</em> target (only when target is a group)</li>
     *   <li>bottom third &rarr; insert <em>after</em> target, into target's parent group</li>
     * </ul>
     * Routed through {@link VariableDeclarationCommands.MoveGroupItemCommand} so the snapshot-based
     * undo/redo system records the change.
     */
    protected void performGroupItemDrop(GroupItemTreeNode dragged, GroupItemTreeNode target, UIEvent e) {
        if (dragged == target) return;
        var draggedItem = dragged.getKey();
        var targetItem = target.getKey();

        var mode = TreeList.isMouseOverNodeAbove(e) ? 0
                : TreeList.isMouseOverNodeCenter(e) ? 1
                : TreeList.isMouseOverNodeBelow(e) ? 2 : -1;
        if (mode < 0) return;

        GroupModelBase targetGroup;
        int insertIdx;
        if (mode == 1) {
            // Drop INTO target — only meaningful when target is a group.
            if (!(targetItem instanceof GroupModelBase tg)) return;
            // Don't drop a group into itself or one of its descendants — that would create a cycle.
            if (isAncestorOrSelf(draggedItem, tg)) return;
            targetGroup = tg;
            insertIdx = tg.getItems().size();
        } else {
            // Drop above / below — sibling-level insertion in the target's parent group.
            var parent = target.getParent();
            if (parent == null || !(parent.getKey() instanceof GroupModelBase parentGroup)) return;
            int targetIdx = parentGroup.getItems().indexOf(targetItem);
            if (targetIdx < 0) return;
            insertIdx = mode == 0 ? targetIdx : targetIdx + 1;
            // Same-parent reorder going forward: removal shifts the list — compensate so the
            // user-visible drop position matches.
            if (draggedItem.getParentGroup() == parentGroup) {
                int fromIdx = parentGroup.getItems().indexOf(draggedItem);
                if (fromIdx >= 0 && fromIdx < insertIdx) insertIdx -= 1;
            }
            targetGroup = parentGroup;
        }

        // Final cycle-guard for the above/below path: don't insert a group into its own subtree.
        if (isAncestorOrSelf(draggedItem, targetGroup)) return;

        graphView.dispatchCommand(new VariableDeclarationCommands.MoveGroupItemCommand(
                draggedItem, targetGroup, insertIdx));
    }

    /** True when {@code candidate} is {@code target} itself or any ancestor of it. */
    private static boolean isAncestorOrSelf(IGroupItemModel candidate, GroupModelBase target) {
        if (!(candidate instanceof GroupModelBase candidateGroup)) return false;
        GroupModelBase current = target;
        while (current != null) {
            if (current == candidateGroup) return true;
            current = current.getParentGroup();
        }
        return false;
    }

    @Override
    protected void onSelectionChanged() {
        if (rootNode == null) return;
        var selected = graphView.getSelected();
        var itemModels = selected.stream().filter(IGroupItemModel.class::isInstance).map(IGroupItemModel.class::cast)
                .collect(Collectors.toSet());
        treeList.setSelected(rootNode.flatten().stream()
                .filter(n -> itemModels.contains(n.getKey()))
                .filter(GroupItemTreeNode.class::isInstance)
                .map(GroupItemTreeNode.class::cast).toList(), false);
    }

    protected void onDragVariablesIntoGraph(UIEvent e, List<VariableDeclarationModelBase> variables) {
        var variablesWithInfo = new ArrayList<Pair<VariableDeclarationModelBase, Vector2f>>();
        for (int i = 0; i < variables.size(); i++) {
            variablesWithInfo.add(Pair.of(
                    variables.get(i),
                    graphView.getContentViewContainer().worldToLocalLayoutOffset(new Vector2f(e.x, e.y).add(0, i * 30))
            ));
        }

        var command = new NodeCommands.CreateNodeCommand();
        var portTarget = e.target.getFirstAncestorOfType(PortElement.class);
        var variablesCount = variablesWithInfo.size();
        for (var info : variablesWithInfo) {
            var model = info.left();
            var position = info.right();
            if (portTarget != null && variablesCount == 1 && portTarget.canAcceptDrop(model)) {
                command.withNodeOnPort(model, portTarget.getModel(), position, null);
            } else {
                command.withNodeOnGraph(model, position, null);
            }
        }

        graphView.dispatchCommand(command);
    }

    protected void onBlackboardMouseUp(UIEvent event) {
        var mui = getModularUI();
        if (event.button == 1 && mui != null) {
            // check if movement is smaller than 1 pixel
            if (new Vector2f(event.x, event.y).sub(mui.getLastMouseDownX(), mui.getLastMouseDownY()).lengthSquared() < 1f) {
                var menu = createMenu();
                if (menu.isEmpty()) return;
                var layoutOffset = mui.ui.rootElement.worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
                var contextMenu = new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                        .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                        .setOnNodeClicked(TreeBuilder.Menu::handle);
                contextMenu.addClass("__blackboard_context-menu__");
                Style.importantPipeline(contextMenu.getLayout(), l -> l.left(layoutOffset.x).top(layoutOffset.y));
                mui.ui.rootElement.addChild(contextMenu);
            }
        }
    }

    protected TreeBuilder.Menu createMenu() {
        var menu = TreeBuilder.Menu.start();
        menu.leaf("graph.commands.create_variable", this::createVariable);
        if (!getSelectedItems().isEmpty()) {
            menu.leaf("graph.commands.delete", graphView::deleteSelectedElements);
        }
        return menu;
    }

    /**
     * Create a new variable with the previous or default values for scope, type and modifiers.
     */
    public void createVariable() {
        var graph = graphView.getGraph();
        if (graph == null) return;

        var selectedGroupInThisSection = getTargetGroupForNewVariable(graph.graphModel.getSectionModel(GraphModel.DEFAULT_SECTION_NAME));

        if (lastVariableInfos == null) {
            lastVariableInfos = new VariableCreationInfos();
        }

        var typeHandle = lastVariableInfos.getTypeHandle();

        var supportedTypes = graph.graphModel.getVariableSupportTypes();
        if (!supportedTypes.isEmpty()
                && !typeHandle.isCustomTypeHandle()
                && !supportedTypes.contains(typeHandle)) {
            typeHandle = supportedTypes.getFirst();
        }

        graphView.dispatchCommand(new VariableDeclarationCommands.CreateGraphVariableDeclarationCommand(
                "New Variable",
                lastVariableInfos.getScope(),
                lastVariableInfos.getVariableType() == null ? graph.graphModel.getVariableDeclarationModelType() : lastVariableInfos.getVariableType(),
                typeHandle,
                selectedGroupInThisSection,
                Integer.MAX_VALUE,
                lastVariableInfos.getModifiers(),
                null
        ));
    }

    public void setGroupModelExpanded(GroupModelBase current, boolean expanded) {
        var node = findNode(treeList.getRoot(), current);
        if (node != null) {
            if (expanded) {
                treeList.expandNode(node);
            } else {
                treeList.collapseNode(node);
            }
        }
    }

    protected @Nullable GroupItemTreeNode findNode(GroupItemTreeNode node, IGroupItemModel element) {
        if (node == null) return null;
        if (node.getKey() == element) return node;
        for (var child : node.getChildren()) {
            var result = findNode(child, element);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * Searches for the correct group that would be used to contain a new variable in the selection.
     * @param section the section to search in.
     * @return Either a group in the section or the section itself.
     */
    public GroupModel getTargetGroupForNewVariable(SectionModel section) {
        for (var selected : getSelectedItems()) {
            if (selected instanceof GroupModel groupModel && groupModel.getSection() == section) {
                return groupModel;
            }
        }
        return section;
    }

    public List<IGroupItemModel> getSelectedItems() {
        return treeList.getSelected().stream().map(GroupItemTreeNode::getKey).toList();
    }
}
