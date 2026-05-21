package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.BlockNodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.Capabilities;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base for a node that lives inside a {@link ContextNodeModel}'s ordered list rather
 * than at the top level of the graph.
 *
 * <p>Block models are <em>not</em> added to {@code GraphModel#nodeModels}. Their lifecycle is
 * owned by their parent context, but their ports are still registered with the graph (via the
 * context's {@code getDependentModels()}) so wires can target them.</p>
 *
 * <p>This base owns block semantics (parent link, "needs container" capability). The
 * user-node binding lives in {@link CustomBlockNodeModelImpl}, mirroring the
 * {@link NodeModel} / {@link CustomNodeModelImpl} split.</p>
 *
 * <p>{@link #createElementUI()} returns {@code null}: blocks are not created as top-level UI
 * elements by {@code GraphView}. The {@code BlockListContainerElement} inside the parent's
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.ContextNodeElement} owns block
 * UI construction and disposal.</p>
 */
public abstract class BlockNodeModel extends NodeModel {
    @Getter @Setter @Nullable
    private ContextNodeModel contextNodeModel;

    protected BlockNodeModel() {
        // Blocks live inside a context; they can't be dragged on the canvas and can't escape
        // back out to the top level via the "ascend" gesture.
        setCapability(Capabilities.MOVABLE, false);
        setCapability(Capabilities.ASCENDABLE, false);
        setCapability(Capabilities.NEEDS_CONTAINER, true);
        // Blocks size to their parent context's column; user-resizable min-width would fight
        // that layout.
        setCapability(Capabilities.RESIZABLE, false);
    }

    /** Convenience: returns the user-facing block node, if this model wraps one. */
    @Nullable
    public BlockNode getBlockNode() {
        return null;
    }

    /** Zero-based position within the parent's block list, or {@code -1} if unattached. */
    public int getIndex() {
        return contextNodeModel == null ? -1 : contextNodeModel.indexOf(this);
    }

    @Override
    public boolean hasNodePreview() {
        return false;
    }

    /**
     * Returns {@code null} — blocks must not be instantiated as top-level UI elements by
     * {@code GraphView}. Their UI is built inside their parent context's
     * {@code BlockListContainerElement}.
     */
    @Override
    public @Nullable BlockNodeElement createElementUI() {
        return new BlockNodeElement(this);
    }

    @Override
    public float getPortWireOffset() {
        return 20;
    }
}
