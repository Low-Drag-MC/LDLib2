package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.GraphNodeCreationData;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.SpawnFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.CustomGraphModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomBlockNodeModelImpl;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestBlockA;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestBlockB;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestContextNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestUnrelatedBlock;
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

import java.util.UUID;

public final class ContextBlockTest {
    private static final String BASIC_OPERATIONS = "context_block_basic_operations";
    private static final String INCOMPATIBLE_BLOCK_REJECTED = "context_block_incompatible_block_rejected";
    private static final String SERIALIZATION_ROUND_TRIP = "context_block_serialization_round_trip";
    private static final String DELETION_CASCADES_BLOCKS = "context_block_deletion_cascades_blocks";
    private static final String COPY_PASTE = "context_block_copy_paste";

    private ContextBlockTest() {
    }

    static void registerFunctions() {
        NodeGraphGameTests.registerFunction(BASIC_OPERATIONS, ContextBlockTest::contextBlockBasicOperations);
        NodeGraphGameTests.registerFunction(INCOMPATIBLE_BLOCK_REJECTED, ContextBlockTest::incompatibleBlockRejected);
        NodeGraphGameTests.registerFunction(SERIALIZATION_ROUND_TRIP, ContextBlockTest::contextBlockSerializationRoundTrip);
        NodeGraphGameTests.registerFunction(DELETION_CASCADES_BLOCKS, ContextBlockTest::contextDeletionCascadesBlocks);
        NodeGraphGameTests.registerFunction(COPY_PASTE, ContextBlockTest::contextWithBlocksCopyPaste);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = NodeGraphGameTests.defaultTestData(environment, "empty");
        NodeGraphGameTests.registerFunctionTest(event, BASIC_OPERATIONS, NodeGraphGameTests.functionKey(BASIC_OPERATIONS), testData);
        NodeGraphGameTests.registerFunctionTest(event, INCOMPATIBLE_BLOCK_REJECTED, NodeGraphGameTests.functionKey(INCOMPATIBLE_BLOCK_REJECTED), testData);
        NodeGraphGameTests.registerFunctionTest(event, SERIALIZATION_ROUND_TRIP, NodeGraphGameTests.functionKey(SERIALIZATION_ROUND_TRIP), testData);
        NodeGraphGameTests.registerFunctionTest(event, DELETION_CASCADES_BLOCKS, NodeGraphGameTests.functionKey(DELETION_CASCADES_BLOCKS), testData);
        NodeGraphGameTests.registerFunctionTest(event, COPY_PASTE, NodeGraphGameTests.functionKey(COPY_PASTE), testData);
    }

    /**
     * Basic flow: create a context, insert two blocks, reorder, remove. Verifies the in-memory
     * model state (block count, parent links, indices, registration with the graph).
     */
    public static void contextBlockBasicOperations(GameTestHelper helper) {
        var graph = new TestGraph();
        var graphModel = graph.graphModel;

        var ctxModel = createContext(graphModel, new Vector2f(0, 0));
        var blockA = makeBlock(graphModel, TestBlockA.class);
        var blockB = makeBlock(graphModel, TestBlockB.class);

        ctxModel.insertBlock(blockA, -1);
        ctxModel.insertBlock(blockB, -1);

        if (ctxModel.getBlockCount() != 2) {
            helper.fail("Expected 2 blocks, got " + ctxModel.getBlockCount()); return;
        }
        if (ctxModel.getBlocks().get(0) != blockA) { helper.fail("Block 0 should be A"); return; }
        if (ctxModel.getBlocks().get(1) != blockB) { helper.fail("Block 1 should be B"); return; }
        if (blockA.getIndex() != 0) { helper.fail("blockA index should be 0"); return; }
        if (blockB.getIndex() != 1) { helper.fail("blockB index should be 1"); return; }
        if (blockA.getContextNodeModel() != ctxModel) { helper.fail("blockA parent link broken"); return; }

        // Block UUIDs must be registered in the graph so wires can resolve to their ports.
        if (graphModel.getModel(blockA.getUid()) != blockA) {
            helper.fail("blockA not registered in graph elementsByUID"); return;
        }

        // Reorder: move B from index 1 to index 0.
        ctxModel.moveBlock(1, 0);
        if (ctxModel.getBlocks().get(0) != blockB) { helper.fail("After move, block 0 should be B"); return; }
        if (ctxModel.getBlocks().get(1) != blockA) { helper.fail("After move, block 1 should be A"); return; }

        // Remove A, B should remain.
        ctxModel.removeBlock(blockA);
        if (ctxModel.getBlockCount() != 1) { helper.fail("After remove, count should be 1"); return; }
        if (ctxModel.getBlocks().get(0) != blockB) { helper.fail("After remove, sole block should be B"); return; }
        if (graphModel.getModel(blockA.getUid()) != null) {
            helper.fail("blockA still registered after removal"); return;
        }
        if (blockA.getContextNodeModel() != null) {
            helper.fail("blockA parent link not cleared after removal"); return;
        }

        helper.succeed();
    }

    /**
     * A BlockNode without {@code @UseWithContext} (or any other compatibility opt-in) must be
     * rejected when inserted into a context.
     */
    public static void incompatibleBlockRejected(GameTestHelper helper) {
        var graph = new TestGraph();
        var graphModel = graph.graphModel;
        var ctxModel = createContext(graphModel, new Vector2f(0, 0));
        var bad = makeBlock(graphModel, TestUnrelatedBlock.class);

        try {
            ctxModel.insertBlock(bad, -1);
            helper.fail("Expected IllegalArgumentException for unrelated block insert");
            return;
        } catch (IllegalArgumentException expected) {
            // ok
        }
        if (ctxModel.getBlockCount() != 0) {
            helper.fail("Block list should still be empty after rejected insert"); return;
        }
        helper.succeed();
    }

    /**
     * Round-trip: context with two blocks, where one block has a non-default input constant,
     * must serialize and deserialize losslessly — blocks preserved in order with their parent
     * links restored and port-level constants intact.
     */
    public static void contextBlockSerializationRoundTrip(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;
        var ctxModel = createContext(graphModel, new Vector2f(50, 50));
        var blockA = makeBlock(graphModel, TestBlockA.class);
        var blockB = makeBlock(graphModel, TestBlockB.class);
        ctxModel.insertBlock(blockA, -1);
        ctxModel.insertBlock(blockB, -1);

        // Set an input constant on blockB's "inA" to verify port-level data survives the trip.
        var inA = blockB.getInputConstantsById().get("inA");
        if (inA == null) { helper.fail("blockB inA constant missing pre-serialize"); return; }
        inA.setValue(123.5f);

        var originalCtxUid = ctxModel.getUid();
        var originalBlockAUid = blockA.getUid();
        var originalBlockBUid = blockB.getUid();

        var serialized = serializeGraph(graphModel, provider);

        var graph2 = new TestGraph();
        var graphModel2 = graph2.graphModel;
        deserializeGraph(graphModel2, serialized, provider);

        // Find the restored context.
        ContextNodeModel restoredCtx = null;
        for (var node : graphModel2.getNodeModels()) {
            if (node instanceof ContextNodeModel cn && cn.getUid().equals(originalCtxUid)) {
                restoredCtx = cn; break;
            }
        }
        if (restoredCtx == null) { helper.fail("Context not found after deserialize"); return; }
        if (restoredCtx.getBlockCount() != 2) {
            helper.fail("Expected 2 blocks after deserialize, got " + restoredCtx.getBlockCount()); return;
        }
        var restoredA = restoredCtx.getBlocks().get(0);
        var restoredB = restoredCtx.getBlocks().get(1);
        if (!restoredA.getUid().equals(originalBlockAUid)) {
            helper.fail("Block 0 UID mismatch: expected " + originalBlockAUid + ", got " + restoredA.getUid()); return;
        }
        if (!restoredB.getUid().equals(originalBlockBUid)) {
            helper.fail("Block 1 UID mismatch"); return;
        }
        if (restoredA.getContextNodeModel() != restoredCtx) {
            helper.fail("Restored blockA parent link broken"); return;
        }
        if (graphModel2.getModel(restoredA.getUid()) != restoredA) {
            helper.fail("Restored blockA not registered in graph elementsByUID"); return;
        }
        var restoredInA = restoredB.getInputConstantsById().get("inA");
        if (restoredInA == null || !(restoredInA.getValue() instanceof Float f) || Math.abs(f - 123.5f) > 0.001f) {
            helper.fail("blockB inA constant not preserved: " + (restoredInA == null ? "null" : restoredInA.getValue()));
            return;
        }

        helper.succeed();
    }

    /**
     * Deleting the context must cascade to its blocks — they should no longer be registered in
     * the graph's UID map, and their parent links should be cleared.
     */
    public static void contextDeletionCascadesBlocks(GameTestHelper helper) {
        var graph = new TestGraph();
        var graphModel = graph.graphModel;
        var ctxModel = createContext(graphModel, new Vector2f(0, 0));
        var blockA = makeBlock(graphModel, TestBlockA.class);
        ctxModel.insertBlock(blockA, -1);
        UUID blockAUid = blockA.getUid();

        graphModel.deleteNode(ctxModel, true, true);

        if (graphModel.getModel(blockAUid) != null) {
            helper.fail("blockA still registered after context deletion"); return;
        }
        if (blockA.getContextNodeModel() != null) {
            helper.fail("blockA parent link not cleared after context deletion"); return;
        }
        helper.succeed();
    }

    /**
     * Copy/paste a context that owns two blocks plus an internal wire between them. Verifies:
     * <ul>
     *   <li>The pasted context and its blocks all receive fresh UIDs (no collision with originals).</li>
     *   <li>Both original and pasted UIDs resolve via {@code graphModel.getModel(uid)} — no overwrite.</li>
     *   <li>The internal block-to-block wire is reproduced against the new ports (not the originals).</li>
     * </ul>
     * Regression for the duplicate-element-UID issue.
     */
    public static void contextWithBlocksCopyPaste(GameTestHelper helper) {
        var provider = helper.getLevel().registryAccess();

        var graph = new TestGraph();
        var graphModel = graph.graphModel;
        var ctxModel = createContext(graphModel, new Vector2f(0, 0));
        var blockA = makeBlock(graphModel, TestBlockA.class);
        var blockB = makeBlock(graphModel, TestBlockB.class);
        ctxModel.insertBlock(blockA, -1);
        ctxModel.insertBlock(blockB, -1);

        // Internal wire: blockA.out -> blockB.inA — must survive copy/paste, hooked to the new blocks.
        var aOut = blockA.getOutputsById().get("out");
        var bInA = blockB.getInputsById().get("inA");
        if (aOut == null || bInA == null) { helper.fail("pre-wire port lookup failed"); return; }
        var originalWire = graphModel.createWire(bInA, aOut);
        if (originalWire == null) { helper.fail("internal wire creation failed"); return; }

        var originalCtxUid = ctxModel.getUid();
        var originalBlockAUid = blockA.getUid();
        var originalBlockBUid = blockB.getUid();
        int originalNodeCount = graphModel.getNodeModels().size();
        int originalWireCount = graphModel.getWireModels().size();

        // Copy + paste (positionOffset chosen to keep the pasted context distinct in space).
        var clipboard = graphModel.copyElements(java.util.List.of(ctxModel), provider);
        var pasted = graphModel.pasteElements(clipboard, new Vector2f(200, 0));

        // Exactly one new context, with two new blocks inside.
        if (pasted.size() != 1) { helper.fail("Expected 1 pasted top-level model, got " + pasted.size()); return; }
        if (!(pasted.get(0) instanceof ContextNodeModel pastedCtx)) {
            helper.fail("Pasted element is not a ContextNodeModel"); return;
        }
        if (pastedCtx.getUid().equals(originalCtxUid)) {
            helper.fail("Pasted context kept the original UID — re-uid is missing"); return;
        }
        if (pastedCtx.getBlockCount() != 2) {
            helper.fail("Pasted context should have 2 blocks, got " + pastedCtx.getBlockCount()); return;
        }
        var pastedA = pastedCtx.getBlocks().get(0);
        var pastedB = pastedCtx.getBlocks().get(1);
        if (pastedA.getUid().equals(originalBlockAUid) || pastedB.getUid().equals(originalBlockBUid)) {
            helper.fail("Pasted blocks kept original UIDs — block re-uid is missing"); return;
        }

        // Both the originals and the copies must resolve through elementsByUID — no overwrite.
        if (graphModel.getModel(originalCtxUid) != ctxModel) {
            helper.fail("Original context lookup was clobbered by paste"); return;
        }
        if (graphModel.getModel(originalBlockAUid) != blockA) {
            helper.fail("Original blockA lookup was clobbered by paste"); return;
        }
        if (graphModel.getModel(pastedCtx.getUid()) != pastedCtx) {
            helper.fail("Pasted context not registered under its new UID"); return;
        }
        if (graphModel.getModel(pastedA.getUid()) != pastedA) {
            helper.fail("Pasted blockA not registered under its new UID"); return;
        }

        // Node-list count: original context still there + one new pasted context. Blocks don't
        // appear in nodeModels (they're dependents of the context).
        if (graphModel.getNodeModels().size() != originalNodeCount + 1) {
            helper.fail("Unexpected nodeModels count after paste: " + graphModel.getNodeModels().size());
            return;
        }

        // Internal wire reproduced and pinned to the NEW block ports, not the originals.
        if (graphModel.getWireModels().size() != originalWireCount + 1) {
            helper.fail("Expected one new wire after paste, got "
                    + (graphModel.getWireModels().size() - originalWireCount)); return;
        }
        var pastedAOut = pastedA.getOutputsById().get("out");
        var pastedBInA = pastedB.getInputsById().get("inA");
        if (pastedAOut == null || pastedBInA == null) {
            helper.fail("pasted ports not found by id"); return;
        }
        boolean foundPastedWire = false;
        for (var wire : graphModel.getWireModels()) {
            if (wire == null) continue;
            if (wire.getFromPort() == pastedAOut && wire.getToPort() == pastedBInA) {
                foundPastedWire = true;
                break;
            }
        }
        if (!foundPastedWire) {
            helper.fail("Internal wire was not reproduced against pasted block ports"); return;
        }
        // And the original wire still hits the original block ports.
        if (originalWire.getFromPort() != aOut || originalWire.getToPort() != bInA) {
            helper.fail("Original wire endpoints corrupted after paste"); return;
        }

        helper.succeed();
    }

    // --- helpers ---

    private static ContextNodeModel createContext(CustomGraphModelImpl graphModel, Vector2f position) {
        var data = new GraphNodeCreationData(graphModel, position, null, null);
        return (ContextNodeModel) CustomGraphModelImpl.createNodeFromData(data, TestContextNode.class);
    }

    private static CompoundTag serializeGraph(CustomGraphModelImpl graphModel, net.minecraft.core.HolderLookup.Provider provider) {
        var output = TagValueOutput.createWithContext(ProblemReporter.Collector.DISCARDING, provider);
        graphModel.serialize(output);
        return output.buildResult();
    }

    private static void deserializeGraph(CustomGraphModelImpl graphModel, CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        graphModel.deserialize(TagValueInput.create(ProblemReporter.Collector.DISCARDING, provider, tag));
    }

    /**
     * Builds a standalone block (not yet attached to any context). Mirrors what
     * {@code BlockCommands.InsertBlockCommand} does internally so the test verifies the same
     * path callers actually use.
     */
    private static BlockNodeModel makeBlock(CustomGraphModelImpl graphModel,
                                            Class<? extends com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode> blockClass) {
        try {
            var blockUserNode = blockClass.getConstructor().newInstance();
            var blockModel = new CustomBlockNodeModelImpl();
            blockModel.setGraphModel(graphModel);
            blockModel.setSpawnFlags(SpawnFlags.DEFAULT);
            blockModel.initCustomNode(blockUserNode);
            blockModel.onCreateNode();
            return blockModel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
