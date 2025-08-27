package com.lowdragmc.lowdraglib2.editor.ui.view.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UIHierarchy extends UIElement {
    public record DraggingNode(UITreeNode draggedNode) {}

    public final UIEditorView editorView;
    public final ScrollerView scrollerView = new ScrollerView();
    public final TreeList<UITreeNode> treeList = new TreeList<>();

    // runtime
    @Getter @Nullable
    private UI ui;
    private long lastClickTime = 0;
    @Getter @Nullable
    private UITreeNode rootNode;

    public UIHierarchy(UIEditorView editorView) {
        this.editorView = editorView;
        this.getLayout().setWidthPercent(100.0F);
        this.getLayout().setHeightPercent(100.0F);

        this.scrollerView.layout((layout) -> {
            layout.setWidthPercent(100.0F);
            layout.setHeightPercent(100.0F);
        });
        this.addChild(this.scrollerView);
        scrollerView.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown, true);
        scrollerView.addScrollViewChild(treeList
                .setSupportMultipleSelection(true)
                .setNodeUISupplier((node) -> {
                    UIElement container = (new UIElement()).layout((layout) -> {
                        layout.setFlexDirection(YogaFlexDirection.ROW);
                        layout.setGap(YogaGutter.ALL, 2.0F);
                        layout.setHeight(10.0F);
                        layout.setFlex(1.0F);
                    }).addChildren();
                    UIElement icon = (new UIElement()).layout((layout) -> {
                        layout.setAspectRatio(1.0F);
                        layout.setHeightPercent(100.0F);
                    }).style((style) -> style.backgroundTexture(node.getKey().getEditorIcon()));
                    TextElement label = new TextElement();
                    label.textStyle((style) -> {
                        style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER);
                        style.textColor(node.getKey().isInternalUI() ? ColorPattern.LIGHT_GRAY.color : ColorPattern.WHITE.color);
                    }).setText(node.getKey().getEditorName()).layout((layout) -> {
                        layout.setHeightPercent(100.0F);
                        layout.setFlex(1.0F);
                    }).setOverflow(YogaOverflow.HIDDEN).addEventListener(UIEvents.TICK, e -> {
                        label.setText(node.getKey().getEditorName());
                    });
                    return container.addChildren(icon, label);
                })
                .setOnSelectedChanged(selected -> {
                    if (selected.size() == 1) {
                        editorView.inspector.inspect(selected.stream().findFirst().get().getKey());
                    } else {
                        editorView.inspector.clear();
                    }
                })
                .setOnNodeUICreated((node, nodeUI) -> {
                    nodeUI.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                        if (e.button == 0) {
                            lastClickTime = System.currentTimeMillis();
                        }
                    });
                    nodeUI.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                        if (lastClickTime != 0 && isMouseDown(0) && treeList.getSelected().size() == 1) {
                            nodeUI.startDrag(new DraggingNode(node), new TextTexture(node.getKey().getEditorName().getString()));
                        }
                        lastClickTime = 0;
                    }, true);
                    nodeUI.addEventListener(UIEvents.MOUSE_UP, e -> {
                        var element = node.getKey();
                        if (treeList.getSelected().size() == 1) {
                            if (editorView.inspector.getInspectedConfigurable() != element) {
                                editorView.inspector.inspect(element);
                            }
                        } else {
                            editorView.inspector.clear();
                        }
                        lastClickTime = 0;
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_ENTER, e -> {
                        if (e.dragHandler.getDraggingObject() instanceof DraggingNode(var dragged) && dragged != node) {
                            var mode = isMouseOverNodeAbove(e) ? 0 : isMouseOverNodeCenter(e) ? 1 : isMouseOverNodeBelow(e) ? 2 : -1;
                            e.currentElement.style(style -> style.overlayTexture(createDraggingOverlay(mode)));
                        }
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_END, e -> {
                        e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_UPDATE, e -> {
                        if (e.dragHandler.getDraggingObject() instanceof DraggingNode(var dragged) && dragged != node) {
                            var mode = isMouseOverNodeAbove(e) ? 0 : isMouseOverNodeCenter(e) ? 1 : isMouseOverNodeBelow(e) ? 2 : -1;
                            e.currentElement.style(style -> style.overlayTexture(createDraggingOverlay(mode)));
                        }
                    });
                    nodeUI.addEventListener(UIEvents.DRAG_PERFORM, e -> {
                        e.currentElement.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
                        if (e.dragHandler.getDraggingObject() instanceof DraggingNode(var dragged) && dragged != node) {

                        }
                    });
                }));
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

    private boolean isMouseOverNodeAbove(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return isMouseOver(x, y, width, height / 3, event.x, event.y);
    }

    private boolean isMouseOverNodeCenter(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return isMouseOver(x, y + height / 3, width, height / 3, event.x, event.y);
    }

    private boolean isMouseOverNodeBelow(UIEvent event) {
        var ui = event.currentElement;
        var x = ui.getPositionX();
        var y = ui.getPositionY();
        var width = ui.getSizeWidth();
        var height = ui.getSizeHeight();
        return isMouseOver(x, y + height * 2 / 3, width, height / 3, event.x, event.y);
    }

    private IGuiTexture createDraggingOverlay(int mode) {
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

    protected void onMouseDown(UIEvent event) {
        if (event.button == 1) {
            editorView.openMenu(event.x, event.y, createMenu());
            event.stopPropagation();
        }
    }

    @Nullable
    protected TreeBuilder.Menu createMenu() {
        if (ui == null) return null;
        var menu = TreeBuilder.Menu.start();
        return menu;
    }

}
