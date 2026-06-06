package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;

import java.util.function.Predicate;

/**
 * {@link PortContainerElement} whose backing container lays vertical ports out in a horizontal
 * strip. Used by {@link CollapsibleInOutNodeElement} for the input row above the title and the
 * output row below the node body.
 */
public class VerticalPortContainerElement extends PortContainerElement {

    public VerticalPortContainerElement(PortNodeModel portNodeModel, Predicate<PortModel> portFilter) {
        super(portNodeModel, portFilter);
        addClass("__vertical-port-container__");
    }

    @Override
    protected PortContainer createPortContainer() {
        return new VerticalPortContainer();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        // Single writer of this element's display: hide the whole row (incl. its padding) when the
        // node is collapsed OR there are no vertical ports of this direction. Driving it here — not
        // from the parent node element — avoids the parent/child IMPORTANT-origin overwrite (parts
        // update after their owner).
        boolean collapsed = portNodeModel instanceof AbstractNodeModel anm && anm.isCollapsed();
        boolean empty = portContainer == null || portContainer.getPortElements().isEmpty();
        Style.importantPipeline(getLayout(), l -> l.display(collapsed || empty ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
    }
}
