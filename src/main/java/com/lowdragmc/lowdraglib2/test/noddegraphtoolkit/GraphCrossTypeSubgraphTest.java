package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.VariableKind;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.SubgraphNodeModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector2f;

/**
 * Cross-type subgraph coverage: a {@link TestGraph} accepts {@link AnnotatedOtherGraph} as a
 * subgraph (but not {@link ModFilteredTestGraph}). Verifies the typed local-subgraph factory,
 * {@code graphClass} persistence, compatibility gating, and backward compatibility with legacy
 * (untagged) saves.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphCrossTypeSubgraphTest {

    // ------------------------------------------------------------------
    // 1. Foreign-type LOCAL subgraph: full round-trip preserves the foreign
    //    type and the outer subgraph node's ports.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void foreignLocalSubgraphRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start foreignLocalSubgraphRoundTrip");

        var root = new TestGraph();
        var rootModel = root.graphModel;

        // Inline subgraph of a DIFFERENT (accepted) graph type.
        var sub = rootModel.createLocalSubgraphInstance(AnnotatedOtherGraph.class);
        if (sub == null) { helper.fail("createLocalSubgraphInstance(AnnotatedOtherGraph) returned null"); return; }
        if (!(sub instanceof CustomGraphModelImpl c) || !(c.getGraph() instanceof AnnotatedOtherGraph)) {
            helper.fail("inline subgraph is not an AnnotatedOtherGraph"); return;
        }
        rootModel.addLocalSubgraph(sub);
        var subNode = rootModel.createNodeWithType(SubgraphNodeModel.class, "sub",
                new Vector2f(50, 50), null,
                n -> n.setLocalSubgraph(sub), SpawnFlags.DEFAULT);

        // Expose a variable inside the foreign subgraph → drives an outer input port.
        ((CustomGraphModelImpl) sub).createVariable("vIn", int.class, 0, VariableKind.INPUT);
        subNode.defineNode();
        assertEq(helper, "outer subNode inputs", 1, subNode.getInputsById().size());

        // Round-trip
        var serialized = rootModel.serializeNBT(provider);
        var root2 = new TestGraph();
        root2.graphModel.deserializeNBT(provider, serialized);

        if (root2.graphModel.getLocalSubGraphs() == null
                || countNonNull(root2.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("localSubGraphs not restored"); return;
        }
        var restoredSub = root2.graphModel.getLocalSubGraphs().get(0);
        if (!(restoredSub instanceof CustomGraphModelImpl rc) || !(rc.getGraph() instanceof AnnotatedOtherGraph)) {
            helper.fail("restored subgraph lost its foreign type (expected AnnotatedOtherGraph)"); return;
        }
        if (!restoredSub.getUid().equals(sub.getUid())) {
            helper.fail("subgraph uid mismatch after deserialize"); return;
        }

        SubgraphNodeModel restoredNode = null;
        for (var n : root2.graphModel.getNodeModels()) {
            if (n instanceof SubgraphNodeModel s && s.getUid().equals(subNode.getUid())) {
                restoredNode = s; break;
            }
        }
        if (restoredNode == null) { helper.fail("SubgraphNodeModel not restored"); return; }
        if (restoredNode.getSubgraphModel() != restoredSub) {
            helper.fail("restored subgraph node not linked to foreign local subgraph"); return;
        }
        assertEq(helper, "restored inputs", 1, restoredNode.getInputsById().size());

        LDLib2.LOGGER.info("End foreignLocalSubgraphRoundTrip - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 2. Compatibility gating: accepted type instantiates, rejected type
    //    returns null. Same-type is always allowed.
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void compatibilityGating(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start compatibilityGating");

        var root = new TestGraph();
        var gm = root.graphModel;

        // Accepted foreign type
        var accepted = gm.createLocalSubgraphInstance(AnnotatedOtherGraph.class);
        if (!(accepted instanceof CustomGraphModelImpl c) || !(c.getGraph() instanceof AnnotatedOtherGraph)) {
            helper.fail("accepted foreign type did not instantiate"); return;
        }
        // Rejected foreign type
        var rejected = gm.createLocalSubgraphInstance(ModFilteredTestGraph.class);
        if (rejected != null) {
            helper.fail("ModFilteredTestGraph should be rejected (not accepted by TestGraph)"); return;
        }
        // Same type is always allowed (both the typed and the no-arg factory).
        var same = gm.createLocalSubgraphInstance(TestGraph.class);
        if (!(same instanceof CustomGraphModelImpl sc) || !(sc.getGraph() instanceof TestGraph)) {
            helper.fail("same-type subgraph should always be allowed"); return;
        }
        var sameNoArg = gm.createLocalSubgraphInstance();
        if (!(sameNoArg instanceof CustomGraphModelImpl nc) || !(nc.getGraph() instanceof TestGraph)) {
            helper.fail("no-arg factory should produce a same-type subgraph"); return;
        }

        LDLib2.LOGGER.info("End compatibilityGating - PASSED");
        helper.succeed();
    }

    // ------------------------------------------------------------------
    // 3. Backward compat: a localSubGraphs entry without graphClass loads as
    //    the owner's own type (legacy, pre-cross-type saves).
    // ------------------------------------------------------------------
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void legacyLocalSubgraphWithoutGraphClass(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();
        LDLib2.LOGGER.info("Start legacyLocalSubgraphWithoutGraphClass");

        var root = new TestGraph();
        var sub = root.graphModel.createLocalSubgraphInstance(); // same type
        if (sub == null) { helper.fail("same-type createLocalSubgraphInstance returned null"); return; }
        root.graphModel.addLocalSubgraph(sub);
        ((CustomGraphModelImpl) sub).createVariable("v", int.class, 0, VariableKind.INPUT);

        var serialized = root.graphModel.serializeNBT(provider);
        // Strip graphClass from every localSubGraphs entry to mimic a legacy save.
        var inner = serialized.contains("_additional") ? serialized.getCompound("_additional") : serialized;
        if (inner.contains("localSubGraphs")) {
            var list = inner.getList("localSubGraphs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ((CompoundTag) list.get(i)).remove("graphClass");
            }
        } else {
            helper.fail("expected localSubGraphs in serialized NBT"); return;
        }

        var root2 = new TestGraph();
        try {
            root2.graphModel.deserializeNBT(provider, serialized);
        } catch (Exception e) {
            helper.fail("legacy (untagged) localSubGraphs deserialize threw: " + e.getMessage()); return;
        }
        if (root2.graphModel.getLocalSubGraphs() == null
                || countNonNull(root2.graphModel.getLocalSubGraphs()) != 1) {
            helper.fail("legacy localSubGraphs not restored"); return;
        }
        var restored = root2.graphModel.getLocalSubGraphs().get(0);
        if (!(restored instanceof CustomGraphModelImpl rc) || !(rc.getGraph() instanceof TestGraph)) {
            helper.fail("legacy subgraph should load as the owner's own type (TestGraph)"); return;
        }
        if (countNonNull(restored.getGraphVariableModels()) != 1) {
            helper.fail("legacy subgraph variables not restored"); return;
        }

        LDLib2.LOGGER.info("End legacyLocalSubgraphWithoutGraphClass - PASSED");
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
