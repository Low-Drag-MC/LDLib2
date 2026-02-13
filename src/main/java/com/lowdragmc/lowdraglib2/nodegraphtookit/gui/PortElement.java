package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.IValueConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortConnectorUI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant.Constant;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModelImpl;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDirection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

public class PortElement extends GraphElement<PortModel> {
    public final UIElement portConnector = new UIElement();
    public final Label portName = new Label();
    public final UIElement constantConfigurator = new UIElement();
    // runtime
    @Getter
    private boolean isWireDragging;
    @Nullable
    private WireDragHelper wireDragHelper;
    @Getter
    private boolean willConnect = false;
    @Nullable
    private PortModelOptions lastOptions = null;
    @Nullable
    private Constant lastEmbeddedValue = null;
    @Nullable
    private Type lastType = null;
    private boolean hasConstantConfigurators = false;

    public PortElement(PortModel portModel) {
        super(portModel);
        getLayout().flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2);
        portConnector.getLayout().aspectRatio(1).width(9);
        portName.getTextStyle().adaptiveWidth(true);
        constantConfigurator.getLayout().flexGrow(1).minWidth(55);
        portConnector.addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        portConnector.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        portConnector.addEventListener(UIEvents.DRAG_END, this::onDragEnd);

        addChildren(portConnector, portName, constantConfigurator);
        internalSetup();
    }

    @Override
    protected void setGraphView(@Nullable GraphView graphView) {
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
        if (willConnect == this.willConnect) return;
        this.willConnect = willConnect;
        updateConnector();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        super.updateUIFromModel(visitor);
        if (visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY) || visitor.hasHint(ChangeHint.DATA)) {
            // update connector icon
            updateConnector();

            // Hide the editor if the port is connected.
            constantConfigurator.setDisplay(hasConstantConfigurators && model.getConnectedWires().isEmpty());
        }
        if (visitor.hasHint(ChangeHint.STYLE)) {
            // update title and tooltips
            portName.setText(model.getDisplayName());
        }
        if (visitor.hasHint(ChangeHint.DATA)) {
            getLayout().direction(model.getDirection() == PortDirection.INPUT ? TaffyDirection.LTR :
                            model.getDirection() == PortDirection.OUTPUT ? TaffyDirection.RTL : TaffyDirection.INHERIT);
            createOrUpdateConstant();
        }
    }

    protected void updateConnector() {
        if (model.getOptions().hasFlag(PortModelOptions.NODE_OPTION)) {
            portConnector.setDisplay(false);
        } else {
            portConnector.setDisplay(true);
            var connectorUI = model instanceof PortModelImpl portModel ? portModel.getConnectorUI() : PortConnectorUI.DEFAULT;
            var icon = connectorUI.getIcon(model.isConnected() || isWillConnect());
            portConnector.getStyle().background(DynamicTexture.of(() -> {
                if (isActive()) return icon;
                else return icon.copy().setColor(ColorPattern.GRAY.color);
            }));
        }
    }

    protected void createOrUpdateConstant() {
        var options = model.getOptions();
        var embeddedValue = model.getEmbeddedValue();
        var valueType = embeddedValue == null ? null : embeddedValue.getType();
        if (lastOptions == options && lastEmbeddedValue == embeddedValue && lastType == valueType ) return;

        constantConfigurator.clearAllChildren();
        hasConstantConfigurators = false;
        if (!model.getOptions().hasFlag(PortModelOptions.NO_EMBEDDED_CONSTANT)
                && model instanceof IValueConfigurable configurable) {
            var container = new ConfiguratorGroup();
            configurable.buildConfigurator(container);
            if (!container.getConfigurators().isEmpty()) {
                for (Configurator configurator : container.getConfigurators()) {
                    constantConfigurator.addChild(configurator);
                }
                hasConstantConfigurators = true;
            }
        }
        constantConfigurator.setDisplay(hasConstantConfigurators && model.getConnectedWires().isEmpty());
        lastOptions = options;
        lastEmbeddedValue = embeddedValue;
        lastType = valueType;
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
            wireDragHelper.setDraggedPort(model);
            if (wireDragHelper.handleMouseDown(event, null)) {
                // Disable all wires except the dragged one.
                WireDragHelper.enableAllWires(wireDragHelper.graphView, false, Set.of(wireDragHelper.getWireCandidateModel()));
                isWireDragging = true;
                event.stopPropagation();
//                // We need to prevent the node on which the port is from being culled because it would detach the port and loose the mouse capture.
//                if (graphView.getModelElement(model.getNodeModel()) instanceof GraphElement<?> nodeUI) {
//                    nodeUI.preventCulling = true;
//                }
                portConnector.startDrag(wireDragHelper, null);
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

}
