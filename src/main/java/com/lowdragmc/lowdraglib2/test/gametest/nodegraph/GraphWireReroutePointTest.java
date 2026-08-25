package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireReroutePointModel;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

/**
 * Reroute points fan out like a wire portal but are pure layout, and those two facts are in tension:
 * these tests pin down that N wires can leave one point while every connection-side API still sees
 * nothing but N ordinary {@code fromPort → toPort} wires.
 */
public final class GraphWireReroutePointTest {
    private static final String CONNECTION_API_UNCHANGED = "graph_wire_reroute_point_connection_api_unchanged";
    private static final String FAN_OUT_SHARES_TRUNK = "graph_wire_reroute_point_fan_out_shares_trunk";
    private static final String BENDING_TRUNK_BENDS_BRANCHES = "graph_wire_reroute_point_bending_trunk_bends_branches";
    private static final String DELETING_SHARED_POINT_KEEPS_BRANCHES = "graph_wire_reroute_point_deleting_shared_point_keeps_branches";
    private static final String OUTLIVES_ONE_BRANCH_NOT_ALL = "graph_wire_reroute_point_outlives_one_branch_not_all";
    private static final String FAN_OUT_SURVIVES_SERIALIZATION = "graph_wire_reroute_point_fan_out_survives_serialization";
    private static final String FAN_OUT_SURVIVES_COPY_PASTE = "graph_wire_reroute_point_fan_out_survives_copy_paste";
    private static final String RE_SOURCING_MOVES_EVERY_BRANCH = "graph_wire_reroute_point_re_sourcing_moves_every_branch";
    private static final String RE_SOURCING_DETACHES_OLD_UPSTREAM = "graph_wire_reroute_point_re_sourcing_detaches_old_upstream";
    private static final String CENTRE_ROUND_TRIPS = "graph_wire_reroute_point_centre_round_trips";

    private GraphWireReroutePointTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(CONNECTION_API_UNCHANGED, GraphWireReroutePointTest::reroutePointsDoNotChangeConnectionApi);
        NodeGraphGameTests.registerFunction(FAN_OUT_SHARES_TRUNK, GraphWireReroutePointTest::fanOutFromReroutePointSharesTheTrunk);
        NodeGraphGameTests.registerFunction(BENDING_TRUNK_BENDS_BRANCHES, GraphWireReroutePointTest::bendingASharedTrunkBendsEveryBranch);
        NodeGraphGameTests.registerFunction(DELETING_SHARED_POINT_KEEPS_BRANCHES, GraphWireReroutePointTest::deletingASharedReroutePointKeepsEveryBranch);
        NodeGraphGameTests.registerFunction(OUTLIVES_ONE_BRANCH_NOT_ALL, GraphWireReroutePointTest::reroutePointOutlivesOneBranchButNotAll);
        NodeGraphGameTests.registerFunction(FAN_OUT_SURVIVES_SERIALIZATION, GraphWireReroutePointTest::fanOutSurvivesSerialization);
        NodeGraphGameTests.registerFunction(FAN_OUT_SURVIVES_COPY_PASTE, GraphWireReroutePointTest::fanOutSurvivesCopyPaste);
        NodeGraphGameTests.registerFunction(RE_SOURCING_MOVES_EVERY_BRANCH, GraphWireReroutePointTest::reSourcingAReroutePointMovesEveryBranch);
        NodeGraphGameTests.registerFunction(RE_SOURCING_DETACHES_OLD_UPSTREAM, GraphWireReroutePointTest::reSourcingDetachesTheOldUpstream);
        NodeGraphGameTests.registerFunction(CENTRE_ROUND_TRIPS, GraphWireReroutePointTest::reroutePointCentreRoundTrips);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, CONNECTION_API_UNCHANGED, NodeGraphGameTests.functionKey(CONNECTION_API_UNCHANGED), testData);
        NodeGraphGameTests.registerFunctionTest(event, FAN_OUT_SHARES_TRUNK, NodeGraphGameTests.functionKey(FAN_OUT_SHARES_TRUNK), testData);
        NodeGraphGameTests.registerFunctionTest(event, BENDING_TRUNK_BENDS_BRANCHES, NodeGraphGameTests.functionKey(BENDING_TRUNK_BENDS_BRANCHES), testData);
        NodeGraphGameTests.registerFunctionTest(event, DELETING_SHARED_POINT_KEEPS_BRANCHES, NodeGraphGameTests.functionKey(DELETING_SHARED_POINT_KEEPS_BRANCHES), testData);
        NodeGraphGameTests.registerFunctionTest(event, OUTLIVES_ONE_BRANCH_NOT_ALL, NodeGraphGameTests.functionKey(OUTLIVES_ONE_BRANCH_NOT_ALL), testData);
        NodeGraphGameTests.registerFunctionTest(event, FAN_OUT_SURVIVES_SERIALIZATION, NodeGraphGameTests.functionKey(FAN_OUT_SURVIVES_SERIALIZATION), testData);
        NodeGraphGameTests.registerFunctionTest(event, FAN_OUT_SURVIVES_COPY_PASTE, NodeGraphGameTests.functionKey(FAN_OUT_SURVIVES_COPY_PASTE), testData);
        NodeGraphGameTests.registerFunctionTest(event, RE_SOURCING_MOVES_EVERY_BRANCH, NodeGraphGameTests.functionKey(RE_SOURCING_MOVES_EVERY_BRANCH), testData);
        NodeGraphGameTests.registerFunctionTest(event, RE_SOURCING_DETACHES_OLD_UPSTREAM, NodeGraphGameTests.functionKey(RE_SOURCING_DETACHES_OLD_UPSTREAM), testData);
        NodeGraphGameTests.registerFunctionTest(event, CENTRE_ROUND_TRIPS, NodeGraphGameTests.functionKey(CENTRE_ROUND_TRIPS), testData);
    }

    // ------------------------------------------------------------------

    /**
     * The headline guarantee: routing a wire through a point changes nothing an NGT consumer can
     * observe. Ports, per-port wire lists, the port-wire index and the node/wire counts must all be
     * identical before and after.
     */
    public static void reroutePointsDoNotChangeConnectionApi(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var producer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var consumer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(300, 0));
        var out = producer.getOutputsById().get("out");
        var in = consumer.getInputsById().get("in1");
        if (out == null || in == null) { helper.fail("setup: ports missing"); return; }
        var wire = graphModel.createWire(in, out);

        int nodesBefore = countNonNull(graphModel.getNodeModels());
        int wiresBefore = countNonNull(graphModel.getWireModels());

        graphModel.insertReroutePointOnWire(wire, 0, new Vector2f(100, 60));
        graphModel.insertReroutePointOnWire(wire, 1, new Vector2f(200, 60));

        assertEq(helper, "reroute point count", 2, wire.getReroutePoints().size());
        if (wire.getFromPort() != out) { helper.fail("fromPort changed after routing"); return; }
        if (wire.getToPort() != in) { helper.fail("toPort changed after routing"); return; }
        assertEq(helper, "node count", nodesBefore, countNonNull(graphModel.getNodeModels()));
        assertEq(helper, "wire count", wiresBefore, countNonNull(graphModel.getWireModels()));
        assertEq(helper, "wires on output port", 1, out.getConnectedWires().size());
        assertEq(helper, "wires on input port", 1, in.getConnectedWires().size());
        assertEq(helper, "port-wire index (output)", 1, graphModel.getWiresForPort(out).size());
        assertEq(helper, "port-wire index (input)", 1, graphModel.getWiresForPort(in).size());
        if (!out.getConnectedPorts().contains(in) || !in.getConnectedPorts().contains(out)) {
            helper.fail("the ports no longer report each other as connected"); return;
        }

        for (var point : wire.getReroutePoints()) {
            if (graphModel.getModel(point.getUid()) != point) {
                helper.fail("reroute point " + point.getUid() + " not registered in the graph"); return;
            }
            if (point.getSourcePort() != out) {
                helper.fail("reroute point does not resolve back to the source port"); return;
            }
        }

        helper.succeed();
    }

    /**
     * The reason a reroute point exists: several wires leave the same point and share the routing
     * that leads to it, exactly like a wire portal — while the graph still holds two plain wires
     * from the same output port.
     */
    public static void fanOutFromReroutePointSharesTheTrunk(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var producer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var first = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));
        var second = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 200));
        var out = producer.getOutputsById().get("out");
        var firstIn = first.getInputsById().get("in1");
        var secondIn = second.getInputsById().get("in1");
        if (out == null || firstIn == null || secondIn == null) { helper.fail("setup: ports missing"); return; }

        var wireA = graphModel.createWire(firstIn, out);
        var trunk = graphModel.insertReroutePointOnWire(wireA, 0, new Vector2f(200, 0));

        // The fan-out: a second wire from the same output, drawn through the same point.
        var wireB = graphModel.createWire(secondIn, out);
        wireB.setRouteVia(trunk);

        // One point, two branches.
        assertEq(helper, "reroute point count", 1, countNonNull(graphModel.getWireReroutePointModels()));
        assertEq(helper, "wires through the trunk", 2, graphModel.getWiresThroughReroutePoint(trunk).size());
        if (wireA.getReroutePoints().get(0) != trunk || wireB.getReroutePoints().get(0) != trunk) {
            helper.fail("the two branches do not share the same reroute point instance"); return;
        }

        // ...and to the connection API this is just an output port with two wires on it.
        assertEq(helper, "wire count", 2, countNonNull(graphModel.getWireModels()));
        assertEq(helper, "wires on the output port", 2, out.getConnectedWires().size());
        assertEq(helper, "port-wire index", 2, graphModel.getWiresForPort(out).size());
        if (wireA.getFromPort() != out || wireB.getFromPort() != out) {
            helper.fail("a branch does not come from the original output port"); return;
        }
        if (wireA.getToPort() != firstIn || wireB.getToPort() != secondIn) {
            helper.fail("a branch does not end at its own input port"); return;
        }
        if (!out.getConnectedPorts().contains(firstIn) || !out.getConnectedPorts().contains(secondIn)) {
            helper.fail("the output port does not report both destinations as connected"); return;
        }
        assertEq(helper, "node count (a reroute point is not a node)", 3,
                countNonNull(graphModel.getNodeModels()));

        helper.succeed();
    }

    /**
     * Bending a shared trunk bends every branch that travels along it — the splice happens upstream
     * of the point, so the whole subtree inherits it without anyone walking the wire list.
     */
    public static void bendingASharedTrunkBendsEveryBranch(GameTestHelper helper) {
        var graph = fannedOutGraph();
        var graphModel = graph.graphModel;
        var trunk = countNonNull(graphModel.getWireReroutePointModels()) == 1
                ? graphModel.getWireReroutePointModels().get(0) : null;
        if (trunk == null) { helper.fail("setup: expected exactly one reroute point"); return; }
        var wires = graphModel.getWiresThroughReroutePoint(trunk);
        assertEq(helper, "branches", 2, wires.size());

        // Bend segment 0 — the piece leaving the output port, which both branches share.
        var upstream = graphModel.insertReroutePointOnWire(wires.get(0), 0, new Vector2f(100, 100));

        assertEq(helper, "reroute point count", 2, countNonNull(graphModel.getWireReroutePointModels()));
        if (upstream.getUpstream() != null) { helper.fail("the new point should start the chain"); return; }
        if (trunk.getUpstream() != upstream) { helper.fail("the trunk was not spliced onto the new point"); return; }
        for (var wire : wires) {
            var chain = wire.getReroutePoints();
            if (chain.size() != 2 || chain.get(0) != upstream || chain.get(1) != trunk) {
                helper.fail("branch did not inherit the shared bend: " + chain); return;
            }
        }

        // Bending the final segment of one branch is that branch's business alone.
        var tail = graphModel.insertReroutePointOnWire(wires.get(0), 2, new Vector2f(300, 100));
        assertEq(helper, "bent branch chain", 3, wires.get(0).getReroutePoints().size());
        assertEq(helper, "other branch chain", 2, wires.get(1).getReroutePoints().size());
        if (wires.get(1).getReroutePoints().contains(tail)) {
            helper.fail("a final-segment bend leaked into the other branch"); return;
        }

        helper.succeed();
    }

    /**
     * Deleting a shared point straightens every branch by one bend and disconnects none of them.
     */
    public static void deletingASharedReroutePointKeepsEveryBranch(GameTestHelper helper) {
        var graphModel = fannedOutGraph().graphModel;
        var trunk = graphModel.getWireReroutePointModels().get(0);
        var wires = graphModel.getWiresThroughReroutePoint(trunk);
        var fromPort = wires.get(0).getFromPort();
        var trunkUid = trunk.getUid();

        graphModel.deleteElements(List.of(trunk));

        assertEq(helper, "wire count after deleting the shared point", 2,
                countNonNull(graphModel.getWireModels()));
        assertEq(helper, "reroute point count", 0, countNonNull(graphModel.getWireReroutePointModels()));
        assertEq(helper, "wires still on the output port", 2, fromPort.getConnectedWires().size());
        for (var wire : wires) {
            if (wire.getFromPort() != fromPort || wire.getToPort() == null) {
                helper.fail("deleting the shared point disturbed a branch's ports"); return;
            }
            if (!wire.getReroutePoints().isEmpty()) {
                helper.fail("branch did not straighten: " + wire.getReroutePoints()); return;
            }
        }
        if (graphModel.getModel(trunkUid) != null) {
            helper.fail("deleted reroute point is still registered in the graph"); return;
        }
        if (!graphModel.getCurrentGraphChangeDescription().getDeletedModels().contains(trunkUid)) {
            helper.fail("deleted reroute point was not reported — the editor would keep a stale dot");
            return;
        }

        helper.succeed();
    }

    /**
     * A point only exists to shape the wires through it, so it survives exactly as long as one of
     * them does. Deleting one branch keeps it; deleting the last one sweeps it away.
     */
    public static void reroutePointOutlivesOneBranchButNotAll(GameTestHelper helper) {
        var graphModel = fannedOutGraph().graphModel;
        var trunk = graphModel.getWireReroutePointModels().get(0);
        var trunkUid = trunk.getUid();
        var wires = graphModel.getWiresThroughReroutePoint(trunk);

        graphModel.deleteElements(List.of(wires.get(0)));
        assertEq(helper, "reroute point after one branch is deleted", 1,
                countNonNull(graphModel.getWireReroutePointModels()));
        if (graphModel.getModel(trunkUid) != trunk) {
            helper.fail("the point was swept while a branch still used it"); return;
        }

        graphModel.deleteElements(List.of(wires.get(1)));
        assertEq(helper, "reroute point after the last branch is deleted", 0,
                countNonNull(graphModel.getWireReroutePointModels()));
        if (graphModel.getModel(trunkUid) != null) {
            helper.fail("the orphaned point was not swept out of the uid map"); return;
        }
        if (!graphModel.getCurrentGraphChangeDescription().getDeletedModels().contains(trunkUid)) {
            helper.fail("the swept point was not reported as deleted"); return;
        }

        helper.succeed();
    }

    /**
     * Round-trip: positions, chain order and — crucially — the <em>sharing</em> survive save/load. A
     * fan-out that reloaded as two independent copies would drift apart the first time it is dragged.
     */
    public static void fanOutSurvivesSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graphModel = fannedOutGraph().graphModel;
        var trunk = graphModel.getWireReroutePointModels().get(0);
        // A second bend upstream, so the chain has depth as well as breadth.
        var upstream = graphModel.insertReroutePointOnWire(
                graphModel.getWiresThroughReroutePoint(trunk).get(0), 0, new Vector2f(111, 222));
        var trunkUid = trunk.getUid();
        var upstreamUid = upstream.getUid();

        var serialized = serializeGraph(graphModel, provider);
        var restored = new TestGraph().graphModel;
        deserializeGraph(restored, serialized, provider);

        assertEq(helper, "restored reroute point count", 2,
                countNonNull(restored.getWireReroutePointModels()));
        var restoredTrunk = restored.getModel(trunkUid) instanceof WireReroutePointModel p ? p : null;
        var restoredUpstream = restored.getModel(upstreamUid) instanceof WireReroutePointModel p ? p : null;
        if (restoredTrunk == null || restoredUpstream == null) {
            helper.fail("a reroute point did not survive the round trip"); return;
        }
        assertPos(helper, "upstream position", new Vector2f(111, 222), restoredUpstream.getPosition());
        if (restoredTrunk.getUpstream() != restoredUpstream) {
            helper.fail("the chain link did not survive the round trip"); return;
        }

        var restoredBranches = restored.getWiresThroughReroutePoint(restoredTrunk);
        assertEq(helper, "restored branches", 2, restoredBranches.size());
        for (var wire : restoredBranches) {
            var chain = wire.getReroutePoints();
            if (chain.size() != 2 || chain.get(0) != restoredUpstream || chain.get(1) != restoredTrunk) {
                helper.fail("restored branch has the wrong chain: " + chain); return;
            }
            if (wire.getFromPort() == null || wire.getToPort() == null) {
                helper.fail("restored branch lost an endpoint"); return;
            }
        }
        // Same instance for both branches, not two lookalike copies.
        if (restoredBranches.get(0).getRouteVia() != restoredBranches.get(1).getRouteVia()) {
            helper.fail("the branches reloaded as separate points instead of a shared one"); return;
        }

        helper.succeed();
    }

    /**
     * Copy/paste carries the routing, shifted by the paste offset, with fresh uids — and the pasted
     * fan-out is still a fan-out rather than two parallel copies of the trunk.
     */
    public static void fanOutSurvivesCopyPaste(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graphModel = fannedOutGraph().graphModel;
        var originalTrunk = graphModel.getWireReroutePointModels().get(0);
        var originalNodes = List.copyOf(graphModel.getNodeModels());

        var copyData = graphModel.copyElements(originalNodes, provider);
        graphModel.pasteElements(copyData, new Vector2f(40, 20));

        assertEq(helper, "reroute points after paste", 2,
                countNonNull(graphModel.getWireReroutePointModels()));
        WireReroutePointModel pastedTrunk = null;
        for (var point : graphModel.getWireReroutePointModels()) {
            if (point != null && point != originalTrunk) pastedTrunk = point;
        }
        if (pastedTrunk == null) { helper.fail("paste did not produce a second reroute point"); return; }

        assertPos(helper, "pasted trunk position",
                originalTrunk.getPosition().add(40, 20, new Vector2f()), pastedTrunk.getPosition());
        assertEq(helper, "pasted branches", 2, graphModel.getWiresThroughReroutePoint(pastedTrunk).size());
        assertEq(helper, "original branches untouched", 2,
                graphModel.getWiresThroughReroutePoint(originalTrunk).size());
        if (pastedTrunk.getUid().equals(originalTrunk.getUid())) {
            helper.fail("the pasted point reused the original uid"); return;
        }

        helper.succeed();
    }

    /**
     * The other direction: dropping an <em>output</em> port onto a reroute point re-sources every
     * branch hanging off it in one edit. Unlike the rest of the feature this really does change the
     * connections, so the assertion is that all of them moved and none of them broke.
     */
    public static void reSourcingAReroutePointMovesEveryBranch(GameTestHelper helper) {
        var graphModel = fannedOutGraph().graphModel;
        var trunk = graphModel.getWireReroutePointModels().get(0);
        var branches = graphModel.getWiresThroughReroutePoint(trunk);
        var originalSource = trunk.getSourcePort();

        // A second producer to re-source onto.
        var newProducer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 400));
        var newSource = newProducer.getOutputsById().get("out");
        if (newSource == null) { helper.fail("setup: no output port on the new producer"); return; }

        graphModel.setReroutePointSource(trunk, newSource);

        assertEq(helper, "wire count is unchanged", 2, countNonNull(graphModel.getWireModels()));
        assertEq(helper, "every branch moved to the new source", 2, newSource.getConnectedWires().size());
        assertEq(helper, "the old source kept none", 0, originalSource.getConnectedWires().size());
        for (var wire : branches) {
            if (wire.getFromPort() != newSource) {
                helper.fail("a branch was left on the old source"); return;
            }
            if (wire.getToPort() == null) { helper.fail("a branch lost its destination"); return; }
            if (!wire.getReroutePoints().contains(trunk)) {
                helper.fail("a branch stopped travelling through the point"); return;
            }
        }
        if (trunk.getSourcePort() != newSource) {
            helper.fail("the point does not resolve to the new source"); return;
        }

        helper.succeed();
    }

    /**
     * Re-sourcing detaches the point from its own upstream chain. Leaving it attached would let one
     * chain feed two different source ports, which no wire through it could agree on.
     */
    public static void reSourcingDetachesTheOldUpstream(GameTestHelper helper) {
        var graphModel = fannedOutGraph().graphModel;
        var trunk = graphModel.getWireReroutePointModels().get(0);
        var branches = graphModel.getWiresThroughReroutePoint(trunk);
        // Give the trunk an upstream, then re-source only the trunk.
        var upstream = graphModel.insertReroutePointOnWire(branches.get(0), 0, new Vector2f(100, 0));
        if (trunk.getUpstream() != upstream) { helper.fail("setup: upstream not spliced in"); return; }

        var newProducer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 400));
        var newSource = newProducer.getOutputsById().get("out");
        graphModel.setReroutePointSource(trunk, newSource);

        if (trunk.getUpstream() != null) {
            helper.fail("the point is still hanging off its old chain"); return;
        }
        for (var wire : graphModel.getWiresThroughReroutePoint(trunk)) {
            var chain = wire.getReroutePoints();
            if (chain.size() != 1 || chain.get(0) != trunk) {
                helper.fail("a branch still routes through the detached upstream: " + chain); return;
            }
        }
        // The abandoned upstream fed nothing else, so it is swept.
        assertEq(helper, "reroute point count after the sweep", 1,
                countNonNull(graphModel.getWireReroutePointModels()));

        helper.succeed();
    }

    /**
     * A point's centre — the spot wires are actually routed through — sits half a dot down-right of
     * its {@link WireReroutePointModel#getPosition()} top-left, and the conversion round-trips.
     */
    public static void reroutePointCentreRoundTrips(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var centre = new Vector2f(120, 80);
        var point = graphModel.createWireReroutePoint(
                WireReroutePointModel.centerToPosition(centre), null, null);
        assertPos(helper, "centre from position", centre, point.getCenter());

        point.setCenter(new Vector2f(200, 30));
        assertPos(helper, "position from centre",
                new Vector2f(200 - WireReroutePointModel.WIDTH / 2f, 30 - WireReroutePointModel.HEIGHT / 2f),
                point.getPosition());
        assertPos(helper, "centre after setCenter", new Vector2f(200, 30), point.getCenter());

        // The two sides wires actually attach to sit on either side of that centre.
        var input = point.getInputAnchor();
        var output = point.getOutputAnchor();
        if (!(input.x < point.getCenter().x && output.x > point.getCenter().x)) {
            helper.fail("the input anchor should sit left of centre and the output right: "
                    + input + " / " + output);
            return;
        }
        if (Math.abs(input.y - output.y) > 0.001f) {
            helper.fail("the two anchors should share a row: " + input + " / " + output);
            return;
        }

        helper.succeed();
    }

    // --- Helpers ---

    /**
     * One producer feeding two consumers, both wires drawn through a single shared reroute point.
     */
    private static TestGraph fannedOutGraph() {
        var graph = new TestGraph();
        var graphModel = graph.graphModel;
        NodeModel producer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        NodeModel first = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));
        NodeModel second = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 200));
        var out = producer.getOutputsById().get("out");
        var wireA = graphModel.createWire(first.getInputsById().get("in1"), out);
        var trunk = graphModel.insertReroutePointOnWire(wireA, 0, new Vector2f(200, 0));
        var wireB = graphModel.createWire(second.getInputsById().get("in1"), out);
        wireB.setRouteVia(trunk);
        return graph;
    }

    private static CompoundTag serializeGraph(CustomGraphModelImpl graphModel, HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(CustomGraphModelImpl graphModel, CompoundTag tag, HolderLookup.Provider provider) {
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
    }

    private static int countNonNull(List<?> list) {
        return (int) list.stream().filter(Objects::nonNull).count();
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertPos(GameTestHelper helper, String label, Vector2f expected, Vector2f actual) {
        if (Math.abs(expected.x - actual.x) > 0.001f || Math.abs(expected.y - actual.y) > 0.001f) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }
}
