package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Driving a {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Menu} from a scenario.
 *
 * <p>Three things about menus defeat the obvious calls, and all three fail the same unhelpful way —
 * "no element matched":</p>
 * <ul>
 *     <li>An entry's label is two levels down. The clickable element carries the style class, but
 *     its children are a flex wrapper (and, on a branch, the little arrow); the label lives inside
 *     the wrapper. {@code Texts.of} only looks one level in, so it reports an entry as having no
 *     text at all — hence the recursive {@link #labelOf} here.</li>
 *     <li>A menu marks its contents as internal UI, and {@code ElementQuery#withText} quietly adds
 *     an exclude-internal filter, so that route is closed even once the label is found.</li>
 *     <li>A submenu only exists while its branch is hovered, and clicking an entry closes the whole
 *     menu — so a builder {@code click(selector)}, which re-resolves its selector on release, looks
 *     for something that is already gone. Press and release are issued together here, at
 *     coordinates resolved once.</li>
 * </ul>
 */
public final class ContextMenus {

    /** The class the node graph view puts on the menu it opens from the canvas. */
    public static final String GRAPH_MENU = ".__node-graph-view_context-menu__";

    private static final String BRANCH = "__menu_branch-node__";
    private static final String LEAF = "__menu_leaf-node__";

    private ContextMenus() {
    }

    /** How many branch entries currently carry this label. Submenus count too. */
    public static int branchCount(TestContext ctx, String translationKey) {
        return find(ctx, BRANCH, translationKey).size();
    }

    /** How many leaf entries currently carry this label. Submenus count too. */
    public static int leafCount(TestContext ctx, String translationKey) {
        return find(ctx, LEAF, translationKey).size();
    }

    /** Hovers a branch entry, which is what opens its submenu. */
    public static void openBranch(TestContext ctx, String translationKey) {
        var bounds = ElementBounds.of(one(ctx, BRANCH, translationKey));
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
    }

    /** Clicks a leaf entry, pressing and releasing before the menu it lives in can close. */
    public static void clickLeaf(TestContext ctx, String translationKey) {
        var bounds = ElementBounds.of(one(ctx, LEAF, translationKey));
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    private static UIElement one(TestContext ctx, String styleClass, String translationKey) {
        var matches = find(ctx, styleClass, translationKey);
        if (matches.size() != 1) {
            throw new IllegalStateException("expected exactly one menu entry labelled '"
                    + Component.translatable(translationKey).getString() + "' but found " + matches.size());
        }
        return matches.getFirst();
    }

    /**
     * Every entry of the given kind under an open graph context menu carrying this label. Walks the
     * menu's own subtree rather than querying the UI, which also reaches submenus: one is a child of
     * the branch entry that opened it.
     */
    private static List<UIElement> find(TestContext ctx, String styleClass, String translationKey) {
        var label = Component.translatable(translationKey).getString();
        var out = new ArrayList<UIElement>();
        for (var menu : ctx.all(GRAPH_MENU)) {
            collect(menu.element(), styleClass, label, out);
        }
        return out;
    }

    private static void collect(UIElement element, String styleClass, String label, List<UIElement> out) {
        if (element.hasClass(styleClass) && label.equals(labelOf(element))) out.add(element);
        for (var child : element.getChildren()) {
            collect(child, styleClass, label, out);
        }
    }

    /** The first text found anywhere below this element, at any depth. */
    private static String labelOf(UIElement element) {
        if (element instanceof TextElement text) return text.getText().getString();
        for (var child : element.getChildren()) {
            var text = labelOf(child);
            if (!text.isEmpty()) return text;
        }
        return "";
    }
}
