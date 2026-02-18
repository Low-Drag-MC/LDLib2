package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;

/**
 * Model for node preview functionality.
 *
 * <p>Node previews allow showing a visual preview of the node's output.
 * TODO: Implement preview rendering for your UI framework.</p>
 */
public class NodePreviewModel extends GraphElementModel {
    private AbstractNodeModel parentNode;
    private boolean isExpanded = true;

    /**
     * Creates a new node preview model.
     */
    public NodePreviewModel() {
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
