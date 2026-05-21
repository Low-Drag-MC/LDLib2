package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel;
import dev.vfyjxf.taffy.style.AlignContent;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * UI for a {@link ContextNodeModel}: title, options, a vertical block list (the body), and the
 * context's own input/output ports.
 */
public class ContextNodeElement extends CollapsibleInOutNodeElement {
    @Getter
    @Nullable
    protected BlockListContainerElement blockListContainer;

    public ContextNodeElement(ContextNodeModel nodeModel) {
        super(nodeModel);
        addClass("__context-node__");
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
        parts.add(this.portContainerElement = new InOutPortContainerElement(model, PortContainerElement.HORIZONTAL_PORT_FILTER));
        parts.add(this.blockListContainer = new BlockListContainerElement(model));
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        if (nodeTittle != null) {
            Style.defaultPipeline(nodeTittle.getStyle(), s -> s.background(Sprites.BORDER1_THICK_RT1));
            Style.defaultPipeline(nodeTittle.getLayout(), l -> l.justifyContent(AlignContent.CENTER).height(25));
            if (nodeTittle.titleContainer != null) {
                Style.defaultPipeline(nodeTittle.titleContainer.getLayout(), l -> l.justifyContent(AlignContent.CENTER));
            }
            if (nodeTittle.nodeTittle != null) {
                Style.defaultPipeline(nodeTittle.nodeTittle.getTextStyle(), s -> s.fontSize(12));
            }
        }
        if (blockListContainer != null) addChild(blockListContainer);
    }

    @Override
    protected boolean showHoverHighlight() {
        if (isSelfOrChildHover()) {
            if (blockListContainer != null && this.blockListContainer.blockContainer != null) {
                return !blockListContainer.isSelfOrChildHover();
            }
        }
        return isUnderRegionSelection();
    }
}
