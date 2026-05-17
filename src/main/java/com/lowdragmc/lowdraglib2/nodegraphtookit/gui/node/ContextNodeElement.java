package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * UI for a {@link ContextNodeModel}: title, options, a vertical block list (the body), and the
 * context's own input/output ports.
 */
public class ContextNodeElement extends NodeElement {
    @Getter
    @Nullable
    protected BlockListContainerElement blockListContainer;

    public ContextNodeElement(ContextNodeModel nodeModel) {
        super(nodeModel);
    }

    @Override
    public ContextNodeModel getModel() {
        return (ContextNodeModel) super.getModel();
    }

    @Override
    protected void buildPartList() {
        parts.add(this.nodeTittle = new NodeTitleElement(getModel()));
        var model = getModel();
        parts.add(this.nodeOptionContainer = new NodeOptionsInspector(model));
        parts.add(this.portContainerElement = new PortContainerElement(model, PortContainerElement.HORIZONTAL_PORT_FILTER));
        parts.add(this.blockListContainer = new BlockListContainerElement(model));
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        if (blockListContainer != null) addChild(blockListContainer);
    }
}
