package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class WirePortalExitModel extends WirePortalModel implements ISingleOutputPortNodeModel{
    @Getter @Setter(AccessLevel.PROTECTED)
    private PortModel outputPort;

    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        setOutputPort(scope.nodeModel.addOutputPort("", getPortDataTypeHandle(), getPortType(),
                null, null));
    }

    @Override
    public boolean canCreateOppositePortal() {
        var portalRefs = getGraphModel().findReferencesInGraph(WirePortalModel.class, getDeclarationModel());
        for (var portalRef : portalRefs) {
            if (portalRef instanceof ISingleInputPortNodeModel) return false;
        }
        return true;
    }
}
