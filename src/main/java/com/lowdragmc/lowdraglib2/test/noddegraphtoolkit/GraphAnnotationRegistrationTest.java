package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Set;

@GameTestHolder(LDLib2.MOD_ID)
public class GraphAnnotationRegistrationTest {

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void nodeTypesAreRegistered(GameTestHelper helper) {
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void supportNodesAreDiscoveredFromAnnotations(GameTestHelper helper) {
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void unboundNodeIsInOtherGraphRegistry(GameTestHelper helper) {
        if (AnnotatedOtherGraph.NODE_REGISTRY.get("unbound_test_node") == null) {
            helper.fail("unbound_test_node should be in AnnotatedOtherGraph registry");
            return;
        }
        helper.succeed();
    }
}
