package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.*;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.Set;

public final class GraphAnnotationRegistrationGameTest {
    private static final String NODE_TYPES_PATH = "graph_annotation_node_types";
    private static final String SUPPORT_NODES_PATH = "graph_annotation_support_nodes";
    private static final String UNBOUND_NODE_PATH = "graph_annotation_unbound_node";

    private GraphAnnotationRegistrationGameTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(NODE_TYPES_PATH, GraphAnnotationRegistrationGameTest::nodeTypesAreRegistered);
        NodeGraphGameTests.registerFunction(SUPPORT_NODES_PATH, GraphAnnotationRegistrationGameTest::supportNodesAreDiscoveredFromAnnotations);
        NodeGraphGameTests.registerFunction(UNBOUND_NODE_PATH, GraphAnnotationRegistrationGameTest::unboundNodeIsInOtherGraphRegistry);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, NODE_TYPES_PATH, NodeGraphGameTests.functionKey(NODE_TYPES_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, SUPPORT_NODES_PATH, NodeGraphGameTests.functionKey(SUPPORT_NODES_PATH), testData);
        NodeGraphGameTests.registerFunctionTest(event, UNBOUND_NODE_PATH, NodeGraphGameTests.functionKey(UNBOUND_NODE_PATH), testData);
    }

    private static void nodeTypesAreRegistered(GameTestHelper helper) {
        var expectedNodeKeys = Set.of("test_add", "test_constant", "test_concat", "test_color_blend");
        for (var key : expectedNodeKeys) {
            if (TestGraph.NODE_REGISTRY.get(key) == null) {
                helper.fail("Missing registered node type: " + key);
                return;
            }
        }

        if (TestGraph.NODE_REGISTRY.get("unbound_test_node") != null) {
            helper.fail("Node bound to another graph should not be in TestGraph registry");
            return;
        }

        if (TestGraph.NODE_REGISTRY.get("mod_filtered_test_node") != null) {
            helper.fail("modID filtered node should not be registered");
            return;
        }

        helper.succeed();
    }

    private static void supportNodesAreDiscoveredFromAnnotations(GameTestHelper helper) {
        var graph = new TestGraph();
        var supportNodes = graph.graphModel.getSupportNodes();
        var expectedNodes = Set.of(
                TestAddNode.class,
                TestConstantNode.class,
                TestStringConcatNode.class,
                TestColorBlendNode.class,
                OptionTestNode.class,
                TestContextNode.class,
                TestBlockA.class,
                TestBlockB.class,
                TestUnrelatedBlock.class,
                CustomCodecTestNode.class,
                EvAccessorFloatNode.class,
                EvCodecFloatNode.class,
                EvCodecValueANode.class,
                EvCodecValueBNode.class,
                EvNoCodecNode.class,
                EvWithoutSerializationNode.class
        );

        if (!supportNodes.containsAll(expectedNodes)) {
            helper.fail("Annotated nodes were not all discovered: " + supportNodes);
            return;
        }
        if (supportNodes.contains(UnboundTestNode.class)) {
            helper.fail("Node bound to another graph should not be supported");
            return;
        }
        if (supportNodes.contains(ModFilteredTestNode.class)) {
            helper.fail("modID filtered node should not be supported");
            return;
        }
        if (supportNodes.size() != expectedNodes.size()) {
            helper.fail("Unexpected support node count: " + supportNodes.size() + " -> " + supportNodes);
            return;
        }

        helper.succeed();
    }

    private static void unboundNodeIsInOtherGraphRegistry(GameTestHelper helper) {
        if (AnnotatedOtherGraph.NODE_REGISTRY.get("unbound_test_node") == null) {
            helper.fail("unbound_test_node should be in AnnotatedOtherGraph registry");
            return;
        }
        helper.succeed();
    }
}
