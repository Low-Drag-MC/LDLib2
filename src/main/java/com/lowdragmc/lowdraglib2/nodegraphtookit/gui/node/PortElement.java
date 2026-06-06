package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortCapacity;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireDragHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.IPlaceHolder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.ModifierFlags;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.variable.VariableDeclarationModelBase;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PortElement extends GraphElement<PortModel> {
    @Getter
    private PortConnectorElement connector;
    @Getter
    private PortConstantEditorElement constant;

    // runtime
    @Getter
    private boolean isWireDragging;
    @Nullable
    private WireDragHelper wireDragHelper;

    public PortElement(PortModel portModel) {
        super(portModel);
        addClass("__port__");
    }

    // region build ui

    @Override
    protected void buildPartList() {
        parts.add(connector = createConnector());
        parts.add(constant = new PortConstantEditorElement(getModel()));
    }

    /**
     * Factory for the connector element. Overridden by {@link VerticalPortElement} to spawn a
     * connector that stacks the wire dot above the type-icon + label.
     */
    protected PortConnectorWithIconElement createConnector() {
        return new PortConnectorWithIconElement(getModel());
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(2)
                .minHeight(14));
        connector.getWireDragParts().forEach(p -> p.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown));
        connector.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        connector.addEventListener(UIEvents.DRAG_END, this::onDragEnd);
        addChildren(connector, constant);
    }

    // endregion

    @Override
    public void setGraphView(@Nullable GraphView graphView) {
        super.setGraphView(graphView);
        if (graphView == null) {
            wireDragHelper = null;
        } else {
            wireDragHelper = new WireDragHelper(graphView);
        }
    }

    @Override
    public boolean hasModelDependenciesChanged() {
        return true;
    }

    @Override
    public void addModelDependencies() {
        var model = getModel();
        for (WireModel wire : model.getConnectedWires()) {
            addDependencyToWireModel(wire);
        }

        // The value configurator needs to be refreshed to enable or disable,
        // if there is an editor and an ancestor or descendant port is changed.
        if (model.getDirection() == PortDirection.INPUT) {
            var parentPort = model.getParentPort();
            while (parentPort != null) {
                getDependencies().addModelDependency(parentPort);
                parentPort = parentPort.getParentPort();
            }

            addSubPorts(model);
        }
    }

    private void addSubPorts(PortModel portModel) {
        for (var subPort : portModel.getSubPorts()){
            getDependencies().addModelDependency(subPort);
            addSubPorts(subPort);
        }
    }

    /**
     * add the wire model as a model dependency to this element.
     */
    public void addDependencyToWireModel(WireModel model) {
        dependencies.addModelDependency(model);
    }

    /**
     * Whether the port will be connected during an edge drag if the mouse is released where it is.
     */
    public void setWillConnect(boolean willConnect) {
        connector.setWillConnect(willConnect);
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        var model = getModel();
        if (visitor.hasHint(ChangeHint.DATA)) {
            // direction follows port direction (model data) — pin via IMPORTANT.
            Style.importantPipeline(getLayout(), l -> l.direction(model.getDirection() == PortDirection.INPUT ? TaffyDirection.LTR :
                            model.getDirection() == PortDirection.OUTPUT ? TaffyDirection.RTL : TaffyDirection.INHERIT));
        }
    }

    protected boolean canPerformConnection(Vector2f localMouse) {
        var mui = getModularUI();
        if (mui == null) return false;
        var lastMouseDown = getLocalMouse(mui.getLastMouseDownX(), mui.getLastMouseDownY());
        return localMouse.distance(lastMouseDown) > WireDragHelper.DISTANCE_THRESHOLD;
    }

    protected void onMouseDown(UIEvent event) {
        if (isWireDragging) {
            event.stopImmediatePropagation();
            return;
        }
        if (event.button == 0 && graphView != null && wireDragHelper != null) {
            wireDragHelper.createWireCandidate();
            wireDragHelper.setDraggedPort(getModel());
            if (wireDragHelper.handleMouseDown(event, null)) {
                // Disable all wires except the dragged one.
                WireDragHelper.enableAllWires(wireDragHelper.graphView, false, Set.of(wireDragHelper.getWireCandidateModel()));
                isWireDragging = true;
                event.stopPropagation();
//                // We need to prevent the node on which the port is from being culled because it would detach the port and loose the mouse capture.
//                if (graphView.getModelElement(model.getNodeModel()) instanceof GraphElement<?> nodeUI) {
//                    nodeUI.preventCulling = true;
//                }
                connector.startDrag(wireDragHelper, null);
            } else {
                wireDragHelper.reset();
            }
        }
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (isWireDragging && graphView != null && graphView.getGraph() != null
                && event.dragHandler.draggingObject == wireDragHelper
                && wireDragHelper != null) {
            wireDragHelper.handleMouseMove(event);
            event.stopPropagation();
        }
    }

    protected void onDragEnd(UIEvent event) {
        if (isWireDragging) {
            if (graphView != null && graphView.getGraph() != null
                    && event.dragHandler.draggingObject == wireDragHelper
                    && wireDragHelper != null) {
                if (canPerformConnection(getLocalMouse(event.x, event.y))) {
                    wireDragHelper.handleMouseUp(event, true, Collections.emptyList(), Collections.emptyList());
                } else {
                    wireDragHelper.reset();
                }
            }
            isWireDragging = false;
            event.stopPropagation();
        }
    }

    public boolean canAcceptDrop(GraphElementModel droppedElement) {
        // The elements that can be dropped: a variable declaration from the Blackboard and any node with a single input or output (eg.: variable and constant nodes).
        return switch (droppedElement) {
            case VariableDeclarationModelBase variableDeclaration -> canAcceptDroppedVariable(variableDeclaration);
            case ISingleInputPortNodeModel ignored ->
                    droppedElement instanceof PortNodeModel portNodeModel && (portNodeModel.getPortFitToConnectTo(getModel()) != null);
            case ISingleOutputPortNodeModel ignored ->
                    droppedElement instanceof PortNodeModel portNodeModel && (portNodeModel.getPortFitToConnectTo(getModel()) != null);
            default -> false;
        };
    }

    protected boolean canAcceptDroppedVariable(VariableDeclarationModelBase variableDeclaration) {
        if (variableDeclaration instanceof IPlaceHolder) return false;

        var model = getModel();
        if (model.getPortCapacity() == PortCapacity.NONE)
            return false;

        if (!variableDeclaration.isInputOrOutput())
            return model.getDirection() == PortDirection.INPUT
                    && Objects.equals(variableDeclaration.getDataTypeHandle(), model.getDataTypeHandle());

        if (!Objects.equals(model.getDataTypeHandle(), variableDeclaration.getDataTypeHandle()))
            return false;

        var isInput = variableDeclaration.getModifiers() == ModifierFlags.READ;
        return (!isInput || model.getDirection() == PortDirection.INPUT) && (isInput || model.getDirection() == PortDirection.OUTPUT);
    }
}
