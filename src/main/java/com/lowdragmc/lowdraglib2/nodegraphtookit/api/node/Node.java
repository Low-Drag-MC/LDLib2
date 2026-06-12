package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodePreviewContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.IPort;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import lombok.Getter;
import net.minecraft.network.chat.Component;

/**
 * The base class for all user-accessible nodes in a graph.
 *
 * <p>Inherit from this class to define custom node types that appear in the graph. The {@link Node} class provides
 * lifecycle hooks, serialization support, and the structure needed to define ports, UI behaviors, and custom logic.
 * This class forms the foundation of all user-defined nodes in a graph-based tool, including variable nodes,
 * context nodes, and subgraph nodes.</p>
 *
 * <p>To create a custom node, derive from {@link Node}, define its input and output ports using a port builder
 * in {@link #onDefinePorts(IPortDefinitionContext)}, and define its options in
 * {@link #onDefineOptions(IOptionDefinitionContext)}.</p>
 *
 * <p>This class is used in combination with other types like {@link INode}, {@link IPort}, and
 * {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph} to construct and manage node-based workflows.</p>
 *
 * @see INode
 * @see IVariableNode
 * @see ISubgraphNode
 */
public abstract class Node implements INode {
    /**
     * Backing implementation model.
     */
    @Getter
    private AbstractNodeModel nodeModel;

    public void setImplementation(NodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public abstract Component getDisplayName();

    public IGuiTexture getNodeIcon() {
        return Icons.NODE;
    }

    /**
     * Gets the minimum width this node requires. Editor-controlled {@code minWidth} cannot be
     * reduced below this value.
     *
     * @return the node-specific minimum width floor, or {@code 0} for no additional floor
     */
    public float getNodeWidth() {
        return 0f;
    }

    /**
     * Defines the structure of the node by building its ports and options.
     *
     * <p>This method calls both {@link #onDefineOptions(IOptionDefinitionContext)} and
     * {@link #onDefinePorts(IPortDefinitionContext)} to allow custom definition of the node.</p>
     */
    public void defineNode() {
        if (nodeModel instanceof NodeModel n) {
            n.defineNode();
        }
    }

                                 /**
     * Called during {@link #defineNode()} to define the options available on the node.
     *
     * <p>This method is called before {@link #onDefinePorts(IPortDefinitionContext)}. Override this method to add node options
     * using the provided {@link IOptionDefinitionContext}.</p>
     *
     * @param context provides methods for defining node options
     */
    public void onDefineOptions(IOptionDefinitionContext context) {}

    /**
     * Called during {@link #defineNode()} to define the input and output ports of the node.
     *
     * <p>This method is called after {@link #onDefineOptions(IOptionDefinitionContext)} and is used to declare the structure
     * of the node's connectivity. Use the provided {@link IPortDefinitionContext} to define input and output ports using a
     * builder pattern.</p>
     *
     * @param context provides methods for defining input and output ports
     */
    public void onDefinePorts(IPortDefinitionContext context) {}

    // region preview

    /**
     * Whether this node shows a preview panel beneath its body. Override to return {@code true} and
     * implement {@link #onBuildNodePreview(NodePreviewContext)} to populate it — e.g. a shader graph node can
     * render a live shader preview. When {@code true}, a {@code NodePreviewModel} is created for the
     * node and a preview panel is rendered inside the node element.
     *
     * @return {@code true} to enable the preview panel; {@code false} (default) for no preview
     */
    public boolean hasNodePreview() {
        return false;
    }

    /**
     * Whether this node's preview starts expanded when the node is freshly created. The returned
     * value is only used for new nodes; after creation, the user's preview-expanded state is
     * persisted by the node model and restored from saved graph data.
     *
     * @return {@code true} (default) to start expanded; {@code false} to start collapsed
     */
    public boolean isNodePreviewExpandedByDefault() {
        return true;
    }

    /**
     * Populates the preview panel's content container. Called when the panel is (re)built. Add the
     * UI that renders this node's preview (e.g. a custom framebuffer/texture element). The
     * {@link NodePreviewContext} provides the content container plus the node model, preview model,
     * and live graph view so the preview can read inputs/options and react to the editor. Default is
     * a no-op.
     *
     * @param context the preview build context (container + node/preview/graph references)
     */
    public void onBuildNodePreview(NodePreviewContext context) {}

    /**
     * Called when the preview panel updates (e.g. an input port value or connection changed). Use it
     * to refresh dynamic preview content — repaint a shader, recompute a value, or
     * {@link NodePreviewContext#rebuild() rebuild} the panel. Default is a no-op (static content
     * built in {@link #onBuildNodePreview} already re-reads the model when it draws).
     *
     * @param context the preview context (same references as the build call)
     */
    public void onUpdateNodePreview(NodePreviewContext context) {}

    // endregion

}
