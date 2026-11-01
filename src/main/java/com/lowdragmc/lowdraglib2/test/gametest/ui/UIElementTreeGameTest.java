package com.lowdragmc.lowdraglib2.test.gametest.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;

/** Id lookups after an attached element's id changes, and the child-order hooks. */
public final class UIElementTreeGameTest {
    private static final String ID_CHANGED_AFTER_ATTACH = "ui_element_id_changed_after_attach_is_looked_up_by_the_new_id";
    private static final String REORDER_KEEPS_CHILDREN_ATTACHED = "ui_element_reorder_children_keeps_children_attached";
    private static final String CONTENT_HOST_IS_WHERE_CHILDREN_GO = "ui_element_content_host_is_where_children_go";

    private UIElementTreeGameTest() {
    }

    static void registerFunctions() {
        UIGameTests.registerFunction(ID_CHANGED_AFTER_ATTACH, UIElementTreeGameTest::idChangedAfterAttachIsLookedUpByTheNewId);
        UIGameTests.registerFunction(REORDER_KEEPS_CHILDREN_ATTACHED, UIElementTreeGameTest::reorderChildrenKeepsChildrenAttached);
        UIGameTests.registerFunction(CONTENT_HOST_IS_WHERE_CHILDREN_GO, UIElementTreeGameTest::contentHostIsWhereChildrenGo);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = UIGameTests.defaultTestData(environment, "empty");
        UIGameTests.registerFunctionTest(event, ID_CHANGED_AFTER_ATTACH, UIGameTests.functionKey(ID_CHANGED_AFTER_ATTACH), testData);
        UIGameTests.registerFunctionTest(event, REORDER_KEEPS_CHILDREN_ATTACHED, UIGameTests.functionKey(REORDER_KEEPS_CHILDREN_ATTACHED), testData);
        UIGameTests.registerFunctionTest(event, CONTENT_HOST_IS_WHERE_CHILDREN_GO, UIGameTests.functionKey(CONTENT_HOST_IS_WHERE_CHILDREN_GO), testData);
    }

    private static ModularUI attach(UIElement root) {
        var mui = new ModularUI(UI.of(root), null);
        mui.setMenu(null);
        return mui;
    }

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
