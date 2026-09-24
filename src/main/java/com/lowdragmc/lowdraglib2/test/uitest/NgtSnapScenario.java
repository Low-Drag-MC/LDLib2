package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphViewPreferences;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap.SnapAnchor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire.WireRouteStyle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Model;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
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
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

/**
 * End-to-end coverage of drag snapping: the grid taking a node by whichever edge is nearest, and
 * alignment to the edges and centres of nearby elements with a guide drawn through the match.
 *
 * <p>{@code SnapEngineTest} pins the geometry down. What only a live editor can answer is whether
 * the engine is fed the right things:</p>
 * <ul>
 *     <li>The rectangle, not just the corner. The node's measured size has to reach the engine, or
 *     "snap by the far edge" silently degrades to the old top-left rounding.</li>
 *     <li>One offset for the whole selection. Snapping each element separately pulls them towards
 *     different lines and quietly changes the gaps between them — dragging a tidy row used to
 *     un-tidy it, and nothing about the result looked wrong enough to notice.</li>
 *     <li>The preview and the drop agreeing. The guides are drawn from the live drag, the model is
 *     written on release; if those two derive the position differently the node lands somewhere the
 *     user was never shown.</li>
 * </ul>
 */
@LDLRegisterClient(name = "ngt_snap", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtSnapScenario implements UIScenario {

    /** How far off the neighbour's edge the drag aims, in content units. Inside the grab range. */
    private static final float MISS_BY = 3f;

    private static final String ANCHOR = "anchor";
    private static final String MOVER = "mover";
    private static final String COLUMN_MATE = "columnMate";
    private static final String FAR = "far";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "snap", "visual").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        var gap = new float[1];

        s.openModularUI("a graph with one node out of column", NgtSnapScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the four nodes are laid out", ctx -> ctx.count(".__node-element__") == 4)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(250)

                .group("baseline", g -> g
                        .check("both snapping modes are on", ctx ->
                                graphView(ctx).isSnapToGrid() && graphView(ctx).isSnapToElements())
                        .check("nothing is dragging, so there are no guides", ctx ->
                                graphView(ctx).getSnapGuides().isEmpty())
                        .screenshot("01_start"))

                // The grid, on its own. Checked through the view rather than through a drag because
                // it is the arithmetic that matters here, and a real drag cannot place the cursor
                // on a chosen sub-pixel.
                .group("the grid takes the node by whichever edge is nearest", g -> g
                        .step("turn element alignment off", ctx -> graphView(ctx).setSnapToElements(false))
                        .step("an arbitrary nudge still lands on the grid", ctx -> {
                            var view = graphView(ctx);
                            var node = node(ctx, MOVER);
                            var offset = view.resolveDragOffset(List.of((Model) node), new Vector2f(37.4f, 21.9f), false);
                            var rect = view.contentRectOf(node);
                            ctx.require("the node has a measured rectangle", rect != null);
                            var x = rect.x + offset.x;
                            var y = rect.y + offset.y;
                            var grid = view.getGridSnapSize();
                            ctx.check("some x anchor sits exactly on a grid line",
                                    onGrid(x, rect.z, grid), "a multiple of " + grid,
                                    "%.2f / %.2f / %.2f".formatted(x, x + rect.z / 2, x + rect.z));
                            ctx.check("some y anchor sits exactly on a grid line",
                                    onGrid(y, rect.w, grid), "a multiple of " + grid,
                                    "%.2f / %.2f / %.2f".formatted(y, y + rect.w / 2, y + rect.w));
                            // Rounding by the nearest of three anchors can never move further than
                            // half a cell; rounding by the corner alone regularly did.
                            ctx.check("the correction is at most half a cell",
                                    Math.abs(offset.x - 37.4f) <= grid / 2 + 0.01f
                                            && Math.abs(offset.y - 21.9f) <= grid / 2 + 0.01f,
                                    "<= " + grid / 2,
                                    "%.2f / %.2f".formatted(offset.x - 37.4f, offset.y - 21.9f));
                        })
                        .check("the grid draws no guides", ctx -> graphView(ctx).getSnapGuides().isEmpty())
                        .step("turn element alignment back on", ctx -> graphView(ctx).setSnapToElements(true)))

                .group("dragging near a neighbour's edge lines up with it", g -> {
                    var aim = new float[2];
                    var from = new float[2];
                    g.step("aim just short of the column", ctx -> {
                                var mover = node(ctx, MOVER);
                                var anchor = node(ctx, ANCHOR);
                                var wanted = new Vector2f(
                                        anchor.getPosition().x + MISS_BY - mover.getPosition().x, 0f);
                                var handle = ElementBounds.of(titleOf(ctx, MOVER));
                                from[0] = handle.centerX();
                                from[1] = handle.centerY();
                                var screen = contentToScreenDelta(ctx, wanted);
                                aim[0] = from[0] + screen.x;
                                aim[1] = from[1] + screen.y;
                                ctx.input().moveTo(from[0], from[1]);
                            })
                            .step("press", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT))
                            .step("drag halfway", ctx -> ctx.input().dragTo(
                                    Mth.lerp(0.5f, from[0], aim[0]), Mth.lerp(0.5f, from[1], aim[1]), Keys.MOUSE_LEFT))
                            .step("arrive", ctx -> ctx.input().dragTo(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .settleMs(120)
                            .step("a guide is showing through the column", ctx -> {
                                var guides = graphView(ctx).getSnapGuides();
                                ctx.check("exactly one guide", guides.size() == 1, 1, guides.size());
                                var guide = guides.getFirst();
                                ctx.check("it is vertical", guide.vertical());
                                ctx.check("it runs through the neighbour's left edge",
                                        Math.abs(guide.position() - node(ctx, ANCHOR).getPosition().x) < 0.5f,
                                        node(ctx, ANCHOR).getPosition().x, guide.position());
                                ctx.check("it says which edge lined up",
                                        guide.anchor() == SnapAnchor.START, SnapAnchor.START, guide.anchor());
                                // Both column members and the dragged node are on this line, so the
                                // guide has to reach all three — that is what makes it readable.
                                var mate = node(ctx, COLUMN_MATE);
                                ctx.check("it spans the whole column",
                                        guide.end() >= mate.getPosition().y, ">= " + mate.getPosition().y, guide.end());
                            })
                            .screenshot("02_guide_while_dragging")
                            .step("drop", ctx -> ctx.input().mouseUp(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .settleMs(150)
                            .step("the node landed exactly on the column", ctx -> {
                                var mover = node(ctx, MOVER).getPosition().x;
                                var anchor = node(ctx, ANCHOR).getPosition().x;
                                ctx.check("left edges match to the pixel",
                                        Math.abs(mover - anchor) < 0.01f, anchor, mover);
                            })
                            .check("the guides are cleared once the drag ends", ctx ->
                                    graphView(ctx).getSnapGuides().isEmpty())
                            .screenshot("03_snapped");
                })

                // The bug that was hiding inside per-element snapping: two nodes dragged together
                // were each pulled towards their own nearest line, so the gap between them changed.
                .group("dragging several elements keeps their spacing exactly", g -> {
                    var aim = new float[2];
                    var from = new float[2];
                    g.step("remember the gap and select both", ctx -> {
                                gap[0] = node(ctx, FAR).getPosition().x - node(ctx, ANCHOR).getPosition().x;
                                var view = graphView(ctx);
                                view.clearAllSelected();
                                view.addSelected(node(ctx, ANCHOR));
                                view.addSelected(node(ctx, FAR));
                            })
                            .step("aim at an untidy offset", ctx -> {
                                var handle = ElementBounds.of(titleOf(ctx, ANCHOR));
                                from[0] = handle.centerX();
                                from[1] = handle.centerY();
                                var screen = contentToScreenDelta(ctx, new Vector2f(53.5f, 29.5f));
                                aim[0] = from[0] + screen.x;
                                aim[1] = from[1] + screen.y;
                                ctx.input().moveTo(from[0], from[1]);
                            })
                            .step("press", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT))
                            .step("drag halfway", ctx -> ctx.input().dragTo(
                                    Mth.lerp(0.5f, from[0], aim[0]), Mth.lerp(0.5f, from[1], aim[1]), Keys.MOUSE_LEFT))
                            .step("arrive", ctx -> ctx.input().dragTo(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .step("drop", ctx -> ctx.input().mouseUp(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .settleMs(150)
                            .step("the gap between them is unchanged", ctx -> {
                                var now = node(ctx, FAR).getPosition().x - node(ctx, ANCHOR).getPosition().x;
                                ctx.check("the two nodes moved as one", Math.abs(now - gap[0]) < 0.01f,
                                        gap[0], now);
                            })
                            .step("and they actually moved", ctx -> ctx.check(
                                    "the drag was not a no-op",
                                    Math.abs(node(ctx, ANCHOR).getPosition().y) > 1f,
                                    "!= 0", node(ctx, ANCHOR).getPosition().y))
                            .screenshot("04_multi_drag");
                })

                // The escape hatch. Without one, snapping is a cage rather than a help.
                .group("holding the override key places it exactly where the cursor is", g -> {
                    var aim = new float[2];
                    var from = new float[2];
                    var target = new float[1];
                    g.step("select only the stray node", ctx -> {
                                var view = graphView(ctx);
                                view.clearAllSelected();
                                view.addSelected(node(ctx, MOVER));
                            })
                            .step("aim just short of the column again", ctx -> {
                                var mover = node(ctx, MOVER);
                                var anchor = node(ctx, ANCHOR);
                                // Start from somewhere off the column so there is a snap to refuse.
                                mover.setPosition(new Vector2f(anchor.getPosition().x + 260f,
                                        mover.getPosition().y));
                                target[0] = anchor.getPosition().x + MISS_BY;
                            })
                            .settleMs(150)
                            .step("take hold of it", ctx -> {
                                var mover = node(ctx, MOVER);
                                var wanted = new Vector2f(target[0] - mover.getPosition().x, 0f);
                                var handle = ElementBounds.of(titleOf(ctx, MOVER));
                                from[0] = handle.centerX();
                                from[1] = handle.centerY();
                                var screen = contentToScreenDelta(ctx, wanted);
                                aim[0] = from[0] + screen.x;
                                aim[1] = from[1] + screen.y;
                                ctx.input().moveTo(from[0], from[1]);
                            })
                            .step("hold the override key",
                                    ctx -> ctx.input().keyDown(InputConstants.KEY_LALT, Keys.MOD_ALT))
                            .step("press", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT))
                            .step("drag halfway", ctx -> ctx.input().dragTo(
                                    Mth.lerp(0.5f, from[0], aim[0]), Mth.lerp(0.5f, from[1], aim[1]), Keys.MOUSE_LEFT))
                            .step("arrive", ctx -> ctx.input().dragTo(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .settleMs(120)
                            .check("no guide is offered while overridden", ctx ->
                                    graphView(ctx).getSnapGuides().isEmpty())
                            .step("drop", ctx -> ctx.input().mouseUp(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .step("release the override key",
                                    ctx -> ctx.input().keyUp(InputConstants.KEY_LALT, 0))
                            .settleMs(150)
                            .step("it stayed where it was put", ctx -> {
                                var mover = node(ctx, MOVER).getPosition().x;
                                var anchor = node(ctx, ANCHOR).getPosition().x;
                                ctx.check("it did not get pulled onto the column",
                                        Math.abs(mover - anchor) > 1f,
                                        "not " + anchor, mover);
                            })
                            .screenshot("05_override");
                })

                .group("turning alignment off stops the guides entirely", g -> {
                    var aim = new float[2];
                    var from = new float[2];
                    g.step("turn element alignment off", ctx -> graphView(ctx).setSnapToElements(false))
                            .step("aim at the column", ctx -> {
                                var mover = node(ctx, MOVER);
                                var anchor = node(ctx, ANCHOR);
                                var wanted = new Vector2f(
                                        anchor.getPosition().x + MISS_BY - mover.getPosition().x, 0f);
                                var handle = ElementBounds.of(titleOf(ctx, MOVER));
                                from[0] = handle.centerX();
                                from[1] = handle.centerY();
                                var screen = contentToScreenDelta(ctx, wanted);
                                aim[0] = from[0] + screen.x;
                                aim[1] = from[1] + screen.y;
                                ctx.input().moveTo(from[0], from[1]);
                            })
                            .step("press", ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT))
                            .step("arrive", ctx -> ctx.input().dragTo(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .settleMs(120)
                            .check("no guides", ctx -> graphView(ctx).getSnapGuides().isEmpty())
                            .step("drop", ctx -> ctx.input().mouseUp(aim[0], aim[1], Keys.MOUSE_LEFT))
                            .step("restore the setting", ctx -> graphView(ctx).setSnapToElements(true));
                })

                // The setup follows the graph type across opens, through a file that is neither the
                // graph nor the client config. Runs last: the reload at the end throws away every
                // model handle the groups above are holding.
                .group("the setup is remembered per graph type", g -> g
                        .step("change every remembered setting", ctx -> {
                            var view = graphView(ctx);
                            view.setSnapToGrid(false);
                            view.setGridSnapSize(24f);
                            view.setSnapToElements(false);
                            view.setWireRouteStyle(WireRouteStyle.OCTILINEAR);
                        })
                        .step("the store has it, filed under the graph's class", ctx ->
                                checkStored(ctx, "in memory"))
                        .step("and so does the file", ctx -> {
                            // Re-pointing at the same path drops what was read earlier, so anything
                            // that survives this came off disk rather than out of the cache.
                            GraphViewPreferences.setFile(GraphViewPreferences.getFile());
                            checkStored(ctx, "on disk");
                        })
                        .step("another graph type is unaffected", ctx -> {
                            var other = GraphViewPreferences.get(NgtSnapScenario.class);
                            ctx.check("an untouched type still reads the defaults",
                                    other.equals(GraphViewPreferences.Entry.DEFAULTS),
                                    GraphViewPreferences.Entry.DEFAULTS, other);
                        })
                        .step("put the view back to the defaults by hand", ctx ->
                                graphView(ctx).applyPreferences(GraphViewPreferences.Entry.DEFAULTS))
                        .check("the view really is back to the defaults", ctx ->
                                graphView(ctx).isSnapToGrid()
                                        && graphView(ctx).getWireRouteStyle() == WireRouteStyle.DEFAULT)
                        .step("reopening the graph restores the remembered setup", ctx ->
                                graphView(ctx).loadGraph(new TestGraph()))
                        .settleMs(200)
                        .step("every setting came back", ctx -> {
                            var view = graphView(ctx);
                            ctx.check("snap to grid", !view.isSnapToGrid(), false, view.isSnapToGrid());
                            ctx.check("grid size", view.getGridSnapSize() == 24f, 24f, view.getGridSnapSize());
                            ctx.check("snap to elements", !view.isSnapToElements(), false, view.isSnapToElements());
                            ctx.check("wire style", view.getWireRouteStyle() == WireRouteStyle.OCTILINEAR,
                                    WireRouteStyle.OCTILINEAR, view.getWireRouteStyle());
                        })
                        .screenshot("06_restored"))

                .step("hand the preference store back to the real config",
                        ctx -> ScenarioPreferences.restore())
                .closeScreen();
    }

    // region helpers

    /** Asserts the store holds what the view was just set to, for {@code TestGraph}. */
    private static void checkStored(TestContext ctx, String where) {
        var entry = GraphViewPreferences.get(TestGraph.class);
        var wanted = new GraphViewPreferences.Entry(false, 24f, false, WireRouteStyle.OCTILINEAR);
        ctx.check("the remembered setup matches " + where, entry.equals(wanted), wanted, entry);
    }

    /** Whether any of the three anchors of a span starting at {@code start} lands on a grid line. */
    private static boolean onGrid(float start, float size, float grid) {
        return isMultiple(start, grid) || isMultiple(start + size / 2f, grid) || isMultiple(start + size, grid);
    }

    private static boolean isMultiple(float value, float grid) {
        return Math.abs(value - Math.round(value / grid) * grid) < 0.01f;
    }

    /**
     * How many screen pixels a content-space delta is worth right now.
     *
     * <p>Probed through the canvas's own screen-to-content conversion and inverted, rather than
     * multiplying by the zoom: the zoom is only one link in the transform chain, and the gui scale
     * sits on the other side of it.</p>
     */
    private static Vector2f contentToScreenDelta(TestContext ctx, Vector2f contentDelta) {
        var probe = graphView(ctx).getContentViewContainer().getLocalMouseNormal(100f, 100f);
        if (Math.abs(probe.x) < 1e-4f || Math.abs(probe.y) < 1e-4f) {
            throw new IllegalStateException("the canvas reports a degenerate transform: " + probe);
        }
        return new Vector2f(contentDelta.x * 100f / probe.x, contentDelta.y * 100f / probe.y);
    }

    private static UIElement titleOf(TestContext ctx, String key) {
        var element = graphView(ctx).getModelElement(node(ctx, key));
        if (!(element instanceof NodeElement nodeElement) || nodeElement.getNodeTittle() == null) {
            throw new IllegalStateException("no node title to grab for " + key);
        }
        return nodeElement.getNodeTittle();
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static GraphModel graphModel(TestContext ctx) {
        var graph = graphView(ctx).getGraph();
        if (graph == null) throw new IllegalStateException("no graph loaded");
        return graph.graphModel;
    }

    private static NodeModel node(TestContext ctx, String key) {
        return ctx.get(key);
    }

    // endregion

    private static ModularUI buildGraphUI(TestContext ctx) {
        // Before loadGraph below, which is what reads the remembered setup.
        ScenarioPreferences.isolate(ctx, "ngt_snap");
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

        // A column of two left-aligned nodes, a stray one to drag into it, and a far one used for
        // the multi-drag. No wires: this is about geometry, and wires would only add noise.
        var graph = new TestGraph();
        ctx.put(ANCHOR, graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0)));
        ctx.put(COLUMN_MATE, graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 700)));
        ctx.put(MOVER, graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(420, 320)));
        ctx.put(FAR, graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(900, 0)));

        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
