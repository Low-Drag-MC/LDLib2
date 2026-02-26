package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class PortModelImpl extends PortModel {
    @Getter
    protected PortConnectorUI connectorUI = PortConnectorUI.DEFAULT;

    public PortModelImpl(PortNodeModel nodeModel, PortDirection direction, PortOrientation orientation, String portId, PortType portType, TypeHandle dataTypeHandle, PortModelOptions options, @Nullable PortModel parentPort) {
        super(nodeModel, direction, orientation, portId, portType, dataTypeHandle, options, parentPort);
    }

    public void setConnectorUI(PortConnectorUI connectorUI) {
        this.connectorUI = connectorUI;
        if (this.graphModel != null) {
            this.graphModel.getCurrentGraphChangeDescription().addChangedModel(this, ChangeHint.STYLE);
        }
    }
}
