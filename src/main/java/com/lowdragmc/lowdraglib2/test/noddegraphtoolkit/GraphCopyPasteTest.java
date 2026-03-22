package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(LDLib2.MOD_ID)
public class GraphCopyPasteTest {

    /**
     * Tests basic copy/paste of 2 connected nodes.
     * Verifies new UUIDs are assigned and wire connects the new ports correctly.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void copyPasteBasicNodes(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create 2 connected AddNodes
        var node1 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(100, 100));
        var node2 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(300, 100));

        var outPort = node1.getOutputsById().get("out");
        var inPort = node2.getInputsById().get("in1");
        graphModel.createWire(inPort, outPort);

        int origNodeCount = countNodes(graphModel);
        int origWireCount = countWires(graphModel);

        // Copy both nodes
        var copyData = graphModel.copyElements(List.of(node1, node2), provider);

        // Paste with offset
        var pasted = graphModel.pasteElements(copyData, new Vector2f(50, 50));

        // Should have 2 new nodes
        if (pasted.size() != 2) {
            helper.fail("Expected 2 pasted elements, got " + pasted.size());
            return;
        }

        // Total counts should double
        assertEq(helper, "node count after paste", origNodeCount + 2, countNodes(graphModel));
        assertEq(helper, "wire count after paste", origWireCount + 1, countWires(graphModel));

        // New nodes must have different UUIDs
        for (var element : pasted) {
            if (element.getUid().equals(node1.getUid()) || element.getUid().equals(node2.getUid())) {
                helper.fail("Pasted node has same UUID as original: " + element.getUid());
                return;
            }
        }

        // Verify the pasted wire connects two pasted nodes (not original nodes)
        var pastedNodeUids = pasted.stream().map(GraphElementModel::getUid).toList();
        boolean foundPastedWire = false;
        for (var wire : graphModel.getWireModels()) {
            if (wire == null || wire.getFromPort() == null || wire.getToPort() == null) continue;
            var fromNodeUid = wire.getFromPort().getNodeModel().getUid();
            var toNodeUid = wire.getToPort().getNodeModel().getUid();
            if (pastedNodeUids.contains(fromNodeUid) && pastedNodeUids.contains(toNodeUid)) {
                foundPastedWire = true;
                break;
            }
        }
        if (!foundPastedWire) {
            helper.fail("No wire found connecting the two pasted nodes");
            return;
        }

        helper.succeed();
    }

    /**
     * Tests partial selection: 3 nodes A→B→C, select only A+C.
     * Paste should produce 2 nodes with no wires (A→B and B→C are not internal).
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void copyPastePartialSelection(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var nodeC = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));

        // A.out → B.in1
        graphModel.createWire(nodeB.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        // B.out → C.in1
        graphModel.createWire(nodeC.getInputsById().get("in1"), nodeB.getOutputsById().get("out"));

        int wiresBefore = countWires(graphModel);

        // Copy only A and C (not B)
        var copyData = graphModel.copyElements(List.of(nodeA, nodeC), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(0, 200));

        assertEq(helper, "pasted node count", 2, pasted.size());
        // No new wires should be created
        assertEq(helper, "wire count unchanged", wiresBefore, countWires(graphModel));

        helper.succeed();
    }

    /**
     * Tests copy/paste of a VariableNode with its declaration.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void copyPasteWithVariable(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create a variable and a variable node
        var variable = graphModel.createVariable("testVar", float.class, 7.5f, VariableKind.LOCAL);
        var variableNode = graphModel.createVariableNode(
                (VariableDeclarationModel) variable,
                new Vector2f(50, 200), null, null);

        int varCountBefore = countVariables(graphModel);

        // Copy the variable node
        var copyData = graphModel.copyElements(List.of(variableNode), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(100, 0));

        assertEq(helper, "pasted count", 1, pasted.size());

        // Variable declaration should be reused (same UID exists), not duplicated
        assertEq(helper, "variable count unchanged", varCountBefore, countVariables(graphModel));

        // Pasted VariableNode should reference the same declaration
        var pastedNode = pasted.get(0);
        if (!(pastedNode instanceof VariableNodeModel pastedVarNode)) {
            helper.fail("Pasted element is not a VariableNodeModel");
            return;
        }
        if (pastedVarNode.getDeclarationModel() == null) {
            helper.fail("Pasted VariableNodeModel has null declaration");
            return;
        }
        if (!pastedVarNode.getDeclarationModel().getUid().equals(((VariableDeclarationModel) variable).getUid())) {
            helper.fail("Pasted VariableNodeModel references wrong declaration");
            return;
        }

        helper.succeed();
    }

    /**
     * Tests that position offset is correctly applied during paste.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void copyPastePositionOffset(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var node = graphModel.createNodeModel(new TestAddNode(), new Vector2f(100, 200));
        var origPos = new Vector2f(node.getPosition());

        var copyData = graphModel.copyElements(List.of(node), provider);
        var offset = new Vector2f(50, -30);
        var pasted = graphModel.pasteElements(copyData, offset);

        assertEq(helper, "pasted count", 1, pasted.size());

        var pastedNode = (AbstractNodeModel) pasted.get(0);
        var expectedX = (int) (origPos.x + offset.x);
        var expectedY = (int) (origPos.y + offset.y);
        assertEq(helper, "position x", expectedX, (int) pastedNode.getPosition().x);
        assertEq(helper, "position y", expectedY, (int) pastedNode.getPosition().y);

        helper.succeed();
    }

    /**
     * Tests that duplicating multiple connected nodes preserves internal connections.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void duplicatePreservesConnections(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        // Create 3 nodes all connected: A→B, A→C, B→C
        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var nodeC = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));

        graphModel.createWire(nodeB.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        graphModel.createWire(nodeC.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        graphModel.createWire(nodeC.getInputsById().get("in2"), nodeB.getOutputsById().get("out"));

        int wiresBefore = countWires(graphModel);

        // Copy all 3 (all wires are internal)
        var copyData = graphModel.copyElements(List.of(nodeA, nodeB, nodeC), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(0, 300));

        assertEq(helper, "pasted node count", 3, pasted.size());
        // 3 new internal wires should be created
        assertEq(helper, "wire count", wiresBefore + 3, countWires(graphModel));

        // All pasted wires should connect only pasted nodes
        var pastedUids = pasted.stream().map(GraphElementModel::getUid).toList();
        int pastedWireCount = 0;
        for (var wire : graphModel.getWireModels()) {
            if (wire == null || wire.getFromPort() == null || wire.getToPort() == null) continue;
            var fromUid = wire.getFromPort().getNodeModel().getUid();
            var toUid = wire.getToPort().getNodeModel().getUid();
            if (pastedUids.contains(fromUid) && pastedUids.contains(toUid)) {
                pastedWireCount++;
            }
        }
        assertEq(helper, "pasted internal wire count", 3, pastedWireCount);

        helper.succeed();
    }

    // --- Helpers ---

    private static int countNodes(GraphModel graphModel) {
        return (int) graphModel.getNodeModels().stream().filter(Objects::nonNull).count();
    }

    private static int countWires(GraphModel graphModel) {
        return (int) graphModel.getWireModels().stream().filter(Objects::nonNull).count();
    }

    private static int countVariables(GraphModel graphModel) {
        return (int) graphModel.getGraphVariableModels().stream().filter(Objects::nonNull).count();
    }

    private static void assertEq(GameTestHelper helper, String label, int expected, int actual) {
        if (expected != actual) {
            helper.fail(label + ": expected " + expected + ", got " + actual);
        }
    }
}
