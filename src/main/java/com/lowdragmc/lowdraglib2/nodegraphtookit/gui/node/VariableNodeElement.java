package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.VariableNodeModel;

public class VariableNodeElement extends CapsuleNodeElement {
    public VariableNodeElement(VariableNodeModel variableNodeModel) {
        super(variableNodeModel);
    }

    @Override
    public VariableNodeModel getModel() {
        return (VariableNodeModel) super.getModel();
    }
}
