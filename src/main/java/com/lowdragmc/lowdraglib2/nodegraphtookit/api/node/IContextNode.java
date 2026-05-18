package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import java.util.List;

/**
 * Public, read-only view of a context node: an ordered container of {@link IBlockNode}s.
 *
 * <p>A context node groups blocks together in a fixed vertical stack. Each context declares
 * which block types it accepts via {@code ContextNode#getSupportBlocks()} and/or the
 * {@link UseWithContext} annotation on block classes.</p>
 *
 * <p>Blocks are <em>not</em> top-level graph nodes: they are reachable only through their
 * parent context.</p>
 */
public interface IContextNode extends INode {

    /** Number of blocks currently contained in this context. */
    int getBlockCount();

    /** Read-only view of the blocks, in display order. */
    List<? extends IBlockNode> getBlocks();

    /**
     * Returns the block at the given index.
     *
     * @param index zero-based index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    IBlockNode getBlock(int index);
}
