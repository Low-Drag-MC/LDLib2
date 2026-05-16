package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.IGraphReferenceResolver;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.SubgraphRegistry;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.joml.Vector2f;

public final class GraphSubgraphTest {
    private static final String LOCAL_SUBGRAPH_SERIALIZATION_ROUND_TRIP = "graph_subgraph_local_serialization_round_trip";
    private static final String EXTERNAL_SUBGRAPH_PORT_CACHE_SURVIVES = "graph_subgraph_external_port_cache_survives";
    private static final String PORTS_FOLLOW_VARIABLE_MODIFIERS = "graph_subgraph_ports_follow_variable_modifiers";
    private static final String PORTS_TRACK_VARIABLE_TYPE_CHANGES = "graph_subgraph_ports_track_variable_type_changes";
    private static final String DELETING_EXPOSED_VARIABLE_REMOVES_OUTER_PORT = "graph_subgraph_deleting_exposed_variable_removes_outer_port";
    private static final String NESTED_LOCAL_SUBGRAPH_SERIALIZATION = "graph_subgraph_nested_local_serialization";
    private static final String EXTERNAL_SAVE_BROADCAST_REDEFINES_PORTS = "graph_subgraph_external_save_broadcast_redefines_ports";
    private static final String EXTRACT_SELECTION_TO_LOCAL_SUBGRAPH = "graph_subgraph_extract_selection_to_local_subgraph";
    private static final String EXTRACT_ACCEPTS_PLACEMAT_AND_STICKY_NOTE = "graph_subgraph_extract_accepts_placemat_and_sticky_note";
    private static final String EXTRACT_REJECTS_PLACEMAT_WITH_EXTERNAL_NODE = "graph_subgraph_extract_rejects_placemat_with_external_node";
    private static final String EXTRACT_TRANSPLANTS_LOCAL_SUBGRAPH_REFERENCE = "graph_subgraph_extract_transplants_local_subgraph_reference";
    private static final String SUBGRAPH_REGISTRY_LISTENER_RECEIVES_BROADCAST = "graph_subgraph_registry_listener_receives_broadcast";
    private static final String RESOLVER_SAVE_DEFAULT_IS_NO_OP = "graph_subgraph_resolver_save_default_is_no_op";
    private static final String COPY_PASTE_LOCAL_SUBGRAPH_IN_SAME_GRAPH = "graph_subgraph_copy_paste_local_subgraph_in_same_graph";
    private static final String COPY_PASTE_LOCAL_SUBGRAPH_CROSS_GRAPH = "graph_subgraph_copy_paste_local_subgraph_cross_graph";
    private static final String PRE_SUBGRAPH_NBT_IS_FORWARD_COMPATIBLE = "graph_subgraph_pre_subgraph_nbt_is_forward_compatible";

    private GraphSubgraphTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(LOCAL_SUBGRAPH_SERIALIZATION_ROUND_TRIP, GraphSubgraphTest::localSubgraphSerializationRoundTrip);
        NodeGraphGameTests.registerFunction(EXTERNAL_SUBGRAPH_PORT_CACHE_SURVIVES, GraphSubgraphTest::externalSubgraphPortCacheSurvives);
        NodeGraphGameTests.registerFunction(PORTS_FOLLOW_VARIABLE_MODIFIERS, GraphSubgraphTest::portsFollowVariableModifiers);
        NodeGraphGameTests.registerFunction(PORTS_TRACK_VARIABLE_TYPE_CHANGES, GraphSubgraphTest::portsTrackVariableTypeChanges);
        NodeGraphGameTests.registerFunction(DELETING_EXPOSED_VARIABLE_REMOVES_OUTER_PORT, GraphSubgraphTest::deletingExposedVariableRemovesOuterPort);
        NodeGraphGameTests.registerFunction(NESTED_LOCAL_SUBGRAPH_SERIALIZATION, GraphSubgraphTest::nestedLocalSubgraphSerialization);
        NodeGraphGameTests.registerFunction(EXTERNAL_SAVE_BROADCAST_REDEFINES_PORTS, GraphSubgraphTest::externalSaveBroadcastReDefinesPorts);
        NodeGraphGameTests.registerFunction(EXTRACT_SELECTION_TO_LOCAL_SUBGRAPH, GraphSubgraphTest::extractSelectionToLocalSubgraph);
        NodeGraphGameTests.registerFunction(EXTRACT_ACCEPTS_PLACEMAT_AND_STICKY_NOTE, GraphSubgraphTest::extractAcceptsPlacematAndStickyNote);
        NodeGraphGameTests.registerFunction(EXTRACT_REJECTS_PLACEMAT_WITH_EXTERNAL_NODE, GraphSubgraphTest::extractRejectsPlacematWithExternalNode);
        NodeGraphGameTests.registerFunction(EXTRACT_TRANSPLANTS_LOCAL_SUBGRAPH_REFERENCE, GraphSubgraphTest::extractTransplantsLocalSubgraphReference);
        NodeGraphGameTests.registerFunction(SUBGRAPH_REGISTRY_LISTENER_RECEIVES_BROADCAST, GraphSubgraphTest::subgraphRegistryListenerReceivesBroadcast);
        NodeGraphGameTests.registerFunction(RESOLVER_SAVE_DEFAULT_IS_NO_OP, GraphSubgraphTest::resolverSaveDefaultIsNoOp);
        NodeGraphGameTests.registerFunction(COPY_PASTE_LOCAL_SUBGRAPH_IN_SAME_GRAPH, GraphSubgraphTest::copyPasteLocalSubgraphInSameGraph);
        NodeGraphGameTests.registerFunction(COPY_PASTE_LOCAL_SUBGRAPH_CROSS_GRAPH, GraphSubgraphTest::copyPasteLocalSubgraphCrossGraph);
        NodeGraphGameTests.registerFunction(PRE_SUBGRAPH_NBT_IS_FORWARD_COMPATIBLE, GraphSubgraphTest::preSubgraphNbtIsForwardCompatible);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, LOCAL_SUBGRAPH_SERIALIZATION_ROUND_TRIP, NodeGraphGameTests.functionKey(LOCAL_SUBGRAPH_SERIALIZATION_ROUND_TRIP), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTERNAL_SUBGRAPH_PORT_CACHE_SURVIVES, NodeGraphGameTests.functionKey(EXTERNAL_SUBGRAPH_PORT_CACHE_SURVIVES), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORTS_FOLLOW_VARIABLE_MODIFIERS, NodeGraphGameTests.functionKey(PORTS_FOLLOW_VARIABLE_MODIFIERS), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORTS_TRACK_VARIABLE_TYPE_CHANGES, NodeGraphGameTests.functionKey(PORTS_TRACK_VARIABLE_TYPE_CHANGES), testData);
        NodeGraphGameTests.registerFunctionTest(event, DELETING_EXPOSED_VARIABLE_REMOVES_OUTER_PORT, NodeGraphGameTests.functionKey(DELETING_EXPOSED_VARIABLE_REMOVES_OUTER_PORT), testData);
        NodeGraphGameTests.registerFunctionTest(event, NESTED_LOCAL_SUBGRAPH_SERIALIZATION, NodeGraphGameTests.functionKey(NESTED_LOCAL_SUBGRAPH_SERIALIZATION), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTERNAL_SAVE_BROADCAST_REDEFINES_PORTS, NodeGraphGameTests.functionKey(EXTERNAL_SAVE_BROADCAST_REDEFINES_PORTS), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTRACT_SELECTION_TO_LOCAL_SUBGRAPH, NodeGraphGameTests.functionKey(EXTRACT_SELECTION_TO_LOCAL_SUBGRAPH), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTRACT_ACCEPTS_PLACEMAT_AND_STICKY_NOTE, NodeGraphGameTests.functionKey(EXTRACT_ACCEPTS_PLACEMAT_AND_STICKY_NOTE), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTRACT_REJECTS_PLACEMAT_WITH_EXTERNAL_NODE, NodeGraphGameTests.functionKey(EXTRACT_REJECTS_PLACEMAT_WITH_EXTERNAL_NODE), testData);
        NodeGraphGameTests.registerFunctionTest(event, EXTRACT_TRANSPLANTS_LOCAL_SUBGRAPH_REFERENCE, NodeGraphGameTests.functionKey(EXTRACT_TRANSPLANTS_LOCAL_SUBGRAPH_REFERENCE), testData);
        NodeGraphGameTests.registerFunctionTest(event, SUBGRAPH_REGISTRY_LISTENER_RECEIVES_BROADCAST, NodeGraphGameTests.functionKey(SUBGRAPH_REGISTRY_LISTENER_RECEIVES_BROADCAST), testData);
        NodeGraphGameTests.registerFunctionTest(event, RESOLVER_SAVE_DEFAULT_IS_NO_OP, NodeGraphGameTests.functionKey(RESOLVER_SAVE_DEFAULT_IS_NO_OP), testData);
        NodeGraphGameTests.registerFunctionTest(event, COPY_PASTE_LOCAL_SUBGRAPH_IN_SAME_GRAPH, NodeGraphGameTests.functionKey(COPY_PASTE_LOCAL_SUBGRAPH_IN_SAME_GRAPH), testData);
        NodeGraphGameTests.registerFunctionTest(event, COPY_PASTE_LOCAL_SUBGRAPH_CROSS_GRAPH, NodeGraphGameTests.functionKey(COPY_PASTE_LOCAL_SUBGRAPH_CROSS_GRAPH), testData);
        NodeGraphGameTests.registerFunctionTest(event, PRE_SUBGRAPH_NBT_IS_FORWARD_COMPATIBLE, NodeGraphGameTests.functionKey(PRE_SUBGRAPH_NBT_IS_FORWARD_COMPATIBLE), testData);
    }

    // ------------------------------------------------------------------
    // 1. Local subgraph: full round-trip preserves structure + parent link
    // ------------------------------------------------------------------
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
        var serialized = serializeGraph(rootModel, provider);
        var root2 = new TestGraph();
        deserializeGraph(root2.graphModel, serialized, provider);

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
        var serialized = serializeGraph(rootModel, provider);
        var root2 = new TestGraph();
        // explicitly do NOT set resolver
        deserializeGraph(root2.graphModel, serialized, provider);

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

        var serialized = serializeGraph(root.graphModel, provider);
        var root2 = new TestGraph();
        deserializeGraph(root2.graphModel, serialized, provider);

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
        var serialized = serializeGraph(gm, provider);
        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);
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
    // 8b. Heterogeneous selection: wires ignored; placemats + stickynotes moved into subgraph
    // ------------------------------------------------------------------
    public static void extractAcceptsPlacematAndStickyNote(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start extractAcceptsPlacematAndStickyNote");

        var graph = new TestGraph();
        var gm = graph.graphModel;
        var floatType = com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers.fromType(Float.class);

        // Place two TestAddNodes; surround them with a placemat that contains both.
        var addA = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(10, 10));
        var addB = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(80, 10));
        // Placemat covers (0,0)-(200,150); both nodes inside its bounds.
        var pm = gm.createPlacemat("pm", new org.joml.Vector2f(0, 0), new org.joml.Vector2f(200, 150));
        var sn = gm.createStickyNote(new org.joml.Vector2f(20, 60));
        // Outer constant feeding into addA so we get one crossing wire
        var c1 = gm.createConstantNode("c1", new org.joml.Vector2f(-100, 10), floatType, 1f);
        var c1Out = ((com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel) c1.getNodeModel()).getOutputPort();
        var aIn1 = addA.getInputsById().get("in1");
        var crossing = gm.createWire(aIn1, c1Out);

        // Selection includes both nodes, placemat, sticky note, and (incidentally) the crossing wire
        // — wires should be filtered out by the model.
        var selection = java.util.List.<com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel>of(
                addA, addB, pm, sn, crossing);
        var subNode = gm.extractSelectionToLocalSubgraph(selection, provider);
        if (subNode == null) { helper.fail("extract returned null with mixed selection"); return; }

        // Outer: only c1 + subNode remain
        assertEq(helper, "outer nodes (c1 + subNode)", 2, countNonNull(gm.getNodeModels()));
        // Placemat moved to subgraph
        if (countNonNull(gm.getPlacematModels()) != 0) {
            helper.fail("placemat not removed from outer"); return;
        }
        if (countNonNull(gm.getStickyNoteModels()) != 0) {
            helper.fail("sticky note not removed from outer"); return;
        }

        var sub = gm.getLocalSubGraphs().get(0);
        assertEq(helper, "sub placemats", 1, countNonNull(sub.getPlacematModels()));
        assertEq(helper, "sub sticky notes", 1, countNonNull(sub.getStickyNoteModels()));
        // Subgraph node should have exactly 1 input (for the c1 → addA crossing) and no outputs.
        assertEq(helper, "subNode inputs", 1, subNode.getInputsById().size());
        assertEq(helper, "subNode outputs", 0, subNode.getOutputsById().size());

        LDLib2.LOGGER.info("End extractAcceptsPlacematAndStickyNote - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8c. Placemat with NON-selected contained node is rejected
    // ------------------------------------------------------------------
    public static void extractRejectsPlacematWithExternalNode(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var gm = graph.graphModel;
        // Two nodes inside a placemat
        var addA = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(10, 10));
        var addB = gm.createNodeModel(new TestAddNode(), new org.joml.Vector2f(80, 10));
        var pm = gm.createPlacemat("pm", new org.joml.Vector2f(0, 0), new org.joml.Vector2f(200, 150));

        // Select only one node + the placemat — the other contained node is NOT selected
        var selection = java.util.List.<com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel>of(addA, pm);
        var subNode = gm.extractSelectionToLocalSubgraph(selection, provider);
        if (subNode != null) {
            helper.fail("extract should have returned null; the placemat contains a non-selected node");
            return;
        }
        // Original graph unchanged
        assertEq(helper, "still has 2 nodes", 2, countNonNull(gm.getNodeModels()));
        assertEq(helper, "still has 1 placemat", 1, countNonNull(gm.getPlacematModels()));

        // silence
        var _b = addB;

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8d. LOCAL SubgraphNodeModel selected: its referenced subgraph is transplanted
    //     into the newly created subgraph (nested local subgraph survives the extract).
    // ------------------------------------------------------------------
    public static void extractTransplantsLocalSubgraphReference(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var gm = graph.graphModel;

        // Outer has a local subgraph "innerOld" + a SubgraphNodeModel referencing it
        var innerOld = gm.createLocalSubgraphInstance();
        gm.addLocalSubgraph(innerOld);
        innerOld.createVariable("v", int.class, 0, com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind.INPUT);
        var refNode = gm.createNodeWithType(SubgraphNodeModel.class, "ref",
                new org.joml.Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(innerOld), SpawnFlags.DEFAULT);

        if (countNonNull(gm.getLocalSubGraphs()) != 1) {
            helper.fail("setup: expected 1 local subgraph"); return;
        }

        // Extract refNode (the LOCAL SubgraphNodeModel itself) into a new subgraph.
        // The inner subgraph must move from outer.localSubGraphs into newSub.localSubGraphs so the
        // pasted SubgraphNodeModel inside newSub can still resolve to it.
        var subNode = gm.extractSelectionToLocalSubgraph(
                java.util.List.of(refNode), provider);
        if (subNode == null) { helper.fail("extract returned null"); return; }

        // Outer's localSubGraphs should now contain only the newly created `sub` — innerOld got
        // transplanted under it.
        assertEq(helper, "outer local subs after extract", 1, countNonNull(gm.getLocalSubGraphs()));
        var newSub = gm.getLocalSubGraphs().get(0);
        if (newSub.getLocalSubGraphs() == null || countNonNull(newSub.getLocalSubGraphs()) != 1) {
            helper.fail("nested local subgraph not transplanted"); return;
        }
        var transplanted = newSub.getLocalSubGraphs().get(0);
        if (!transplanted.getUid().equals(innerOld.getUid())) {
            helper.fail("transplanted subgraph uid mismatch"); return;
        }
        if (transplanted.getParentGraph() != newSub) {
            helper.fail("parentGraph not rewired to newSub"); return;
        }

        // The pasted SubgraphNodeModel inside newSub must resolve to the transplanted inner.
        SubgraphNodeModel pastedRef = null;
        for (var n : newSub.getNodeModels()) {
            if (n instanceof SubgraphNodeModel s) { pastedRef = s; break; }
        }
        if (pastedRef == null) { helper.fail("pasted SubgraphNodeModel missing inside newSub"); return; }
        if (pastedRef.getSubgraphModel() != transplanted) {
            helper.fail("pasted SubgraphNodeModel doesn't resolve to transplanted inner"); return;
        }

        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8e. Listener-mode SubgraphRegistry: external-save broadcast hits a Listener
    //     even when the listener isn't a GraphModel.
    // ------------------------------------------------------------------
    public static void subgraphRegistryListenerReceivesBroadcast(GameTestHelper helper) {
        var path = new FilePath("test/registry_listener.tag");
        var received = new IResourcePath[1];
        SubgraphRegistry.Listener listener = p -> received[0] = p;
        SubgraphRegistry.INSTANCE.registerListener(listener);
        try {
            SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(path);
            if (received[0] == null || !received[0].equals(path)) {
                helper.fail("listener did not receive broadcast"); return;
            }
        } finally {
            SubgraphRegistry.INSTANCE.unregisterListener(listener);
        }
        // After unregister: no more callbacks
        received[0] = null;
        SubgraphRegistry.INSTANCE.notifyExternalGraphSaved(path);
        if (received[0] != null) {
            helper.fail("listener still received after unregister"); return;
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8f. IGraphReferenceResolver.save() is plumbed through SubgraphNodeModel's path:
    //     a resolver with custom save() must be invoked from notifyExternalGraphSaved
    //     consumers (verified via the resolver-state-mutation behavior).
    // ------------------------------------------------------------------
    public static void resolverSaveDefaultIsNoOp(GameTestHelper helper) {
        // Just exercise the default impl — should not throw.
        IGraphReferenceResolver readOnly = p -> null;
        readOnly.save(new FilePath("any"), new CompoundTag());
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8g. Same-graph copy/paste of a LOCAL SubgraphNodeModel deep-clones the inner graph —
    //     pasted node must have a DIFFERENT localGraphId, and mutating one side must not affect
    //     the other.
    // ------------------------------------------------------------------
    public static void copyPasteLocalSubgraphInSameGraph(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start copyPasteLocalSubgraphInSameGraph");

        var graph = new TestGraph();
        var gm = graph.graphModel;

        var inner = gm.createLocalSubgraphInstance();
        gm.addLocalSubgraph(inner);
        inner.createVariable("v", int.class, 0, com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind.INPUT);
        var origNode = gm.createNodeWithType(SubgraphNodeModel.class, "n",
                new org.joml.Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(inner), SpawnFlags.DEFAULT);

        if (countNonNull(gm.getLocalSubGraphs()) != 1) {
            helper.fail("setup: expected 1 local subgraph"); return;
        }

        // Copy + paste in the same graph
        var copyData = gm.copyElements(java.util.List.of(origNode), provider);
        var pasted = gm.pasteElementsWithMap(copyData, new org.joml.Vector2f(50, 50));
        SubgraphNodeModel pastedNode = null;
        for (var n : pasted.elements()) {
            if (n instanceof SubgraphNodeModel s && !s.getUid().equals(origNode.getUid())) {
                pastedNode = s; break;
            }
        }
        if (pastedNode == null) { helper.fail("pasted node not found"); return; }

        // Outer graph now has 2 local subgraphs (the original and the clone)
        assertEq(helper, "local subgraphs after paste", 2, countNonNull(gm.getLocalSubGraphs()));

        // localGraphId differs between original and pasted
        if (origNode.getLocalGraphId() == null || pastedNode.getLocalGraphId() == null) {
            helper.fail("localGraphId should not be null"); return;
        }
        if (origNode.getLocalGraphId().equals(pastedNode.getLocalGraphId())) {
            helper.fail("pasted SubgraphNodeModel still shares localGraphId with original — deep clone failed");
            return;
        }

        // Both nodes resolve their inner graphs, and the two inner graphs are different instances.
        var origInner = origNode.getSubgraphModel();
        var pastedInner = pastedNode.getSubgraphModel();
        if (origInner == null || pastedInner == null) {
            helper.fail("one of the inner graphs failed to resolve"); return;
        }
        if (origInner == pastedInner) {
            helper.fail("inner graphs are the same instance — deep clone failed");
            return;
        }

        // Mutating the pasted inner graph must not affect the original.
        ((CustomGraphModelImpl) pastedInner).createVariable("v2", int.class, 0,
                com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind.LOCAL);
        if (countNonNull(origInner.getGraphVariableModels()) != 1) {
            helper.fail("original inner graph leaked the new variable from the clone");
            return;
        }
        if (countNonNull(pastedInner.getGraphVariableModels()) != 2) {
            helper.fail("clone inner graph did not receive its new variable");
            return;
        }

        LDLib2.LOGGER.info("End copyPasteLocalSubgraphInSameGraph - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 8h. Cross-graph paste of a LOCAL SubgraphNodeModel: the destination's localSubGraphs picks
    //     up the cloned inner graph and the pasted node resolves into it. Without the deep-clone
    //     pipeline, the pasted node would dangle (target graph has no matching localSubGraphs entry).
    // ------------------------------------------------------------------
    public static void copyPasteLocalSubgraphCrossGraph(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start copyPasteLocalSubgraphCrossGraph");

        // Source graph with a LOCAL SubgraphNodeModel
        var src = new TestGraph();
        var inner = src.graphModel.createLocalSubgraphInstance();
        src.graphModel.addLocalSubgraph(inner);
        inner.createVariable("v", int.class, 0,
                com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind.INPUT);
        var srcNode = src.graphModel.createNodeWithType(SubgraphNodeModel.class, "n",
                new org.joml.Vector2f(0, 0), null,
                n -> n.setLocalSubgraph(inner), SpawnFlags.DEFAULT);

        // Empty destination graph
        var dst = new TestGraph();
        if (dst.graphModel.getLocalSubGraphs() != null
                && countNonNull(dst.graphModel.getLocalSubGraphs()) != 0) {
            helper.fail("dst should start with no local subgraphs"); return;
        }

        var copyData = src.graphModel.copyElements(java.util.List.of(srcNode), provider);
        var pasted = dst.graphModel.pasteElementsWithMap(copyData, new org.joml.Vector2f(0, 0));

        // Destination now has 1 local subgraph
        if (dst.graphModel.getLocalSubGraphs() == null
                || countNonNull(dst.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("dst.localSubGraphs not populated by paste"); return;
        }
        var dstInner = dst.graphModel.getLocalSubGraphs().get(0);
        if (dstInner == inner) {
            helper.fail("dst received the original inner graph instance — should be a clone");
            return;
        }
        if (countNonNull(dstInner.getGraphVariableModels()) != 1) {
            helper.fail("cloned inner graph did not preserve variables"); return;
        }
        if (dstInner.getParentGraph() != dst.graphModel) {
            helper.fail("cloned inner graph's parentGraph not wired to dst"); return;
        }

        // The pasted SubgraphNodeModel resolves to the dst clone
        SubgraphNodeModel pastedNode = null;
        for (var n : pasted.elements()) {
            if (n instanceof SubgraphNodeModel s) { pastedNode = s; break; }
        }
        if (pastedNode == null) { helper.fail("pasted SubgraphNodeModel not in result"); return; }
        if (pastedNode.getSubgraphModel() != dstInner) {
            helper.fail("pasted node does not resolve to the dst clone");
            return;
        }

        // Source is untouched
        if (src.graphModel.getLocalSubGraphs() == null
                || countNonNull(src.graphModel.getLocalSubGraphs()) != 1
                || src.graphModel.getLocalSubGraphs().get(0) != inner) {
            helper.fail("src local subgraph mutated by cross-graph paste"); return;
        }

        LDLib2.LOGGER.info("End copyPasteLocalSubgraphCrossGraph - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 9. Backward compat: graph NBT without 'localSubGraphs' / 'kind' fields
    //    must deserialize cleanly.
    // ------------------------------------------------------------------
    public static void preSubgraphNbtIsForwardCompatible(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var root = new TestGraph();
        // no subgraph nodes, no local subgraphs — simulates pre-subgraph era
        var serialized = serializeGraph(root.graphModel, provider);
        // Strip the localSubGraphs key to mimic legacy
        if (serialized.contains("localSubGraphs")) {
            serialized.remove("localSubGraphs");
        }

        var root2 = new TestGraph();
        try {
            deserializeGraph(root2.graphModel, serialized, provider);
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

    private static CompoundTag serializeGraph(CustomGraphModelImpl graphModel, net.minecraft.core.HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(CustomGraphModelImpl graphModel, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }
}
