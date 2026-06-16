package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * The {@code TreeBuilder} class provides an API for constructing and managing a hierarchical tree structure
 * with key-value pairs. It supports the creation of branches, leaves, and dynamic modification of tree content.
 * Also provides static methods to start building trees.
 *
 * @param <K> the type of keys used in the tree
 * @param <V> the type of values associated with keys in the tree
 */
@KJSBindings
public class TreeBuilder<K, V> {
    protected final Stack<TreeNode<K, V>> stack = new Stack<>();

    public TreeBuilder(K key) {
        stack.push(createNode(key));
    }

    protected TreeNode<K, V> createNode(K key) {
        return new TreeNode<>(key);
    }

    public static <K, V> TreeBuilder<K, V> start(K key){
        return new TreeBuilder<>(key);
    }

    /**
     * Creates or navigates to a branch in the tree structure based on the given {@code key}.
     * If a branch matching the {@code key} exists and is not a leaf, it will be used.
     * Otherwise, a new branch for the {@code key} will be created.
     * The specified {@link Consumer} is applied to the {@link TreeBuilder} to define
     * the contents or properties of the branch.
     *
     * @param key the key to identify or create the branch in the tree
     * @param builderConsumer a {@link Consumer} that accepts the {@link TreeBuilder}
     *                        and is used to modify or define the branch
     * @return the current instance of {@link TreeBuilder} for method chaining
     */
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

    /**
     * Navigates through or creates a branch structure in the tree based on the provided {@code paths}.
     * For each element in {@code paths}, a corresponding branch is either created or accessed recursively.
     * The given {@link Consumer} is invoked on the {@link TreeBuilder} instance once the navigation or creation
     * is complete.
     *
     * @param paths a {@link List} of keys representing the path of branches to navigate or create
     * @param builderConsumer a {@link Consumer} that operates on the {@link TreeBuilder} instance at the deepest resolved branch
     * @return the current instance of {@link TreeBuilder} for method chaining
     */
    public TreeBuilder<K, V> diveBranch(List<K> paths, Consumer<TreeBuilder<K, V>> builderConsumer) {
        diveBranch(new ArrayList<>(paths), this, builderConsumer);
        return this;
    }

    private void diveBranch(ArrayList<K> paths, TreeBuilder<K, V> current, Consumer<TreeBuilder<K, V>> menu) {
        // if found
        if (paths.isEmpty()) {
            menu.accept(current);
            return;
        }
        // dive into deeper branches
        var path = paths.getFirst();
        paths.removeFirst();
        current.branch(path, m -> diveBranch(paths, m, menu));
    }

    /**
     * Creates or navigates to a branch in the tree structure with the specified {@code key}.
     * If a branch associated with the {@code key} exists, it navigates to it.
     * If the branch does not exist, a new branch is created for the {@code key}.
     *
     * @param key the key to identify or create the branch in the tree
     * @return the current instance of {@link TreeBuilder} for method chaining
     */
    public TreeBuilder<K, V> startBranch(K key) {
        stack.push(stack.peek().getOrCreateChild(key));
        return this;
    }

    /**
     * Ends the current branch and navigates back to the parent branch in the tree structure.
     * Removes the current branch from the internal stack used for tracking hierarchy.
     *
     * @return the current instance of {@link TreeBuilder} for method chaining
     */
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

    public ITreeNode<K, V> peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        if (stack.isEmpty()) return true;
        return stack.peek().getChildren().isEmpty();
    }

    public TreeNode<K, V> build() {
        while (stack.size() > 1) {
            stack.pop();
        }
        return stack.peek();
    }

    @KJSBindings("MenuBuilder")
    public static class Menu extends TreeBuilder<Tuple<IGuiTexture, Component>, Runnable> {
        @Deprecated(since = "26.1")
        private static class MenuTreeNode extends TreeNode<Tuple<IGuiTexture, Component>, Runnable> {
            public MenuTreeNode(Tuple<IGuiTexture, Component> key) {
                super(key);
            }

            public MenuTreeNode(@Nullable TreeNode<Tuple<IGuiTexture, Component>, Runnable> parent, int dimension, Tuple<IGuiTexture, Component> key) {
                super(parent, dimension, key);
            }

            @Override
            protected TreeNode<Tuple<IGuiTexture, Component>, Runnable> createNode(@Nullable TreeNode<Tuple<IGuiTexture, Component>, Runnable> parent, int dimension, Tuple<IGuiTexture, Component> key) {
                return new MenuTreeNode(parent, dimension, key);
            }

            @Override
            public boolean areKeysEqual(Tuple<IGuiTexture, Component> key1, Tuple<IGuiTexture, Component> key2) {
                if (key1 == key2) return true;
                if (key1 == null || key2 == null) return false;
                return Objects.equals(key1.getA(), key2.getA()) && Objects.equals(key1.getB(), key2.getB());
            }
        }

        public static Tuple<IGuiTexture, Component> CROSS_LINE = new Tuple<>(IGuiTexture.EMPTY, Component.empty());

        private Menu(Tuple<IGuiTexture, Component> key) {
            super(key);
        }

        @Override
        protected TreeNode<Tuple<IGuiTexture, Component>, Runnable> createNode(Tuple<IGuiTexture, Component> key) {
            return new MenuTreeNode(key);
        }

        public static Menu start(){
            return new Menu(new Tuple<>(IGuiTexture.EMPTY, Component.empty()));
        }

        /**
         * Adds a cross-line as a child node to the current node in the menu structure if certain conditions are met.
         * A cross-line is added only when the current node does not already end with a cross-line and has children.
         *
         * @return the current {@code Menu} instance for method chaining.
         */
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
                        endBranch();
                        return this;
                    }
                }
            }
            return branch(IGuiTexture.EMPTY, name, menuConsumer);
        }

        public Menu endBranch() {
            var peek = stack.peek();
            if (!peek.getChildren().isEmpty() && peek.getChildren().getLast().getKey() == CROSS_LINE) {
                peek.removeChild(peek.getChildren().getLast());
            }
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
                    layout.height(1);
                    layout.marginHorizontal(3);
                }).style(style -> style.backgroundTexture(ColorPattern.GRAY.rectTexture()));
            }
            return new UIElement().layout(layout -> {
                layout.height(12);
                layout.widthPercent(100);
                layout.gapAll(2);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
            }).addChild(new UIElement().layout(layout -> {
                layout.marginLeft(2);
                layout.width(10);
                layout.height(10);
            }).style(style -> style.backgroundTexture(node.getA())))
                    .addChild(new Label().textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL))
                            .setText(node.getB()).layout(layout -> {
                                layout.setFlexGrow(1);
                            }).setOverflowVisible(false));

        }

        public static IGuiTexture hoverTextureProvider(ITreeNode<Tuple<IGuiTexture, Component>, Runnable> node) {
            return isCrossLine(node.getKey()) ? IGuiTexture.EMPTY :ColorPattern.BLUE.rectTexture();
        }
    }

}
