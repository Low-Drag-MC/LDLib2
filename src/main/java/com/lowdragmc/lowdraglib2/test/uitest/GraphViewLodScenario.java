package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
// Two different things are called GraphView: the node editor (imported here) and the generic
// pan/zoom canvas it is built on. Java has no import aliases, so the canvas stays qualified at its
// single use site rather than leaving both ambiguous.
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestConstantNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.joml.Vector2f;

/**
 * Visual and behavioural coverage of the graph canvas' level-of-detail rendering.
 *
 * <p>The LOD thresholds themselves are unit-tested in
 * {@code com.lowdragmc.lowdraglib2.test.ui.GraphViewLodTest}; what cannot be checked headlessly is
 * that each level actually <em>draws</em> — that a simplified node is still a recognisable node, that
 * selection borders survive, and that wires come back when zooming in past the threshold. That last
 * one is the interesting case: block level skips the endpoint resolve entirely, so the geometry has
 * to be rebuilt on the way back up or the wires stay invisible.
 *
 * <p>The screenshots are the deliverable here. Read them; the assertions only prove the canvas
 * reported the level the zoom implies.
 */
@LDLRegisterClient(name = "graph_lod", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class GraphViewLodScenario implements UIScenario {

    private static final int GRID = 6;

    @Override
    public void configure(ScenarioOptions options) {
        // Pinned: the lowest pixel scale tested is only reachable at GUI scale 2, because
        // 0.2 px/unit needs a 0.1 zoom and that is the canvas minimum.
        options.defaultSettleMs(60).tags("graph", "lod", "visual").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("graph editor", ctx -> buildGraphUI(ctx))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                // Nodes are built asynchronously from the model, so wait for the real count rather
                // than guessing at a settle long enough to cover it.
                .waitUntil("all %d nodes are laid out".formatted(GRID * GRID),
                        ctx -> ctx.count(".__node-element__") == GRID * GRID)

                .step("frame the whole graph", ctx -> graphView(ctx).fitGraphChildren(40f))
                .settleMs(150)

                // Zooms are expressed as a target pixel scale and converted, so the scenario means
                // the same thing at any GUI scale - which is the whole point of the LOD keying on
                // pixels rather than on the raw zoom.
                .group("full detail", g -> g
                        .step("zoom to 2.0 px/unit", ctx -> zoomToPixelScale(ctx, 2.0f))
                        .check("canvas reports FULL", ctx -> canvas(ctx).getLod() == GraphViewLod.FULL)
                        .screenshot("01_full"))

                // Just above the simplified threshold: text is small but still legible, and this is
                // the case that used to drop to flat rects far too eagerly.
                .group("still full when small but readable", g -> g
                        .step("zoom to 0.75 px/unit", ctx -> zoomToPixelScale(ctx, 0.75f))
                        .check("0.75 px/unit is still FULL",
                                ctx -> canvas(ctx).getLod() == GraphViewLod.FULL)
                        .screenshot("02_full_small"))

                .group("simplified", g -> g
                        .step("zoom to 0.45 px/unit", ctx -> zoomToPixelScale(ctx, 0.45f))
                        .check("canvas reports SIMPLIFIED",
                                ctx -> canvas(ctx).getLod() == GraphViewLod.SIMPLIFIED)
                        .screenshot("03_simplified"))

                .group("block", g -> g
                        .step("zoom to 0.2 px/unit", ctx -> zoomToPixelScale(ctx, 0.2f))
                        .check("canvas reports BLOCK", ctx -> canvas(ctx).getLod() == GraphViewLod.BLOCK)
                        .screenshot("04_block"))

                // The round trip back up. Wire geometry is skipped entirely at block level, so this
                // is what proves it is rebuilt rather than left stale and invisible.
                .group("back to full", g -> g
                        .step("zoom back to 2.0 px/unit", ctx -> zoomToPixelScale(ctx, 2.0f))
                        .check("canvas reports FULL again", ctx -> canvas(ctx).getLod() == GraphViewLod.FULL)
                        .check("wires still exist", ctx -> ctx.count(".__wire__") > 0)
                        .screenshot("05_full_again"))

                // LOD must be defeatable, or a downstream user who dislikes it has no way out.
                .group("lod disabled", g -> g
                        .step("disable LOD and zoom out", ctx -> {
                            Style.importantPipeline(canvas(ctx).getGraphViewStyle(),
                                    style -> style.lodEnabled(false));
                            zoomToPixelScale(ctx, 0.2f);
                        })
                        .settleMs(150)
                        .check("LOD is off, so the canvas reports FULL at minimum zoom",
                                ctx -> canvas(ctx).getLod() == GraphViewLod.FULL)
                        .screenshot("06_lod_disabled"))

                // Walks the grid straight through a level change. With base 64 and a 14-unit minimum
                // the boundary sits at ~0.875 zoom, so these frames straddle it: the accent lattice
                // must hold still and the subdivision lines must fade, with nothing popping.
                .group("grid level boundary sweep", g -> {
                    float[] zooms = {0.80f, 0.85f, 0.87f, 0.88f, 0.90f, 0.95f};
                    for (int i = 0; i < zooms.length; i++) {
                        float zoom = zooms[i];
                        g.step("zoom %.2f".formatted(zoom), ctx -> canvas(ctx).setScale(zoom))
                                .settleMs(80)
                                .screenshot("07_grid_sweep_%d_zoom%03d".formatted(i, (int) (zoom * 100)));
                    }
                })

                .closeScreen();
    }

    /**
     * Sets the zoom so one content unit covers the given number of physical screen pixels. Also
     * asserts the canvas agrees, which catches a GUI-scale mismatch between the harness and the view.
     */
    private static void zoomToPixelScale(TestContext ctx, float targetPixelScale) {
        var canvas = canvas(ctx);
        float guiScale = (float) ctx.mc().getWindow().getGuiScale();
        canvas.setScale(targetPixelScale / guiScale);
        ctx.check("pixel scale is ~%.2f".formatted(targetPixelScale),
                Math.abs(canvas.getPixelScale() - targetPixelScale) < 0.02f,
                targetPixelScale, canvas.getPixelScale());
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    /** The generic pan/zoom canvas inside the node editor — the thing that owns the zoom and the LOD. */
    private static com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView canvas(TestContext ctx) {
        return graphView(ctx).graphView;
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
        editor.loadGraph(buildGrid());
        return new ModularUI(UI.of(root), ctx.player());
    }

    /**
     * A grid of connected nodes, big enough that the difference between levels is obvious in a
     * screenshot and that skipping the subtree walk is worth anything.
     */
    private static Graph buildGrid() {
        var graph = new TestGraph();
        var nodes = new NodeModel[GRID][GRID];
        for (int row = 0; row < GRID; row++) {
            for (int column = 0; column < GRID; column++) {
                var node = column == 0
                        ? graph.graphModel.createNodeModel(new TestConstantNode(),
                                new Vector2f(column * 220f, row * 150f))
                        : graph.graphModel.createNodeModel(new TestAddNode(),
                                new Vector2f(column * 220f, row * 150f));
                nodes[row][column] = node;
            }
        }
        for (int row = 0; row < GRID; row++) {
            for (int column = 1; column < GRID; column++) {
                var from = nodes[row][column - 1].getOutputsById().get("out");
                var to = nodes[row][column].getInputsById().get("in1");
                if (from != null && to != null) {
                    graph.graphModel.createWire(from, to);
                }
            }
        }
        return graph;
    }
}
