package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.IVariableNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.variable.IVariable;

public class VariableNodeModelImpl extends VariableNodeModel implements IVariableNode {
    @Override
    public AbstractNodeModel getNodeModel() {
        return this;
    }

    @Override
    public IVariable getVariable() {
        return getVariableDeclarationModel();
    }
}
