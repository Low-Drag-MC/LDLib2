package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;

public class CollapsibleInOutNodeElement extends NodeElement {
    /** CSS class applied while the node is collapsed — exposed for stylesheet rules. */
    public static final String COLLAPSED_CLASS = "__collapsed__";

    public CollapsibleInOutNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__collapsible-in-out-node__");
    }

    @Override
    protected void buildPartList() {
        // todo vertical
//        PartList.AppendPart(VerticalPortContainerPart.Create(topPortContainerPartName, Model, this, ussClassName, BasePortContainerPart.inputPortFilter));
        parts.add(this.nodeTittle = new CollapsibleNodeTitleElement(getModel()));
        if (getModel() instanceof NodeModel nodeModel) {
            parts.add(this.nodeOptionContainer = new NodeOptionsInspector(nodeModel));
        }
        if (getModel() instanceof PortNodeModel portNodeNode) {
            parts.add(this.portContainerElement = new InOutPortContainerElement(portNodeNode, PortContainerElement.HORIZONTAL_PORT_FILTER));
        }
//        PartList.AppendPart(VerticalPortContainerPart.Create(bottomPortContainerPartName, Model, this, ussClassName, BasePortContainerPart.outputPortFilter));
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        applyCollapsedState(getModel().isCollapsed());
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.LAYOUT)) {
            applyCollapsedState(getModel().isCollapsed());
        }
    }

    /**
     * Hides the options and ports while keeping the title bar visible. Stylesheets can hook into
     * {@link #COLLAPSED_CLASS} for additional theming (e.g. rounded bottom corners on the title).
     */
    protected void applyCollapsedState(boolean collapsed) {
        if (collapsed) {
            addClass(COLLAPSED_CLASS);
        } else {
            removeClass(COLLAPSED_CLASS);
        }
        if (nodeOptionContainer != null) {
            Style.importantPipeline(nodeOptionContainer.getLayout(),
                    l -> l.display(collapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        }
        if (portContainerElement != null) {
            StylemportantPipeline(portContainerElement.getLayout(),
                    l -> l.display(collapsed ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        }
    }
}
