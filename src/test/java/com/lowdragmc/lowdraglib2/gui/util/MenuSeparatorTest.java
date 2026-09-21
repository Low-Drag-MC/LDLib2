package com.lowdragmc.lowdraglib2.gui.util;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separators are the one thing in a menu that is not unique.
 *
 * <p>26.2 dropped {@code MenuTreeNode}, which is where a menu's key comparison used to live, and the
 * plain {@link TreeNode#equals} it fell back to compares key and content — both of which every
 * cross-line shares with every other. Anything that removes a separator <em>by value</em> therefore
 * takes the first one rather than the one it was handed, which is only visible once a menu has two:
 * the one in the middle disappears and the trailing line it was trimming stays.</p>
 */
class MenuSeparatorTest {

    private static List<? extends TreeNode<?, Runnable>> childrenOf(TreeBuilder.Menu menu) {
        return menu.build().getChildren();
    }

    private static boolean isSeparator(TreeNode<?, Runnable> node) {
        return node.getKey() == TreeBuilder.Menu.CROSS_LINE;
    }

    @Test
    void aMenuKeepsEverySeparatorItAsksFor() {
        var menu = TreeBuilder.Menu.start()
                .leaf(Component.literal("one"), () -> {})
                .crossLine()
                .leaf(Component.literal("two"), () -> {})
                .crossLine()
                .leaf(Component.literal("three"), () -> {});

        var children = childrenOf(menu);
        assertEquals(5, children.size());
        assertTrue(isSeparator(children.get(1)), "the first separator survives");
        assertTrue(isSeparator(children.get(3)), "so does the second");
    }

    @Test
    void aTrailingSeparatorIsTrimmedAndTheMiddleOneIsNot() {
        // The graph canvas menu, in miniature: one separator before the view-preference block and
        // another before the selection block, with the selection block turning out to be empty.
        var menu = TreeBuilder.Menu.start()
                .leaf(Component.literal("add node"), () -> {})
                .crossLine()
                .leaf(Component.literal("wire style"), () -> {})
                .crossLine();

        var children = childrenOf(menu);
        assertEquals(3, children.size(), "the trailing separator is dropped");
        assertTrue(isSeparator(children.get(1)), "the separator in the middle is kept");
        assertTrue(children.stream().noneMatch(node -> node == children.getLast() && isSeparator(node)),
                "the menu does not end on a separator");
    }

    @Test
    void aTrailingSeparatorInsideABranchIsTrimmedTheSameWay() {
        // endBranch() runs the same trim as build(), against the branch rather than the root.
        var menu = TreeBuilder.Menu.start()
                .branch(Component.literal("sub"), sub -> sub
                        .leaf(Component.literal("a"), () -> {})
                        .crossLine()
                        .leaf(Component.literal("b"), () -> {})
                        .crossLine());

        var branch = childrenOf(menu).getFirst();
        assertEquals(3, branch.getChildren().size());
        assertTrue(isSeparator(branch.getChildren().get(1)));
    }

    @Test
    void consecutiveCrossLinesCollapseIntoOne() {
        // Pre-existing contract: crossLine() is a no-op when the branch already ends on one, so a
        // menu assembled from optional blocks never doubles its rules.
        var menu = TreeBuilder.Menu.start()
                .leaf(Component.literal("one"), () -> {})
                .crossLine()
                .crossLine()
                .leaf(Component.literal("two"), () -> {});

        assertEquals(3, childrenOf(menu).size());
    }

    @Test
    void aLeadingCrossLineIsNeverAdded() {
        var menu = TreeBuilder.Menu.start()
                .crossLine()
                .leaf(Component.literal("one"), () -> {});

        var children = childrenOf(menu);
        assertEquals(1, children.size());
        assertTrue(!isSeparator(children.getFirst()));
    }
}
