package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IFieldValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.WirePortalModel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PortConstantEditorElement extends ModelElement {
    public final PortModel portModel;
    // runtime
    @Getter
    @Nullable
    protected FieldValueInspector editor;
    protected TypeHandle lastDataType;
    protected boolean lastIsConnected;

    public PortConstantEditorElement(PortModel portModel) {
        this.portModel = portModel;
    }

    /**
     * Determines whether a constant editor should be displayed for a port.
     */
    protected boolean isPortRequireEditor() {
        var isPortal = portModel.getNodeModel() instanceof WirePortalModel;
        return portModel.getDirection() == PortDirection.INPUT && !isPortal;
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        getLayout().flexGrow(1);
        if (isPortRequireEditor()) {
            if (portModel instanceof IFieldValueConfigurable) {
                buildConstantEditor();
            }
        }
    }

    protected void buildConstantEditor() {
        if (portModel instanceof IFieldValueConfigurable filedValue && isPortRequireEditor()) {
            var isConnected = portModel.isConnected();
            var embeddedValue = portModel.getEmbeddedValue();
            var valueType = embeddedValue == null ? null : embeddedValue.getTypeHandle();

            // Rebuild editor if port data type changed.
            if (this.editor != null && (!Objects.equals(lastDataType, valueType) || isConnected != lastIsConnected || isConnected)) {
                editor.removeSelf();
                editor = null;
            }

            lastIsConnected = isConnected;

            if (editor == null && !isConnected) {
                if (portModel.getDirection() == PortDirection.INPUT && portModel.getEmbeddedValue() != null) {
                    lastDataType = portModel.getEmbeddedValue().getTypeHandle();
                    editor = new FieldValueInspector();
                    editor.loadValueField(filedValue);
                    addChild(editor);
                }
            }
        }
    }


    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        buildConstantEditor();
        if (editor != null) {
            var ancestorIsConnected = false;
            var allSubPortsConnected = false;
            var hideEditor = portModel.isConnected() && (portModel.getGraphModel() == null || (portModel.getGraphModel().hideConnectedPortsEditor()));
            if (!hideEditor) {
                var parent = portModel.getParentPort();
                while (parent != null) {
                    if (parent.isConnected()) {
                        ancestorIsConnected = true;
                        break;
                    }
                    parent = parent.getParentPort();
                }

                boolean subPortHandled = false;
                if (!portModel.getSubPorts().isEmpty()) {
                    // todo sub ports
//                if (m_Editor is ConstantField constantField)
//                subPortHandled = constantField.HandleEnabledStateWithWiredSubPorts();

                    // If it was not possible to specifically disable sub-field editors, check if all sub ports are connected and enable/disable the entire field if all sub ports are connected.
                    // It is better to leave the field enabled if at least one sub port is not connected because the user might want to change the value for that sub-field.
//                if (!subPortHandled)
//                {
//                    allSubPortsConnected = true;
//                    foreach (var subPort in portModel.SubPorts)
//                    {
//                        if (!subPort.IsConnected())
//                        {
//                            allSubPortsConnected = false;
//                            break;
//                        }
//                    }
//                }
                }
                editor.setActive(!ancestorIsConnected && !allSubPortsConnected);
            }
            editor.setDisplay(!hideEditor);
        }
    }
}
