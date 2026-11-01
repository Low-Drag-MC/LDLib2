package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.GraphLayoutAlgorithm;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

/**
 * Picking an entry from a context menu hands focus back, so shortcuts keep working afterwards.
 *
 * <p>A menu that closed while it held focus used to leave {@code ModularUI}'s focus on itself after it
 * had been removed, and every key and command was then sent to an element no longer in the tree.
 */
@LDLRegisterClient(name = "menu_close_focus", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class MenuCloseFocusScenario implements UIScenario {

    private static final int NODES = 3;

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("menu", "graph", "ngt").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a small graph", MenuCloseFocusScenario::graphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the nodes are laid out", ctx -> ctx.count(".__node-element__") == NODES)
                .step("frame the graph", ctx -> graph(ctx).fitGraphChildren(80f))
                .settleMs(200)
                .clickAt("an empty spot", MenuCloseFocusScenario::emptySpot, Keys.MOUSE_RIGHT)
                .waitUntil("the context menu is open", ctx -> ctx.count(ContextMenus.GRAPH_MENU) > 0)
                .hover("the Auto Layout entry", ctx -> ContextMenus.branch(ctx, "graph.auto_layout"))
                .click("the Layered entry", ctx -> ContextMenus.leaf(ctx, GraphLayoutAlgorithm.LAYERED.getTranslationKey()),
                        Keys.MOUSE_LEFT)
                .waitUntil("the menu is gone", ctx -> ctx.count(ContextMenus.GRAPH_MENU) == 0)
                .check("focus is not left on the closed menu", ctx -> {
                    UIElement focused = ctx.requireUI().getFocusedElement();
                    ctx.log("focused " + (focused == null ? "nothing" : focused.getClass().getSimpleName()));
                    return focused == null || focused.getModularUI() != null;
                })
                .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                .check("ctrl+A still reaches the graph", ctx -> graph(ctx).getSelected().size() >= NODES)
                .closeScreen();
    }

    private static float[] emptySpot(TestContext ctx) {
        var spot = CanvasPoints.emptySpot(ctx, graph(ctx));
        return new float[]{spot.x, spot.y};
    }

    private static GraphView graph(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static ModularUI graphUI(TestContext ctx) {
        ScenarioPreferences.isolate(ctx, "menu_close_focus");
        var root = new UIElement().setId("root");
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        var view = new GraphView();
        view.setId("graph");
        view.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addChildren(view);

        var graph = new TestGraph();
        var nodes = new NodeModel[NODES];
        for (int i = 0; i < NODES; i++) {
            nodes[i] = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(400 - i * 200, i * 90));
        }
        for (int i = 0; i + 1 < NODES; i++) {
            graph.graphModel.createWire(nodes[i + 1].getInputsById().get("in1"), nodes[i].getOutputsById().get("out"));
        }
        view.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
