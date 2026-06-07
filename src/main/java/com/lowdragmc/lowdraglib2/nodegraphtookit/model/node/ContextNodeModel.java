package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.ContextNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.ContextNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Abstract base for a node that holds an ordered list of {@link BlockNodeModel}s. Blocks are
 * <em>not</em> registered in the graph's top-level {@code nodeModels} list — they live as
 * dependent models of the context, so UID lookups (ports, wires) still resolve, but graph-wide
 * node iteration skips them.
 *
 * <p>This base owns the container semantics (block list, ordering, cascade-delete,
 * serialization). The user-node binding (annotation discovery, port definition delegation)
 * lives in {@link CustomContextNodeModelImpl}, mirroring how {@link NodeModel} and
 * {@link CustomNodeModelImpl} are split.</p>
 *
 * <p>Mutations (insert / remove / move) emit {@link ChangeHint#GRAPH_TOPOLOGY} on this model.
 * The associated {@link ContextNodeElement} listens and rebuilds its block list. Block
 * lifecycle notifications (addNewModel / addDeletedModel) are <em>not</em> emitted for blocks
 * themselves — that would cause {@code GraphView} to also try to instantiate top-level
 * elements for them, which is wrong. The UI tree for blocks is owned exclusively by
 * {@code BlockListContainerElement}.</p>
 */
public abstract class ContextNodeModel extends NodeModel {
    @Getter
    private final List<BlockNodeModel> blocks = new ArrayList<>();

    protected ContextNodeModel() {
        // Contexts don't collapse (the block list IS the body — collapsing makes no sense).
        setCapability(Capabilities.COLLAPSIBLE, false);
    }

    public int getBlockCount() {
        return blocks.size();
    }

    @Override
    public Stream<GraphElementModel> getDependentModels() {
        return Stream.concat(super.getDependentModels(), blocks.stream());
    }

    // ------------------------------------------------------------------
    // Block list operations
    // ------------------------------------------------------------------

    /**
     * Extension point for restricting which blocks this context accepts. Default permits any
     * block — subclasses (e.g. {@link CustomContextNodeModelImpl}) override to delegate to the
     * user-facing {@link ContextNode#acceptsBlock(Class)}.
     */
    public boolean acceptsBlock(BlockNodeModel block) {
        return true;
    }

    /**
     * Block-class equivalent of {@link #acceptsBlock(BlockNodeModel)} — used by UI code that
     * wants to list compatible block types before any instance exists. Default empty.
     */
    public List<Class<? extends BlockNode>> getSupportBlockClasses() {
        return List.of();
    }

    /**
     * Inserts an existing block model into this context at the given index.
     *
     * @param block the block to insert. Must have been instantiated with its graph model and
     *              (if applicable) user node already linked.
     * @param index target index; {@code -1} appends to the end.
     * @throws IllegalArgumentException if {@link #acceptsBlock(BlockNodeModel)} rejects this block.
     * @throws IllegalStateException    if the block already belongs to another context.
     */
    public void insertBlock(BlockNodeModel block, int index) {
        if (block == null) return;
        if (block.getContextNodeModel() != null && block.getContextNodeModel() != this) {
            throw new IllegalStateException("Block already belongs to another context.");
        }
        if (!acceptsBlock(block)) {
            throw new IllegalArgumentException("Block " + block + " is not accepted by context " + this);
        }
        if (index < 0 || index > blocks.size()) index = blocks.size();

        block.setContextNodeModel(this);
        if (graphModel != null) {
            block.setGraphModel(graphModel);
        }
        blocks.add(index, block);
        if (graphModel != null && !block.getSpawnFlags().isOrphan()) {
            block.syncNodePreview();
        }

        if (graphModel != null) {
            graphModel.registerBlockNode(block);
            // Only the parent's GRAPH_TOPOLOGY hint is fired. We intentionally do NOT call
            // addNewModel(block) here — that would tell GraphView to create a top-level UI for
            // the block, but the block's UI is owned by the parent's BlockListContainerElement.
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
        }
    }

    /** Removes the block at the given index. No-op if the index is out of range. */
    @Nullable
    public BlockNodeModel removeBlock(int index) {
        if (index < 0 || index >= blocks.size()) return null;
        var block = blocks.get(index);
        return removeBlock(block) ? block : null;
    }

    /**
     * Removes the given block from this context. Disconnects all wires, unregisters the block
     * (and its ports) from the graph, and emits a topology change on this context.
     *
     * @return {@code true} if the block was a member of this context and was removed.
     */
    public boolean removeBlock(BlockNodeModel block) {
        if (block == null) return false;
        int idx = blocks.indexOf(block);
        if (idx < 0) return false;

        if (graphModel != null) {
            graphModel.deleteWires(block.getConnectedWires());
        }
        blocks.remove(idx);
        block.onDeleteNode();

        if (graphModel != null) {
            graphModel.unregisterBlockNode(block);
            // Same rationale as insertBlock: don't fire addDeletedModel(block) — the parent's
            // GRAPH_TOPOLOGY hint drives the UI to drop the BlockNodeElement (with proper
            // setGraphView(null) cleanup inside BlockListContainerElement).
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
        }
        block.setContextNodeModel(null);
        return true;
    }

    /** Reorders the block from one index to another. No-op if either index is out of range. */
    public void moveBlock(int from, int to) {
        if (from == to) return;
        if (from < 0 || from >= blocks.size()) return;
        if (to < 0 || to >= blocks.size()) return;
        var block = blocks.remove(from);
        blocks.add(to, block);
        if (graphModel != null) {
            graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.GRAPH_TOPOLOGY);
        }
    }

    /** Returns the index of the given block within this context, or {@code -1} if not contained. */
    public int indexOf(BlockNodeModel block) {
        return blocks.indexOf(block);
    }

    @Override
    public void onDeleteNode() {
        // Cascade: remove all child blocks first so their wires are cleaned up and they're
        // unregistered from the graph's UID map.
        if (!blocks.isEmpty()) {
            // copy to avoid concurrent modification
            for (var block : new ArrayList<>(blocks)) {
                removeBlock(block);
            }
        }
        super.onDeleteNode();
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    @Override
    public Tag serializeAdditionalNBT(HolderLookup.Provider provider) {
        var base = super.serializeAdditionalNBT(provider);
        var tag = base instanceof CompoundTag ct ? ct : new CompoundTag();
        if (!blocks.isEmpty()) {
            var listTag = new ListTag();
            for (var block : blocks) {
                if (block == null) continue;
                var blockTag = block.serializeNBT(provider);
                // Mirrors the GraphModel.serialize loop for top-level custom nodes: ICustomNodeModel
                // blocks carry their user-node class name so deserialize can rebuild the right one.
                if (block instanceof ICustomNodeModel custom && custom.getNode() != null) {
                    blockTag.putString("nodeClass", custom.getNode().getClass().getName());
                }
                listTag.add(blockTag);
            }
            tag.put("blocks", listTag);
        }
        return tag;
    }

    @Override
    public void deserializeAdditionalNBT(Tag tag, HolderLookup.Provider provider) {
        super.deserializeAdditionalNBT(tag, provider);
        blocks.clear();
        if (!(tag instanceof CompoundTag compound) || !compound.contains("blocks")) return;
        if (graphModel == null) {
            LDLib2.LOGGER.warn("Cannot deserialize blocks: context model has no graph model.");
            return;
        }
        var listTag = compound.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            var blockTag = listTag.getCompound(i);
            try {
                // Concrete block model: today only the user-node-backed impl. If future block
                // model variants are introduced, swap this for a discriminator switch.
                var blockModel = new CustomBlockNodeModelImpl();
                blockModel.setGraphModel(graphModel);
                blockModel.deserializeNBT(provider, blockTag);

                var nodeClassName = blockTag.getString("nodeClass");
                var blockUserNode = graphModel.findNodeByClassName(nodeClassName);
                if (blockUserNode == null) {
                    LDLib2.LOGGER.warn("Could not find block node class: {}", nodeClassName);
                    continue;
                }
                blockModel.initCustomNode(blockUserNode);
                blockModel.setContextNodeModel(this);
                blocks.add(blockModel);
                blockModel.syncNodePreview();
                // defineNode is intentionally NOT called here. The caller (GraphModel deserialize
                // or paste) calls defineNode on the context AFTER any UID re-assignment, and our
                // overridden defineNode cascades to each block. Defining ports here would compute
                // port UIDs from the block's current UID — which is the original UID, leading to
                // duplicate-UID errors when the paste path subsequently re-uids the context.
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to deserialize block: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Defines the context's own ports/options, then cascades to each contained block so its
     * ports are computed from the block's current UID. The cascade matters for the paste path:
     * paste re-uids each block before calling defineNode here, and the block's port UIDs are
     * derived from that fresh UID — preventing UID collisions with the source-graph originals.
     */
    @Override
    public void defineNode() {
        super.defineNode();
        for (var block : blocks) {
            if (block != null) block.defineNode();
        }
    }

    @Override
    public boolean hasNodePreview() {
        return false;
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new ContextNodeElement(this);
    }
}
