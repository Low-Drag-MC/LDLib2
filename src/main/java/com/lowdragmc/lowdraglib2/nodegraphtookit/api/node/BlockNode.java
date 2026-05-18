package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomContextNodeModelImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for block nodes — nodes that live inside a {@link ContextNode}'s ordered list.
 *
 * <p>Blocks behave like regular nodes (have ports, options, wires) but they are not
 * top-level graph nodes; they are reachable only through their parent context. A block is
 * compatible with a context when either:</p>
 * <ul>
 *   <li>The block class is annotated {@code @UseWithContext(MyContext.class)}, or</li>
 *   <li>The context's {@code getSupportBlocks()} explicitly includes the block class.</li>
 * </ul>
 *
 * <p>Subclass and annotate with {@link NodeAttribute}. The framework auto-creates the
 * backing {@link com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomBlockNodeModelImpl}
 * when the block is inserted into a context.</p>
 */
public abstract class BlockNode extends Node implements IBlockNode {

    /** Typed accessor for the backing {@link BlockNodeModel}. */
    @Nullable
    public BlockNodeModel getBlockNodeModel() {
        return getNodeModel() instanceof BlockNodeModel m ? m : null;
    }

    @Override
    @Nullable
    public IContextNode getContextNode() {
        var model = getBlockNodeModel();
        if (model == null) return null;
        var parent = model.getContextNodeModel();
        if (parent instanceof CustomContextNodeModelImpl impl && impl.getNode() instanceof IContextNode ctx) {
            return ctx;
        }
        return null;
    }

    @Override
    public int getIndex() {
        var model = getBlockNodeModel();
        return model == null ? -1 : model.getIndex();
    }
}
