package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodePreviewElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IGraphElementUIModel;
import org.jetbrains.annotations.Nullable;

/**
 * Model for a node's preview panel — a visual preview of the node's output rendered beneath the
 * node body (e.g. a live shader preview in a shader graph).
 *
 * <p>A node opts into a preview via {@link AbstractNodeModel#hasNodePreview()} (custom nodes delegate
 * to {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node#hasNodePreview()}). The preview's
 * UI is produced by {@link #createElementUI()}; override {@link AbstractNodeModel#createNodePreview()}
 * to supply a subclass with a custom {@link #createElementUI()} for fully bespoke previews.</p>
 */
public class NodePreviewModel extends GraphElementModel implements IGraphElementUIModel {
    private AbstractNodeModel parentNode;
    private boolean isExpanded = true;

    /**
     * Creates a new node preview model.
     */
    public NodePreviewModel() {
    }

    @Override
    public @Nullable GraphElement<?> createElementUI() {
        return new NodePreviewElement(this);
    }

    /**
     * Gets the parent node that this preview belongs to.
     *
     * @return the parent node
     */
    public AbstractNodeModel getParentNode() {
        return parentNode;
    }

    /**
     * Sets the parent node.
     *
     * @param parentNode the parent node
     */
    public void setParentNode(AbstractNodeModel parentNode) {
        this.parentNode = parentNode;
    }

    /**
     * Checks if the preview is expanded.
     *
     * @return {@code true} if expanded
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     * Sets whether the preview is expanded.
     *
     * @param expanded {@code true} to expand
     */
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    /**
     * Called when the preview is created.
     *
     * @param nodeModel the parent node model
     */
    public void onCreateNodePreview(AbstractNodeModel nodeModel) {
        this.parentNode = nodeModel;
        setGraphModel(nodeModel.getGraphModel());
    }

    /**
     * Called when duplicating a node preview.
     *
     * @param sourcePreview the source preview to copy from
     */
    public void onDuplicateNodePreview(NodePreviewModel sourcePreview) {
        if (sourcePreview != null) {
            this.isExpanded = sourcePreview.isExpanded;
        }
    }
}
