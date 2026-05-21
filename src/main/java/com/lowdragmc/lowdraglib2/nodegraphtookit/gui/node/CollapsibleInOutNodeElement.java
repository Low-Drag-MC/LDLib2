package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortNodeModel;

public class CollapsibleInOutNodeElement extends NodeElement {

    public CollapsibleInOutNodeElement(AbstractNodeModel nodeModel) {
        super(nodeModel);
        addClass("__collapsible-in-out-node__");
    }

    @Override
    protected void buildPartList() {
        // todo vertical
//        PartList.AppendPart(VerticalPortContainerPart.Create(topPortContainerPartName, Model, this, ussClassName, BasePortContainerPart.inputPortFilter));
        parts.add(this.nodeTittle = new NodeTitleElement(getModel()));
        if (getModel() instanceof NodeModel nodeModel) {
            parts.add(this.nodeOptionContainer = new NodeOptionsInspector(nodeModel));
        }
        if (getModel() instanceof PortNodeModel portNodeNode) {
            parts.add(this.portContainerElement = new InOutPortContainerElement(portNodeNode, PortContainerElement.HORIZONTAL_PORT_FILTER));
        }
//        PartList.AppendPart(VerticalPortContainerPart.Create(bottomPortContainerPartName, Model, this, ussClassName, BasePortContainerPart.outputPortFilter));
    }
}
