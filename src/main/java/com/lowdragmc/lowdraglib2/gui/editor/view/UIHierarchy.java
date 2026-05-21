package com.lowdragmc.lowdraglib2.gui.editor.view;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.Consumer;

public class UIHierarchy extends UIElement {
    public record DraggingUINode(UITreeNode draggedNode) {}
    public record NodeCopy(List<CompoundTag> copiedNodes) {}

    public final ScrollerView scrollerView = new ScrollerView();
    public final TreeList<UITreeNode> treeList = new TreeList<>();

    // runtime
    @Setter
    protected Consumer<Set<UITreeNode>> onSelectedChanged = Consumers.nop();

    @Getter @Nullable
    private UI ui;
    private long lastClickTime = 0;
    @Getter @Nullable
    private UITreeNode rootNode;

    public UIHierarchy() {
        this.getLayout().widthPercent(100.0F);
        this.getLayout().heightPercent(100.0F);

        this.scrollerView.layout((layout) -> {
            layout.widthPercent(100.0F);
            layout.heightPercent(100.0F);
        });
        this.addChild(this.scrollerView);
        scrollerView.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
        scrollerView.addScrollViewChild(treeList
                .setSupportMultipleSelection(true)
                .setWidthFitsContent(true)
                .setNodeUISupplier((node) -> {
                    UIElement container = (new UIElement()).layout((layout) -> {
                        layout.flexDirection(FlexDirection.ROW);
                        layout.gapAll(2.0F);
                        layout.height(10.0F);
                    }).addChildren();
                    UIElement icon = (new UIElement()).layout((layout) -> {
                        layout.setAspectRatio(1.0F);
                        layout.heightPercent(100.0F);
                    }).style((style) -> style.backgroundTexture(node.getKey().getEditorIcon()));
                    TextElement label = new TextElement();
                    label.textStyle((style) -> {
                        style.adaptiveWidth(true);
                        style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER);
                        style.textColor(node.getKey().isInternalUI() ? ColorPattern.LIGHT_GRAY.color : ColorPattern.WHITE.color);
                    }).setText(node.getKey().getEditorName()).layout((layout) -> {
                        layout.heightPercent(100.0F);
                    }).addEventListener(UIEvents.TICK, e -> {
                        label.setText(node.getKey().getEditorName());
                    });
                    return container.addChildren(icon, label);
                })
                .setOnNodeUICreated((node, nodeUI) -> {
                    nodeUI.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                        if (e.button == 0) {
                            lastClickTime = System.currentTimeMillis();
                        }
                    });
                    nodeUI.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                        if (lastClickTime != 0 && isMouseDown(0) && treeList.getSelected().size() == 1 && node != rootNode && !node.getKey().isInternalUI()) {
                            nodeUI.startDrag(new DraggingUINode(node), new TextTexture(node.getKey().getEditorName().getString()));
                        }
                        lastClickTime = 0;
                    }, true);
                    nodeUI.addEventListener(UIEvents.MOUSE_UP, e -> {
                        lastClickTime = 0;
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_ENTER, e -> {
                        if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                            var mode = TreeList.isMouseOverNodeAbove(e) ? 0 : TreeList.isMouseOverNodeCenter(e) ? 1 : TreeList.isMouseOverNodeBelow(e) ? 2 : -1;
                            e.currentElement.style(style -> style.overlayTexture(TreeList.createDraggingOverlay(mode)));
                        }
                    }, true);
                    nodeUI.addEventListener(UIEvents.DRAG_LEAVE, e -> {
                        e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                    }, true);
                    nodeUI.addEventListener(UIEvents.DRAG_END, e -> {
                        e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_UPDATE, e -> {
                        if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                            var mode = TreeList.isMouseOverNodeAbove(e) ? 0 : TreeList.isMouseOverNodeCenter(e) ? 1 : TreeList.isMouseOverNodeBelow(e) ? 2 : -1;
                            e.currentElement.style(style -> style.overlayTexture(TreeList.createDraggingOverlay(mode)));
                        } else {
                            e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                        }
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_PERFORM, e -> {
                        e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                        if (e.dragHandler.getDraggingObject() instanceof DraggingUINode(var dragged) && dragged != node) {
                            var target = node.getKey();
                            var toMoved = dragged.getKey();
                            if (toMoved.isAncestorOf(target)) return;
                            if (TreeList.isMouseOverNodeAbove(e)) {
                                // sibling
                                var originalParent = toMoved.getParent();
                                var originalSiblingIndex = toMoved.getSiblingIndex();
                                var newParent = target.getParent();
                                var newSiblingIndex = target.getSiblingIndex();
                                if (newParent == null) return;
                                if (originalParent == newParent) {
                                    if (originalSiblingIndex < newSiblingIndex) {
                                        newSiblingIndex--;
                                    }
                                    toMoved.removeSelf();
                                }
                                newParent.addEditorChild(toMoved, newSiblingIndex);
                            } else if (TreeList.isMouseOverNodeCenter(e)) {
                                // children
                                var originalParent = toMoved.getParent();
                                if (originalParent != target) {
                                    target.addEditorChild(toMoved, -1);
                                }
                            } else if (TreeList.isMouseOverNodeBelow(e)) {
                                // sibling
                                var originalParent = toMoved.getParent();
                                var originalSiblingIndex = toMoved.getSiblingIndex();
                                var newParent = target.getParent();
                                var newSiblingIndex = target.getSiblingIndex() + 1;
                                if (newParent == null) return;
                                if (originalParent == newParent) {
                                    if (originalSiblingIndex < newSiblingIndex) {
                                        newSiblingIndex--;
                                    }
                                    toMoved.removeSelf();
                                }
                                newParent.addEditorChild(toMoved, newSiblingIndex);
                            }
                        }
                    });
                }));

        treeList.setOnSelectedChanged(this::onSelectedChanged);

        setFocusable(true);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);
    }

    protected void onSelectedChanged(Set<UITreeNode> selected) {
        this.onSelectedChanged.accept(selected);
    }

    public void clearUI() {
        this.treeList.setRoot(null);
        this.rootNode = null;
        this.ui = null;
    }

    public void loadUI(@Nonnull UI ui) {
        this.rootNode = new UITreeNode(ui.rootElement);
        this.treeList.setRoot(rootNode);
        this.ui = ui;
    }

    public UIElement[] getSelectedNodes() {
        return treeList.getSelected().stream().map(UITreeNode::getKey).toArray(UIElement[]::new);
    }

    public Optional<UIElement> getSelectedOne() {
        var elements = getSelectedNodes();
        return elements.length == 1 ? Optional.of(elements[0]) : Optional.empty();
    }

    public void focusNode(@Nullable UITreeNode node) {
        if (node == null) return;
        treeList.expandNodeAlongPath(node);
        treeList.setSelected(List.of(node), true);
        var nodeUI = treeList.getNodeUIs().get(node);
        if (nodeUI != null) {
            var scrollRange = scrollerView.getContainerHeight() - scrollerView.viewPort.getContentHeight();
            if (scrollRange > 0) {
                var targetTop = nodeUI.getTaffyLayout().location().y;
                var targetBottom = targetTop + nodeUI.getSizeHeight();
                var currentTop = scrollerView.verticalScroller.getValue() * scrollRange;
                var currentBottom = currentTop + scrollerView.viewPort.getContentHeight();
                if (targetTop < currentTop) {
                    scrollerView.verticalScroller.setValue(targetTop / scrollRange, true);
                } else if (targetBottom > currentBottom) {
                    scrollerView.verticalScroller.setValue((targetBottom - scrollerView.viewPort.getContentHeight()) / scrollRange, true);
                }
            }
            nodeUI.focus();
        }
    }

    protected void onMouseDown(UIEvent event) {
        focus();
        if (event.button == 1) {
            openMenu(event.x, event.y, createMenu());
            event.stopPropagation();
        }
    }

    public void openMenu(float posX, float posY, @Nullable TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null || menuBuilder.isEmpty() || getModularUI() == null) return;
        var root = getModularUI().ui.rootElement;
        var layoutOffset = root.worldToLocalLayoutOffset(new Vector2f(posX, posY));
        root.addChildren(new Menu<>(menuBuilder.build(), TreeBuilder.Menu::uiProvider)
                .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                .setOnNodeClicked(TreeBuilder.Menu::handle)
                .layout(layout -> {
                    layout.left(layoutOffset.x);
                    layout.top(layoutOffset.y);
                }));
    }

    protected void onValidateCommand(UIEvent event) {
        if (CommandEvents.COPY.equals(event.command)) {
            event.stopPropagation();
        }
        if (CommandEvents.PASTE.equals(event.command)) {
            event.stopPropagation();
        }
    }

    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.COPY.equals(event.command)) {
            copySelected();
        }
        if (CommandEvents.PASTE.equals(event.command)) {
            pasteToSelected();
        }
    }

    private boolean isSelectedNodeValid(Set<UITreeNode> selected) {
        if (selected.isEmpty()) return false;
        UIElement parent = null;
        for (var node : selected) {
            if (node == rootNode) return false;
            var element = node.getKey();
            if (element.isInternalUI()) return false;
            if (element.getParent() != parent) {
                if (parent != null) return false;
                parent = element.getParent();
            }
        }
        return true;
    }

    @Nullable
    protected TreeBuilder.Menu createMenu() {
        if (ui == null) return null;
        var menu = TreeBuilder.Menu.start();
        if (treeList.getSelected().size() <= 1) {
            // add elements
            menu.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.new", m -> {
                var father = treeList.getSelected().stream().findFirst()
                        .map(UITreeNode::getKey).orElse(ui.rootElement);
                for (var holder : LDLib2Registries.UI_ELEMENTS) {
                    if (father.canAddEditorChild(holder)) {
                        var annotation = holder.annotation();
                        var group = annotation.group();
                        var name = annotation.name();
                        Consumer<TreeBuilder.Menu> buildNode = targetNode -> targetNode.leaf(name, () -> {
                            var uiElement = holder.value().get();
                            uiElement.initEditorTemplate();
                            father.addEditorChild(uiElement, -1);
                        });
                        if (group.isEmpty()) {
                            buildNode.accept(m);
                        } else {
                            var paths = group.split("\\.");
                            diveBranch(paths, m, buildNode);
                        }
                    }
                }
            });
        }
        var selected = treeList.getSelected();
        if (isSelectedNodeValid(selected)) {
            menu.leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", () -> {
                var nodes = treeList.getSelected();
                if (!isSelectedNodeValid(nodes)) return;
                for (UITreeNode node : nodes) {
                    var element = node.getKey();
                    if (element.isInternalUI()) continue;
                    element.removeSelf();
                }
            });
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", this::copySelected);
        }
        if (ClipboardManager.INSTANCE.getClipboardType() == NodeCopy.class && selected.size() == 1) {
            menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", this::pasteToSelected);
        }
        return menu;
    }

    public void copySelected() {
        var nodes = treeList.getSelected();
        if (!isSelectedNodeValid(nodes)) return;
        var tags = nodes.stream()
                .sorted(Comparator.comparingInt(node -> node.getKey().getSiblingIndex()))
                .map(node -> CODEC.encodeStart(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), node.key)
                        .result().orElse(null))
                .filter(Objects::nonNull)
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .toList();
        var nodeCopy = new NodeCopy(tags);
        ClipboardManager.INSTANCE.copyDirect(nodeCopy);
    }

    public void pasteToSelected() {
        if (ClipboardManager.INSTANCE.getClipboardType() != NodeCopy.class) return;
        var nodes = treeList.getSelected();
        if (nodes.size() != 1) return;
        var parent = nodes.iterator().next().getKey();
        if (ClipboardManager.INSTANCE.paste() instanceof NodeCopy(List<CompoundTag> copiedNodes)) {
            copiedNodes.forEach(tag -> {
                CODEC.parse(Platform.getFrozenRegistry().createSerializationContext(NbtOps.INSTANCE), tag).result().ifPresent(element -> {
                    parent.addEditorChild(element, -1);
                });
            });
        }
    }

    private void diveBranch(String[] paths, TreeBuilder.Menu current, Consumer<TreeBuilder.Menu> menu) {
        if (paths.length == 0) {
            menu.accept(current);
            return;
        }
        var path = paths[0];
        var nextPaths = new String[paths.length - 1];
        System.arraycopy(paths, 1, nextPaths, 0, nextPaths.length);
        current.branch(path, m -> diveBranch(nextPaths, m, menu));
    }
}
