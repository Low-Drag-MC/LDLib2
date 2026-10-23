package com.lowdragmc.lowdraglib2.test;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Id lookups after an attached element's id changes, and the child-order hooks. */
@GameTestHolder(LDLib2.MOD_ID)
public class UIElementTreeTest {

    private static ModularUI attach(UIElement root) {
        var mui = new ModularUI(UI.of(root), null);
        mui.setMenu(null);
        return mui;
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void idChangedAfterAttachIsLookedUpByTheNewId(GameTestHelper helper) {
        var child = new UIElement().setId("before");
        var root = new UIElement().addChild(child);
        var mui = attach(root);
        if (mui.getElementById("before") != child) {
            helper.fail("not found by its id once attached");
            return;
        }
        child.setId("after");
        if (mui.getElementById("before") != null) {
            helper.fail("still found by its old id");
            return;
        }
        if (mui.getElementById("after") != child) {
            helper.fail("not found by its new id");
            return;
        }
        child.setId(null);
        if (mui.getElementById("after") != null) {
            helper.fail("still found after its id was cleared");
            return;
        }
        child.setId("again");
        root.removeChild(child);
        if (mui.getElementById("again") != null) {
            helper.fail("still found after it was removed");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void reorderChildrenKeepsChildrenAttached(GameTestHelper helper) {
        var a = new UIElement().setId("a");
        var b = new UIElement().setId("b");
        var c = new UIElement().setId("c");
        var root = new UIElement().addChildren(a, b, c);
        var mui = attach(root);

        root.reorderChildren(List.of(c, a, b));
        if (!root.getChildren().equals(List.of(c, a, b))) {
            helper.fail("children not in the new order: " + root.getChildren());
            return;
        }
        if (a.getParent() != root || a.getModularUI() != mui || mui.getElementById("a") != a) {
            helper.fail("a reordered child was detached");
            return;
        }
        try {
            root.reorderChildren(List.of(a, b));
            helper.fail("accepted an order missing a child");
            return;
        } catch (IllegalArgumentException expected) {
        }
        try {
            root.reorderChildren(List.of(a, a, b));
            helper.fail("accepted a child listed twice");
            return;
        } catch (IllegalArgumentException expected) {
        }
        if (!root.getChildren().equals(List.of(c, a, b))) {
            helper.fail("a refused order changed the children: " + root.getChildren());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void contentHostIsWhereChildrenGo(GameTestHelper helper) {
        var plain = new UIElement();
        if (plain.getContentHost() != plain) {
            helper.fail("a plain element's content host is not itself");
            return;
        }
        var scroller = new ScrollerView();
        if (scroller.getContentHost() != scroller.viewContainer) {
            helper.fail("a scroller view's content host is not its view container");
            return;
        }
        helper.succeed();
    }
}
