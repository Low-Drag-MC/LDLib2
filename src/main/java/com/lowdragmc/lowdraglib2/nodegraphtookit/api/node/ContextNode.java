package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomBlockNodeModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.CustomContextNodeModelImpl;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for context nodes — nodes that hold an ordered list of {@link BlockNode}s.
 *
 * <p>Subclass and annotate with {@link NodeAttribute}. The framework will auto-create the
 * backing {@link CustomContextNodeModelImpl} when the node is added to a graph. Use
 * {@link #onDefinePorts} / {@link #onDefineOptions} for the context's own ports / options.</p>
 *
 * <p>To restrict which block types this context accepts, either:</p>
 * <ul>
 *   <li>Place {@link UseWithContext} on each {@link BlockNode} subclass (declarative), or</li>
 *   <li>Override {@link #getSupportBlocks()} to return an explicit list (programmatic).</li>
 * </ul>
 *
 * <p>The default {@link #getSupportBlocks()} scans the host graph's registered nodes for
 * {@link BlockNode}s whose {@link UseWithContext} includes this context's class.</p>
 */
public abstract class ContextNode extends Node implements IContextNode {

    /**
     * Returns the block types this context will accept. The default implementation scans the
     * host graph's {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#getSupportNodes()}
     * for {@link BlockNode} subclasses whose {@link UseWithContext} value contains this
     * context's runtime class.
     *
     * <p>Override to opt in additional blocks, exclude blocks, or replace the discovery
     * entirely (e.g. to support contexts whose accepted blocks vary by configuration).</p>
     */
    public List<Class<? extends BlockNode>> getSupportBlocks() {
        var model = getContextNodeModel();
        if (model == null) return Collections.emptyList();
        var graph = model.getGraphModel();
        if (graph == null) return Collections.emptyList();
        var result = new ArrayList<Class<? extends BlockNode>>();
        for (var nodeClass : graph.getSupportNodes()) {
            if (!BlockNode.class.isAssignableFrom(nodeClass)) continue;
            @SuppressWarnings("unchecked")
            var blockClass = (Class<? extends BlockNode>) nodeClass;
            var annotation = blockClass.getAnnotation(UseWithContext.class);
            if (annotation == null) continue;
            for (var ctx : annotation.value()) {
                if (ctx.isInstance(this)) {
                    result.add(blockClass);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns whether this context can host a block of the given type. Default: checks
     * {@link UseWithContext} on {@code blockType}, and falls back to {@link #getSupportBlocks()}.
     */
    public boolean acceptsBlock(Class<? extends BlockNode> blockType) {
        var annotation = blockType.getAnnotation(UseWithContext.class);
        if (annotation != null) {
            for (var ctx : annotation.value()) {
                if (ctx.isInstance(this)) return true;
            }
        }
        return getSupportBlocks().contains(blockType);
    }

    /** Typed accessor for the backing {@link ContextNodeModel}. */
    @Nullable
    public ContextNodeModel getContextNodeModel() {
        return getNodeModel() instanceof ContextNodeModel m ? m : null;
    }

    @Override
    public int getBlockCount() {
        var model = getContextNodeModel();
        return model == null ? 0 : model.getBlockCount();
    }

    @Override
    public List<? extends IBlockNode> getBlocks() {
        var model = getContextNodeModel();
        if (model == null) return Collections.emptyList();
        var out = new ArrayList<IBlockNode>(model.getBlockCount());
        for (var blockModel : model.getBlocks()) {
            if (blockModel instanceof CustomBlockNodeModelImpl impl && impl.getNode() instanceof IBlockNode block) {
                out.add(block);
            }
        }
        return out;
    }

    @Override
    public IBlockNode getBlock(int index) {
        var model = getContextNodeModel();
        if (model == null) throw new IndexOutOfBoundsException(index);
        var blockModel = model.getBlocks().get(index);
        if (blockModel instanceof CustomBlockNodeModelImpl impl && impl.getNode() instanceof IBlockNode block) {
            return block;
        }
        throw new IllegalStateException("Block at index " + index + " is not a user-defined BlockNode");
    }
}
