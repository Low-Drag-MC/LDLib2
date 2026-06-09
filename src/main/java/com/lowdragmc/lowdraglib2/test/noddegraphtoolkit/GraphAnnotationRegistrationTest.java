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
    public static void portOrientationFollowsBuilder(GameTestHelper helper) {
        var graph = new TestGraph();
        var node = graph.graphModel.createNodeModel(new TestVerticalNode(), new org.joml.Vector2f(0, 0));

        var inputs = node.getInputsById();
        var outputs = node.getOutputsById();

        if (inputs.get("v_in1") == null
                || inputs.get("v_in1").getOrientation() != com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation.Vertical) {
            helper.fail("v_in1 should be a Vertical input port"); return;
        }
        if (inputs.get("h_in") == null
                || inputs.get("h_in").getOrientation() != com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation.Horizontal) {
            helper.fail("h_in should be a Horizontal input port"); return;
        }
        if (outputs.get("v_out1") == null
                || outputs.get("v_out1").getOrientation() != com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation.Vertical) {
            helper.fail("v_out1 should be a Vertical output port"); return;
        }
        if (outputs.get("h_out") == null
                || outputs.get("h_out").getOrientation() != com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation.Horizontal) {
            helper.fail("h_out should be a Horizontal output port"); return;
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
