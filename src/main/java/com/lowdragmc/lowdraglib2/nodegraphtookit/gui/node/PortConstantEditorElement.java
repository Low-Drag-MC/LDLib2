package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.WirePortalModel;
import dev.vfyjxf.taffy.style.TaffyDisplay;
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
        addClass("__port-constant-editor__");
    }

    /**
     * Determines whether a constant editor should be displayed for a port.
     */
    protected boolean isPortRequireEditor() {
        var isPortal = portModel.getNodeModel() instanceof WirePortalModel;
        if (portModel.getDirection() != PortDirection.INPUT || isPortal) return false;
        // Vertical ports default to no inline configurator; the host graph can opt back in.
        if (portModel.getOrientation() == PortOrientation.Vertical) {
            var gm = portModel.getGraphModel();
            return gm != null && gm.showVerticalPortConfigurator();
        }
        return true;
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        Style.defaultPipeline(getLayout(), l -> l.flexGrow(1));
        if (isPortRequireEditor()) {
            buildConstantEditor();
        }
    }

    protected void buildConstantEditor() {
        if (isPortRequireEditor()) {
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
                    if (getGraphView() != null) editor.setHistoryStack(getGraphView().getHistoryStack());
                    editor.loadValueField(portModel);
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
            // Hide editor when port is connected (data-driven) — pin via IMPORTANT.
            Style.importantPipeline(getLayout(), l -> l.display(hideEditor ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        } else {
            Style.importantPipeline(getLayout(), l -> l.display(TaffyDisplay.NONE));
        }
    }
}
