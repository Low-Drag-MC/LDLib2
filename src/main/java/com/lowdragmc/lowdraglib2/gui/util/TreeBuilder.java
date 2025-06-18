package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.appliedenergistics.yoga.*;

import java.util.Stack;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TreeBuilder
 */
public class TreeBuilder<K, V> {
    protected final Stack<TreeNode<K, V>> stack = new Stack<>();

    public TreeBuilder(K key) {
        stack.push(new TreeNode<>(0, key));
    }

    public static <K, V> TreeBuilder<K, V> start(K key){
        return new TreeBuilder<>(key);
    }

    public TreeBuilder<K, V> branch(K key, Consumer<TreeBuilder<K, V>> builderConsumer) {
        var children = stack.peek().getChildren();
        if (!children.isEmpty()) {
            for (var child : children) {
                if (!child.isLeaf() && child.key.equals(key)) {
                    stack.push(child);
                    builderConsumer.accept(this);
                    endBranch();
                    return this;
                }
            }
        }

        stack.push(stack.peek().getOrCreateChild(key));
        builderConsumer.accept(this);
        endBranch();
        return this;
    }

    public TreeBuilder<K, V> startBranch(K key) {
        stack.push(stack.peek().getOrCreateChild(key));
        return this;
    }

    public TreeBuilder<K, V> endBranch() {
        stack.pop();
        return this;
    }

    public TreeBuilder<K, V> content(V content) {
        stack.peek().content = content;
        return this;
    }

    public TreeBuilder<K, V> leaf(K key, V content) {
        stack.peek().addContent(key, content);
        return this;
    }

    public TreeBuilder<K, V> remove(K key) {
        stack.peek().removeChild(key);
        return this;
    }

    public TreeNode<K, V> build() {
        while (stack.size() > 1) {
            stack.pop();
        }
        return stack.peek();
    }

    public static class Menu extends TreeBuilder<Tuple<IGuiTexture, Component>, Runnable> {
        public static Tuple<IGuiTexture, Component> CROSS_LINE = new Tuple<>(IGuiTexture.EMPTY, Component.empty());

        private Menu(Tuple<IGuiTexture, Component> key) {
            super(key);
        }

        public static Menu start(){
            return new Menu(new Tuple<>(IGuiTexture.EMPTY, Component.empty()));
        }

        public boolean isEmpty() {
            if (stack.isEmpty()) return true;
            return stack.peek().getChildren().isEmpty();
        }

        public Menu crossLine() {
            if (stack.peek().getChildren().isEmpty() || stack.peek().getChildren().getLast().getKey() == CROSS_LINE) {
                return this;
            }
            stack.peek().createChild(CROSS_LINE);
            return this;
        }

        public Menu branch(IGuiTexture icon, String name, Consumer<Menu> menuConsumer) {
            return branch(icon, Component.translatable(name), menuConsumer);
        }

        public Menu branch(IGuiTexture icon, Component name, Consumer<Menu> menuConsumer) {
            var key = new Tuple<>(icon, name);
            var child = stack.peek().getOrCreateChild(key);
            stack.push(child);
            menuConsumer.accept(this);
            if (child.getChildren() != null && !child.getChildren().isEmpty() && child.getChildren().getLast().getKey() == CROSS_LINE) {
                child.removeChild(child.getChildren().getLast());
            }
            endBranch();
            return this;
        }

        public Menu branch(String name, Consumer<Menu> menuConsumer) {
            return branch(Component.translatable(name), menuConsumer);
        }

        public Menu branch(Component name, Consumer<Menu> menuConsumer) {
            var children = stack.peek().getChildren();
            if (!children.isEmpty()) {
                for (var child : children) {
                    if (!child.isLeaf() && child.getKey().getB().equals(name)) {
                        stack.push(child);
                        menuConsumer.accept(this);
                        child.getChildren();
                        if (!child.getChildren().isEmpty() && child.getChildren().getLast().getKey() == CROSS_LINE) {
                            child.removeChild(child.getChildren().getLast());
                        }
                        endBranch();
                        return this;
                    }
                }
            }
            return branch(IGuiTexture.EMPTY, name, menuConsumer);
        }

        public Menu endBranch() {
            super.endBranch();
            return this;
        }

        public Menu leaf(IGuiTexture icon, String name, Runnable runnable) {
            return leaf(icon, Component.translatable(name), runnable);
        }

        public Menu leaf(IGuiTexture icon, Component name, Runnable runnable) {
            super.leaf(new Tuple<>(icon, name), runnable);
            return this;
        }

        public Menu leaf(String name, Runnable runnable) {
            return leaf(Component.translatable(name), runnable);
        }

        public Menu leaf(Component name, Runnable runnable) {
            super.leaf(new Tuple<>(IGuiTexture.EMPTY, name), runnable);
            return this;
        }

        public Menu remove(String name) {
            return remove(Component.translatable(name));
        }

        public Menu remove(Component name) {
            var children = stack.peek().getChildren();
            if (!children.isEmpty()) {
                for (TreeNode<Tuple<IGuiTexture, Component>, Runnable> child : children) {
                    if (child.getKey().getB().equals(name)) {
                        stack.peek().removeChild(child.getKey());
                        return this;
                    }
                }
            }
            return this;
        }

        @Override
        public TreeNode<Tuple<IGuiTexture, Component>, Runnable> build() {
            var root = super.build();
            if (!root.getChildren().isEmpty() && root.getChildren().getLast().getKey() == CROSS_LINE) {
                root.removeChild(root.getChildren().getLast());
            }
            return root;
        }

        public static IGuiTexture getIcon(Tuple<IGuiTexture, Component> key) {
            return key.getA();
        }

        public static Component getName(Tuple<IGuiTexture, Component> key) {
            return key.getB();
        }

        public static void handle(ITreeNode<Tuple<IGuiTexture, Component>, Runnable> node) {
            if (node.isLeaf() && node.getContent() != null) {
                node.getContent().run();
            }
        }

        public static boolean isCrossLine(Tuple<IGuiTexture, Component> key) {
            return key == CROSS_LINE;
        }

        public static UIElement uiProvider(Tuple<IGuiTexture, Component> node) {
            if (node == CROSS_LINE) {
                return new UIElement().layout(layout -> {
                    layout.setHeight(1);
                    layout.setMargin(YogaEdge.HORIZONTAL, 3);
                }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            }
            return new UIElement().layout(layout -> {
                layout.setHeight(12);
                layout.setWidthPercent(100);
                layout.setGap(YogaGutter.ALL, 2);
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setAlignItems(YogaAlign.CENTER);
            }).addChild(new UIElement().layout(layout -> {
                layout.setMargin(YogaEdge.LEFT, 2);
                layout.setWidth(10);
                layout.setHeight(10);
            }).style(style -> style.backgroundTexture(node.getA())))
                    .addChild(new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                            .setText(node.getB()).layout(layout -> {
                                layout.setFlexGrow(1);
                            }).setOverflow(YogaOverflow.HIDDEN));

        }

        public static IGuiTexture hoverTextureProvider(ITreeNode<Tuple<IGuiTexture, Component>, Runnable> node) {
            return isCrossLine(node.getKey()) ? IGuiTexture.EMPTY :ColorPattern.BLUE.rectTexture();
        }
    }

}
