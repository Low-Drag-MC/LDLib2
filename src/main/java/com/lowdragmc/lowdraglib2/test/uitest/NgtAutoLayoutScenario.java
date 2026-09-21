package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.LayoutCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout.GraphLayoutAlgorithm;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.PlacematElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
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
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * End-to-end coverage of "Auto Layout", including the two things the pure layout tests cannot see:
 * that the wires in a real graph are translated into the right edges, and that placemats are handled
 * the two different ways the gesture implies.
 *
 * <p>The graph is deliberately scrambled to start — the source node is the rightmost and the sink is
 * top-left — so "it was already laid out" cannot pass for a result.</p>
 *
 * <p>The load-bearing assertions, in order of how badly each one fails silently:</p>
 * <ul>
 *     <li>Every wire ends up pointing forwards: the source's box ends before the target's begins.
 *     One wrong port-to-node mapping and the layers are built from the wrong graph, which still
 *     produces a tidy-looking picture of nothing in particular.</li>
 *     <li>A selected placemat moves as one piece: its contents keep their exact offsets.</li>
 *     <li>A placemat whose contents were laid out grows to hold the result, and never shrinks.</li>
 *     <li>Undo puts every position back, which is what makes the whole thing safe to try.</li>
 * </ul>
 */
@LDLRegisterClient(name = "ngt_auto_layout", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtAutoLayoutScenario implements UIScenario {

    private static final String[] CHAIN = {"n0", "n1", "n2", "n3"};
    private static final String PAIR_HEAD = "p0";
    private static final String PAIR_TAIL = "p1";
    private static final String PLACEMAT = "placemat";
    /** Gap left around the pair when the tight placemat is built, in content units. */
    private static final float HUG = 8f;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "layout", "visual").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        var placematRectBefore = new Vector4f();
        var pairOffsets = new Vector2f[2];
        var signature = new Object[1];

        s.openModularUI("a deliberately scrambled graph", NgtAutoLayoutScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the six nodes are laid out", ctx -> ctx.count(".__node-element__") == 6)
                .waitUntil("the five wires are laid out", ctx -> ctx.count(".__wire__") == 5)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(200)

                .group("baseline", g -> g
                        .check("the graph really is scrambled", ctx ->
                                node(ctx, "n0").getPosition().x > node(ctx, "n3").getPosition().x)
                        .screenshot("01_scrambled"))

                // Driven through the real menu once, so the entry existing and being wired to the
                // command is covered rather than assumed. Nothing is selected, so this arranges the
                // whole graph.
                .group("auto layout the whole graph from the contextual menu", g -> g
                        .step("nothing is selected", ctx -> graphView(ctx).clearAllSelected())
                        .step("right-click empty canvas", NgtAutoLayoutScenario::rightClickEmptyCanvas)
                        .waitUntil("the contextual menu is open",
                                ctx -> ctx.count(".__node-graph-view_context-menu__") > 0)
                        .step("the menu offers an auto layout branch", ctx -> ctx.check(
                                "an 'Auto Layout' branch exists",
                                ContextMenus.branchCount(ctx, "graph.auto_layout") == 1))
                        .step("hover the branch to open its submenu",
                                ctx -> ContextMenus.openBranch(ctx, "graph.auto_layout"))
                        .settleMs(120)
                        .step("the submenu lists every algorithm", ctx -> {
                            for (var algorithm : GraphLayoutAlgorithm.values()) {
                                ctx.check("the submenu offers " + algorithm,
                                        ContextMenus.leafCount(ctx, algorithm.getTranslationKey()) == 1);
                            }
                        })
                        .step("pick the layered algorithm",
                                ctx -> ContextMenus.clickLeaf(ctx, GraphLayoutAlgorithm.LAYERED.getTranslationKey()))
                        .waitUntil("the source node moved to the left of the sink",
                                ctx -> node(ctx, "n0").getPosition().x < node(ctx, "n3").getPosition().x)
                        .settleMs(250)
                        .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                        .settleMs(200)
                        .step("every wire now points forwards", NgtAutoLayoutScenario::checkWiresPointForwards)
                        .step("no two nodes overlap", ctx -> checkNoOverlap(ctx, graphModel(ctx).getNodeModels()))
                        .step("the chain is one node per layer", ctx -> {
                            for (var i = 0; i + 1 < CHAIN.length; i++) {
                                var left = rect(ctx, node(ctx, CHAIN[i]));
                                var right = rect(ctx, node(ctx, CHAIN[i + 1]));
                                ctx.check(CHAIN[i] + " ends before " + CHAIN[i + 1] + " begins",
                                        left.x + left.z <= right.x + 0.5f,
                                        "<= " + right.x, left.x + left.z);
                            }
                        })
                        .screenshot("02_layered"))

                // The "lay out what is inside a placemat" reading of the gesture. The pair is first
                // stacked vertically and wrapped tight, so a horizontal layout cannot possibly fit
                // in the box it starts in — the growth is forced, not hoped for.
                .group("laying out a placemat's contents grows the placemat", g -> g
                        .step("stack the pair vertically and wrap it tight",
                                NgtAutoLayoutScenario::wrapPairInTightPlacemat)
                        .settleMs(250)
                        .check("the placemat reports both nodes as its contents", ctx ->
                                containedNodes(ctx).size() == 2)
                        .step("remember the placemat's rect", ctx ->
                                placematRectBefore.set(rect(ctx.<PlacematModel>get(PLACEMAT))))
                        .step("select only the contents", ctx -> {
                            var view = graphView(ctx);
                            view.clearAllSelected();
                            view.addSelected(node(ctx, PAIR_HEAD));
                            view.addSelected(node(ctx, PAIR_TAIL));
                        })
                        .step("auto layout the selection", ctx -> dispatchLayout(ctx, GraphLayoutAlgorithm.LAYERED))
                        .settleMs(250)
                        .step("the pair was laid out along the flow axis", ctx -> {
                            var head = rect(ctx, node(ctx, PAIR_HEAD));
                            var tail = rect(ctx, node(ctx, PAIR_TAIL));
                            ctx.check("the head ends before the tail begins",
                                    head.x + head.z <= tail.x + 0.5f, "<= " + tail.x, head.x + head.z);
                        })
                        .step("the placemat grew to hold the result", ctx -> {
                            var after = rect(ctx.<PlacematModel>get(PLACEMAT));
                            ctx.check("it got wider", after.z > placematRectBefore.z,
                                    "> " + placematRectBefore.z, after.z);
                            ctx.check("it still covers everything it used to", covers(after, placematRectBefore),
                                    placematRectBefore.toString(), after.toString());
                        })
                        .step("both nodes ended up inside it", ctx -> {
                            var placemat = rect(ctx.<PlacematModel>get(PLACEMAT));
                            for (var key : List.of(PAIR_HEAD, PAIR_TAIL)) {
                                ctx.check(key + " is inside the placemat",
                                        covers(placemat, rect(ctx, node(ctx, key))),
                                        placemat.toString(), rect(ctx, node(ctx, key)).toString());
                            }
                        })
                        .check("the placemat still reports both nodes", ctx -> containedNodes(ctx).size() == 2)
                        .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                        .settleMs(200)
                        .screenshot("03_placemat_grown"))

                .group("a selected placemat moves as one piece", g -> g
                        .step("remember where the contents sit relative to it", ctx -> {
                            var origin = ctx.<PlacematModel>get(PLACEMAT).getPosition();
                            pairOffsets[0] = new Vector2f(node(ctx, PAIR_HEAD).getPosition()).sub(origin);
                            pairOffsets[1] = new Vector2f(node(ctx, PAIR_TAIL).getPosition()).sub(origin);
                        })
                        .step("select the placemat and an unrelated node", ctx -> {
                            var view = graphView(ctx);
                            view.clearAllSelected();
                            view.addSelected(ctx.<PlacematModel>get(PLACEMAT));
                            view.addSelected(node(ctx, "n3"));
                        })
                        .step("auto layout the selection", ctx -> dispatchLayout(ctx, GraphLayoutAlgorithm.LAYERED))
                        .settleMs(250)
                        .step("the contents kept their exact offsets", ctx -> {
                            var origin = ctx.<PlacematModel>get(PLACEMAT).getPosition();
                            var head = new Vector2f(node(ctx, PAIR_HEAD).getPosition()).sub(origin);
                            var tail = new Vector2f(node(ctx, PAIR_TAIL).getPosition()).sub(origin);
                            ctx.check("the head rode along", head.distance(pairOffsets[0]) < 0.5f,
                                    pairOffsets[0].toString(), head.toString());
                            ctx.check("the tail rode along", tail.distance(pairOffsets[1]) < 0.5f,
                                    pairOffsets[1].toString(), tail.toString());
                        })
                        .check("the placemat still reports both nodes", ctx -> containedNodes(ctx).size() == 2)
                        .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                        .settleMs(200)
                        .screenshot("04_placemat_as_unit"))

                .group("grid tidies a selection into rows and columns", g -> g
                        .step("select the chain", ctx -> {
                            var view = graphView(ctx);
                            view.clearAllSelected();
                            for (var key : CHAIN) view.addSelected(node(ctx, key));
                        })
                        .step("auto layout the selection", ctx -> dispatchLayout(ctx, GraphLayoutAlgorithm.GRID))
                        .settleMs(250)
                        .step("four nodes land on a two by two grid", ctx -> {
                            var columns = new ArrayList<Float>();
                            var rows = new ArrayList<Float>();
                            for (var key : CHAIN) {
                                var position = node(ctx, key).getPosition();
                                if (columns.stream().noneMatch(v -> Math.abs(v - position.x) < 0.5f)) {
                                    columns.add(position.x);
                                }
                                if (rows.stream().noneMatch(v -> Math.abs(v - position.y) < 0.5f)) {
                                    rows.add(position.y);
                                }
                            }
                            ctx.check("two distinct columns", columns.size() == 2, 2, columns.size());
                            ctx.check("two distinct rows", rows.size() == 2, 2, rows.size());
                        })
                        // Only the chain was selected, so only the chain is promised to be tidy.
                        .step("no two of the laid-out nodes overlap", ctx -> checkNoOverlap(ctx,
                                Arrays.stream(CHAIN).map(key -> node(ctx, key)).toList()))
                        .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                        .settleMs(200)
                        .screenshot("05_grid"))

                .group("force directed also leaves nothing overlapping", g -> g
                        .step("select everything but the placemat", ctx -> {
                            var view = graphView(ctx);
                            view.clearAllSelected();
                            for (var node : graphModel(ctx).getNodeModels()) view.addSelected(node);
                        })
                        .step("auto layout the selection",
                                ctx -> dispatchLayout(ctx, GraphLayoutAlgorithm.FORCE_DIRECTED))
                        .settleMs(250)
                        .step("no two nodes overlap", ctx -> checkNoOverlap(ctx, graphModel(ctx).getNodeModels()))
                        .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                        .settleMs(200)
                        .screenshot("06_force_directed"))

                // A selection can be disconnected even when the graph is not — here, four nodes
                // whose only surviving wire is n2 -> n3. Nothing in a spring model holds unconnected
                // clusters together, so before components were packed this flung them across the
                // canvas and left a graph made mostly of empty space.
                .group("force directed keeps disconnected pieces together", g -> {
                    var disconnected = List.of("n0", "n2", "n3", PAIR_HEAD);
                    g.step("select a deliberately disconnected subset", ctx -> {
                                var view = graphView(ctx);
                                view.clearAllSelected();
                                for (var key : disconnected) view.addSelected(node(ctx, key));
                            })
                            .step("auto layout the selection",
                                    ctx -> dispatchLayout(ctx, GraphLayoutAlgorithm.FORCE_DIRECTED))
                            .settleMs(250)
                            .step("the pieces land in one compact block", ctx -> {
                                var rects = disconnected.stream().map(key -> rect(ctx, node(ctx, key))).toList();
                                var content = 0f;
                                var minX = Float.MAX_VALUE;
                                var minY = Float.MAX_VALUE;
                                var maxX = -Float.MAX_VALUE;
                                var maxY = -Float.MAX_VALUE;
                                for (var r : rects) {
                                    content += r.z * r.w;
                                    minX = Math.min(minX, r.x);
                                    minY = Math.min(minY, r.y);
                                    maxX = Math.max(maxX, r.x + r.z);
                                    maxY = Math.max(maxY, r.y + r.w);
                                }
                                var framed = (maxX - minX) * (maxY - minY);
                                ctx.check("the framed area stays within 12x the content",
                                        framed < content * 12f, "< " + (content * 12f), framed);
                            })
                            .step("no two of the laid-out nodes overlap", ctx ->
                                    checkNoOverlap(ctx, disconnected.stream().map(key -> node(ctx, key)).toList()))
                            // One of the four was a placemat member. The placemat is not what was
                            // being laid out, so it stays where it is rather than stretching after
                            // the member until it has swallowed the rest of the graph.
                            .step("the placemat did not chase the member that left", ctx -> {
                                var rect = rect(ctx.<PlacematModel>get(PLACEMAT));
                                ctx.check("it is still a placemat-sized box",
                                        rect.z < 2000f && rect.w < 2000f,
                                        "< 2000 x 2000", "%.0f x %.0f".formatted(rect.z, rect.w));
                            })
                            .step("frame the result", ctx -> graphView(ctx).fitGraphChildren(60f))
                            .settleMs(200)
                            .screenshot("07_force_directed_components");
                })

                // Undo restores the graph by deserialising it, which replaces every model instance —
                // so it has to run last, and the comparison has to be by value rather than by the
                // handles stashed at build time.
                .group("undo puts every position back", g -> g
                        .step("remember every position", ctx -> signature[0] = positionSignature(ctx))
                        .step("lay the whole graph out again", ctx -> {
                            graphView(ctx).clearAllSelected();
                            dispatchLayout(ctx, GraphLayoutAlgorithm.GRID);
                        })
                        .settleMs(200)
                        .step("something actually moved", ctx -> ctx.check(
                                "the layout changed at least one position",
                                !positionSignature(ctx).equals(signature[0]),
                                "different from " + signature[0], positionSignature(ctx)))
                        .step("undo", ctx -> graphView(ctx).getHistoryStack().undo())
                        .settleMs(250)
                        .step("every position is back", ctx -> ctx.check(
                                "the positions match the pre-layout snapshot",
                                positionSignature(ctx).equals(signature[0]),
                                signature[0], positionSignature(ctx)))
                        .screenshot("08_undone"))

                .step("hand the preference store back to the real config",
                        ctx -> ScenarioPreferences.restore())
                .closeScreen();
    }

    // region checks

    /**
     * Every wire has to run forwards after a layered pass: the box it leaves ends before the box it
     * arrives at begins. Wires inside a placemat that moved as a unit are exempt — their contents
     * were never laid out, only carried.
     */
    private static void checkWiresPointForwards(TestContext ctx) {
        var checked = 0;
        for (var wire : graphModel(ctx).getWireModels()) {
            var fromPort = wire.getFromPort();
            var toPort = wire.getToPort();
            if (fromPort == null || toPort == null) continue;
            var from = rect(ctx, fromPort.getNodeModel());
            var to = rect(ctx, toPort.getNodeModel());
            ctx.check("a wire runs forwards (%s -> %s)".formatted(from, to),
                    from.x + from.z <= to.x + 0.5f, "<= " + to.x, from.x + from.z);
            checked++;
        }
        ctx.check("all five wires were checked", checked == 5, 5, checked);
    }

    private static void checkNoOverlap(TestContext ctx, List<? extends AbstractNodeModel> nodes) {
        for (var i = 0; i < nodes.size(); i++) {
            for (var j = i + 1; j < nodes.size(); j++) {
                var a = rect(ctx, nodes.get(i));
                var b = rect(ctx, nodes.get(j));
                var separated = a.x + a.z <= b.x + 0.5f || b.x + b.z <= a.x + 0.5f
                        || a.y + a.w <= b.y + 0.5f || b.y + b.w <= a.y + 0.5f;
                ctx.check("two nodes do not overlap (%s vs %s)".formatted(a, b), separated);
            }
        }
    }

    /** {@code true} when {@code outer} fully contains {@code inner}. Both are (x, y, w, h). */
    private static boolean covers(Vector4f outer, Vector4f inner) {
        return inner.x >= outer.x - 0.5f
                && inner.y >= outer.y - 0.5f
                && inner.x + inner.z <= outer.x + outer.z + 0.5f
                && inner.y + inner.w <= outer.y + outer.w + 0.5f;
    }

    private static List<String> positionSignature(TestContext ctx) {
        var model = graphModel(ctx);
        var out = new ArrayList<String>();
        for (var node : model.getNodeModels()) {
            out.add("n %.1f %.1f".formatted(node.getPosition().x, node.getPosition().y));
        }
        for (var placemat : model.getPlacematModels()) {
            out.add("p %.1f %.1f %.1f %.1f".formatted(placemat.getPosition().x, placemat.getPosition().y,
                    placemat.getSize().x, placemat.getSize().y));
        }
        out.sort(null);
        return out;
    }

    // endregion

    // region gestures and setup

    private static void dispatchLayout(TestContext ctx, GraphLayoutAlgorithm algorithm) {
        var view = graphView(ctx);
        var selection = view.getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .toList();
        view.dispatchCommand(new LayoutCommands.AutoLayoutCommand(selection, algorithm));
    }

    /**
     * Stacks the pair vertically in a free corner of the canvas and draws a placemat that hugs them.
     * Built from the measured element sizes rather than from guessed numbers: a box that only just
     * fits a vertical stack is guaranteed to be too narrow for a horizontal one, which is what makes
     * the growth assertion meaningful rather than lucky.
     */
    private static void wrapPairInTightPlacemat(TestContext ctx) {
        var view = graphView(ctx);
        var head = node(ctx, PAIR_HEAD);
        var tail = node(ctx, PAIR_TAIL);
        var headSize = size(ctx, head);
        var tailSize = size(ctx, tail);

        var originX = -900f;
        var originY = 1200f;
        head.setPosition(new Vector2f(originX, originY));
        tail.setPosition(new Vector2f(originX, originY + headSize.y + 30f));

        var width = Math.max(headSize.x, tailSize.x) + HUG * 2f;
        var height = headSize.y + 30f + tailSize.y + HUG * 2f + PlacematElement.TITLE_BAR_HEIGHT;
        var placemat = graphModel(ctx).createPlacemat("Group",
                new Vector2f(originX - HUG, originY - HUG - PlacematElement.TITLE_BAR_HEIGHT),
                new Vector2f(width, height));
        ctx.put(PLACEMAT, placemat);
        view.clearAllSelected();
    }

    /** Right-clicks a spot on the canvas with nothing on it. */
    private static void rightClickEmptyCanvas(TestContext ctx) {
        var screen = CanvasPoints.emptySpot(ctx, graphView(ctx));
        ctx.input().moveTo(screen.x, screen.y);
        ctx.input().mouseDown(screen.x, screen.y, Keys.MOUSE_RIGHT);
        ctx.input().mouseUp(screen.x, screen.y, Keys.MOUSE_RIGHT);
    }

    // endregion

    // region lookups

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

    private static Vector2f size(TestContext ctx, AbstractNodeModel node) {
        var element = graphView(ctx).getModelElement(node);
        if (element == null) throw new IllegalStateException("no element for " + node);
        return new Vector2f(element.getSizeWidth(), element.getSizeHeight());
    }

    /** A node's rect in canvas content coordinates: model position, UI-measured size. */
    private static Vector4f rect(TestContext ctx, AbstractNodeModel node) {
        var position = node.getPosition();
        var size = size(ctx, node);
        return new Vector4f(position.x, position.y, size.x, size.y);
    }

    private static Vector4f rect(PlacematModel placemat) {
        return new Vector4f(placemat.getPosition().x, placemat.getPosition().y,
                placemat.getSize().x, placemat.getSize().y);
    }

    private static List<AbstractNodeModel> containedNodes(TestContext ctx) {
        var view = graphView(ctx);
        return ctx.<PlacematModel>get(PLACEMAT).getContainedNodes(node -> {
            var element = view.getModelElement(node);
            return element == null ? null : new Vector2f(element.getSizeWidth(), element.getSizeHeight());
        });
    }

    // endregion

    private static ModularUI buildGraphUI(TestContext ctx) {
        // Before loadGraph below, which is what reads the remembered setup.
        ScenarioPreferences.isolate(ctx, "ngt_auto_layout");
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
        // n0 -> n1 -> n2 -> n3, with p0 -> p1 -> n1 joining halfway. Positions are shuffled on
        // purpose: the source starts to the right of the sink.
        var positions = new Vector2f[]{
                new Vector2f(620, 120), new Vector2f(180, 430), new Vector2f(720, 640), new Vector2f(90, 60),
        };
        var chain = new NodeModel[CHAIN.length];
        for (var i = 0; i < CHAIN.length; i++) {
            chain[i] = graph.graphModel.createNodeModel(new TestAddNode(), positions[i]);
            ctx.put(CHAIN[i], chain[i]);
        }
        var head = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(-420, 240));
        var tail = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(-200, 620));
        ctx.put(PAIR_HEAD, head);
        ctx.put(PAIR_TAIL, tail);

        for (var i = 0; i + 1 < chain.length; i++) {
            graph.graphModel.createWire(input(chain[i + 1], "in1"), output(chain[i]));
        }
        graph.graphModel.createWire(input(tail, "in1"), output(head));
        graph.graphModel.createWire(input(chain[1], "in2"), output(tail));

        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static PortModel output(NodeModel node) {
        var port = node.getOutputsById().get("out");
        if (port == null) throw new IllegalStateException("TestAddNode has no 'out' port");
        return port;
    }

    private static PortModel input(NodeModel node, String id) {
        var port = node.getInputsById().get(id);
        if (port == null) throw new IllegalStateException("TestAddNode has no '" + id + "' port");
        return port;
    }
}
