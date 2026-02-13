package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IPlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.PlaceholderModelHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.NodeDefinitionScope;

public class NodePlaceholder extends NodeModel implements IPlaceHolder {
    @Override
    protected void onDefineNode(NodeDefinitionScope<? extends NodeModel> scope) {
        PlaceholderModelHelper.setPlaceholderCapabilities(this);
    }

    @Override
    protected void disconnectPort(PortModel portModel) {
        // We do not want to disconnect ports that are unused, to create missing ports.
    }
}
