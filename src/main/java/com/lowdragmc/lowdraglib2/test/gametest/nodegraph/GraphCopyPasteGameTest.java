package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.joml.Vector2f;

import java.util.List;
import java.util.Objects;

public final class GraphCopyPasteGameTest {
    private static final String BASIC_PATH = "graph_copy_paste_basic";
    private static final String PARTIAL_PATH = "graph_copy_paste_partial";
    private static final String VARIABLE_PATH = "graph_copy_paste_variable";
    private static final String OFFSET_PATH = "graph_copy_paste_offset";
    private static final String DUPLICATE_PATH = "graph_copy_paste_duplicate";

    private GraphCopyPasteGameTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(BASIC_PATH, GraphCopyPasteGameTest::copyPasteBasicNodes);
        NodeGraphGameTests.registerFunction(PARTIAL_PATH, GraphCopyPasteGameTest::copyPastePartialSelection);
        NodeGraphGameTests.registerFunction(VARIABLE_PATH, GraphCopyPasteGameTest::copyPasteWithVariable);
        NodeGraphGameTests.registerFunction(OFFSET_PATH, GraphCopyPasteGameTest::copyPastePositionOffset);
        NodeGraphGameTests.registerFunction(DUPLICATE_PATH, GraphCopyPasteGameTest::duplicatePreservesConnections);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, BASIC_PATH, NodeGraphGameTests.functionKey(BASIC_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, PARTIAL_PATH, NodeGraphGameTests.functionKey(PARTIAL_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, VARIABLE_PATH, NodeGraphGameTests.functionKey(VARIABLE_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, OFFSET_PATH, NodeGraphGameTests.functionKey(OFFSET_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, DUPLICATE_PATH, NodeGraphGameTests.functionKey(DUPLICATE_PATH), testData);
    }

    private static void copyPasteBasicNodes(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var node1 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(100, 100));
        var node2 = graphModel.createNodeModel(new TestAddNode(), new Vector2f(300, 100));

        var outPort = node1.getOutputsById().get("out");
        var inPort = node2.getInputsById().get("in1");
        graphModel.createWire(inPort, outPort);

        int origNodeCount = countNodes(graphModel);
        int origWireCount = countWires(graphModel);

        var copyData = graphModel.copyElements(List.of(node1, node2), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(50, 50));

        if (pasted.size() != 2) {
            helper.fail("Expected 2 pasted elements, got " + pasted.size());
            return;
        }

        assertEq(helper, "node count after paste", origNodeCount + 2, countNodes(graphModel));
        assertEq(helper, "wire count after paste", origWireCount + 1, countWires(graphModel));

        for (var element : pasted) {
            if (element.getUid().equals(node1.getUid()) || element.getUid().equals(node2.getUid())) {
                helper.fail("Pasted node has same UUID as original: " + element.getUid());
                return;
            }
        }

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

    private static void copyPastePartialSelection(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var nodeC = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));

        graphModel.createWire(nodeB.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        graphModel.createWire(nodeC.getInputsById().get("in1"), nodeB.getOutputsById().get("out"));

        int wiresBefore = countWires(graphModel);

        var copyData = graphModel.copyElements(List.of(nodeA, nodeC), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(0, 200));

        assertEq(helper, "pasted node count", 2, pasted.size());
        assertEq(helper, "wire count unchanged", wiresBefore, countWires(graphModel));

        helper.succeed();
    }

    private static void copyPasteWithVariable(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var variable = graphModel.createVariable("testVar", float.class, 7.5f, VariableKind.LOCAL);
        var variableNode = graphModel.createVariableNode(
                (VariableDeclarationModel) variable,
                new Vector2f(50, 200), null, null);

        int varCountBefore = countVariables(graphModel);

        var copyData = graphModel.copyElements(List.of(variableNode), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(100, 0));

        assertEq(helper, "pasted count", 1, pasted.size());
        assertEq(helper, "variable count unchanged", varCountBefore, countVariables(graphModel));

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

    private static void copyPastePositionOffset(GameTestHelper helper) {
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

    private static void duplicatePreservesConnections(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var nodeA = graphModel.createNodeModel(new TestAddNode(), new Vector2f(0, 0));
        var nodeB = graphModel.createNodeModel(new TestAddNode(), new Vector2f(200, 0));
        var nodeC = graphModel.createNodeModel(new TestAddNode(), new Vector2f(400, 0));

        graphModel.createWire(nodeB.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        graphModel.createWire(nodeC.getInputsById().get("in1"), nodeA.getOutputsById().get("out"));
        graphModel.createWire(nodeC.getInputsById().get("in2"), nodeB.getOutputsById().get("out"));

        int wiresBefore = countWires(graphModel);

        var copyData = graphModel.copyElements(List.of(nodeA, nodeB, nodeC), provider);
        var pasted = graphModel.pasteElements(copyData, new Vector2f(0, 300));

        assertEq(helper, "pasted node count", 3, pasted.size());
        assertEq(helper, "wire count", wiresBefore + 3, countWires(graphModel));

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
