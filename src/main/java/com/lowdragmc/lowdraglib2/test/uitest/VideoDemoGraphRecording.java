package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.GraphLayoutAlgorithm;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIRecording;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

/** Recording demo: a node dragged, the graph tidied up from its context menu, the tidy undone, everything selected. Made for a 3840x2160 window. */
@LDLRegisterClient(name = "demo_graph", group = "ldlib2", registry = UIRecording.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class VideoDemoGraphRecording implements UIRecording {

    private static final String[] NODES = {"a", "b", "c", "d", "e"};

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("demo").guiScale(4);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a scrambled graph", VideoDemoGraphRecording::graphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the nodes are laid out", ctx -> ctx.count(".__node-element__") == NODES.length)
                .step("frame the graph", ctx -> graph(ctx).fitGraphChildren(80f))
                .settleMs(250)
                .take("graph", o -> o.size(1920, 1080).burnCaptions("en"), t -> t
                        .caption("Drag a node by its title")
                        .caption("zh", "按住标题拖动节点")
                        .hold(500)
                        .drag("node e by its title", ctx -> titleOf(ctx, "e"), ctx -> {
                            var title = ElementBounds.of(titleOf(ctx, "e"));
                            return new float[]{title.centerX() + 70, title.centerY() + 70};
                        })
                        .hold(300)
                        .step("remember where the nodes are", ctx -> ctx.put("moved", positions(ctx)))
                        .check("the drag moved node e", ctx -> !node(ctx, "e").getPosition().equals(520, 120))
                        // Auto layout arranges just the selection when there is one, and the drag selected the node.
                        .clickAt("an empty spot, to clear the selection", VideoDemoGraphRecording::emptySpot, Keys.MOUSE_LEFT)
                        .caption("Right click the canvas for the context menu")
                        .caption("zh", "在画布上右键打开菜单")
                        .clickAt("an empty spot on the canvas", VideoDemoGraphRecording::emptySpot, Keys.MOUSE_RIGHT)
                        .waitUntil("the context menu is open", ctx -> ctx.count(ContextMenus.GRAPH_MENU) > 0)
                        .zoomTo("the context menu, with room for its submenu", ctx -> {
                            var menu = ctx.el(ContextMenus.GRAPH_MENU).bounds();
                            return new float[]{menu.x(), menu.y(), menu.width() * 2.3f, menu.height() * 1.5f};
                        }, 600)
                        .caption("Auto Layout, Layered tidies the whole graph")
                        .caption("zh", "自动布局 → 分层，一键整理整张图")
                        .hover("the Auto Layout entry", ctx -> ContextMenus.branch(ctx, "graph.auto_layout"))
                        .hold(350)
                        .click("the Layered entry", ctx -> ContextMenus.leaf(ctx, GraphLayoutAlgorithm.LAYERED.getTranslationKey()),
                                Keys.MOUSE_LEFT)
                        .zoomOut(700)
                        .waitUntil("the graph is rearranged", ctx -> !positions(ctx).equals(ctx.get("moved")))
                        .hold(600)
                        .caption("Ctrl+Z undoes it")
                        .caption("zh", "Ctrl+Z 撤销")
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .hold(900)
                        .check("ctrl+Z put every node back where the drag left it", ctx -> {
                            List<Vector2f> moved = ctx.get("moved");
                            var now = positions(ctx);
                            for (int i = 0; i < NODES.length; i++) {
                                if (now.get(i).distance(moved.get(i)) > 0.5f) return false;
                            }
                            return true;
                        })
                        .caption("Ctrl+A selects everything")
                        .caption("zh", "Ctrl+A 全选")
                        .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                        .hold(1200))
                .check("every node is selected", ctx -> graph(ctx).getSelected().size() >= NODES.length)
                .closeScreen();
    }

    private static float[] emptySpot(TestContext ctx) {
        var spot = CanvasPoints.emptySpot(ctx, graph(ctx), 0.45f, 0.8f);
        return new float[]{spot.x, spot.y};
    }

    private static UIElement titleOf(TestContext ctx, String key) {
        var element = graph(ctx).getModelElement(node(ctx, key));
        if (!(element instanceof NodeElement node) || node.getNodeTittle() == null) {
            throw new IllegalStateException("no node title to grab for " + key);
        }
        return node.getNodeTittle();
    }

    private static GraphView graph(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    /** Looked up by id every time: an undo rebuilds the graph's models from a snapshot. */
    private static NodeModel node(TestContext ctx, String key) {
        UUID uid = ctx.get(key);
        for (var node : graph(ctx).getGraph().graphModel.getNodeModels()) {
            if (uid.equals(node.getUid()) && node instanceof NodeModel model) return model;
        }
        throw new IllegalStateException("node " + key + " is not in the graph");
    }

    private static List<Vector2f> positions(TestContext ctx) {
        return java.util.Arrays.stream(NODES).map(key -> new Vector2f(node(ctx, key).getPosition())).toList();
    }

    private static ModularUI graphUI(TestContext ctx) {
        ScenarioPreferences.isolate(ctx, "video_demo");
        var root = new UIElement().setId("root");
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        var view = new GraphView();
        view.setId("graph");
        view.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addChildren(view);

        var graph = new TestGraph();
        var positions = new Vector2f[]{
                new Vector2f(980, 40), new Vector2f(80, 230), new Vector2f(1120, 200), new Vector2f(0, 0), new Vector2f(520, 120)};
        var nodes = new NodeModel[NODES.length];
        for (int i = 0; i < NODES.length; i++) {
            nodes[i] = graph.graphModel.createNodeModel(new TestAddNode(), positions[i]);
            ctx.put(NODES[i], nodes[i].getUid());
        }
        for (int i = 0; i + 1 < nodes.length; i++) {
            graph.graphModel.createWire(nodes[i + 1].getInputsById().get("in1"), nodes[i].getOutputsById().get("out"));
        }
        view.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
