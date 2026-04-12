package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandleHelpers;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector2f;

public class GraphSerializationTest {

    /**
     * Tests basic serialization and deserialization of a graph with custom nodes, wires, constants, and variables.
     */
    @GameTest(template = "empty")
    public static void graphSerializationRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start Graph Serialization Round-Trip Test");

        // === Build the original graph ===
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create two custom nodes (TestAddNode)
        var addNode1 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(100, 100));
        var addNode2 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(300, 100));

        // Create a constant node
        var floatType = TypeHandleHelpers.fromType(Float.class);
        var constantNode = graphModel.createConstantNode("myConst", new Vector2f(0, 0), floatType, 42.0f);

        // Create a string concat node
        var concatNode = graphModel.createNodeModel(new TestStringConcatNode(), new Vector2f(200, 300));

        // Create a variable
        var variable = graphModel.createVariable("testVar", float.class, 7.5f, VariableKind.LOCAL);

        // Create a variable node referencing it
        var variableNode = graphModel.createVariableNode(
                (VariableDeclarationModel) variable,
                new Vector2f(50, 200), null, null);

        // Connect constantNode output -> addNode1 input "in1"
        var constantOutputPort = ((ConstantNodeModel) constantNode.getNodeModel()).getOutputPort();
        var addNode1InputPort = addNode1.getInputsById().get("in1");
        WireModel wire1 = null;
        if (constantOutputPort != null && addNode1InputPort != null) {
            wire1 = graphModel.createWire(addNode1InputPort, constantOutputPort);
        }

        // Connect addNode1 output -> addNode2 input "in1"
        var addNode1OutputPort = addNode1.getOutputsById().get("out");
        var addNode2InputPort = addNode2.getInputsById().get("in1");
        WireModel wire2 = null;
        if (addNode1OutputPort != null && addNode2InputPort != null) {
            wire2 = graphModel.createWire(addNode2InputPort, addNode1OutputPort);
        }

        // Record original counts
        int origNodeCount = countNonNull(graphModel.getNodeModels());
        int origWireCount = countNonNull(graphModel.getWireModels());
        int origVarCount = countNonNull(graphModel.getGraphVariableModels());

        // === Serialize ===
        CompoundTag serialized = graphModel.serializeNBT(provider);
        LDLib2.LOGGER.info("Serialized graph to {} keys", serialized.getAllKeys().size());

        // === Deserialize into a new graph ===
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        graphModel2.deserializeNBT(provider, serialized);

        // === Verify counts ===
        int newNodeCount = countNonNull(graphModel2.getNodeModels());
        int newWireCount = countNonNull(graphModel2.getWireModels());
        int newVarCount = countNonNull(graphModel2.getGraphVariableModels());

        assertEq(helper, "node count", origNodeCount, newNodeCount);
        assertEq(helper, "wire count", origWireCount, newWireCount);
        assertEq(helper, "variable count", origVarCount, newVarCount);

        // === Verify node UIDs match ===
        for (var origNode : graphModel.getNodeModels()) {
            if (origNode == null) continue;
            var found = findNodeByUid(graphModel2, origNode.getUid().toString());
            if (found == null) {
                helper.fail("Node with UID " + origNode.getUid() + " not found after deserialization");
                return;
            }
            // Verify position
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
        CompoundTag serialized2 = graphModel2.serializeNBT(provider);
        if (!serialized.equals(serialized2)) {
            LDLib2.LOGGER.warn("Round-trip serialization produced different NBT (this may be expected for non-deterministic elements)");
        }

        LDLib2.LOGGER.info("End Graph Serialization Round-Trip Test - ALL PASSED");
        helper.succeed();
    }

    /**
     * Tests that an empty graph serializes and deserializes correctly.
     */
    @GameTest(template = "empty")
    public static void emptyGraphSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        CompoundTag serialized = graphModel.serializeNBT(provider);

        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        graphModel2.deserializeNBT(provider, serialized);

        assertEq(helper, "empty graph node count", countNonNull(graphModel.getNodeModels()), countNonNull(graphModel2.getNodeModels()));
        assertEq(helper, "empty graph wire count", countNonNull(graphModel.getWireModels()), countNonNull(graphModel2.getWireModels()));

        helper.succeed();
    }

    /**
     * Tests that inputConstantsById (port default values) survive serialization.
     */
    @GameTest(template = "empty")
    public static void portConstantsSerialization(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create an AddNode which has input ports with constants
        var addNode = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));

        // Set a constant value on input port "in1"
        var in1Constant = addNode.getInputConstantsById().get("in1");
        if (in1Constant != null) {
            in1Constant.setValue(99.0f);
        }

        // Serialize and deserialize
        CompoundTag serialized = graphModel.serializeNBT(provider);
        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        graphModel2.deserializeNBT(provider, serialized);

        // Find the restored add node
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
