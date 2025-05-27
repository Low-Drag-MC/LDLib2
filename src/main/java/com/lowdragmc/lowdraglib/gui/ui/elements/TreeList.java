package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.*;
import org.appliedenergistics.yoga.style.StyleSizeLength;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Accessors(chain = true)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TreeList<K, T> extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class TreeListStyle extends Style {
        @Getter
        @Setter
        private IGuiTexture nodeTexture = IGuiTexture.EMPTY;
        @Getter @Setter
        private IGuiTexture leafTexture = IGuiTexture.EMPTY;
        @Getter @Setter
        private IGuiTexture nodeHoverTexture = ColorPattern.BLUE.rectTexture();
        @Getter @Setter
        private IGuiTexture leafHoverTexture = ColorPattern.BLUE.rectTexture();
        @Getter @Setter
        private IGuiTexture collapseIcon = Icons.RIGHT_ARROW_NO_BAR_S_WHITE;
        @Getter @Setter
        private IGuiTexture expandIcon = Icons.DOWN_ARROW_NO_BAR_S_WHITE;

        public TreeListStyle(UIElement holder) {
            super(holder);
        }

        public TreeListStyle copyFrom(TreeListStyle other) {
            this.nodeTexture = other.nodeTexture;
            this.leafTexture = other.leafTexture;
            this.nodeHoverTexture = other.nodeHoverTexture;
            this.leafHoverTexture = other.leafHoverTexture;
            this.expandIcon = other.expandIcon;
            this.collapseIcon = other.collapseIcon;
            return this;
        }
    }

    public final TreeNode<K, T> root;
    @Getter
    private final TreeListStyle treeListStyle = new TreeListStyle(this);
    @Setter
    protected Function<K, IGuiTexture> keyIconSupplier = k -> IGuiTexture.EMPTY;
    @Setter
    protected Function<K, String> keyNameSupplier = Object::toString;
    @Setter
    protected Function<T, IGuiTexture> contentIconSupplier = k -> IGuiTexture.EMPTY;
    @Setter
    protected Function<T, String> contentNameSupplier = Object::toString;
    @Setter
    protected Consumer<TreeNode<K, T>> onSelected = Consumers.nop();
    @Setter
    protected Consumer<TreeNode<K, T>> onDoubleClickLeaf = Consumers.nop();
    @Setter
    protected boolean canSelectNode;

    // runtime
    private final TreeList<K, T> rootList;
    @Getter
    private boolean isExpanded = false;
    @Getter
    private UIElement rootElement;
    private UIElement arrowIcon;
    @Getter
    private UIElement childrenContainer;
    @Getter
    private Map<TreeNode<K, T>, UIElement> childrenElements = new HashMap<>();
    @Getter
    @Nullable
    protected TreeNode<K, T> selected = null;
    @Nullable
    protected UIElement selectedElement = null;

    public TreeList(TreeNode<K, T> root) {
        this(root, null);
    }

    public TreeList(TreeNode<K, T> root, @Nullable TreeList<K, T> rootList) {
        getLayout().setWidthPercent(100);
        getLayout().setGap(YogaGutter.ALL, 1);
        this.root = root;
        this.rootList = rootList;
        initList();
    }

    public TreeList<K, T> menuStyle(Consumer<TreeListStyle> treeListStyle) {
        treeListStyle.accept(this.treeListStyle);
        onStyleChanged();
        initList();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        treeListStyle.applyStyles(values);
    }

    public TreeList<K, T> initList() {
        clearAllChildren();
        rootElement = new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidthPercent(100);
            layout.setHeight(12);
            layout.setAlignItems(YogaAlign.CENTER);
            layout.setGap(YogaGutter.ALL, 2);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.setWidth(10);
                    layout.setHeight(10);
                }).style(style -> {
                    style.backgroundTexture(keyIconSupplier.apply(root.getKey()));
                }),
                new Label().textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                        .setText(keyNameSupplier.apply(root.getKey())).layout(layout -> {
                            layout.setHeightPercent(100);
                            layout.setFlex(1);
                        }).setOverflow(YogaOverflow.HIDDEN),
                arrowIcon = new UIElement().layout(layout -> {
                    layout.setWidth(8);
                    layout.setHeight(8);
                }).style(style -> style.backgroundTexture(treeListStyle.collapseIcon))
        ).addEventListener(UIEvents.MOUSE_DOWN, e -> onNodeClicked(root, e));

        childrenContainer = new UIElement();
        childrenContainer.layout(layout -> {
            layout.setFlex(1);
            layout.setGap(YogaGutter.ALL, 1);
        }).style(style -> style.backgroundTexture(IGuiTexture.EMPTY));

        if (isExpanded) {
            buildChildren();
        }

        addChildren(rootElement, new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(new UIElement().layout(layout -> layout.setWidth(5)), childrenContainer));
        return this;
    }

    public void buildChildren() {
        childrenContainer.clearAllChildren();
        childrenElements.clear();
        for (var child : root.getChildren()) {
            if (child.isLeaf() && child.getContent() != null) {
                childrenContainer.addChild(new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setWidthPercent(100);
                    layout.setHeight(12);
                    layout.setAlignItems(YogaAlign.CENTER);
                    layout.setGap(YogaGutter.ALL, 2);
                }).style(style -> style.backgroundTexture(treeListStyle.leafTexture)).addChildren(
                        new UIElement().layout(layout -> {
                            layout.setWidth(10);
                            layout.setHeight(10);
                        }).style(style -> style.backgroundTexture(contentIconSupplier.apply(child.getContent()))),
                        new Label().textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                                .setText(contentNameSupplier.apply(child.getContent())).layout(layout -> {
                                    layout.setHeightPercent(100);
                                    layout.setFlex(1);
                                }).setOverflow(YogaOverflow.HIDDEN)
                ).addEventListener(UIEvents.MOUSE_DOWN, e -> onLeafClicked(child, e)))
                        .addEventListener(UIEvents.DOUBLE_CLICK, e -> {
                            if (e.button == 0) {
                                onDoubleClickLeaf.accept(child);
                            }
                        });
                childrenElements.put(child, childrenContainer.getChildren().getLast());
            } else if (!child.isLeaf()) {
                childrenContainer.addChild(new TreeList<>(child, rootList == null ? this : rootList)
                        .setCanSelectNode(canSelectNode)
                        .setOnSelected(onSelected)
                        .setOnDoubleClickLeaf(onDoubleClickLeaf)
                        .setKeyIconSupplier(keyIconSupplier)
                        .setKeyNameSupplier(keyNameSupplier)
                        .setContentIconSupplier(contentIconSupplier)
                        .setContentNameSupplier(contentNameSupplier)
                        .menuStyle(style -> style.copyFrom(this.treeListStyle))
                        .layout(layout -> layout.setWidth(StyleSizeLength.AUTO)));
                childrenElements.put(child, childrenContainer.getChildren().getLast());
            }
        }
    }

    public void setExpanded(boolean expanded) {
        if (isExpanded == expanded) return;
        isExpanded = expanded;
        if (expanded) {
            buildChildren();
        } else {
            childrenContainer.clearAllChildren();
            childrenElements.clear();
        }
    }

    protected void onLeafClicked(TreeNode<K, T> node, UIEvent event) {
        if (event.button == 0) {
            setSelected(node, event.currentElement);
        }
    }

    protected void onNodeClicked(TreeNode<K, T> node, UIEvent event) {
        if (event.button == 0) {
            setExpanded(!isExpanded);
            arrowIcon.style(style -> style.backgroundTexture(isExpanded ? treeListStyle.expandIcon : treeListStyle.collapseIcon));
            if (!canSelectNode) return;
            setSelected(node, event.currentElement);
        }
    }

    protected void setSelected(TreeNode<K, T> node, UIElement element) {
        if (rootList != null) {
            rootList.setSelected(node, element);
            return;
        }
        if (selected == node) return;
        Widget.playButtonClickSound();
        if (selected != null && selectedElement != null) {
            selectedElement.style(style -> style.backgroundTexture(selected.isLeaf() ? treeListStyle.leafTexture : treeListStyle.nodeTexture));
        }
        selected = node;
        selectedElement = element;
        selectedElement.style(style -> style.backgroundTexture(node.isLeaf() ? treeListStyle.leafHoverTexture : treeListStyle.nodeHoverTexture));
        onSelected.accept(node);
    }
}
