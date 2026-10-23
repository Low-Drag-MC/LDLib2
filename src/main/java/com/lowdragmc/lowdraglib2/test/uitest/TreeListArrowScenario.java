package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.uitest.TreeListRediffScenario.Node;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

import java.util.List;

/**
 * A {@link TreeList} row's arrow takes its own clicks: a click on it opens or folds the row without selecting it, a double
 * click on it turns the row twice and is no double click on the row. The row itself still selects, and a leaf's empty arrow
 * is part of its row. With click to expand the arrow is part of its row too, but for a branch the filter turns away.
 */
@LDLRegisterClient(name = "tree_list_arrow", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class TreeListArrowScenario implements UIScenario {

    private static final String TREE = "tree_list";
    private static final String ROOT = "tree_root";
    private static final String SELECTIONS = "tree_selections";
    private static final String DOUBLE_CLICKS = "tree_double_clicks";
    private static final String A_OPEN = "tree_a_open";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("tree_list").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("tree list", ctx -> new ModularUI(UI.of(root(ctx)), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#arrow_A")
                .check("A is folded", ctx -> !ctx.exists("#row_A1"))

                .group("a click on the arrow turns the row and selects nothing", g -> g
                        .click("#arrow_A")
                        .frames(3)
                        .check("A opened", ctx -> ctx.exists("#row_A1"))
                        .check("nothing selected, nobody told", ctx -> selectedKeys(ctx).isEmpty() && count(ctx, SELECTIONS) == 0)
                        // apart, so the two clicks are no double click
                        .waitMs(400)
                        .click("#arrow_A")
                        .frames(3)
                        .check("A folded", ctx -> !ctx.exists("#row_A1"))
                        .check("still nothing selected", ctx -> selectedKeys(ctx).isEmpty() && count(ctx, SELECTIONS) == 0))

                .group("a double click on the arrow turns the row twice and is no double click on the row", g -> g
                        .waitMs(400)
                        .doubleClick("#arrow_A")
                        .frames(3)
                        .check("A opened and folded again", ctx -> !ctx.exists("#row_A1") && !tree(ctx).isNodeExpanded(a(ctx)))
                        .check("the row was not double-clicked", ctx -> count(ctx, DOUBLE_CLICKS) == 0)
                        .check("nothing selected", ctx -> selectedKeys(ctx).isEmpty()))

                .group("the row still selects, and a double click on it turns it", g -> g
                        .waitMs(400)
                        .click("#label_A")
                        .frames(3)
                        .check("A selected", ctx -> selectedKeys(ctx).equals(List.of("A")) && count(ctx, SELECTIONS) == 1)
                        .waitMs(400)
                        .doubleClick("#label_A")
                        .frames(3)
                        .check("A opened by the double click", ctx -> ctx.exists("#row_A1") && count(ctx, DOUBLE_CLICKS) == 1))

                .group("a leaf's arrow has nothing to turn: a click there is a click on its row", g -> g
                        .waitMs(400)
                        .click("#arrow_B")
                        .frames(3)
                        .check("B selected", ctx -> selectedKeys(ctx).equals(List.of("B"))))

                .group("with click to expand, a click on the arrow is a click on the row, but for a branch the filter turns away", g -> g
                        .waitMs(400)
                        .step("click to expand, all but C", ctx -> {
                            tree(ctx).setClickToExpand(true).setClickToExpandFilter(node -> !node.key.equals("C"));
                            ctx.put(A_OPEN, new boolean[]{tree(ctx).isNodeExpanded(a(ctx))});
                        })
                        .click("#arrow_A")
                        .frames(3)
                        .check("A turned once and selected", ctx -> tree(ctx).isNodeExpanded(a(ctx)) != ((boolean[]) ctx.get(A_OPEN))[0]
                                && selectedKeys(ctx).equals(List.of("A")))
                        .waitMs(400)
                        .click("#arrow_C")
                        .frames(3)
                        .check("C opened by its arrow, nothing else selected", ctx -> ctx.exists("#row_C1") && selectedKeys(ctx).equals(List.of("A")))
                        .waitMs(400)
                        .click("#label_C")
                        .frames(3)
                        .check("C selected by its row and left open", ctx -> ctx.exists("#row_C1") && selectedKeys(ctx).equals(List.of("C"))))

                .teardown("close the screen", ctx -> ctx.mc().setScreen(null));
    }

    // ------------------------------------------------------------------------------------ fixtures

    private static UIElement root(TestContext ctx) {
        var model = new Node(null, "root");
        model.add("A").add("A1");
        model.add("B");
        model.add("C").add("C1");
        ctx.put(ROOT, model);
        ctx.put(SELECTIONS, new int[1]);
        ctx.put(DOUBLE_CLICKS, new int[1]);

        var tree = new TreeList<Node>();
        tree.setFlattenRoot(true)
                .setSupportMultipleSelection(true)
                .setDoubleClickToExpand(true)
                .setOnNodeUICreated((node, ui) -> {
                    ui.setId("row_" + node.key);
                    ui.getChildren().getFirst().setId("arrow_" + node.key);
                    ui.getChildren().get(1).setId("label_" + node.key);
                })
                .setOnSelectedChanged(selected -> ((int[]) ctx.get(SELECTIONS))[0]++)
                .setOnDoubleClickNode(node -> ((int[]) ctx.get(DOUBLE_CLICKS))[0]++)
                .setRoot(model);
        tree.layout(layout -> layout.width(160));
        ctx.put(TREE, tree);

        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(20))
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF101014)))
                .addChild(tree);
        return root;
    }

    // ------------------------------------------------------------------------------------- helpers

    private static TreeList<Node> tree(TestContext ctx) {
        return ctx.get(TREE);
    }

    private static Node a(TestContext ctx) {
        Node root = ctx.get(ROOT);
        return root.children.getFirst();
    }

    private static List<String> selectedKeys(TestContext ctx) {
        return tree(ctx).getSelected().stream().map(node -> node.key).toList();
    }

    private static int count(TestContext ctx, String key) {
        int[] count = ctx.get(key);
        return count == null ? -1 : count[0];
    }
}
