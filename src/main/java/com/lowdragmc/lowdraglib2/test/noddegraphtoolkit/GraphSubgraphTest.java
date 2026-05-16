package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.IGraphReferenceResolver;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.SubgraphRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

@GameTestHolder(LDLib2.MOD_ID)
public class GraphSubgraphTest {

    // ------------------------------------------------------------------
    // 1. Local subgraph: full round-trip preserves structure + parent link
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void localSubgraphSerializationRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start localSubgraphSerializationRoundTrip");

        var root = new TestGraph();
        var rootModel = root.graphModel;

        // Build inline subgraph + node
        var sub = rootModel.createLocalSubgraphInstance();
        if (sub == null) { helper.fail("createLocalSubgraphInstance returned null"); return; }
        rootModel.addLocalSubgraph(sub);
        var subNode = rootModel.createNodeWithType(SubgraphNodeModel.class, "sub",
                new Vector2f(50, 50), null,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        // Two exposed variables inside the subgraph
        var vIn = sub.createVariable("vIn", int.class, 0, VariableKind.INPUT);
        var vOut = sub.createVariable("vOut", String.class, "", VariableKind.OUTPUT);
        // create+addLocalSubgraph happens before subNode is fully wired — explicit redefine
        subNode.defineNode();

        assertEq(helper, "outer subNode inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "outer subNode outputs", 1, subNode.getOutputsById().size());

        // Round-trip
        var serialized = rootModel.serializeNBT(provider);
        var root2 = new TestGraph();
        root2.graphModel.deserializeNBT(provider, serialized);

        // localSubGraphs preserved + parent pointer rebuilt
        if (root2.graphModel.getLocalSubGraphs() == null
                || countNonNull(root2.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("localSubGraphs not restored");
            return;
        }
        var restoredSub = root2.graphModel.getLocalSubGraphs().get(0);
        if (restoredSub.getParentGraph() != root2.graphModel) {
            helper.fail("parentGraph not restored");
            return;
        }
        if (!restoredSub.getUid().equals(sub.getUid())) {
            helper.fail("subgraph uid mismatch after deserialize");
            return;
        }

        // SubgraphNodeModel restored + still LOCAL kind, linked
        SubgraphNodeModel restoredNode = null;
        for (var n : root2.graphModel.getNodeModels()) {
            if (n instanceof SubgraphNodeModel s && s.getUid().equals(subNode.getUid())) {
                restoredNode = s;
                break;
            }
        }
        if (restoredNode == null) { helper.fail("SubgraphNodeModel not restored"); return; }
        if (restoredNode.getKind() != SubgraphNodeModel.Kind.LOCAL) {
            helper.fail("kind mismatch: " + restoredNode.getKind());
            return;
        }
        if (restoredNode.getSubgraphModel() != restoredSub) {
            helper.fail("Restored subgraph node not linked to local subgraph");
            return;
        }
        assertEq(helper, "restored inputs", 1, restoredNode.getInputsById().size());
        assertEq(helper, "restored outputs", 1, restoredNode.getOutputsById().size());

        // Variables inside restored subgraph
        assertEq(helper, "restored sub variable count",
                2, countNonNull(restoredSub.getGraphVariableModels()));

        // silence unused warnings
        var _vIn = vIn; var _vOut = vOut;

        LDLib2.LOGGER.info("End localSubgraphSerializationRoundTrip - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 2. External subgraph: portCache restores port shape when unresolvable
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void externalSubgraphPortCacheSurvives(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start externalSubgraphPortCacheSurvives");

        // The "external" target graph we'll point to
        var external = new TestGraph();
        external.graphModel.createVariable("ein", int.class, 0, VariableKind.INPUT);
        external.graphModel.createVariable("eout", String.class, "", VariableKind.OUTPUT);

        var root = new TestGraph();
        var rootModel = root.graphModel;
        var path = new FilePath("test/sub_external.tag");

        // Resolver returns our standalone external graph
        IGraphReferenceResolver resolver = p -> p.equals(path) ? external : null;
        rootModel.setReferenceResolver(resolver);

        var subNode = rootModel.createNodeWithType(SubgraphNodeModel.class, "ext",
                new Vector2f(0, 0), null,
                n -> n.setExternalSubgraph(path), SpawnFlags.DEFAULT);
        subNode.defineNode();
        assertEq(helper, "pre-serialize inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "pre-serialize outputs", 1, subNode.getOutputsById().size());

        // Serialize, deserialize into a NEW root WITHOUT resolver — should still produce the same
        // port shape via portCache (with type-handles preserved).
        var serialized = rootModel.serializeNBT(provider);
        var root2 = new TestGraph();
        // explicitly do NOT set resolver
        root2.graphModel.deserializeNBT(provider, serialized);

        SubgraphNodeModel restoredNode = null;
        for (var n : root2.graphModel.getNodeModels()) {
            if (n instanceof SubgraphNodeModel s && s.getUid().equals(subNode.getUid())) {
                restoredNode = s;
                break;
            }
        }
        if (restoredNode == null) { helper.fail("external SubgraphNodeModel not restored"); return; }
        if (restoredNode.getKind() != SubgraphNodeModel.Kind.EXTERNAL) {
            helper.fail("kind mismatch: " + restoredNode.getKind());
            return;
        }
        // resolver is null → getSubgraphModel returns null → ports come from cache
        if (restoredNode.getSubgraphModel() != null) {
            helper.fail("getSubgraphModel should be null without resolver");
            return;
        }
        assertEq(helper, "cache-restored inputs", 1, restoredNode.getInputsById().size());
        assertEq(helper, "cache-restored outputs", 1, restoredNode.getOutputsById().size());

        // path round-trips
        var restoredPath = restoredNode.getExternalPath();
        if (restoredPath == null || !path.equals(restoredPath)) {
            helper.fail("external path not restored: " + restoredPath);
            return;
        }

        LDLib2.LOGGER.info("End externalSubgraphPortCacheSurvives - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 3. Variable modifier changes drive outer port direction
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void portsFollowVariableModifiers(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start portsFollowVariableModifiers");

        var root = new TestGraph();
        var sub = root.graphModel.createLocalSubgraphInstance();
        if (sub == null) { helper.fail("createLocalSubgraphInstance returned null"); return; }
        root.graphModel.addLocalSubgraph(sub);
        var subNode = root.graphModel.createNodeWithType(SubgraphNodeModel.class, "sub",
                new Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        // READ → input on outer
        var v1 = (VariableDeclarationModel) sub.createVariable("v1", int.class, 0, VariableKind.INPUT);
        subNode.defineNode();
        assertEq(helper, "READ → inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "READ → no outputs", 0, subNode.getOutputsById().size());

        // WRITE → output on outer
        v1.setModifiers(ModifierFlags.WRITE);
        assertEq(helper, "WRITE → no inputs", 0, subNode.getInputsById().size());
        assertEq(helper, "WRITE → outputs", 1, subNode.getOutputsById().size());

        // READ_WRITE → one input + one output (suffixed ids)
        v1.setModifiers(ModifierFlags.READ_WRITE);
        assertEq(helper, "READ_WRITE → inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "READ_WRITE → outputs", 1, subNode.getOutputsById().size());
        boolean inOk = subNode.getInputsById().keySet().stream().anyMatch(k -> k.endsWith("-in"));
        boolean outOk = subNode.getOutputsById().keySet().stream().anyMatch(k -> k.endsWith("-out"));
        if (!inOk || !outOk) {
            helper.fail("READ_WRITE port ids missing direction suffix: in=" + inOk + " out=" + outOk);
            return;
        }

        // NONE → no ports
        v1.setModifiers(ModifierFlags.NONE);
        assertEq(helper, "NONE → no inputs", 0, subNode.getInputsById().size());
        assertEq(helper, "NONE → no outputs", 0, subNode.getOutputsById().size());

        LDLib2.LOGGER.info("End portsFollowVariableModifiers - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 4. Variable type change: port type updates, ports keyed by variable uid
    //    so the same variable's port survives a rename (id-stable) but changes
    //    type when the variable's data type changes.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void portsTrackVariableTypeChanges(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start portsTrackVariableTypeChanges");

        var root = new TestGraph();
        var sub = root.graphModel.createLocalSubgraphInstance();
        if (sub == null) { helper.fail("createLocalSubgraphInstance returned null"); return; }
        root.graphModel.addLocalSubgraph(sub);
        var subNode = root.graphModel.createNodeWithType(SubgraphNodeModel.class, "sub",
                new Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        var v = (VariableDeclarationModel) sub.createVariable("v", int.class, 0, VariableKind.INPUT);
        subNode.defineNode();
        var ports = subNode.getInputsById();
        if (ports.size() != 1) { helper.fail("expected 1 input port, got " + ports.size()); return; }
        var port = ports.values().iterator().next();
        if (!port.getDataTypeHandle().equals(TypeHandles.INT)) {
            helper.fail("initial port type wrong: " + port.getDataTypeHandle());
            return;
        }

        // Change type — port should re-bind to new type
        v.setDataTypeHandle(TypeHandles.STRING);
        var ports2 = subNode.getInputsById();
        if (ports2.size() != 1) { helper.fail("after type change: expected 1 input, got " + ports2.size()); return; }
        var port2 = ports2.values().iterator().next();
        if (!port2.getDataTypeHandle().equals(TypeHandles.STRING)) {
            helper.fail("port type not updated to STRING: " + port2.getDataTypeHandle());
            return;
        }

        // Rename — port count stays, id (variable uid) stays
        v.setName("renamed");
        if (subNode.getInputsById().size() != 1) {
            helper.fail("rename caused port count drift");
            return;
        }

        LDLib2.LOGGER.info("End portsTrackVariableTypeChanges - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 5. Variable deletion: port disappears
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void deletingExposedVariableRemovesOuterPort(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start deletingExposedVariableRemovesOuterPort");

        var root = new TestGraph();
        var sub = root.graphModel.createLocalSubgraphInstance();
        if (sub == null) { helper.fail("createLocalSubgraphInstance returned null"); return; }
        root.graphModel.addLocalSubgraph(sub);
        var subNode = root.graphModel.createNodeWithType(SubgraphNodeModel.class, "sub",
                new Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        var v = (VariableDeclarationModel) sub.createVariable("v", int.class, 0, VariableKind.INPUT);
        subNode.defineNode();
        assertEq(helper, "before delete", 1, subNode.getInputsById().size());

        sub.deleteVariableDeclaration(v, true);
        assertEq(helper, "after delete", 0, subNode.getInputsById().size());

        LDLib2.LOGGER.info("End deletingExposedVariableRemovesOuterPort - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 6. Nested local subgraphs: 3 levels round-trip, parent pointers rebuilt
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void nestedLocalSubgraphSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start nestedLocalSubgraphSerialization");

        var root = new TestGraph();
        var subA = root.graphModel.createLocalSubgraphInstance();
        root.graphModel.addLocalSubgraph(subA);
        var subB = subA.createLocalSubgraphInstance();
        subA.addLocalSubgraph(subB);

        // node refs
        var nodeA = root.graphModel.createNodeWithType(SubgraphNodeModel.class, "A",
                new Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(subA), SpawnFlags.DEFAULT);
        var nodeB = subA.createNodeWithType(SubgraphNodeModel.class, "B",
                new Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(subB), SpawnFlags.DEFAULT);
        subB.createVariable("deep", int.class, 0, VariableKind.INPUT);
        nodeB.defineNode();
        nodeA.defineNode();

        var serialized = root.graphModel.serializeNBT(provider);
        var root2 = new TestGraph();
        root2.graphModel.deserializeNBT(provider, serialized);

        if (root2.graphModel.getLocalSubGraphs() == null
                || countNonNull(root2.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("root.localSubGraphs not restored"); return;
        }
        var rA = root2.graphModel.getLocalSubGraphs().get(0);
        if (rA.getParentGraph() != root2.graphModel) {
            helper.fail("A.parentGraph not root"); return;
        }
        if (rA.getLocalSubGraphs() == null || countNonNull(rA.getLocalSubGraphs()) != 1) {
            helper.fail("A.localSubGraphs not restored"); return;
        }
        var rB = rA.getLocalSubGraphs().get(0);
        if (rB.getParentGraph() != rA) {
            helper.fail("B.parentGraph not A"); return;
        }
        if (countNonNull(rB.getGraphVariableModels()) != 1) {
            helper.fail("B.variables not restored"); return;
        }

        LDLib2.LOGGER.info("End nestedLocalSubgraphSerialization - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 7. External save broadcast: SubgraphRegistry forwards path-save events
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void externalSaveBroadcastReDefinesPorts(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start externalSaveBroadcastReDefinesPorts");

        var externalA = new TestGraph();
        var externalB = new TestGraph();
        // initially external has 1 exposed variable
        externalA.graphModel.createVariable("ein", int.class, 0, VariableKind.INPUT);

        var path = new FilePath("test/sub_broadcast.tag");
        // Resolver returns externalA initially, then externalB after "save"
        final Graph[] target = { externalA };
        IGraphReferenceResolver resolver = p -> p.equals(path) ? target[0] : null;

        var root = new TestGraph();
        root.graphModel.setReferenceResolver(resolver);
        var subNode = root.graphModel.createNodeWithType(SubgraphNodeModel.class, "ext",
                new Vector2f(0, 0), null,
                n -> n.setExternalSubgraph(path), SpawnFlags.DEFAULT);
        subNode.defineNode();
        assertEq(helper, "initial inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "initial outputs", 0, subNode.getOutputsById().size());

        // Register root with SubgraphRegistry — emulates an editor opening this graph
        SubgraphRegistry.INSTANCE.register(root.graphModel);
        try {
            // The "external" was saved with a new shape: now an OUTPUT instead of an INPUT
            externalB.graphModel.createVariable("eout", String.class, "", VariableKind.OUTPUT);
            target[0] = externalB;

            SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(path);

            assertEq(helper, "post-broadcast inputs", 0, subNode.getInputsById().size());
            assertEq(helper, "post-broadcast outputs", 1, subNode.getOutputsById().size());
        } finally {
            SubgraphRegistry.INSTANCE.unregister(root.graphModel);
        }

        LDLib2.LOGGER.info("End externalSaveBroadcastReDefinesPorts - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8. Extract selection to subgraph: crossing wires get auto-variables and reconnects
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void extractSelectionToLocalSubgraph(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start extractSelectionToLocalSubgraph");

        var graph = new TestGraph();
        var gm = graph.graphModel;
        var floatType = com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers.fromType(Float.class);

        // Outer constants (external_OUT side)
        var c1 = gm.createConstantNode("c1", new org.joml.Vector2f(0, 0), floatType, 1f);
        var c2 = gm.createConstantNode("c2", new org.joml.Vector2f(0, 50), floatType, 2f);
        // Selection candidates
        var addA = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(200, 0));
        var addB = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(400, 0));
        // Outer consumer (external_IN side)
        var addC = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(600, 0));

        var c1Out = ((com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel) c1.getNodeModel()).getOutputPort();
        var c2Out = ((com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel) c2.getNodeModel()).getOutputPort();
        var aIn1 = addA.getInputsById().get("in1");
        var aIn2 = addA.getInputsById().get("in2");
        var aOut = addA.getOutputsById().get("out");
        var bIn1 = addB.getInputsById().get("in1");
        var bOut = addB.getOutputsById().get("out");
        var cIn1 = addC.getInputsById().get("in1");

        // c1.out → addA.in1   (crossing-READ)
        gm.createWire(aIn1, c1Out);
        // c2.out → addA.in2   (crossing-READ)
        gm.createWire(aIn2, c2Out);
        // addA.out → addB.in1 (internal)
        gm.createWire(bIn1, aOut);
        // addB.out → addC.in1 (crossing-WRITE)
        gm.createWire(cIn1, bOut);

        int wiresBefore = countNonNull(gm.getWireModels());
        int nodesBefore = countNonNull(gm.getNodeModels());

        // Selection: addA + addB
        var subNode = gm.extractSelectionToLocalSubgraph(
                java.util.List.of(addA, addB), provider);
        if (subNode == null) { helper.fail("extract returned null"); return; }

        // Outer graph: c1, c2, addC, subNode (4 nodes). Originals A, B removed.
        assertEq(helper, "outer node count after extract", 4, countNonNull(gm.getNodeModels()));
        // Local subgraph created
        if (gm.getLocalSubGraphs() == null || countNonNull(gm.getLocalSubGraphs()) != 1) {
            helper.fail("local subgraph not created"); return;
        }
        var sub = gm.getLocalSubGraphs().get(0);

        // Subgraph node has 2 inputs (for in1/in2 crossings) + 1 output (for out crossing)
        assertEq(helper, "subNode inputs", 2, subNode.getInputsById().size());
        assertEq(helper, "subNode outputs", 1, subNode.getOutputsById().size());

        // Variables inside: 2 READ + 1 WRITE = 3
        assertEq(helper, "sub variables", 3, countNonNull(sub.getGraphVariableModels()));
        int readCount = 0, writeCount = 0;
        for (var v : sub.getGraphVariableModels()) {
            if (v == null) continue;
            if (v.getModifiers() == ModifierFlags.READ) readCount++;
            else if (v.getModifiers() == ModifierFlags.WRITE) writeCount++;
        }
        assertEq(helper, "READ var count", 2, readCount);
        assertEq(helper, "WRITE var count", 1, writeCount);

        // Pasted nodes inside sub: 2 (addA, addB copies)
        long pastedTestAdds = sub.getNodeModels().stream()
                .filter(java.util.Objects::nonNull)
                .filter(n -> n instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl)
                .count();
        if (pastedTestAdds != 2) {
            helper.fail("expected 2 pasted custom nodes, got " + pastedTestAdds); return;
        }

        // Outer wires: c1→subNode, c2→subNode, subNode→addC = 3 wires.
        // Internal/old wires were deleted with addA/addB.
        int outerWires = countNonNull(gm.getWireModels());
        assertEq(helper, "outer wires after extract", 3, outerWires);

        // Each outer wire endpoint must include the SubgraphNodeModel
        for (var w : gm.getWireModels()) {
            if (w == null) continue;
            var fn = w.getFromPort().getNodeModel();
            var tn = w.getToPort().getNodeModel();
            if (fn != subNode && tn != subNode) {
                helper.fail("Outer wire doesn't touch subgraph node: " + w);
                return;
            }
        }

        // Round-trip the whole graph and confirm it deserializes
        var serialized = gm.serializeNBT(provider);
        var graph2 = new TestGraph();
        graph2.graphModel.deserializeNBT(provider, serialized);
        if (graph2.graphModel.getLocalSubGraphs() == null
                || countNonNull(graph2.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("local subgraph not restored after extract+round-trip");
            return;
        }

        // silence unused
        var _ignored = new int[] { wiresBefore, nodesBefore };

        LDLib2.LOGGER.info("End extractSelectionToLocalSubgraph - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 9. Backward compat: graph NBT without 'localSubGraphs' / 'kind' fields
    //    must deserialize cleanly.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void preSubgraphNbtIsForwardCompatible(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var root = new TestGraph();
        // no subgraph nodes, no local subgraphs — simulates pre-subgraph era
        var serialized = root.graphModel.serializeNBT(provider);
        // Strip the localSubGraphs key to mimic legacy
        if (serialized.contains("localSubGraphs")) {
            serialized.remove("localSubGraphs");
        }

        var root2 = new TestGraph();
        try {
            root2.graphModel.deserializeNBT(provider, serialized);
        } catch (Exception e) {
            helper.fail("legacy NBT deserialize threw: " + e.getMessage());
            return;
        }
        if (root2.graphModel.getLocalSubGraphs() != null
                && countNonNull(root2.graphModel.getLocalSubGraphs()) != 0) {
            helper.fail("legacy NBT produced non-empty localSubGraphs");
            return;
        }

        helper.succeed();
    }

    // --- Helpers ---

    private static int countNonNull(java.util.List<?> list) {
        if (list == null) return 0;
        return (int) list.stream().filter(java.util.Objects::nonNull).count();
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }
}
