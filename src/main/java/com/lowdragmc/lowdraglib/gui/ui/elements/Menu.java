package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.style.Style;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class Menu<K, T> extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class MenuStyle extends Style {
        @Getter @Setter
        private IGuiTexture nodeTexture = IGuiTexture.EMPTY;
        @Getter @Setter
        private IGuiTexture leafTexture = IGuiTexture.EMPTY;
        @Getter @Setter
        private IGuiTexture nodeHoverTexture = ColorPattern.BLUE.rectTexture();
        @Getter @Setter
        private IGuiTexture leafHoverTexture = ColorPattern.BLUE.rectTexture();
        @Getter @Setter
        private IGuiTexture arrowIcon = Icons.RIGHT_ARROW_NO_BAR_S_WHITE;

        public MenuStyle(UIElement holder) {
            super(holder);
        }

        public MenuStyle copyFrom(MenuStyle other) {
            this.nodeTexture = other.nodeTexture;
            this.leafTexture = other.leafTexture;
            this.nodeHoverTexture = other.nodeHoverTexture;
            this.leafHoverTexture = other.leafHoverTexture;
            this.arrowIcon = other.arrowIcon;
            return this;
        }
    }
    public final TreeNode<K, T> root;
    @Getter
    private final MenuStyle menuStyle = new MenuStyle(this);
    @Nonnull
    protected Function<K, UIElement> uiProvider;
    @Setter @Nullable
    protected Consumer<TreeNode<K, T>> onNodeClicked;
    @Setter
    protected boolean autoClose = true;
    @Getter
    protected final Map<TreeNode<K, T>, UIElement> nodeUIs = new LinkedHashMap<>();
    // runtime
    @Nullable
    protected TreeNode<K, T> openedNode;
    @Nullable
    protected Menu<K, T> opened;

    public Menu(TreeNode<K, T> root) {
        this(root, (key) -> new TextElement().setText(key.toString()));
    }

    public Menu(TreeNode<K, T> root, Function<K, UIElement> uiProvider) {
        this.root = root;
        this.uiProvider = uiProvider;

        getLayout().setPadding(YogaEdge.ALL, 2);
        getLayout().setGap(YogaGutter.ALL, 2);
        getLayout().setPositionType(YogaPositionType.ABSOLUTE);
        getLayout().setMinWidth(120);
        getStyle().backgroundTexture(Sprites.RECT_SOLID);
        getStyle().zIndex(100);
        setFocusable(true);
        addEventListener(UIEvents.BLUR, this::onBlur, true);

        initMenu();
    }

    protected void onBlur(UIEvent event) {
        if (event.relatedTarget != null && this.isAncestor(event.relatedTarget)) { // focus on children
            return;
        }

        if (event.target == this) { // lose focus
            if (isChildHover() && event.relatedTarget == null) {
                focus();
            } else {
                if(autoClose) {
                    close();
                }
            }
        } else { // child lose focus
            if (event.relatedTarget == null && isChildHover()) {
                focus();
            } else {
                if(autoClose) {
                    close();
                }
            }
        }
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        var mui = getModularUI();
        if (mui != null) {
            // if outside the screen, move it back to the screen
            var screenWidth = mui.getScreenWidth();
            var screenHeight = mui.getScreenHeight();
            var x = getPositionX();
            var y = getPositionY();
            var width = getSizeWidth();
            var height = getSizeHeight();
            // check head out of screen
            if (y < 0) {
                layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() - y));
            } else if (y + height > screenHeight) {
                layout(layout -> layout.setPosition(YogaEdge.TOP, getLayoutY() + screenHeight - (y + height)));
            }
            if (x < 0) {
                layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() - x));
            } else if (x + width > screenWidth) {
                if (x > width) {
                    // move to the left first
                    layout(layout -> layout.setPosition(YogaEdge.LEFT, 0 - width));
                } else {
                    layout(layout -> layout.setPosition(YogaEdge.LEFT, getLayoutX() + screenWidth - (x + width)));
                }
            }
        }
    }

    @Override
    protected void _setModularUIInternal(@Nullable ModularUI gui) {
        super._setModularUIInternal(gui);
        if (gui != null) {
            gui.requestFocus(this);
        }
    }

    public Menu<K, T> setUiProvider(Function<K, UIElement> uiProvider) {
        this.uiProvider = uiProvider;
        clearAllChildren();
        initMenu();
        return this;
    }

    public Menu<K, T> menuStyle(Consumer<MenuStyle> menuStyle) {
        menuStyle.accept(this.menuStyle);
        onStyleChanged();
        nodeUIs.forEach((node, element) -> {
            element.style(style -> style.backgroundTexture(node.isLeaf() ? this.menuStyle.leafTexture : this.menuStyle.nodeTexture));
        });
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        menuStyle.applyStyles(values);
    }

    public void close(){
        if (this.getParent() != null) {
            this.getParent().removeChild(this);
        }
    }

    protected void initMenu() {
        if (!root.isLeaf()) {
            for (TreeNode<K, T> child : root.getChildren()) {
                var container = new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setAlignItems(YogaAlign.CENTER);
                }).style(style -> style.backgroundTexture(child.isLeaf() ? menuStyle.leafTexture : menuStyle.nodeTexture))
                        .addChild(new UIElement().layout(layout -> {
                            layout.setFlex(1);
                        }).addChild(uiProvider.apply(child.getKey())))
                        .addEventListener(UIEvents.MOUSE_DOWN, e -> {
                            if (e.button == 0) {
                                if (child.isLeaf()) {
                                    if (onNodeClicked != null) {
                                        onNodeClicked.accept(child);
                                    }
                                    if (autoClose) {
                                        close();
                                    }
                                }
                            }
                        }).addEventListener(UIEvents.MOUSE_ENTER, e -> {
                            e.currentElement.style(style -> style.backgroundTexture(child.isLeaf() ? menuStyle.leafHoverTexture : menuStyle.nodeHoverTexture));
                            if (!child.isLeaf()) { // open a new menu
                                if (opened != null) {
                                    if (openedNode == child) return;
                                    opened.close();
                                }
                                openedNode = child;
                                opened = new Menu<>(child, uiProvider);
                                opened.setAutoClose(autoClose);
                                opened.getMenuStyle().copyFrom(menuStyle);
                                opened.getStyle().copyFrom(this.getStyle());
                                opened.getLayout().setAlignSelf(YogaAlign.FLEX_START);
                                opened.getLayout().setPosition(YogaEdge.LEFT, e.currentElement.getSizeWidth());
                                opened.setOnNodeClicked(node -> {
                                    if (onNodeClicked != null) {
                                        onNodeClicked.accept(node);
                                    }
                                    if (autoClose){
                                        close();
                                    }
                                });
                                e.currentElement.addChild(opened);
                            } else {
                                if (opened != null) {
                                    opened.close();
                                    openedNode = null;
                                    focus();
                                }
                            }
                        }, true)
                        .addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                            e.currentElement.style(style -> style.backgroundTexture(child.isLeaf() ? menuStyle.leafTexture : menuStyle.nodeTexture));
                        }, true);
                if (!child.isLeaf()) {
                    container.addChild(new UIElement().layout(layout -> {
                        layout.setWidth(8);
                        layout.setHeight(8);
                        layout.setMargin(YogaEdge.HORIZONTAL, 2);
                    }).style(style -> style.backgroundTexture(menuStyle.arrowIcon)));
                }
                nodeUIs.put(child, container);
                addChild(container);
            }
        }
    }

}
