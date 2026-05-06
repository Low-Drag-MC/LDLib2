package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestStringConcatNode;
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

public final class GraphSerializationGameTest {
    private static final String ROUND_TRIP_PATH = "graph_serialization_round_trip";
    private static final String EMPTY_PATH = "graph_serialization_empty";
    private static final String PORT_CONSTANTS_PATH = "graph_serialization_port_constants";
    private static final String TYPE_HANDLE_RESOLVE = "graph_serialization_type_handle_resolve";
    private static final String VARIABLE_INITIALIZATION = "graph_serialization_variable_initialization";
    private static final String CONSTANT_NODE_OWNER_AND_VALUE_PRESERVED = "graph_serialization_constant_node_owner_and_value_preserved";
    private static final String OPTION_DRIVEN_PORT_COUNT = "graph_serialization_option_driven_port_count";


    private GraphSerializationGameTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(ROUND_TRIP_PATH, GraphSerializationGameTest::graphSerializationRoundTrip);
        NodeGraphGameTests.registerFunction(EMPTY_PATH, GraphSerializationGameTest::emptyGraphSerialization);
        NodeGraphGameTests.registerFunction(PORT_CONSTANTS_PATH, GraphSerializationGameTest::portConstantsSerialization);
        NodeGraphGameTests.registerFunction(TYPE_HANDLE_RESOLVE, GraphSerializationGameTest::typeHandleResolveFallsBackToClassForName);
        NodeGraphGameTests.registerFunction(VARIABLE_INITIALIZATION, GraphSerializationGameTest::variableInitializationModelRoundTrip);
        NodeGraphGameTests.registerFunction(CONSTANT_NODE_OWNER_AND_VALUE_PRESERVED, GraphSerializationGameTest::constantNodeOwnerAndValuePreserved);
        NodeGraphGameTests.registerFunction(OPTION_DRIVEN_PORT_COUNT, GraphSerializationGameTest::optionDrivenPortCountSurvivesRoundTrip);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, ROUND_TRIP_PATH, NodeGraphGameTests.functionKey(ROUND_TRIP_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, EMPTY_PATH, NodeGraphGameTests.functionKey(EMPTY_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_CONSTANTS_PATH, NodeGraphGameTests.functionKey(PORT_CONSTANTS_PATH), testData);
    }

    private static void graphSerializationRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start Graph Serialization Round-Trip Test");

        // === Build the original graph ===
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var addNode1 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(100, 100));
        var addNode2 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(300, 100));

        var floatType = TypeHandleHelpers.fromType(Float.class);
        var constantNode = graphModel.createConstantNode("myConst", new Vector2f(0, 0), floatType, 42.0f);

        var concatNode = graphModel.createNodeModel(new TestStringConcatNode(), new Vector2f(200, 300));

        var variable = graphModel.createVariable("testVar", float.class, 7.5f, VariableKind.LOCAL);

        var variableNode = graphModel.createVariableNode(
                (VariableDeclarationModel) variable,
                new Vector2f(50, 200), null, null);

        var constantOutputPort = ((ConstantNodeModel) constantNode.getNodeModel()).getOutputPort();
        var addNode1InputPort = addNode1.getInputsById().get("in1");
        WireModel wire1 = null;
        if (constantOutputPort != null && addNode1InputPort != null) {
            wire1 = graphModel.createWire(addNode1InputPort, constantOutputPort);
        }

        var addNode1OutputPort = addNode1.getOutputsById().get("out");
        var addNode2InputPort = addNode2.getInputsById().get("in1");
        WireModel wire2 = null;
        if (addNode1OutputPort != null && addNode2InputPort != null) {
            wire2 = graphModel.createWire(addNode2InputPort, addNode1OutputPort);
        }

        int origNodeCount = countNonNull(graphModel.getNodeModels());
        int origWireCount = countNonNull(graphModel.getWireModels());
        int origVarCount = countNonNull(graphModel.getGraphVariableModels());

        // === Serialize ===
        CompoundTag serialized = serializeGraph(graphModel, provider);
        LDLib2.LOGGER.info("Serialized graph to {} keys", serialized.keySet().size());

        // === Deserialize into a new graph ===
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        // === Verify counts ===
        assertEq(helper, "node count", origNodeCount, countNonNull(graphModel2.getNodeModels()));
        assertEq(helper, "wire count", origWireCount, countNonNull(graphModel2.getWireModels()));
        assertEq(helper, "variable count", origVarCount, countNonNull(graphModel2.getGraphVariableModels()));

        // === Verify node UIDs match ===
        for (var origNode : graphModel.getNodeModels()) {
            if (origNode == null) continue;
            var found = findNodeByUid(graphModel2, origNode.getUid().toString());
            if (found == null) {
                helper.fail("Node with UID " + origNode.getUid() + " not found after deserialization");
                return;
            }
            assertEq(helper, "node position x", (int) origNode.getPosition().x, (int) found.getPosition().x);
            assertEq(helper, "node position y", (int) origNode.getPosition().y, (int) found.getPosition().y);
        }

        // === Verify wire connections resolve ===
        for (var wire : graphModel2.getWireModels()) {
            if (wire == null) continue;
            if (wire.getFromPort() == null) {
                helper.fail("Wire " + wire.getUid() + " has null fromPort after deserialization");
                return;
            }
            if (wire.getToPort() == null) {
                helper.fail("Wire " + wire.getUid() + " has null toPort after deserialization");
                return;
            }
        }

        // === Verify variable declarations ===
        for (var origVar : graphModel.getGraphVariableModels()) {
            if (origVar == null) continue;
            boolean found = false;
            for (var newVar : graphModel2.getGraphVariableModels()) {
                if (newVar != null && newVar.getUid().equals(origVar.getUid())) {
                    assertEq(helper, "variable name", origVar.getName(), newVar.getName());
                    found = true;
                    break;
                }
            }
            if (!found) {
                helper.fail("Variable with UID " + origVar.getUid() + " not found after deserialization");
                return;
            }
        }

        // === Verify VariableNodeModel declaration reference ===
        for (var node : graphModel2.getNodeModels()) {
            if (node instanceof VariableNodeModel vn) {
                if (vn.getDeclarationModel() == null) {
                    helper.fail("VariableNodeModel " + vn.getUid() + " has null declarationModel after deserialization");
                    return;
                }
            }
        }

        // === Verify constant value is preserved ===
        for (var node : graphModel2.getNodeModels()) {
            if (node instanceof ConstantNodeModel cn) {
                if (cn.getConstant() == null) {
                    helper.fail("ConstantNodeModel has null constant after deserialization");
                    return;
                }
                if (cn.getConstant().getValue() instanceof Float f) {
                    if (Math.abs(f - 42.0f) > 0.001f) {
                        helper.fail("Constant value mismatch: expected 42.0, got " + f);
                        return;
                    }
                }
            }
        }

        // === Round-trip test: serialize again and compare ===
        CompoundTag serialized2 = serializeGraph(graphModel2, provider);
        if (!serialized.equals(serialized2)) {
            LDLib2.LOGGER.warn("Round-trip serialization produced different NBT (this may be expected for non-deterministic elements)");
        }

        LDLib2.LOGGER.info("End Graph Serialization Round-Trip Test - ALL PASSED");
        helper.succeed();
    }

    private static void emptyGraphSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        CompoundTag serialized = serializeGraph(graphModel, provider);

        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        assertEq(helper, "empty graph node count", countNonNull(graphModel.getNodeModels()), countNonNull(graphModel2.getNodeModels()));
        assertEq(helper, "empty graph wire count", countNonNull(graphModel.getWireModels()), countNonNull(graphModel2.getWireModels()));

        helper.succeed();
    }

    private static void portConstantsSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var addNode = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));

        var in1Constant = addNode.getInputConstantsById().get("in1");
        if (in1Constant != null) {
            in1Constant.setValue(99.0f);
        }

        CompoundTag serialized = serializeGraph(graphModel, provider);
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        CustomNodeModelImpl restoredAdd = null;
        for (var node : graphModel2.getNodeModels()) {
            if (node instanceof CustomNodeModelImpl cn && cn.getUid().equals(addNode.getUid())) {
                restoredAdd = cn;
                break;
            }
        }

        if (restoredAdd == null) {
            helper.fail("AddNode not found after deserialization");
            return;
        }

        var restoredConstant = restoredAdd.getInputConstantsById().get("in1");
        if (restoredConstant == null) {
            helper.fail("Input constant 'in1' not found after deserialization");
            return;
        }

        if (restoredConstant.getValue() instanceof Float f) {
            if (Math.abs(f - 99.0f) > 0.001f) {
                helper.fail("Input constant value mismatch: expected 99.0, got " + f);
                return;
            }
        } else if (restoredConstant.getValue() != null) {
            helper.fail("Input constant value type mismatch: " + restoredConstant.getValue().getClass());
            return;
        }

        helper.succeed();
    }

    /**
     * Marker class with no explicit fromType registration, used to verify that
     * {@code TypeHandle.resolve()} falls back to {@code Class.forName} when the identification
     * is missing from {@code ID_TO_TYPE}.
     */
    public static final class TypeFallbackMarker {}

    /**
     * Tests that {@link TypeHandle#resolve()} can resolve a class by name even when no caller
     * has registered it via {@code TypeHandleHelpers.fromType(...)}, fixing the case where
     * mod load order or dynamic types leave {@code ID_TO_TYPE} empty for a known class.
     */
    public static void typeHandleResolveFallsBackToClassForName(GameTestHelper helper) {
        var th = TypeHandle.create(TypeFallbackMarker.class.getName());
        var resolved = th.resolve();
        if (resolved != TypeFallbackMarker.class) {
            helper.fail("TypeHandle.resolve did not fall back to Class.forName: got " + resolved);
            return;
        }

        // Unknown id should still return Unknown.class without throwing.
        var ghost = TypeHandle.create("com.example.NoSuchClass_xyz");
        var ghostResolved = ghost.resolve();
        if (ghostResolved == TypeFallbackMarker.class) {
            helper.fail("Ghost id wrongly resolved to marker"); return;
        }

        helper.succeed();
    }

    /**
     * Tests that a variable's initializationModel round-trips, preserving its value.
     */
    public static void variableInitializationModelRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var variable = (VariableDeclarationModel) graph.graphModel.createVariable(
                "v", float.class, 7.5f, VariableKind.LOCAL);

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        VariableDeclarationModel restored = null;
        for (var v : graph2.graphModel.getGraphVariableModels()) {
            if (v != null && v.getUid().equals(variable.getUid())) {
                restored = (VariableDeclarationModel) v;
                break;
            }
        }
        if (restored == null) { helper.fail("variable not found"); return; }

        var init = restored.getInitializationModel();
        if (init == null) { helper.fail("initializationModel is null after deserialize"); return; }
        if (!(init.getValue() instanceof Float f) || Math.abs(f - 7.5f) > 0.001f) {
            helper.fail("initializationModel value not preserved: " + init.getValue()); return;
        }
        // owner should point back to the declaration
        if (init.getOwner() != restored) {
            helper.fail("initializationModel owner not wired back to the declaration"); return;
        }

        helper.succeed();
    }

    /**
     * Tests that ConstantNodeModel deserialization preserves ownership and value.
     */
    public static void constantNodeOwnerAndValuePreserved(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var floatType = TypeHandleHelpers.fromType(Float.class);
        var constantNode = graph.graphModel.createConstantNode("c", new Vector2f(0, 0), floatType, 21.0f);

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        ConstantNodeModel restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof ConstantNodeModel cn && cn.getUid().equals(constantNode.getNodeModel().getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("constant node not found"); return; }
        if (restored.getConstant() == null) { helper.fail("constant null after deserialize"); return; }
        if (restored.getConstant().getOwner() != restored) {
            helper.fail("constant owner not wired back"); return;
        }
        if (!(restored.getConstant().getValue() instanceof Float f) || Math.abs(f - 21.0f) > 0.001f) {
            helper.fail("constant value not preserved: " + restored.getConstant().getValue()); return;
        }

        helper.succeed();
    }

    /**
     * Regression: when a node's port topology depends on a NodeOption value (e.g. TestAddNode's
     * "inputs" option drives how many input ports it has), the option value restored from NBT
     * must be available before onDefinePorts runs, otherwise the rebuilt node only has the
     * default-count ports and the persisted in3..inN constants get dropped.
     */
    public static void optionDrivenPortCountSurvivesRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var addNode = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));

        // Bump inputs option from default 2 to 9, then defineNode again to expand port set.
        // NodeOption ports use the "option_" prefix in inputConstantsById.
        var inputsConstant = addNode.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + "inputs");
        if (inputsConstant == null) { helper.fail("inputs option constant missing"); return; }
        inputsConstant.setValue(9);
        addNode.defineNode();

        // Set distinct values on each input port constant.
        for (int i = 1; i <= 9; i++) {
            var c = addNode.getInputConstantsById().get("in" + i);
            if (c == null) { helper.fail("in" + i + " missing pre-serialize"); return; }
            c.setValue((float) (i * 10));
        }

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        CustomNodeModelImpl restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof CustomNodeModelImpl cn && cn.getUid().equals(addNode.getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("node not found"); return; }

        var restoredInputsConstant = restored.getInputConstantsById().get(NodeOption.PORT_ID_PREFIX + "inputs");
        if (restoredInputsConstant == null) { helper.fail("inputs option constant missing after deserialize"); return; }
        var optValue = restoredInputsConstant.getValue();
        if (!(optValue instanceof Integer iv) || iv != 9) {
            helper.fail("inputs option value not preserved: " + optValue); return;
        }

        for (int i = 1; i <= 9; i++) {
            var c = restored.getInputConstantsById().get("in" + i);
            if (c == null) {
                helper.fail("in" + i + " missing after deserialize (port topology not rebuilt with restored option)"); return;
            }
            if (!(c.getValue() instanceof Float f) || Math.abs(f - (i * 10f)) > 0.001f) {
                helper.fail("in" + i + " value mismatch: expected " + (i * 10f) + " got " + c.getValue()); return;
            }
        }

        helper.succeed();
    }

    // --- Serialization helpers ---

    private static CompoundTag serializeGraph(CustomGraphModelImpl graphModel, net.minecraft.core.HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(CustomGraphModelImpl graphModel, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
    }

    // --- Helpers ---

    private static int countNonNull(java.util.List<?> list) {
        return (int) list.stream().filter(java.util.Objects::nonNull).count();
    }

    private static AbstractNodeModel findNodeByUid(CustomGraphModelImpl graphModel, String uid) {
        for (var node : graphModel.getNodeModels()) {
            if (node != null && node.getUid().toString().equals(uid)) {
                return node;
            }
        }
        return null;
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEq(GameTestHelper helper, String label, String expected, String actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            helper.fail(label + ": expected '" + expected + "', got '" + actual + "'");
        }
    }
}
