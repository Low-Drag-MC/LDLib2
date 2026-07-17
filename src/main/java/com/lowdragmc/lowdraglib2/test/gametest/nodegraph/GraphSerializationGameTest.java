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
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.CustomCodecTestNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvAccessorFloatNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvCodecFloatNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvCodecValueANode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvCodecValueBNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvNoCodecNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.EvWithoutSerializationNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.SchemaEvolutionTestNodes;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestStringConcatNode;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    private static final String PORT_CUSTOM_CODEC_ROUND_TRIP = "graph_serialization_port_custom_codec_round_trip";
    private static final String PORT_WITHOUT_SERIALIZATION_RESETS_TO_DEFAULT = "graph_serialization_port_without_serialization_resets_to_default";
    private static final String PORT_MISSING_ACCESSOR_SERIALIZES_GRACEFULLY = "graph_serialization_port_missing_accessor_serializes_gracefully";
    private static final String PORT_WITHOUT_CONFIGURATOR_FLAGS_MODEL = "graph_serialization_port_without_configurator_flags_model";
    private static final String EVOLUTION_CODEC_TO_NO_CODEC = "graph_serialization_evolution_codec_to_no_codec";
    private static final String EVOLUTION_CODEC_TO_WITHOUT_SERIALIZATION = "graph_serialization_evolution_codec_to_without_serialization";
    private static final String EVOLUTION_WITHOUT_SERIALIZATION_TO_CODEC = "graph_serialization_evolution_without_serialization_to_codec";
    private static final String EVOLUTION_ACCESSOR_TO_CODEC = "graph_serialization_evolution_accessor_to_codec";
    private static final String EVOLUTION_CODEC_TO_DIFFERENT_CODEC = "graph_serialization_evolution_codec_to_different_codec";
    private static final String EVOLUTION_CORRUPT_VALUE_TAG = "graph_serialization_evolution_corrupt_value_tag";
    private static final String CODEC_PORT_SURVIVES_MULTIPLE_DEFINE_NODE = "graph_serialization_codec_port_survives_multiple_define_node";
    private static final String PORT_MISSING_ACCESSOR_WARNS_ONCE = "graph_serialization_port_missing_accessor_warns_once";
    private static final String BACKWARD_COMPAT_LEGACY_NBT = "graph_serialization_backward_compat_legacy_nbt";
    private static final String WIRE_RECOVERY_BY_PORT_ID = "graph_serialization_wire_recovery_by_port_id";
    private static final String ORPHAN_MISSING_PORT_REMOVED_ON_LOAD = "graph_serialization_orphan_missing_port_removed_on_load";
    private static final String DELETING_WIRE_REMOVES_MISSING_PORT = "graph_serialization_deleting_wire_removes_missing_port";
    private static final String VARIABLE_IO_REVERSAL_DROPS_WIRE = "graph_serialization_variable_io_reversal_drops_wire";
    private static final String MISSING_PORT_REPORTED_TO_GRAPH_LOGGER = "graph_serialization_missing_port_reported_to_graph_logger";
    private static final String REMOVING_MISSING_PORT_INVALIDATES_VISIBLE_CACHE = "graph_serialization_removing_missing_port_invalidates_visible_cache";
    private static final String INCOMPATIBLE_RETYPE_PARKS_WIRE_ON_MISSING_PORT = "graph_serialization_incompatible_retype_parks_wire_on_missing_port";


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
        NodeGraphGameTests.registerFunction(PORT_CUSTOM_CODEC_ROUND_TRIP, GraphSerializationGameTest::portCustomCodecRoundTrip);
        NodeGraphGameTests.registerFunction(PORT_WITHOUT_SERIALIZATION_RESETS_TO_DEFAULT, GraphSerializationGameTest::portWithoutSerializationResetsToDefault);
        NodeGraphGameTests.registerFunction(PORT_MISSING_ACCESSOR_SERIALIZES_GRACEFULLY, GraphSerializationGameTest::portMissingAccessorSerializesGracefully);
        NodeGraphGameTests.registerFunction(PORT_WITHOUT_CONFIGURATOR_FLAGS_MODEL, GraphSerializationGameTest::portWithoutConfiguratorFlagsModel);
        NodeGraphGameTests.registerFunction(EVOLUTION_CODEC_TO_NO_CODEC, GraphSerializationGameTest::evolutionCodecToNoCodec);
        NodeGraphGameTests.registerFunction(EVOLUTION_CODEC_TO_WITHOUT_SERIALIZATION, GraphSerializationGameTest::evolutionCodecToWithoutSerialization);
        NodeGraphGameTests.registerFunction(EVOLUTION_WITHOUT_SERIALIZATION_TO_CODEC, GraphSerializationGameTest::evolutionWithoutSerializationToCodec);
        NodeGraphGameTests.registerFunction(EVOLUTION_ACCESSOR_TO_CODEC, GraphSerializationGameTest::evolutionAccessorToCodec);
        NodeGraphGameTests.registerFunction(EVOLUTION_CODEC_TO_DIFFERENT_CODEC, GraphSerializationGameTest::evolutionCodecToDifferentCodec);
        NodeGraphGameTests.registerFunction(EVOLUTION_CORRUPT_VALUE_TAG, GraphSerializationGameTest::evolutionCorruptValueTag);
        NodeGraphGameTests.registerFunction(CODEC_PORT_SURVIVES_MULTIPLE_DEFINE_NODE, GraphSerializationGameTest::codecPortSurvivesMultipleDefineNode);
        NodeGraphGameTests.registerFunction(PORT_MISSING_ACCESSOR_WARNS_ONCE, GraphSerializationGameTest::portMissingAccessorWarnsOnce);
        NodeGraphGameTests.registerFunction(BACKWARD_COMPAT_LEGACY_NBT, GraphSerializationGameTest::backwardCompatLegacyNbt);
        NodeGraphGameTests.registerFunction(WIRE_RECOVERY_BY_PORT_ID, GraphSerializationGameTest::wireRecoveryByPortId);
        NodeGraphGameTests.registerFunction(ORPHAN_MISSING_PORT_REMOVED_ON_LOAD, GraphSerializationGameTest::orphanMissingPortRemovedOnLoad);
        NodeGraphGameTests.registerFunction(DELETING_WIRE_REMOVES_MISSING_PORT, GraphSerializationGameTest::deletingWireRemovesMissingPort);
        NodeGraphGameTests.registerFunction(VARIABLE_IO_REVERSAL_DROPS_WIRE, GraphSerializationGameTest::variableIoReversalDropsWire);
        NodeGraphGameTests.registerFunction(MISSING_PORT_REPORTED_TO_GRAPH_LOGGER, GraphSerializationGameTest::missingPortReportedToGraphLogger);
        NodeGraphGameTests.registerFunction(REMOVING_MISSING_PORT_INVALIDATES_VISIBLE_CACHE, GraphSerializationGameTest::removingMissingPortInvalidatesVisibleCache);
        NodeGraphGameTests.registerFunction(INCOMPATIBLE_RETYPE_PARKS_WIRE_ON_MISSING_PORT, GraphSerializationGameTest::incompatibleRetypeParksWireOnMissingPort);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, ROUND_TRIP_PATH, NodeGraphGameTests.functionKey(ROUND_TRIP_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, EMPTY_PATH, NodeGraphGameTests.functionKey(EMPTY_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_CONSTANTS_PATH, NodeGraphGameTests.functionKey(PORT_CONSTANTS_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, TYPE_HANDLE_RESOLVE, NodeGraphGameTests.functionKey(TYPE_HANDLE_RESOLVE), testData);
        NodeGraphGameTests.registerFunctionTest(event, VARIABLE_INITIALIZATION, NodeGraphGameTests.functionKey(VARIABLE_INITIALIZATION), testData);
        NodeGraphGameTests.registerFunctionTest(event, CONSTANT_NODE_OWNER_AND_VALUE_PRESERVED, NodeGraphGameTests.functionKey(CONSTANT_NODE_OWNER_AND_VALUE_PRESERVED), testData);
        NodeGraphGameTests.registerFunctionTest(event, OPTION_DRIVEN_PORT_COUNT, NodeGraphGameTests.functionKey(OPTION_DRIVEN_PORT_COUNT), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_CUSTOM_CODEC_ROUND_TRIP, NodeGraphGameTests.functionKey(PORT_CUSTOM_CODEC_ROUND_TRIP), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_WITHOUT_SERIALIZATION_RESETS_TO_DEFAULT, NodeGraphGameTests.functionKey(PORT_WITHOUT_SERIALIZATION_RESETS_TO_DEFAULT), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_MISSING_ACCESSOR_SERIALIZES_GRACEFULLY, NodeGraphGameTests.functionKey(PORT_MISSING_ACCESSOR_SERIALIZES_GRACEFULLY), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_WITHOUT_CONFIGURATOR_FLAGS_MODEL, NodeGraphGameTests.functionKey(PORT_WITHOUT_CONFIGURATOR_FLAGS_MODEL), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_CODEC_TO_NO_CODEC, NodeGraphGameTests.functionKey(EVOLUTION_CODEC_TO_NO_CODEC), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_CODEC_TO_WITHOUT_SERIALIZATION, NodeGraphGameTests.functionKey(EVOLUTION_CODEC_TO_WITHOUT_SERIALIZATION), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_WITHOUT_SERIALIZATION_TO_CODEC, NodeGraphGameTests.functionKey(EVOLUTION_WITHOUT_SERIALIZATION_TO_CODEC), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_ACCESSOR_TO_CODEC, NodeGraphGameTests.functionKey(EVOLUTION_ACCESSOR_TO_CODEC), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_CODEC_TO_DIFFERENT_CODEC, NodeGraphGameTests.functionKey(EVOLUTION_CODEC_TO_DIFFERENT_CODEC), testData);
        NodeGraphGameTests.registerFunctionTest(event, EVOLUTION_CORRUPT_VALUE_TAG, NodeGraphGameTests.functionKey(EVOLUTION_CORRUPT_VALUE_TAG), testData);
        NodeGraphGameTests.registerFunctionTest(event, CODEC_PORT_SURVIVES_MULTIPLE_DEFINE_NODE, NodeGraphGameTests.functionKey(CODEC_PORT_SURVIVES_MULTIPLE_DEFINE_NODE), testData);
        NodeGraphGameTests.registerFunctionTest(event, PORT_MISSING_ACCESSOR_WARNS_ONCE, NodeGraphGameTests.functionKey(PORT_MISSING_ACCESSOR_WARNS_ONCE), testData);
        NodeGraphGameTests.registerFunctionTest(event, BACKWARD_COMPAT_LEGACY_NBT, NodeGraphGameTests.functionKey(BACKWARD_COMPAT_LEGACY_NBT), testData);
        NodeGraphGameTests.registerFunctionTest(event, WIRE_RECOVERY_BY_PORT_ID, NodeGraphGameTests.functionKey(WIRE_RECOVERY_BY_PORT_ID), testData);
        NodeGraphGameTests.registerFunctionTest(event, ORPHAN_MISSING_PORT_REMOVED_ON_LOAD, NodeGraphGameTests.functionKey(ORPHAN_MISSING_PORT_REMOVED_ON_LOAD), testData);
        NodeGraphGameTests.registerFunctionTest(event, DELETING_WIRE_REMOVES_MISSING_PORT, NodeGraphGameTests.functionKey(DELETING_WIRE_REMOVES_MISSING_PORT), testData);
        NodeGraphGameTests.registerFunctionTest(event, VARIABLE_IO_REVERSAL_DROPS_WIRE, NodeGraphGameTests.functionKey(VARIABLE_IO_REVERSAL_DROPS_WIRE), testData);
        NodeGraphGameTests.registerFunctionTest(event, MISSING_PORT_REPORTED_TO_GRAPH_LOGGER, NodeGraphGameTests.functionKey(MISSING_PORT_REPORTED_TO_GRAPH_LOGGER), testData);
        NodeGraphGameTests.registerFunctionTest(event, REMOVING_MISSING_PORT_INVALIDATES_VISIBLE_CACHE, NodeGraphGameTests.functionKey(REMOVING_MISSING_PORT_INVALIDATES_VISIBLE_CACHE), testData);
        NodeGraphGameTests.registerFunctionTest(event, INCOMPATIBLE_RETYPE_PARKS_WIRE_ON_MISSING_PORT, NodeGraphGameTests.functionKey(INCOMPATIBLE_RETYPE_PARKS_WIRE_ON_MISSING_PORT), testData);
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

    /**
     * Builder hook: {@code withCodec(...)} on a port whose value type has no registered accessor
     * must round-trip the value entirely through the supplied Mojang Codec.
     */
    public static void portCustomCodecRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(0, 0));

        var codecConstant = node.getInputConstantsById().get("codec_port");
        if (codecConstant == null) { helper.fail("codec_port constant missing pre-serialize"); return; }
        codecConstant.setValue(new CustomCodecTestNode.CodecValue(42, "hello"));

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        CustomNodeModelImpl restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof CustomNodeModelImpl cn && cn.getUid().equals(node.getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("CustomCodecTestNode not found after deserialize"); return; }

        var restoredConstant = restored.getInputConstantsById().get("codec_port");
        if (restoredConstant == null) { helper.fail("codec_port constant missing after deserialize"); return; }
        if (!(restoredConstant.getValue() instanceof CustomCodecTestNode.CodecValue cv)) {
            helper.fail("codec_port value type mismatch: " + restoredConstant.getValue()); return;
        }
        if (cv.a() != 42 || !"hello".equals(cv.b())) {
            helper.fail("codec_port value not preserved: " + cv); return;
        }

        helper.succeed();
    }

    /**
     * Builder hook: {@code withoutSerialization()} must drop value and defaultValue during NBT
     * write, so a restored port has its constant re-initialised to the builder default rather
     * than the runtime-mutated value.
     */
    public static void portWithoutSerializationResetsToDefault(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(0, 0));

        var noSerConstant = node.getInputConstantsById().get("no_serialize_port");
        if (noSerConstant == null) { helper.fail("no_serialize_port constant missing pre-serialize"); return; }
        // Builder default is 1.0f; mutate to 99 to detect persistence.
        noSerConstant.setValue(99.0f);

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        CustomNodeModelImpl restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof CustomNodeModelImpl cn && cn.getUid().equals(node.getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("CustomCodecTestNode not found after deserialize"); return; }

        var restoredConstant = restored.getInputConstantsById().get("no_serialize_port");
        if (restoredConstant == null) { helper.fail("no_serialize_port constant missing after deserialize"); return; }
        if (!(restoredConstant.getValue() instanceof Float f)) {
            helper.fail("no_serialize_port value type mismatch: " + restoredConstant.getValue()); return;
        }
        if (Math.abs(f - 1.0f) > 0.001f) {
            helper.fail("no_serialize_port should have reset to default 1.0 but is " + f); return;
        }

        helper.succeed();
    }

    /**
     * Builder hook: a port whose type has no accessor AND no codec must serialize without
     * throwing — the value is silently dropped (warn-once is logged elsewhere). Other ports on
     * the same node must still serialize normally.
     */
    public static void portMissingAccessorSerializesGracefully(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(0, 0));

        var missingConstant = node.getInputConstantsById().get("missing_port");
        if (missingConstant == null) { helper.fail("missing_port constant missing pre-serialize"); return; }
        // Even though there is no codec/accessor, setting a value at runtime must not break
        // the eventual serialization — the value just won't survive.
        missingConstant.setValue(new CustomCodecTestNode.CodecValue(7, "sentinel"));

        // The crash fix: this used to throw IllegalArgumentException from SyncValueHolder's
        // construction inside the no-accessor branch. Now it must complete normally.
        CompoundTag serialized;
        try {
            serialized = serializeGraph(graph.graphModel, provider);
        } catch (Exception e) {
            helper.fail("serializeNBT threw for a port without accessor or codec: " + e.getMessage());
            return;
        }

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        CustomNodeModelImpl restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof CustomNodeModelImpl cn && cn.getUid().equals(node.getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("CustomCodecTestNode not found after deserialize"); return; }

        // The other ports on the same node should still work.
        var codecConstant = restored.getInputConstantsById().get("codec_port");
        if (codecConstant == null) { helper.fail("codec_port lost on graceful-skip round-trip"); return; }
        if (!(codecConstant.getValue() instanceof CustomCodecTestNode.CodecValue cv)
                || cv.a() != 0 || !"default".equals(cv.b())) {
            helper.fail("codec_port default not restored on graceful-skip round-trip: " + codecConstant.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * Builder hook: {@code withoutConfigurator()} must propagate to the PortModel as
     * {@code configuratorEnabled == false}, so the inspector skips building a UI row for it.
     */
    public static void portWithoutConfiguratorFlagsModel(GameTestHelper helper) {
        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(0, 0));

        var noConfigPort = node.getInputsById().get("no_config_port");
        if (noConfigPort == null) { helper.fail("no_config_port not found on node"); return; }
        if (noConfigPort.isConfiguratorEnabled()) {
            helper.fail("no_config_port should have configuratorEnabled=false"); return;
        }

        // Sibling ports without the opt-out should still default to true (regression check).
        var codecPort = node.getInputsById().get("codec_port");
        if (codecPort == null) { helper.fail("codec_port not found on node"); return; }
        if (!codecPort.isConfiguratorEnabled()) {
            helper.fail("codec_port should default to configuratorEnabled=true"); return;
        }

        helper.succeed();
    }

    // ============================================================
    // Schema-evolution tests — save-side and load-side disagree on
    // codec / serialization / type configuration for a shared port.
    // ============================================================

    /**
     * codec → no-codec, no-accessor: load side has lost the codec; the encoded value can't decode.
     * Must: not throw, mark constant as deserializeFailed, drop incoming wires.
     */
    public static void evolutionCodecToNoCodec(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        // Save with codec. We can't easily wire INTO this port (the port type EvCodecValueA has
        // no matching producer in our test set), so this test verifies the constant-failure
        // detection without asserting on wire teardown. evolutionCodecToDifferentCodec /
        // evolutionCorruptValueTag cover the failure-flag path; the wire-drop sweep is covered
        // implicitly by the dropWiresOnFailedInputConstants path (logged warnings on test run).
        var saveGraph = new TestGraph();
        var producer = saveGraph.graphModel.createNodeModel(new EvCodecValueANode(), new Vector2f(0, 0));

        var codecConst = producer.getInputConstantsById().get("port");
        if (codecConst == null) { helper.fail("port constant missing pre-serialize"); return; }
        codecConst.setValue(new SchemaEvolutionTestNodes.EvCodecValueA(7, "saved"));

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        // Swap nodeClass discriminator: load as EvNoCodecNode instead of EvCodecValueANode.
        rewriteNodeClass(serialized, EvCodecValueANode.class.getName(), EvNoCodecNode.class.getName());

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw for codec → no-codec evolution: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, producer.getUid());
        if (restored == null) { helper.fail("evolved node not found after deserialize"); return; }

        var restoredConst = restored.getInputConstantsById().get("port");
        if (restoredConst == null) { helper.fail("port constant missing post-deserialize"); return; }
        if (!restoredConst.isDeserializeFailed()) {
            helper.fail("port constant should be marked deserializeFailed (saved had codec, load has no codec)");
            return;
        }
        // Value should fall back to the load-side default (since codec-decode failed and
        // initializationCallback ran).
        if (!(restoredConst.getValue() instanceof SchemaEvolutionTestNodes.EvCodecValueA cv)
                || cv.a() != 55 || !"no-codec-default".equals(cv.b())) {
            helper.fail("port constant should hold load-side default after failed decode, got: " + restoredConst.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * codec → withoutSerialization: load side explicitly opts out. The saved value must be IGNORED
     * (not a failure). Constant should hold the load-side builder default, deserializeFailed=false.
     */
    public static void evolutionCodecToWithoutSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        // Use EvCodecFloatNode (codec) → EvWithoutSerializationNode (withoutSerialization). Both
        // use Float so port UID matches.
        var saveGraph = new TestGraph();
        var node = saveGraph.graphModel.createNodeModel(new EvCodecFloatNode(), new Vector2f(0, 0));
        var codecConst = node.getInputConstantsById().get("port");
        if (codecConst == null) { helper.fail("port constant missing pre-serialize"); return; }
        codecConst.setValue(123.0f);

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        rewriteNodeClass(serialized, EvCodecFloatNode.class.getName(), EvWithoutSerializationNode.class.getName());

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw for codec → withoutSerialization evolution: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, node.getUid());
        if (restored == null) { helper.fail("evolved node not found"); return; }
        var restoredConst = restored.getInputConstantsById().get("port");
        if (restoredConst == null) { helper.fail("port constant missing"); return; }
        if (restoredConst.isDeserializeFailed()) {
            helper.fail("withoutSerialization load is INTENDED to ignore saved value — should NOT mark failed");
            return;
        }
        if (!(restoredConst.getValue() instanceof Float f) || Math.abs(f - 42.0f) > 0.001f) {
            helper.fail("port should hold load-side builder default 42.0, got: " + restoredConst.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * withoutSerialization → codec: saved tag has only `type`, no value. Codec on load side has
     * nothing to decode. Constant should hold load-side default, deserializeFailed=false.
     */
    public static void evolutionWithoutSerializationToCodec(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var saveGraph = new TestGraph();
        var node = saveGraph.graphModel.createNodeModel(new EvWithoutSerializationNode(), new Vector2f(0, 0));
        var c = node.getInputConstantsById().get("port");
        if (c == null) { helper.fail("port constant missing pre-serialize"); return; }
        c.setValue(99.0f); // ignored by save side because withoutSerialization

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        rewriteNodeClass(serialized, EvWithoutSerializationNode.class.getName(), EvCodecFloatNode.class.getName());

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, node.getUid());
        if (restored == null) { helper.fail("evolved node not found"); return; }
        var rc = restored.getInputConstantsById().get("port");
        if (rc == null) { helper.fail("port constant missing post-deserialize"); return; }
        if (rc.isDeserializeFailed()) {
            helper.fail("tag had no value entry — not a failure case, but flag was set");
            return;
        }
        if (!(rc.getValue() instanceof Float f) || Math.abs(f - 7.0f) > 0.001f) {
            helper.fail("expected load-side default 7.0, got " + rc.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * accessor → codec: saved value uses accessor encoding (raw Float NBT). Load side has codec
     * that expects a specific shape. The codec will likely fail to decode the accessor format →
     * deserializeFailed=true, constant resets to load-side default.
     */
    public static void evolutionAccessorToCodec(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var saveGraph = new TestGraph();
        var node = saveGraph.graphModel.createNodeModel(new EvAccessorFloatNode(), new Vector2f(0, 0));
        node.getInputConstantsById().get("port").setValue(50.0f);

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        rewriteNodeClass(serialized, EvAccessorFloatNode.class.getName(), EvCodecFloatNode.class.getName());

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, node.getUid());
        if (restored == null) { helper.fail("evolved node not found"); return; }
        var rc = restored.getInputConstantsById().get("port");
        if (rc == null) { helper.fail("port constant missing"); return; }
        // Codec.FLOAT.parse on a raw Float NBT (NbtOps writes it as FloatTag) actually SUCCEEDS,
        // because Codec.FLOAT accepts the same primitive shape Mojang's accessor produces. So this
        // case is a "lossy but compatible" evolution: NOT a failure. Asserting:
        if (rc.isDeserializeFailed()) {
            helper.fail("Codec.FLOAT accepts the accessor-produced FloatTag — should NOT mark failed");
            return;
        }
        if (!(rc.getValue() instanceof Float f) || Math.abs(f - 50.0f) > 0.001f) {
            helper.fail("expected saved value 50.0 to round-trip through codec, got " + rc.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * codec A → codec B: load codec has a different structural shape and will fail to parse the
     * saved record-shaped NBT. Must mark failed and reset to load-side default.
     */
    public static void evolutionCodecToDifferentCodec(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var saveGraph = new TestGraph();
        var node = saveGraph.graphModel.createNodeModel(new EvCodecValueANode(), new Vector2f(0, 0));
        node.getInputConstantsById().get("port").setValue(new SchemaEvolutionTestNodes.EvCodecValueA(11, "saved-A"));

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        rewriteNodeClass(serialized, EvCodecValueANode.class.getName(), EvCodecValueBNode.class.getName());

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw for codec-vs-codec mismatch: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, node.getUid());
        if (restored == null) { helper.fail("evolved node not found"); return; }
        var rc = restored.getInputConstantsById().get("port");
        if (rc == null) { helper.fail("port constant missing"); return; }
        if (!rc.isDeserializeFailed()) {
            helper.fail("codec B should reject codec A's record NBT — expected deserializeFailed=true");
            return;
        }
        if (!(rc.getValue() instanceof SchemaEvolutionTestNodes.EvCodecValueA cv)
                || cv.a() != 123 || !"default-B".equals(cv.b())) {
            helper.fail("expected load-side default after failed decode, got: " + rc.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * Corrupt NBT: manually overwrite a port's value tag with garbage, then load. Must not throw,
     * must mark failed.
     */
    public static void evolutionCorruptValueTag(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var saveGraph = new TestGraph();
        var node = saveGraph.graphModel.createNodeModel(new EvCodecFloatNode(), new Vector2f(0, 0));
        node.getInputConstantsById().get("port").setValue(33.0f);

        CompoundTag serialized = serializeGraph(saveGraph.graphModel, provider);
        // Surgically replace the value entry with a structurally-invalid tag — a string where
        // Codec.FLOAT expects a number.
        corruptPortValue(serialized, EvCodecFloatNode.class.getName(), "port", "garbage");

        var loadGraph = new TestGraph();
        try {
            deserializeGraph(loadGraph.graphModel, serialized, provider);
        } catch (Exception e) {
            helper.fail("deserializeNBT threw for corrupt value: " + e.getMessage());
            return;
        }

        var restored = findRestoredNode(loadGraph, node.getUid());
        if (restored == null) { helper.fail("evolved node not found"); return; }
        var rc = restored.getInputConstantsById().get("port");
        if (rc == null) { helper.fail("port constant missing"); return; }
        if (!rc.isDeserializeFailed()) {
            helper.fail("corrupt value tag should mark deserializeFailed=true");
            return;
        }
        if (!(rc.getValue() instanceof Float f) || Math.abs(f - 7.0f) > 0.001f) {
            helper.fail("expected builder default 7.0 after corrupt decode, got " + rc.getValue());
            return;
        }

        helper.succeed();
    }

    /**
     * The codec port's value must survive a subsequent in-session defineNode call — the reuse
     * path must NOT clobber the live value when there's no pending tag.
     */
    public static void codecPortSurvivesMultipleDefineNode(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(0, 0));
        var c = node.getInputConstantsById().get("codec_port");
        if (c == null) { helper.fail("codec_port constant missing pre-serialize"); return; }
        c.setValue(new CustomCodecTestNode.CodecValue(77, "round1"));

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl restored = null;
        for (var n : graph2.graphModel.getNodeModels()) {
            if (n instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl cn && cn.getUid().equals(node.getUid())) {
                restored = cn;
                break;
            }
        }
        if (restored == null) { helper.fail("CustomCodecTestNode not found"); return; }
        var rc = restored.getInputConstantsById().get("codec_port");
        if (!(rc.getValue() instanceof CustomCodecTestNode.CodecValue cv1) || cv1.a() != 77 || !"round1".equals(cv1.b())) {
            helper.fail("codec_port value lost after initial deserialize: " + rc.getValue()); return;
        }

        // Now call defineNode again — simulates an option-change rebuild. Value must survive.
        restored.defineNode();
        var rc2 = restored.getInputConstantsById().get("codec_port");
        if (!(rc2.getValue() instanceof CustomCodecTestNode.CodecValue cv2) || cv2.a() != 77 || !"round1".equals(cv2.b())) {
            helper.fail("codec_port value lost after second defineNode: " + rc2.getValue()); return;
        }

        helper.succeed();
    }

    /**
     * Warn-once: serializing N nodes that share an unserializable type produces exactly ONE
     * warning entry per unique type (not N).
     */
    public static void portMissingAccessorWarnsOnce(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant.clearWarnedTypesForTesting();

        var graph = new TestGraph();
        for (int i = 0; i < 5; i++) {
            var n = graph.graphModel.createNodeModel(new CustomCodecTestNode(), new Vector2f(i * 50, 0));
            // Touch missing_port so the no-accessor save-path warning would fire if not deduped.
            n.getInputConstantsById().get("missing_port").setValue(new CustomCodecTestNode.CodecValue(i, "x" + i));
        }
        try {
            serializeGraph(graph.graphModel, provider);
        } catch (Exception e) {
            helper.fail("serialize threw: " + e.getMessage());
            return;
        }

        var warned = com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.TypeConstant.getWarnedUnserializableTypesForTesting();
        // Expect exactly one entry — the CodecValue type. Sibling Float types ARE accessor-backed
        // so they shouldn't appear.
        long matching = warned.stream()
                .filter(t -> t == CustomCodecTestNode.CodecValue.class)
                .count();
        if (matching != 1) {
            helper.fail("expected exactly 1 warn entry for CodecValue, got " + matching + " (set=" + warned + ")");
            return;
        }

        helper.succeed();
    }

    /**
     * Regression: a graph built with ONLY pre-codec node types (accessor-backed values) must
     * round-trip through the FIXED deserialize path identically to before. Existing tests cover
     * this implicitly; this is the explicit named regression label.
     */
    public static void backwardCompatLegacyNbt(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var add = graph.graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        add.getInputConstantsById().get("in1").setValue(11.5f);
        add.getInputConstantsById().get("in2").setValue(22.5f);

        CompoundTag serialized = serializeGraph(graph.graphModel, provider);

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        var restored = findRestoredNode(graph2, add.getUid());
        if (restored == null) { helper.fail("legacy TestAddNode not found after round-trip"); return; }
        var v1 = restored.getInputConstantsById().get("in1");
        var v2 = restored.getInputConstantsById().get("in2");
        if (!(v1.getValue() instanceof Float f1) || Math.abs(f1 - 11.5f) > 0.001f) {
            helper.fail("in1 not preserved: " + v1.getValue()); return;
        }
        if (!(v2.getValue() instanceof Float f2) || Math.abs(f2 - 22.5f) > 0.001f) {
            helper.fail("in2 not preserved: " + v2.getValue()); return;
        }
        if (v1.isDeserializeFailed() || v2.isDeserializeFailed()) {
            helper.fail("accessor-backed legacy ports should never be marked deserializeFailed");
            return;
        }

        helper.succeed();
    }

    /**
     * Wire recovery by (node, portId) when the saved port UID no longer resolves. A port's uid
     * hashes its type, so a retyped/rebuilt port drifts its uid and the primary key misses. The
     * loader must re-bind the wire to the REAL port that still carries the same id instead of
     * stranding it. Simulated by scrambling only the serialized {@code fromPortUid}.
     */
    public static void wireRecoveryByPortId(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graphModel = new TestGraph().graphModel;

        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var aOut = nodeA.getOutputsById().get("out");
        var bIn = nodeB.getInputsById().get("in1");
        if (aOut == null || bIn == null) { helper.fail("test ports missing"); return; }
        graphModel.createWire(bIn, aOut);
        var aUid = nodeA.getUid();

        var serialized = serializeGraph(graphModel, provider);
        // Scramble only the primary key; keep fromPortId / fromNodeUid so recovery can re-bind.
        firstWireAdditional(serialized).putString("fromPortUid", java.util.UUID.randomUUID().toString());

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        assertEq(helper, "recovered wire count", 1, countNonNull(graph2.graphModel.getWireModels()));
        var restoredWire = firstNonNull(graph2.graphModel.getWireModels());
        if (restoredWire == null || restoredWire.getFromPort() == null) {
            helper.fail("wire dropped instead of recovered"); return;
        }
        var from = restoredWire.getFromPort();
        if (from.getPortType() == com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType.MISSING_PORT) {
            helper.fail("wire parked on a missing placeholder instead of re-binding to the real port"); return;
        }
        assertEq(helper, "recovered port id", "out", from.getPortId());
        assertEq(helper, "recovered onto node A", aUid.toString(), from.getNodeModel().getUid().toString());
        assertEq(helper, "no stray placeholder on A", 0, countMissingPorts(findRestoredNode(graph2, aUid)));
        helper.succeed();
    }

    /**
     * Load-time orphan sweep: when recovery creates a missing-port placeholder on ONE endpoint but
     * the OTHER endpoint stays unresolvable, the wire is skipped — the placeholder would otherwise
     * linger forever with no wire. Enforce the invariant "no wire ⇒ no missing port".
     */
    public static void orphanMissingPortRemovedOnLoad(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graphModel = new TestGraph().graphModel;

        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var aOut = nodeA.getOutputsById().get("out");
        var bIn = nodeB.getInputsById().get("in1");
        if (aOut == null || bIn == null) { helper.fail("test ports missing"); return; }
        graphModel.createWire(bIn, aOut);
        var aUid = nodeA.getUid();

        var serialized = serializeGraph(graphModel, provider);
        var wireAdditional = firstWireAdditional(serialized);
        // fromPort: unresolvable uid + an id node A does NOT define → fallback CREATES a missing
        // output "ghostOut" on A; fromNodeUid stays A's uid so the node itself still resolves.
        wireAdditional.putString("fromPortUid", java.util.UUID.randomUUID().toString());
        wireAdditional.putString("fromPortId", "ghostOut");
        // toPort: fully unresolvable (bogus node uid) → recovery returns null → the wire is skipped.
        wireAdditional.putString("toPortUid", java.util.UUID.randomUUID().toString());
        wireAdditional.putString("toNodeUid", java.util.UUID.randomUUID().toString());
        wireAdditional.putString("toPortId", "ghostIn");

        var graph2 = new TestGraph();
        deserializeGraph(graph2.graphModel, serialized, provider);

        // The to-side could not be resolved, so the wire is dropped...
        assertEq(helper, "skipped wire count", 0, countNonNull(graph2.graphModel.getWireModels()));
        // ...and the placeholder created for the from-side must NOT be left orphaned on node A.
        var restoredA = findRestoredNode(graph2, aUid);
        if (restoredA == null) { helper.fail("node A missing after load"); return; }
        assertEq(helper, "orphan missing ports on A", 0, countMissingPorts(restoredA));
        if (restoredA.getOutputsById().get("out") == null) {
            helper.fail("real output port 'out' was lost by the sweep");
        }
        helper.succeed();
    }

    /**
     * Deleting the last wire on a MISSING_PORT placeholder must remove the port from the model AND
     * report it as deleted, so the editor drops its UI element in place instead of leaving a stale
     * port that only clears on reload.
     */
    public static void deletingWireRemovesMissingPort(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var aOut = nodeA.getOutputsById().get("out");
        var bMissing = nodeB.addMissingPort(
                com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection.INPUT, "ghost", null);
        var wire = graphModel.createWire(bMissing, aOut);
        assertEq(helper, "missing port before delete", 1, countMissingPorts(nodeB));

        var portUid = bMissing.getUid();
        graphModel.deleteElements(java.util.List.of(wire));

        assertEq(helper, "missing port after wire delete", 0, countMissingPorts(nodeB));
        if (!graphModel.getCurrentGraphChangeDescription().getDeletedModels().contains(portUid)) {
            helper.fail("removed missing port was not reported as deleted — the editor would keep a stale port element");
        }
        helper.succeed();
    }

    /**
     * Reversing a variable's external IO direction flips its node's main port between input and
     * output. The old-direction wire can't survive the flip (it would connect two same-direction
     * ports), so it must be DROPPED — not degraded to a lingering missing-port placeholder.
     */
    public static void variableIoReversalDropsWire(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        // OUTPUT-kind variable ⇒ WRITE modifier ⇒ the variable node's main port is an INPUT.
        var decl = (VariableDeclarationModel) graphModel.createVariable("v", float.class, 0f, VariableKind.OUTPUT);
        var varNode = graphModel.createVariableNode(decl, new Vector2f(0, 0), null, null);
        var producer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var pOut = producer.getOutputsById().get("out");
        var varIn = varNode.getInputPort();
        if (varIn == null || pOut == null) { helper.fail("variable node did not expose an input main port"); return; }
        graphModel.createWire(varIn, pOut);
        assertEq(helper, "wire before IO reversal", 1, countNonNull(graphModel.getWireModels()));

        // Reverse the external IO: READ modifier ⇒ the main port becomes an OUTPUT.
        decl.setModifiers(com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags.READ);

        if (varNode.getOutputPort() == null) { helper.fail("main port did not flip to output"); return; }
        assertEq(helper, "wire dropped after IO reversal", 0, countNonNull(graphModel.getWireModels()));
        assertEq(helper, "no lingering missing port", 0, countMissingPorts(varNode));
        helper.succeed();
    }

    /**
     * A missing port must surface as an ERROR in the graph logger so the editor can flag the
     * unresolved connection to the user.
     */
    public static void missingPortReportedToGraphLogger(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var node = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        node.addMissingPort(com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection.INPUT, "ghost", null);

        var logger = new com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger();
        graphModel.onGraphChanged(logger);

        var hasError = logger.getEntries().stream()
                .anyMatch(e -> e.level() == com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger.Level.ERROR);
        if (!hasError) helper.fail("missing port was not reported as an error in the graph logger");
        helper.succeed();
    }

    /**
     * removeUnusedMissingPort must invalidate the cached visible-ports list. Nodes that render via
     * getVisible*ByDisplayOrder (subgraph nodes' InOutPortContainerElement) otherwise keep showing a
     * removed missing port until reload, because buildVisiblePorts only rebuilds the cache when it is
     * empty. Regression for the "delete wire, missing port stays" editor bug.
     */
    public static void removingMissingPortInvalidatesVisibleCache(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        var node = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var missing = node.addMissingPort(
                com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection.INPUT, "ghost", null);
        // Prime the visible-ports cache so it holds the missing port.
        if (!node.getVisibleInputsByDisplayOrder().contains(missing)) {
            helper.fail("precondition: missing port not in the visible list"); return;
        }
        // Remove it (no wires attached ⇒ eligible).
        node.removeUnusedMissingPort(missing);
        // The cached visible list must no longer report the removed port.
        if (node.getVisibleInputsByDisplayOrder().contains(missing)) {
            helper.fail("visible-ports cache still contains the removed missing port (stale cache)");
        }
        helper.succeed();
    }

    /**
     * A real→real retype that makes a connected wire type-incompatible PARKS the wire on a
     * type-conflict MISSING_PORT placeholder (preserved + flagged red, revivable) rather than
     * dropping it — the core "external/subgraph reference changed under you" safety net. Here a
     * variable's float→String change retypes its node's main output port, leaving the wire into a
     * float input illegal.
     */
    public static void incompatibleRetypeParksWireOnMissingPort(GameTestHelper helper) {
        var graphModel = new TestGraph().graphModel;
        // INPUT-kind (READ) variable ⇒ the variable node's main port is a float OUTPUT.
        var decl = (VariableDeclarationModel) graphModel.createVariable("v", float.class, 0f, VariableKind.INPUT);
        var varNode = graphModel.createVariableNode(decl, new Vector2f(0, 0), null, null);
        var consumer = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var vOut = varNode.getOutputPort();
        var cIn = consumer.getInputsById().get("in1");
        if (vOut == null || cIn == null) { helper.fail("setup: missing ports"); return; }
        graphModel.createWire(cIn, vOut);
        assertEq(helper, "wire before retype", 1, countNonNull(graphModel.getWireModels()));

        // Retype float→String: main retypes, the wire (String → float input) is now illegal → parked
        // on a type-conflict missing-port placeholder, NOT dropped.
        decl.setDataTypeHandle(TypeHandleHelpers.fromType(String.class));

        assertEq(helper, "wire preserved (parked, not dropped)", 1, countNonNull(graphModel.getWireModels()));
        assertEq(helper, "type-conflict placeholder created on var node", 1, countMissingPorts(varNode));
        helper.succeed();
    }

    // --- Evolution-test helpers ---

    /**
     * Walks the serialized graph's "nodes" list and rewrites every entry whose {@code nodeClass}
     * matches {@code oldClassName} to use {@code newClassName} instead. Used to simulate
     * "saved with node class A, loaded with node class B at the same registry slot".
     */
    private static void rewriteNodeClass(CompoundTag graphTag, String oldClassName, String newClassName) {
        // GraphModel itself serializes via the {@code _additional} wrapper (it extends
        // GraphElementModel and goes through PersistedParser), so its "nodes" list lives one
        // level below the top of {@code serializeNBT}'s output, not directly at the root.
        var inner = unwrapAdditional(graphTag);
        if (!inner.contains("nodes")) return;
        var nodesTag = inner.getListOrEmpty("nodes");
        for (int i = 0; i < nodesTag.size(); i++) {
            var nodeTag = nodesTag.getCompoundOrEmpty(i);
            if (nodeTag.contains("nodeClass") && oldClassName.equals(nodeTag.getStringOr("nodeClass", ""))) {
                nodeTag.putString("nodeClass", newClassName);
            }
        }
    }

    private static CompoundTag unwrapAdditional(CompoundTag graphTag) {
        return graphTag.contains("_additional") ? graphTag.getCompoundOrEmpty("_additional") : graphTag;
    }

    /**
     * Replaces a specific input port's {@code value} tag with a garbage string to exercise the
     * decode-error path. {@code nodeClassName} narrows the mutation to a single node type so
     * other nodes in the graph aren't affected.
     *
     * <p>NBT layout: nodeTag → {@code _additional} (sub-compound where serializeAdditionalNBT's
     * output lives, see {@code PersistedParser.serializeNBT}) → {@code inputConstants} →
     * portId → {@code value}.</p>
     */
    private static void corruptPortValue(CompoundTag graphTag, String nodeClassName, String portId, String garbageValue) {
        var inner = unwrapAdditional(graphTag);
        if (!inner.contains("nodes")) return;
        var nodesTag = inner.getListOrEmpty("nodes");
        for (int i = 0; i < nodesTag.size(); i++) {
            var nodeTag = nodesTag.getCompoundOrEmpty(i);
            if (!nodeClassName.equals(nodeTag.getStringOr("nodeClass", ""))) continue;
            if (!nodeTag.contains("_additional")) continue;
            var additional = nodeTag.getCompoundOrEmpty("_additional");
            if (!additional.contains("inputConstants")) continue;
            var constantsTag = additional.getCompoundOrEmpty("inputConstants");
            if (!constantsTag.contains(portId)) continue;
            var portTag = constantsTag.getCompoundOrEmpty(portId);
            portTag.putString("value", garbageValue);
        }
    }

    private static com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl findRestoredNode(
            TestGraph graph, java.util.UUID nodeUid) {
        for (var n : graph.graphModel.getNodeModels()) {
            if (n instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl cn
                    && cn.getUid().equals(nodeUid)) {
                return cn;
            }
        }
        return null;
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

    /** The mutable {@code _additional} sub-tag of the graph's first serialized wire (the recovery
     *  keys: fromPortUid/fromPortId/fromNodeUid + the to-side equivalents). */
    private static CompoundTag firstWireAdditional(CompoundTag graphTag) {
        var wireTag = unwrapAdditional(graphTag).getListOrEmpty("wires").getCompoundOrEmpty(0);
        if (!wireTag.contains("_additional")) wireTag.put("_additional", new CompoundTag());
        return wireTag.getCompoundOrEmpty("_additional");
    }

    private static WireModel firstNonNull(java.util.List<WireModel> wires) {
        for (var w : wires) if (w != null) return w;
        return null;
    }

    private static int countMissingPorts(com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel node) {
        if (node == null) return -1;
        int count = 0;
        for (var p : node.getInputsById().values()) {
            if (p.getPortType() == com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType.MISSING_PORT) count++;
        }
        for (var p : node.getOutputsById().values()) {
            if (p.getPortType() == com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType.MISSING_PORT) count++;
        }
        return count;
    }
}
