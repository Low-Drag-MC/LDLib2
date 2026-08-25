package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
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
import org.lwjgl.glfw.GLFW;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * End-to-end coverage of reroute points — the dots a double-click drops onto a wire, and the handles
 * branches are pulled out of.
 *
 * <p>{@code GraphWireReroutePointTest} pins the model contract down headlessly. What it cannot reach
 * is everything this feature actually <em>is</em>: whether a double-click lands on the wire at all,
 * whether the route follows the dot, whether the dot wins hit-testing against the wire drawn beneath
 * it, and — the part that makes it a reroute point rather than a bend — whether its two connector
 * dots really fan several wires out of one output port, in both drag directions.</p>
 *
 * <p>The load-bearing assertion is the pair that runs after every mutation: the routing changes, and
 * the ports do <em>not</em>. That is the whole promise, checked against a live editor.</p>
 */
@LDLRegisterClient(name = "ngt_wire_reroute", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtWireReroutePointScenario implements UIScenario {

    /** How far the dot is dragged, in screen pixels. Big enough that the bend is unmistakable. */
    private static final float DRAG_UP = 70f;

    private static final String SOURCE_PORT = "sourcePort";
    private static final String FIRST_INPUT = "firstInput";
    private static final String SECOND_INPUT = "secondInput";
    private static final String THIRD_INPUT = "thirdInput";
    private static final String OTHER_SOURCE = "otherSource";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "wire", "visual").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        // A screen point on the wire, and the wire's box height before it is bent — both only knowable
        // once the canvas has framed the graph.
        var clickPoint = new float[2];
        var straightWireHeight = new float[1];

        s.openModularUI("graph editor with one wire", NgtWireReroutePointScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the five nodes are laid out", ctx -> ctx.count(".__node-element__") == 5)
                .waitUntil("the wire is laid out", ctx -> ctx.count(".__wire__") == 1)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(150)

                .group("baseline", g -> g
                        .checkCount(".__wire__", 1)
                        .checkCount(".__node-element__", 5)
                        .checkNotExists(".__wire-reroute-point__")
                        .step("remember the wire's box", ctx -> {
                            var box = ctx.el(".__wire__").bounds();
                            straightWireHeight[0] = box.height();
                            ctx.log("wire box " + box);
                        })
                        .screenshot("01_straight_wire"))

                // The gesture. Aimed at the midpoint of the wire's middle segment, which is also the
                // midpoint of its two endpoints — the control-point offsets are equal and opposite, so
                // they cancel. Hit-tested before clicking so "the test clicked empty canvas and passed"
                // is impossible.
                .group("double-click the wire", g -> g
                        .step("aim at the middle of the wire", ctx -> {
                            var point = wireMidpointOnScreen(ctx);
                            clickPoint[0] = point.x;
                            clickPoint[1] = point.y;
                            ctx.input().moveTo(point.x, point.y);
                        })
                        .step("the wire is what is under the cursor", ctx -> {
                            var hit = ctx.requireUI().hitTestAtScreen(clickPoint[0], clickPoint[1]);
                            ctx.check("hit test at the wire midpoint resolves to the wire",
                                    hit instanceof WireElement,
                                    "WireElement",
                                    hit == null ? "none" : hit.getClass().getSimpleName());
                        })
                        // Both clicks have to land inside ModularUI's 300 ms double-click window, so
                        // the settles between them are deliberately short.
                        .settleMs(20)
                        .step("press 1", ctx -> ctx.input().mouseDown(clickPoint[0], clickPoint[1], Keys.MOUSE_LEFT))
                        .settleMs(20)
                        .step("release 1", ctx -> ctx.input().mouseUp(clickPoint[0], clickPoint[1], Keys.MOUSE_LEFT))
                        .settleMs(20)
                        .step("press 2", ctx -> ctx.input().mouseDown(clickPoint[0], clickPoint[1], Keys.MOUSE_LEFT))
                        .step("release 2", ctx -> ctx.input().mouseUp(clickPoint[0], clickPoint[1], Keys.MOUSE_LEFT))
                        // The command lands on the model immediately; the element is built on the next
                        // GraphView#screenTick, which is a client tick away.
                        .waitUntil("a reroute point element exists",
                                ctx -> ctx.count(".__wire-reroute-point__") == 1)
                        .screenshot("02_reroute_created"))

                .group("the point is layout, not topology", g -> g
                        .checkCount(".__wire__", 1)
                        .checkCount(".__node-element__", 5)
                        .check("the wire still has its original ports", ctx ->
                                wire(ctx).getFromPort() == ctx.<PortModel>get(SOURCE_PORT)
                                        && wire(ctx).getToPort() == ctx.<PortModel>get(FIRST_INPUT))
                        .check("the graph still holds one wire and five nodes", ctx ->
                                graphModel(ctx).getWireModels().size() == 1
                                        && graphModel(ctx).getNodeModels().size() == 5)
                        .check("the source port still reports exactly one wire", ctx ->
                                ctx.<PortModel>get(SOURCE_PORT).getConnectedWires().size() == 1)
                        .check("the point resolves back to the wire's source port", ctx -> {
                            var points = wire(ctx).getReroutePoints();
                            return points.size() == 1
                                    && points.get(0).getSourcePort() == wire(ctx).getFromPort();
                        }))

                // Hit-testing is the reason the dot carries a raised z-index: it is drawn on top of
                // the very wire that would otherwise swallow the clicks meant for it.
                .group("the point is grabbable", g -> g
                        .hover(".__wire-reroute-point__")
                        .checkHovered(".__wire-reroute-point__"))

                // Dragging is what proves the route is bound to the dot rather than merely drawn near
                // it: the wire's box has to grow to follow it.
                .group("drag the point off the wire", g -> {
                    var pointModelY = new float[1];
                    g.step("remember where the point started", ctx ->
                            pointModelY[0] = wire(ctx).getReroutePoints().get(0).getPosition().y);
                    // The body between the two dots is the move handle; the dots are connectors.
                    drag(g, "the point body", element(".__wire-reroute-point__"), offsetBy(0f, -DRAG_UP));
                    g.settleMs(150)
                            .step("the wire re-routed through the point", ctx -> {
                                var box = ctx.el(".__wire__").bounds();
                                var dot = ctx.el(".__wire-reroute-point__").bounds();
                                ctx.check("the wire's box grew to follow the point",
                                        box.height() > straightWireHeight[0] + DRAG_UP / 2f,
                                        "> " + (straightWireHeight[0] + DRAG_UP / 2f), box.height());
                                ctx.check("the wire's box contains the point",
                                        contains(box, dot.centerX(), dot.centerY()),
                                        box.toString(), "(%.1f, %.1f)".formatted(dot.centerX(), dot.centerY()));
                            })
                            // The drag preview only moves the element; the model is written by the
                            // MoveElementsCommand on drop. Checking the model is what proves the drop
                            // committed rather than the wire tracking a floating handle.
                            .step("the drop committed to the model", ctx -> {
                                float now = wire(ctx).getReroutePoints().get(0).getPosition().y;
                                ctx.check("the point's model position moved up",
                                        now < pointModelY[0] - 10f, "< " + (pointModelY[0] - 10f), now);
                            })
                            .screenshot("03_reroute_dragged");
                })

                // The part that makes this a reroute point and not a bend: pulling a second wire out
                // of it, the way you would out of a portal exit.
                .group("fan out a second wire from the output side", g -> {
                    g.checkExists(".__wire-reroute-point_input__")
                            .checkExists(".__wire-reroute-point_output__");
                    drag(g, "the output dot", element(".__wire-reroute-point_output__"),
                            centreOf(connector(SECOND_INPUT)));
                    g.waitUntil("a second wire exists", ctx -> ctx.count(".__wire__") == 2)
                            .checkCount(".__node-element__", 5)
                            .checkCount(".__wire-reroute-point__", 1)
                            .check("both wires leave the same output port", ctx -> {
                                var source = ctx.<PortModel>get(SOURCE_PORT);
                                return source.getConnectedWires().size() == 2
                                        && graphModel(ctx).getWireModels().stream()
                                        .filter(java.util.Objects::nonNull)
                                        .allMatch(w -> w.getFromPort() == source);
                            })
                            .check("the branch reaches the second consumer", ctx -> {
                                var target = ctx.<PortModel>get(SECOND_INPUT);
                                return target.getConnectedWires().size() == 1
                                        && ctx.<PortModel>get(SOURCE_PORT).getConnectedPorts().contains(target);
                            })
                            .check("both wires share the one reroute point", ctx -> {
                                var points = graphModel(ctx).getWireReroutePointModels();
                                if (points.size() != 1) return false;
                                var shared = points.get(0);
                                return graphModel(ctx).getWiresThroughReroutePoint(shared).size() == 2;
                            })
                            .screenshot("04_fanned_out");
                })

                // The other direction: grab a node's own input port and drop it on the reroute point.
                // That path runs through the stock PortElement drag, so it also proves a reroute point
                // is a drop target for ordinary wire drags and not just a source.
                .group("drop a port's own drag onto the point", g -> {
                    drag(g, "the third consumer's input", connector(THIRD_INPUT),
                            centreOf(element(".__wire-reroute-point_input__")));
                    g.waitUntil("a third wire exists", ctx -> ctx.count(".__wire__") == 3)
                            .checkCount(".__wire-reroute-point__", 1)
                            .check("the third branch also leaves the shared source", ctx -> {
                                var source = ctx.<PortModel>get(SOURCE_PORT);
                                var target = ctx.<PortModel>get(THIRD_INPUT);
                                return source.getConnectedWires().size() == 3
                                        && target.getConnectedWires().size() == 1
                                        && source.getConnectedPorts().contains(target);
                            })
                            .check("all three branches share the one reroute point", ctx -> {
                                var points = graphModel(ctx).getWireReroutePointModels();
                                return points.size() == 1
                                        && graphModel(ctx).getWiresThroughReroutePoint(points.get(0)).size() == 3;
                            })
                            .screenshot("05_dropped_onto_point");
                })

                // Moving the shared point has to move every branch, which is the reason they share it.
                .group("the shared point moves every branch", g -> {
                    drag(g, "the point body", element(".__wire-reroute-point__"), offsetBy(-40f, 0f));
                    g.settleMs(150)
                            .step("both wire boxes contain the point", ctx -> {
                                var dot = ctx.el(".__wire-reroute-point__").bounds();
                                var wires = ctx.all(".__wire__");
                                ctx.check("three wires are drawn", wires.size() == 3, 3, wires.size());
                                for (var wireRef : wires) {
                                    ctx.check("wire box " + wireRef.bounds() + " contains the point",
                                            contains(wireRef.bounds(), dot.centerX(), dot.centerY()));
                                }
                            })
                            .screenshot("06_shared_point_moved");
                })

                // The input side, which is what makes it a two-ported node rather than a fan-out tap:
                // dragging off it reaches compatible *output* ports, and dropping on one re-points every
                // branch at that producer in a single edit.
                //
                // Has to run before the undo group: undo restores the graph by deserializing it, which
                // replaces every node and port instance, so the PortModel handles stashed at build time
                // are stale from that point on.
                .group("re-source the point from its input side", g -> {
                    drag(g, "the input dot", element(".__wire-reroute-point_input__"),
                            centreOf(connector(OTHER_SOURCE)));
                    g.waitUntil("every branch moved to the other producer", ctx ->
                                    ctx.<PortModel>get(OTHER_SOURCE).getConnectedWires().size() == 3)
                            .checkCount(".__wire__", 3)
                            .checkCount(".__wire-reroute-point__", 1)
                            .check("the original source kept none", ctx ->
                                    ctx.<PortModel>get(SOURCE_PORT).getConnectedWires().isEmpty())
                            .check("all three branches still travel through the point", ctx -> {
                                var points = graphModel(ctx).getWireReroutePointModels();
                                return points.size() == 1
                                        && graphModel(ctx).getWiresThroughReroutePoint(points.get(0)).size() == 3;
                            })
                            .check("every destination is still connected", ctx -> {
                                var source = ctx.<PortModel>get(OTHER_SOURCE);
                                return source.getConnectedPorts().contains(ctx.<PortModel>get(FIRST_INPUT))
                                        && source.getConnectedPorts().contains(ctx.<PortModel>get(SECOND_INPUT))
                                        && source.getConnectedPorts().contains(ctx.<PortModel>get(THIRD_INPUT));
                            })
                            .screenshot("09_resourced");
                })

                .group("delete the shared point", g -> g
                        .focus("#graph")
                        .click(".__wire-reroute-point__")
                        .key(Keys.DELETE)
                        .waitUntil("the point element is gone",
                                ctx -> ctx.count(".__wire-reroute-point__") == 0)
                        .checkCount(".__wire__", 3)
                        .check("deleting the shared point left every connection alone", ctx -> {
                            var source = ctx.<PortModel>get(OTHER_SOURCE);
                            return source.getConnectedWires().size() == 3
                                    && source.getConnectedPorts().contains(ctx.<PortModel>get(FIRST_INPUT))
                                    && source.getConnectedPorts().contains(ctx.<PortModel>get(SECOND_INPUT))
                                    && source.getConnectedPorts().contains(ctx.<PortModel>get(THIRD_INPUT));
                        })
                        .screenshot("07_reroute_deleted"))

                // Undo goes through a full graph reload and UI rebuild, so this also covers the
                // serialize/deserialize path — including that the two branches come back sharing one
                // point rather than reloading as two lookalike copies.
                .group("undo brings the shared point back", g -> g
                        .focus("#graph")
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .waitUntil("the point element is back",
                                ctx -> ctx.count(".__wire-reroute-point__") == 1)
                        .checkCount(".__wire__", 3)
                        .checkCount(".__node-element__", 5)
                        .check("all three branches are routed through the one restored point", ctx -> {
                            var points = graphModel(ctx).getWireReroutePointModels();
                            return points.size() == 1
                                    && graphModel(ctx).getWiresThroughReroutePoint(points.get(0)).size() == 3;
                        })
                        .screenshot("08_reroute_restored"))

                .closeScreen();
    }

    // region gestures

    /**
     * A drag expanded across frames, from a selector to a destination both resolved at run time — the
     * builder's own {@code dragTo} takes absolute coordinates fixed when the scenario is written, and
     * where anything lands depends on how the canvas framed itself.
     */
    private static void drag(ScenarioBuilder s, String label,
                             Function<TestContext, UIElement> source,
                             BiFunction<TestContext, Vector2f, Vector2f> destination) {
        var from = new float[2];
        var to = new float[2];
        s.step("drag " + label + " :aim", ctx -> {
                    var sourceElement = source.apply(ctx);
                    var bounds = ElementBounds.of(sourceElement);
                    from[0] = bounds.centerX();
                    from[1] = bounds.centerY();
                    var target = destination.apply(ctx, new Vector2f(from[0], from[1]));
                    to[0] = target.x;
                    to[1] = target.y;
                    ctx.input().moveTo(from[0], from[1]);
                    // The builder's own click/drag steps guard against dragging thin air; this one
                    // resolves its own coordinates, so it has to make the same check itself.
                    var hit = ctx.requireUI().hitTestAtScreen(from[0], from[1]);
                    ctx.check("the drag starts on " + label,
                            hit != null && (hit == sourceElement
                                    || hit.getStructurePath().contains(sourceElement)),
                            label, hit == null ? "none" : hit.getElementName());
                })
                .step("drag " + label + " :press",
                        ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT))
                .step("drag " + label + " :move",
                        ctx -> ctx.input().dragTo(Mth.lerp(0.5f, from[0], to[0]), Mth.lerp(0.5f, from[1], to[1]),
                                Keys.MOUSE_LEFT))
                .step("drag " + label + " :arrive",
                        ctx -> ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT))
                // A jitter at the destination so the target sees a drag update while hovered, not only
                // the enter that arrived with it.
                .step("drag " + label + " :settle",
                        ctx -> ctx.input().dragTo(to[0] + 1, to[1], Keys.MOUSE_LEFT))
                .step("drag " + label + " :drop",
                        ctx -> ctx.input().mouseUp(to[0], to[1], Keys.MOUSE_LEFT));
    }

    private static Function<TestContext, UIElement> element(String selector) {
        return ctx -> ctx.el(selector).element();
    }

    /**
     * A port's connector dot. Both ends of a wire drag are only recognised on the connector itself,
     * so aiming at the port row would silently drag or drop nothing.
     */
    private static UIElement connectorIcon(TestContext ctx, String portKey) {
        var port = ctx.<PortModel>get(portKey);
        if (!(graphView(ctx).getModelElement(port) instanceof PortElement portElement)) {
            throw new IllegalStateException("no PortElement for " + portKey);
        }
        return portElement.getConnector().getConnectorIcon();
    }

    private static Function<TestContext, UIElement> connector(String portKey) {
        return ctx -> connectorIcon(ctx, portKey);
    }

    /** Drags to a fixed screen-space delta from wherever the source turned out to be. */
    private static BiFunction<TestContext, Vector2f, Vector2f> offsetBy(float dx, float dy) {
        return (ctx, origin) -> new Vector2f(origin.x + dx, origin.y + dy);
    }

    private static BiFunction<TestContext, Vector2f, Vector2f> centreOf(Function<TestContext, UIElement> target) {
        return (ctx, origin) -> {
            var bounds = ElementBounds.of(target.apply(ctx));
            return new Vector2f(bounds.centerX(), bounds.centerY());
        };
    }

    // endregion

    /**
     * A screen point guaranteed to be <em>on</em> a straight wire.
     *
     * <p>The wire is drawn as {@code [from, from+offset, to-offset, to]}; the two control-point
     * offsets are equal in size and opposite in direction, so the midpoint of the straight middle
     * segment is exactly the midpoint of the two endpoints. Aiming at the bounding-box centre would
     * not do — for anything but a perfectly straight wire that point is beside the line, not on it.
     */
    private static Vector2f wireMidpointOnScreen(TestContext ctx) {
        var wireElement = ctx.el(".__wire__").as(WireElement.class);
        var parent = wireElement.getParent();
        if (parent == null) throw new IllegalStateException("the wire element is not in the tree");
        // from/to are layout offsets relative to the wire layer; add the layer's own position back
        // before projecting, the inverse of the worldToLocalLayoutOffset the element applied.
        return parent.getWorldMouse(
                (wireElement.getFrom().x + wireElement.getTo().x) / 2f + parent.getPositionX(),
                (wireElement.getFrom().y + wireElement.getTo().y) / 2f + parent.getPositionY());
    }

    private static boolean contains(ElementBounds bounds, float x, float y) {
        return x >= bounds.x() && x <= bounds.right() && y >= bounds.y() && y <= bounds.bottom();
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static GraphModel graphModel(TestContext ctx) {
        var graph = graphView(ctx).getGraph();
        if (graph == null) throw new IllegalStateException("no graph loaded");
        return graph.graphModel;
    }

    /** The only wire. Valid until the fan-out group adds a second one. */
    private static WireModel wire(TestContext ctx) {
        return ctx.el(".__wire__").as(WireElement.class).getModel();
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

        // One producer, three consumers, one wire. The second and third consumers start unconnected:
        // one is reached by dragging out of the reroute point, the other by dragging its own input
        // port back onto the point — the two directions this feature has to support.
        var graph = new TestGraph();
        var producer = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var first = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));
        var second = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 170));
        var third = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 340));
        var otherProducer = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 340));
        var out = producer.getOutputsById().get("out");
        var firstIn = first.getInputsById().get("in1");
        var secondIn = second.getInputsById().get("in1");
        var thirdIn = third.getInputsById().get("in1");
        if (out == null || firstIn == null || secondIn == null || thirdIn == null) {
            throw new IllegalStateException("TestAddNode did not define the expected ports");
        }
        graph.graphModel.createWire(firstIn, out);
        ctx.put(SOURCE_PORT, out);
        ctx.put(FIRST_INPUT, firstIn);
        ctx.put(SECOND_INPUT, secondIn);
        ctx.put(THIRD_INPUT, thirdIn);
        ctx.put(OTHER_SOURCE, otherProducer.getOutputsById().get("out"));

        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
