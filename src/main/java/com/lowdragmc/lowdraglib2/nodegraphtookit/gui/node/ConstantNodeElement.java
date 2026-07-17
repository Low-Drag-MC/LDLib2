package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ConstantNodeModel;

public class ConstantNodeElement extends CapsuleNodeElement {
    public ConstantNodeElement(ConstantNodeModel nodeModel) {
        super(nodeModel);
    }

    @Override
    protected void buildPartList() {
        if (getModel() instanceof ConstantNodeModel constantNodeModel) {
            parts.add(this.constant = new ConstantNodeEditorElement(constantNodeModel));
        }
    }

}
