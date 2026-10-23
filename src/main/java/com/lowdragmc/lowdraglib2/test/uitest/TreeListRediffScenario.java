package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A {@link TreeList} follows a model that changes under it: with the root flattened the top rows follow the root's
 * children too, and a re-diff keeps the selection of the rows that are still there while dropping the rows that went.
 */
@LDLRegisterClient(name = "tree_list_rediff", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class TreeListRediffScenario implements UIScenario {

    private static final String TREE = "tree_list";
    private static final String ROOT = "tree_root";
    private static final String NOTIFIED = "tree_notified";

    /** A node whose children are changed in place. */
    static final class Node implements ITreeNode<String, String> {
        final String key;
        @Nullable
        final Node parent;
        final List<Node> children = new ArrayList<>();

        Node(@Nullable Node parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        Node add(String child) {
            var node = new Node(this, child);
            children.add(node);
            return node;
        }

        @Override
        public int getDimension() {
            return parent == null ? 0 : parent.getDimension() + 1;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getContent() {
            return key;
        }

        @Override
        public @Nullable Node getParent() {
            return parent;
        }

        @Override
        public List<Node> getChildren() {
            return children;
        }
    }

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("tree_list").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("tree list", ctx -> new ModularUI(UI.of(root(ctx)), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#row_A")
                .check("the top rows are shown", ctx -> ctx.exists("#row_B") && !ctx.exists("#row_A1"))

                .group("a node added to the flattened root shows as a top row", g -> g
                        .step("add C to the root", ctx -> model(ctx).add("C"))
                        .frames(3)
                        .check("C's row is there", ctx -> ctx.exists("#row_C"))
                        .step("take C away", ctx -> model(ctx).children.removeIf(node -> node.key.equals("C")))
                        .frames(3)
                        .check("and gone", ctx -> !ctx.exists("#row_C")))

                .group("a re-diff keeps the selection of the rows that stay", g -> g
                        .step("open A, select A1", ctx -> {
                            var a = model(ctx).children.getFirst();
                            tree(ctx).expandNode(a);
                            tree(ctx).setSelected(Set.of(a.children.getFirst()), false);
                            ctx.put(NOTIFIED, new int[1]);
                        })
                        .frames(3)
                        .step("add A2 beside A1", ctx -> model(ctx).children.getFirst().add("A2"))
                        .frames(3)
                        .check("A2's row is there", ctx -> ctx.exists("#row_A2"))
                        .check("A1 is still selected", ctx -> selectedKeys(ctx).equals(List.of("A1")))
                        .check("and nobody was told the selection changed", ctx -> notified(ctx) == 0)
                        .step("select B as well", ctx -> {
                            var b = model(ctx).children.get(1);
                            var selected = new ArrayList<Node>(tree(ctx).getSelected());
                            selected.add(b);
                            tree(ctx).setSelected(selected, false);
                        })
                        .step("add D to the root", ctx -> model(ctx).add("D"))
                        .frames(3)
                        .check("D's row is there", ctx -> ctx.exists("#row_D"))
                        .check("A1 and B are still selected", ctx -> selectedKeys(ctx).equals(List.of("A1", "B"))))

                .group("a re-diff drops the selection of the rows that went", g -> g
                        .step("take A1 away", ctx -> model(ctx).children.getFirst().children.removeFirst())
                        .frames(3)
                        .check("its row is gone", ctx -> !ctx.exists("#row_A1"))
                        .check("only B is selected", ctx -> selectedKeys(ctx).equals(List.of("B")))
                        .check("told once", ctx -> notified(ctx) == 1))

                .teardown("close the screen", ctx -> ctx.mc().gui.setScreen(null));
    }

    // ------------------------------------------------------------------------------------ fixtures

    private static UIElement root(TestContext ctx) {
        var model = new Node(null, "root");
        model.add("A").add("A1");
        model.add("B");
        ctx.put(ROOT, model);

        var tree = new TreeList<Node>();
        tree.setFlattenRoot(true)
                .setSupportMultipleSelection(true)
                .setOnNodeUICreated((node, ui) -> ui.setId("row_" + node.key))
                .setOnSelectedChanged(selected -> {
                    int[] notified = ctx.get(NOTIFIED);
                    if (notified != null) {
                        notified[0]++;
                    }
                })
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

    private static Node model(TestContext ctx) {
        return ctx.get(ROOT);
    }

    private static List<String> selectedKeys(TestContext ctx) {
        return tree(ctx).getSelected().stream().map(node -> node.key).toList();
    }

    private static int notified(TestContext ctx) {
        int[] notified = ctx.get(NOTIFIED);
        return notified == null ? -1 : notified[0];
    }
}
