package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

/** Ctrl+A in the graph view selects every canvas element, and leaves the blackboard alone. */
@LDLRegisterClient(name = "ngt_select_all", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtSelectAllScenario implements UIScenario {

    private static final String LEFT = "left";
    private static final String CANVAS_MODELS = "canvasModels";
    private static final String VARIABLE = "variable";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "selection").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a graph with one of every canvas element", NgtSelectAllScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("both nodes are laid out", ctx -> ctx.count(".__node-element__") == 2)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(250)

                .step("click one node", ctx -> {
                    var title = ElementBounds.of(titleOf(ctx, LEFT));
                    ctx.input().mouseDown(title.centerX(), title.centerY(), Keys.MOUSE_LEFT);
                    ctx.input().mouseUp(title.centerX(), title.centerY(), Keys.MOUSE_LEFT);
                })
                .check("only that node is selected", ctx ->
                        graphView(ctx).getSelected().equals(Set.of(ctx.<Model>get(LEFT))))
                .check("the graph view has the focus", ctx -> graphView(ctx).isFocused())

                .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                .step("every canvas element is selected", ctx -> {
                    var selected = graphView(ctx).getSelected();
                    List<Model> canvas = ctx.get(CANVAS_MODELS);
                    for (var model : canvas) {
                        ctx.check(model.getClass().getSimpleName() + " is selected", selected.contains(model));
                    }
                    ctx.check("the blackboard variable is not", !selected.contains(ctx.<Model>get(VARIABLE)));
                    ctx.check("nothing else is", selected.size() == canvas.size(), canvas.size(), selected.size());
                })
                .screenshot("01_all_selected")
                .closeScreen();
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static UIElement titleOf(TestContext ctx, String key) {
        var element = graphView(ctx).getModelElement(ctx.<Model>get(key));
        if (!(element instanceof NodeElement nodeElement) || nodeElement.getNodeTittle() == null) {
            throw new IllegalStateException("no node title to click for " + key);
        }
        return nodeElement.getNodeTittle();
    }

    private static ModularUI buildGraphUI(TestContext ctx) {
        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        var editor = new GraphView();
        editor.setId("graph");
        editor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        root.addChildren(editor);

        var graph = new TestGraph();
        var model = graph.graphModel;
        var left = model.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var right = model.createNodeModel(new TestAddNode(), new Vector2f(400, 0));
        var out = left.getOutputsById().get("out");
        var in = right.getInputsById().get("in1");
        if (out == null || in == null) {
            throw new IllegalStateException("TestAddNode did not define the expected ports");
        }
        var wire = model.createWire(in, out);
        var note = model.createStickyNote(new Vector2f(0, 300));
        var placemat = model.createPlacemat("Group", new Vector2f(400, 300), new Vector2f(200, 120));
        ctx.put(LEFT, left);
        ctx.put(CANVAS_MODELS, List.<Model>of(left, right, wire, note, placemat));
        ctx.put(VARIABLE, model.createVariable("speed", float.class, 0f, VariableKind.LOCAL));

        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
