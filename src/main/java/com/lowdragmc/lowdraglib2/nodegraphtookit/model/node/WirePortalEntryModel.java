package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class WirePortalEntryModel extends WirePortalModel implements ISingleInputPortNodeModel{
    @Getter @Setter(AccessLevel.PROTECTED)
    private PortModel inputPort;

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        setInputPort(scope.nodeModel.addInputPort("", getPortDataTypeHandle(), getPortType(),
                null, null, null, null));
    }

    @Override
    public boolean canHaveAnotherPortalWithSameDirectionAndDeclaration() {
        return false;
    }
}
