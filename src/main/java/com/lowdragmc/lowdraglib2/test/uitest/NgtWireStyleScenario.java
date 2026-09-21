package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire.WireRouteStyle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
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

import java.util.List;

/**
 * End-to-end coverage of the wire route styles, driven from the canvas contextual menu.
 *
 * <p>The geometry is unit tested in {@code WireRouterTest}; what only a live editor can answer is
 * whether the wires actually adopt it. Three things go wrong here and nowhere else:</p>
 * <ul>
 *     <li>The element is not told the setting changed and keeps drawing the old route. The style is
 *     deliberately <em>not</em> pushed out to the wires — each one notices on its next draw — so
 *     "did anything happen at all" is a real question.</li>
 *     <li>The wire's bounding box is computed from the waypoints rather than the route, which is
 *     invisible until a backward wire detours outside the box its endpoints imply and is then
 *     culled or unclickable. The box is checked against every point of every route.</li>
 *     <li>The routing quietly changes the graph. It is presentation; the ports must not move.</li>
 * </ul>
 *
 * <p>The three wires are chosen to hit all three branches of the router: one shallower than 45°,
 * one steeper, and one running backwards.</p>
 */
@LDLRegisterClient(name = "ngt_wire_style", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtWireStyleScenario implements UIScenario {

    /** Slack on the "is this segment axis-aligned or 45°" test, in content units. */
    private static final float TOLERANCE = 0.05f;
    /** Slack on the bounding-box containment test: the box is built with a 2-unit border. */
    private static final float BOX_TOLERANCE = 0.5f;
    /** Turn a curve's sampling may produce anywhere without further justification. */
    private static final float MAX_GENTLE_TURN = 45f;
    /** Segment length below which a sharp turn reads as curvature rather than as a corner. */
    private static final float TIGHT_BEND_SPAN = 25f;

    private static final String SOURCE_PORT = "sourcePort";
    private static final String SHALLOW_INPUT = "shallowInput";
    private static final String STEEP_INPUT = "steepInput";
    private static final String BACKWARD_INPUT = "backwardInput";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "wire", "visual").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("graph editor with three differently shaped wires", NgtWireStyleScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the four nodes are laid out", ctx -> ctx.count(".__node-element__") == 4)
                .waitUntil("the three wires are laid out", ctx -> ctx.count(".__wire__") == 3)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(200)

                .group("baseline", g -> g
                        .check("the canvas is at full detail", ctx ->
                                graphView(ctx).graphView.getLod() == GraphViewLod.FULL)
                        .check("the default style is in force", ctx ->
                                graphView(ctx).getWireRouteStyle() == WireRouteStyle.DEFAULT)
                        .step("every wire is a four-point run", ctx -> {
                            for (var ref : ctx.all(".__wire__")) {
                                var points = ref.as(WireElement.class).getRoutePoints();
                                ctx.check("default route is from/stub/stub/to", points.size() == 4, 4, points.size());
                            }
                        })
                        .step("remember the ports", NgtWireStyleScenario::rememberPorts)
                        .screenshot("01_default"))

                // Positions are resolved once and the input driven directly: the builder's click
                // re-resolves its selector on release, and a menu that closes on press is gone by
                // then.
                .group("switch to octilinear from the contextual menu", g -> g
                        .step("right-click empty canvas", NgtWireStyleScenario::rightClickEmptyCanvas)
                        .waitUntil("the contextual menu is open",
                                ctx -> ctx.count(".__node-graph-view_context-menu__") > 0)
                        .step("the menu offers a wire style branch", ctx -> ctx.check(
                                "a 'Wire Style' branch exists",
                                ContextMenus.branchCount(ctx, "graph.wire_style") == 1))
                        .step("hover the branch to open its submenu",
                                ctx -> ContextMenus.openBranch(ctx, "graph.wire_style"))
                        .settleMs(120)
                        .step("the submenu lists every style", ctx -> {
                            for (var style : WireRouteStyle.values()) {
                                ctx.check("the submenu offers " + style,
                                        ContextMenus.leafCount(ctx, style.getTranslationKey()) == 1);
                            }
                        })
                        .step("pick Octilinear",
                                ctx -> ContextMenus.clickLeaf(ctx, WireRouteStyle.OCTILINEAR.getTranslationKey()))
                        .waitUntil("the view switched to octilinear",
                                ctx -> graphView(ctx).getWireRouteStyle() == WireRouteStyle.OCTILINEAR)
                        .settleMs(200)
                        .step("every segment is axis aligned or 45°",
                                ctx -> checkRoutes(ctx, "octilinear", true))
                        .step("every route stays inside its wire's box", NgtWireStyleScenario::checkBoxes)
                        .step("the wires still connect the same ports", NgtWireStyleScenario::checkPortsUnchanged)
                        .screenshot("02_octilinear"))

                // A backward wire is the case the shared-trunk arithmetic cannot produce from the
                // endpoints alone, so it is the one that catches a box computed from the waypoints.
                .group("the backward wire really does detour", g -> g
                        .step("its route leaves the box its endpoints would imply", ctx -> {
                            var element = backwardWire(ctx);
                            var points = element.getRoutePoints();
                            var startX = points.getFirst().x;
                            var maxX = points.stream().map(p -> p.x).max(Float::compare).orElseThrow();
                            ctx.check("the route pushes past the source port before turning back",
                                    maxX > startX + 1f, "> " + (startX + 1f), maxX);
                        }))

                .group("switch to curved", g -> g
                        .step("set the style", ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.CURVED))
                        .settleMs(200)
                        .step("every wire is a sampled curve, not an elbow", ctx -> {
                            for (var ref : ctx.all(".__wire__")) {
                                var points = ref.as(WireElement.class).getRoutePoints();
                                ctx.check("the route is sampled", points.size() > 4, "> 4", points.size());
                                checkNoKinks(ctx, points);
                            }
                        })
                        .step("every route stays inside its wire's box", NgtWireStyleScenario::checkBoxes)
                        .step("the wires still connect the same ports", NgtWireStyleScenario::checkPortsUnchanged)
                        .screenshot("03_curved"))

                .group("switch to orthogonal", g -> g
                        .step("set the style", ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.ORTHOGONAL))
                        .settleMs(200)
                        .step("no segment runs diagonally", ctx -> checkRoutes(ctx, "orthogonal", false))
                        .step("every route stays inside its wire's box", NgtWireStyleScenario::checkBoxes)
                        .step("the wires still connect the same ports", NgtWireStyleScenario::checkPortsUnchanged)
                        .screenshot("04_orthogonal"))

                .group("switch to straight", g -> g
                        .step("set the style", ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.STRAIGHT))
                        .settleMs(200)
                        .step("every wire is a single segment", ctx -> {
                            for (var ref : ctx.all(".__wire__")) {
                                var points = ref.as(WireElement.class).getRoutePoints();
                                ctx.check("straight route is port to port", points.size() == 2, 2, points.size());
                            }
                        })
                        .step("every route stays inside its wire's box", NgtWireStyleScenario::checkBoxes)
                        .screenshot("05_straight"))

                .group("back to the default", g -> g
                        .step("set the style", ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.DEFAULT))
                        .settleMs(200)
                        .step("every wire is a four-point run again", ctx -> {
                            for (var ref : ctx.all(".__wire__")) {
                                var points = ref.as(WireElement.class).getRoutePoints();
                                ctx.check("default route is from/stub/stub/to", points.size() == 4, 4, points.size());
                            }
                        })
                        .step("the wires still connect the same ports", NgtWireStyleScenario::checkPortsUnchanged)
                        .screenshot("06_back_to_default"))

                // Hit-testing runs against the same polyline, so a route the element does not know
                // about would leave the wire unclickable. Checked on the steep wire, whose route is
                // nowhere near the straight line between its ports.
                .group("an octilinear wire is still clickable along its route", g -> g
                        .step("set the style", ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.OCTILINEAR))
                        .settleMs(200)
                        .step("the hit test resolves to the wire on a mid-route point", ctx -> {
                            var element = steepWire(ctx);
                            var point = midRouteOnScreen(element);
                            ctx.input().moveTo(point.x, point.y);
                            var hit = ctx.requireUI().hitTestAtScreen(point.x, point.y);
                            ctx.check("a point on the route hit-tests as the wire",
                                    hit instanceof WireElement,
                                    "WireElement", hit == null ? "none" : hit.getClass().getSimpleName());
                        })
                        .screenshot("07_octilinear_hit"))

                // Wire style is the one contextual entry a read-only graph keeps: it decides how the
                // graph is drawn, not what it holds. Everything else in that menu goes through
                // dispatchCommand, which read-only refuses outright.
                .group("a read-only graph still offers wire style, and nothing that edits", g -> g
                        .step("set the style back to the default",
                                ctx -> graphView(ctx).setWireRouteStyle(WireRouteStyle.DEFAULT))
                        .step("make the graph read-only", ctx -> graphView(ctx).setReadOnly(true))
                        .settleMs(120)
                        .step("right-click empty canvas", NgtWireStyleScenario::rightClickEmptyCanvas)
                        .waitUntil("the contextual menu is open",
                                ctx -> ctx.count(ContextMenus.GRAPH_MENU) > 0)
                        .step("it offers the wire style branch", ctx -> ctx.check(
                                "a 'Wire Style' branch exists",
                                ContextMenus.branchCount(ctx, "graph.wire_style") == 1))
                        .step("and nothing that would change the graph", ctx -> {
                            for (var key : List.of("graph.commands.add_node", "graph.auto_layout")) {
                                ctx.check("read-only does not offer " + key,
                                        ContextMenus.branchCount(ctx, key) == 0
                                                && ContextMenus.leafCount(ctx, key) == 0);
                            }
                        })
                        .step("hover the branch to open its submenu",
                                ctx -> ContextMenus.openBranch(ctx, "graph.wire_style"))
                        .settleMs(120)
                        .step("pick Orthogonal",
                                ctx -> ContextMenus.clickLeaf(ctx, WireRouteStyle.ORTHOGONAL.getTranslationKey()))
                        .waitUntil("the read-only view switched to orthogonal",
                                ctx -> graphView(ctx).getWireRouteStyle() == WireRouteStyle.ORTHOGONAL)
                        .settleMs(200)
                        .step("no segment runs diagonally", ctx -> checkRoutes(ctx, "orthogonal", false))
                        .step("the wires still connect the same ports", NgtWireStyleScenario::checkPortsUnchanged)
                        .screenshot("08_read_only_wire_style")
                        .step("hand the graph back", ctx -> graphView(ctx).setReadOnly(false)))

                .step("hand the preference store back to the real config",
                        ctx -> ScenarioPreferences.restore())
                .closeScreen();
    }

    // region checks

    /**
     * Asserts the polyline reads as a curve rather than as a corner.
     *
     * <p>Not "it never turns sharply": a wire that has to double back hooks through nearly 180°,
     * and so it should. What separates a curve from an elbow is that a curve only turns sharply
     * <em>over short segments</em> — an elbow puts a 90° corner between two long runs. So a sharp
     * turn is allowed, as long as the segments meeting there are short enough for it to read as
     * curvature rather than as a kink.</p>
     */
    private static void checkNoKinks(TestContext ctx, List<Vector2f> points) {
        for (var i = 0; i + 2 < points.size(); i++) {
            var inX = points.get(i + 1).x - points.get(i).x;
            var inY = points.get(i + 1).y - points.get(i).y;
            var outX = points.get(i + 2).x - points.get(i + 1).x;
            var outY = points.get(i + 2).y - points.get(i + 1).y;
            var inLength = (float) Math.sqrt(inX * inX + inY * inY);
            var outLength = (float) Math.sqrt(outX * outX + outY * outY);
            if (inLength < 1e-3f || outLength < 1e-3f) continue;
            var cos = (inX * outX + inY * outY) / (inLength * outLength);
            var turn = (float) Math.toDegrees(Math.acos(Math.clamp(cos, -1f, 1f)));
            if (turn <= MAX_GENTLE_TURN) continue;
            ctx.check("a %.0f° turn at point %d is a tight bend, not a kink".formatted(turn, i + 1),
                    Math.max(inLength, outLength) < TIGHT_BEND_SPAN,
                    "segments < " + TIGHT_BEND_SPAN,
                    "%.1f / %.1f".formatted(inLength, outLength));
        }
    }

    private static void checkRoutes(TestContext ctx, String label, boolean allowDiagonal) {
        var wires = ctx.all(".__wire__");
        ctx.check("three wires are drawn", wires.size() == 3, 3, wires.size());
        for (var ref : wires) {
            var points = ref.as(WireElement.class).getRoutePoints();
            ctx.check(label + ": the wire has a route", points.size() >= 2, ">= 2", points.size());
            for (var i = 0; i + 1 < points.size(); i++) {
                var dx = Math.abs(points.get(i + 1).x - points.get(i).x);
                var dy = Math.abs(points.get(i + 1).y - points.get(i).y);
                var axisAligned = dx < TOLERANCE || dy < TOLERANCE;
                var diagonal = allowDiagonal && Math.abs(dx - dy) < TOLERANCE;
                ctx.check(label + ": segment " + i + " of " + points.size() + " is legal",
                        axisAligned || diagonal,
                        allowDiagonal ? "axis or 45°" : "axis",
                        "d=(%.3f, %.3f)".formatted(dx, dy));
            }
        }
    }

    /**
     * Every point of every route has to lie inside the element it belongs to. Both are in absolute
     * layout coordinates — {@code getPositionX} is the cumulative sum up the parent chain, and the
     * route is built against exactly that space.
     */
    private static void checkBoxes(TestContext ctx) {
        for (var ref : ctx.all(".__wire__")) {
            var element = ref.as(WireElement.class);
            var left = element.getPositionX() - BOX_TOLERANCE;
            var top = element.getPositionY() - BOX_TOLERANCE;
            var right = left + element.getSizeWidth() + 2 * BOX_TOLERANCE;
            var bottom = top + element.getSizeHeight() + 2 * BOX_TOLERANCE;
            for (var point : element.getRoutePoints()) {
                ctx.check("route point %s is inside the wire's box".formatted(point),
                        point.x >= left && point.x <= right && point.y >= top && point.y <= bottom,
                        "[%.1f..%.1f] x [%.1f..%.1f]".formatted(left, right, top, bottom),
                        point.toString());
            }
        }
    }

    private static void rememberPorts(TestContext ctx) {
        var model = graphModel(ctx);
        ctx.check("the graph holds three wires", model.getWireModels().size() == 3, 3,
                model.getWireModels().size());
    }

    private static void checkPortsUnchanged(TestContext ctx) {
        var source = ctx.<PortModel>get(SOURCE_PORT);
        ctx.check("the source port still drives all three wires",
                source.getConnectedWires().size() == 3, 3, source.getConnectedWires().size());
        for (var key : List.of(SHALLOW_INPUT, STEEP_INPUT, BACKWARD_INPUT)) {
            var target = ctx.<PortModel>get(key);
            ctx.check(key + " is still connected", source.getConnectedPorts().contains(target));
        }
    }

    // endregion

    // region gestures

    /** Right-clicks a spot on the canvas with nothing on it. */
    private static void rightClickEmptyCanvas(TestContext ctx) {
        var screen = CanvasPoints.emptySpot(ctx, graphView(ctx));
        ctx.input().moveTo(screen.x, screen.y);
        ctx.input().mouseDown(screen.x, screen.y, Keys.MOUSE_RIGHT);
        ctx.input().mouseUp(screen.x, screen.y, Keys.MOUSE_RIGHT);
    }

    /** A screen point on the wire's route — the midpoint of its longest segment. */
    private static Vector2f midRouteOnScreen(WireElement element) {
        var points = element.getRoutePoints();
        var parent = element.getParent();
        if (parent == null || points.size() < 2) throw new IllegalStateException("the wire has no route");
        var best = 0;
        var bestLength = -1f;
        for (var i = 0; i + 1 < points.size(); i++) {
            var length = points.get(i).distance(points.get(i + 1));
            if (length > bestLength) {
                bestLength = length;
                best = i;
            }
        }
        // The route is in absolute layout space and getWorldMouse expects exactly that.
        var mid = new Vector2f(points.get(best)).add(points.get(best + 1)).mul(0.5f);
        return parent.getWorldMouse(mid.x, mid.y);
    }

    // endregion

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel graphModel(TestContext ctx) {
        var graph = graphView(ctx).getGraph();
        if (graph == null) throw new IllegalStateException("no graph loaded");
        return graph.graphModel;
    }

    private static WireElement wireInto(TestContext ctx, String portKey) {
        var target = ctx.<PortModel>get(portKey);
        for (var ref : ctx.all(".__wire__")) {
            var element = ref.as(WireElement.class);
            if (element.getModel().getToPort() == target) return element;
        }
        throw new IllegalStateException("no wire into " + portKey);
    }

    private static WireElement steepWire(TestContext ctx) {
        return wireInto(ctx, STEEP_INPUT);
    }

    private static WireElement backwardWire(TestContext ctx) {
        return wireInto(ctx, BACKWARD_INPUT);
    }

    private static ModularUI buildGraphUI(TestContext ctx) {
        // Before loadGraph below, which is what reads the remembered setup.
        ScenarioPreferences.isolate(ctx, "ngt_wire_style");
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
        // One producer feeding three consumers placed to hit each branch of the router: a gentle
        // drop, a steep one, and a consumer sitting behind the producer.
        var producer = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 260));
        var shallow = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(460, 330));
        var steep = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(360, 700));
        var backward = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(-460, 560));
        var out = producer.getOutputsById().get("out");
        var shallowIn = shallow.getInputsById().get("in1");
        var steepIn = steep.getInputsById().get("in1");
        var backwardIn = backward.getInputsById().get("in1");
        if (out == null || shallowIn == null || steepIn == null || backwardIn == null) {
            throw new IllegalStateException("TestAddNode did not define the expected ports");
        }
        graph.graphModel.createWire(shallowIn, out);
        graph.graphModel.createWire(steepIn, out);
        graph.graphModel.createWire(backwardIn, out);
        ctx.put(SOURCE_PORT, out);
        ctx.put(SHALLOW_INPUT, shallowIn);
        ctx.put(STEEP_INPUT, steepIn);
        ctx.put(BACKWARD_INPUT, backwardIn);

        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
