package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import org.jetbrains.annotations.Nullable;

/**
 * Public, read-only view of a block node: a node that lives inside a {@link IContextNode}'s
 * ordered list. Blocks behave like regular nodes (ports, options, wires) but cannot exist
 * standalone in the graph.
 */
public interface IBlockNode extends INode {

    /** The parent context that owns this block, or {@code null} if the block is not yet attached. */
    @Nullable
    IContextNode getContextNode();

    /**
     * Zero-based position of this block within its parent context's block list, or {@code -1}
     * if the block has no parent.
     */
    int getIndex();
}
