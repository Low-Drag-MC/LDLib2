package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandles;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.NodeCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.WireCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.GhostWireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.IGhostWireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireSide;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WireDragHelper {
    public final static float DISTANCE_THRESHOLD = 10;
    public final GraphView graphView;
    private List<PortModel> allPorts;
    private List<PortModel> compatiblePorts;
    private WireElement ghostWire;
    private WireModel ghostWireModel;
    private WireElement wireCandidate;
    @Getter
    private GhostWireModel wireCandidateModel;
    @Getter @Setter
    private PortModel draggedPort;
    @Getter @Setter
    private WireElement originalWire;
    private PortModel previousEndPortModel;


    public WireDragHelper(GraphView graphView) {
        this.graphView = graphView;
        reset();
    }

    /**
     * Reset the wire drag helper
     */
    public void reset() {
        if (allPorts != null) {
            for (var port : allPorts) {
                var element = graphView.getModelElement(port);
                if (element != null) {
                    element.setActive(true);
                }
            }
            allPorts = null;
        }
        compatiblePorts = null;

        if (ghostWire != null) {
            graphView.removeElement(ghostWire);
        }

        if (wireCandidate != null) {
            graphView.removeElement(wireCandidate);
        }

        if (wireCandidateModel != null && ghostWireModel != null) {
            clearWillConnect(ghostWireModel, wireCandidateModel.getToPort() == null ? WireSide.TO : WireSide.FROM);
        }

        if (draggedPort != null) {
            clearWillConnect(draggedPort);
            draggedPort = null;
        }

        ghostWire = null;
        clearWireCandidate();
        enableAllWires(graphView, true, null);
        graphView.isWireDragging = false;
    }

    protected Pair<WireElement, GhostWireModel> createGhostWire(GraphModel graphModel) {
        var ghostWire = new GhostWireModel();
        ghostWire.setGraphModel(graphModel);
        var ui = ghostWire.createElementUI();
        return Pair.of(ui, ghostWire);
    }

    protected void clearWillConnect(@Nullable WireModel wireModel) {
        clearWillConnect(wireModel, WireSide.FROM);
        clearWillConnect(wireModel, WireSide.TO);

    }

    protected void clearWillConnect(@Nullable WireModel wireModel, WireSide side) {
        clearWillConnect(wireModel == null ? null : wireModel.getPort(side));
    }

    protected void clearWillConnect(@Nullable PortModel portModel) {
        if (portModel != null && graphView.getModelElement(portModel) instanceof PortElement portElement) {
            portElement.setWillConnect(false);
        }
    }

    protected void clearWireCandidate() {
        wireCandidate = null;
        wireCandidateModel = null;
    }

    public static void enableAllWires(GraphView graphView, boolean enable, @Nullable Set<WireModel> exemptedWires) {
        if (graphView.getGraph() == null) return;
        var graphModel = graphView.getGraph().graphModel;
        for (WireModel wireModel : graphModel.getWireModels()) {
            if (exemptedWires == null || wireModel != null && !exemptedWires.contains(wireModel)) {
                var element = graphView.getModelElement(wireModel);
                if (element != null) {
                    element.setActive(enable);
                }
            }
        }
    }

    public void createWireCandidate() {
        if (graphView.getGraph() == null) return;
        var candidate = createGhostWire(graphView.getGraph().graphModel);
        wireCandidate = candidate.first();
        wireCandidateModel = candidate.second();
    }

    public boolean handleMouseDown(UIEvent event, @Nullable Predicate<PortModel> compatiblePortsFilter) {
        var mousePosition = new Vector2f(event.x, event.y);
        if (draggedPort == null || wireCandidateModel == null || wireCandidate == null
                || draggedPort.getPortType() == PortType.MISSING_PORT || draggedPort.getDataTypeHandle().equals(TypeHandles.MISSING_PORT)) {
            return false;
        }

        if (wireCandidate.getParent() == null) {
            graphView.addElement(wireCandidate);
        }

        var startFromOutput = draggedPort.getDirection() == PortDirection.OUTPUT;

        wireCandidate.setActive(false);
        wireCandidateModel.setFromWorldPoint(mousePosition);
        wireCandidateModel.setToWorldPoint(mousePosition);

        if (startFromOutput) {
            wireCandidateModel.setFromPort(draggedPort);
            wireCandidateModel.setToPort(null);
        } else {
            wireCandidateModel.setFromPort(null);
            wireCandidateModel.setToPort(draggedPort);
        }

        getCompatiblePorts(compatiblePortsFilter);

        highlightCompatiblePorts();

        graphView.isWireDragging = true;

        wireCandidate.doCompleteUpdate();

        return true;
    }

    public void handleMouseMove(UIEvent event) {
        var mousePosition = new Vector2f(event.x, event.y);

        if (draggedPort.getDirection() == PortDirection.OUTPUT) {
            wireCandidateModel.setToWorldPoint(mousePosition);
        } else {
            wireCandidateModel.setFromWorldPoint(mousePosition);
        }
        wireCandidate.doCompleteUpdate();

        // Draw ghost wire if possible port exists.
        var endPort = getEndPort(mousePosition);

        if (previousEndPortModel != null && previousEndPortModel != (endPort == null ? null : endPort.getModel())) {
            clearWillConnect(previousEndPortModel);
        }

        if (endPort != null) {
            if (ghostWire == null) {
                var gw = createGhostWire(endPort.getModel().getGraphModel());
                ghostWire = gw.first();
                ghostWireModel = gw.second();
//                ghostWire.pickingMode = PickingMode.Ignore;
                graphView.addElement(ghostWire);
            }

            var sideForEndPort = wireCandidateModel.getFromPort() == null ? WireSide.FROM : WireSide.TO;
            var previousEndPort = ghostWireModel == null ? null : ghostWireModel.getPort(sideForEndPort);
            if (previousEndPort != null && previousEndPort.getUid() != endPort.getModel().getUid())
                clearWillConnect(previousEndPort);

            if (ghostWireModel != null) {
                ghostWireModel.setPort(sideForEndPort, endPort.getModel());
            }
            endPort.setWillConnect(true);

            // When the port will connect, show the node hover border.
//            ToggleNodeHoverBorder(endPort, true);

            var otherSide = sideForEndPort.getOpposite();
            if (ghostWireModel != null) {
                ghostWireModel.setPort(otherSide, wireCandidateModel.getPort(otherSide));
            }

            ghostWire.doCompleteUpdate();
            previousEndPortModel = endPort.getModel();
        } else if (ghostWire != null && ghostWireModel != null) {
            clearWillConnect(ghostWireModel, wireCandidateModel.getToPort() == null ? WireSide.TO : WireSide.FROM);

            graphView.removeElement(ghostWire);
            ghostWireModel.setToPort(null);
            ghostWireModel.setFromPort(null);
            ghostWireModel = null;
            ghostWire = null;
        }
    }

    public void handleMouseUp(UIEvent event, boolean isFirstWire, List<WireElement> otherWires, List<PortModel> otherPorts) {
        var mousePosition = new Vector2f(event.x, event.y);

        stopHighlightingCompatiblePorts();

        // Clean up ghost wires.
        if (ghostWireModel != null) {
            clearWillConnect(ghostWireModel);

            graphView.removeElement(ghostWire);
            ghostWireModel.setToPort(null);
            ghostWireModel.setFromPort(null);
            ghostWireModel = null;
            ghostWire = null;
        }

        clearWillConnect(wireCandidateModel);

        var removeWireCandidate =
                (wireCandidateModel == null ? null : wireCandidateModel.getToPort()) == null ||
                wireCandidateModel.getFromPort() == null;

        var endPort = getEndPort(mousePosition);
        if (endPort != null) {
            wireCandidate.setActive(true);

            if (wireCandidateModel != null) {
                if (wireCandidateModel.getFromPort() == null)
                    wireCandidateModel.setFromPort(endPort.getModel());
                else
                    wireCandidateModel.setToPort(endPort.getModel());
            }
        }

        // Let the first wire handle the batch command for all wires
        if (isFirstWire) {
            var affectedWires = new ArrayList<WireElement>();
            if (originalWire != null) {
                affectedWires.add(originalWire);
            }
            affectedWires.addAll(otherWires);

            if (endPort != null) {
                if (originalWire == null)
                    createNewWire(wireCandidate.getModel().getFromPort(), wireCandidate.getModel().getToPort());
                else
                    moveWires(affectedWires, endPort);
            } else {
                removeWireCandidate = false;

                if (originalWire == null) {
                    dropWiresOutside(List.of(wireCandidate), List.of(draggedPort), mousePosition);
                } else {
                    affectedWires.add(wireCandidate);
                    var ports = new ArrayList<PortModel>();
                    ports.add(draggedPort);
                    ports.addAll(otherPorts);
                    dropWiresOutside(affectedWires, ports, mousePosition);
                }
            }
        }

        if (removeWireCandidate) {
            // If it is an existing valid wire then delete and notify the model (using deleteElements()).
            graphView.removeElement(wireCandidate);
        }

        clearWireCandidate();
        compatiblePorts = null;
        allPorts = null;
        reset();

        originalWire = null;
    }

    @Nullable
    private PortElement getEndPort(Vector2f mousePosition) {
        PortElement endPort = null;
        for (PortModel compatiblePort : compatiblePorts) {
            if (graphView.getModelElement(compatiblePort) instanceof PortElement portUI && portUI.isVisible()) {
                NodeElement nodeUI = null;
                if (portUI.getModel().getNodeModel() != null && graphView.getModelElement(portUI.getModel().getNodeModel()) instanceof NodeElement nodeElement) {
                    nodeUI = nodeElement;
                }
                if (nodeUI != null && nodeUI.isCulled()) continue;
                if (portUI.getConnector().isSelfOrChildHover()) {
                    endPort = portUI;
                    break;
                }
            }
        }
        return endPort;
    }

    protected void getCompatiblePorts(@Nullable Predicate<PortModel> compatiblePortsFilter) {
        if (graphView.getGraph() == null) return;
        var graphModel = graphView.getGraph().graphModel;
        allPorts = graphModel.getPortModels().toList();
        compatiblePorts = graphModel.getCompatiblePorts(allPorts, draggedPort);

        // Filter compatible ports
        if (compatiblePortsFilter != null) {
            compatiblePorts = compatiblePorts.stream().filter(compatiblePortsFilter).toList();
        }
    }

    protected void highlightCompatiblePorts() {
        // Only light compatible anchors when dragging a wire.
        for (PortModel allPort : allPorts) {
            if (graphView.getModelElement(allPort) instanceof PortElement portElement) {
                portElement.setActive(false);
            }
        }

        for (PortModel allPort : compatiblePorts) {
            if (graphView.getModelElement(allPort) instanceof PortElement portElement) {
                portElement.setActive(true);
            }
        }

        if (graphView.getModelElement(draggedPort) instanceof PortElement portElement) {
            portElement.setActive(true);
            portElement.setWillConnect(true);
        }
    }

    protected void stopHighlightingCompatiblePorts() {
        for (var port : allPorts) {
            if (graphView.getModelElement(port) instanceof PortElement portElement) {
                portElement.setActive(true);
            }
        }
    }

    protected void createNewWire(PortModel fromPort, PortModel toPort) {
        graphView.dispatchCommand(new WireCommands.CreateWireCommand(toPort, fromPort));
    }

    protected void moveWires(List<WireElement> wires, PortElement endPort) {
    }

    /**
     * Handler for when wires are dropped outside of any port.
     */
    protected void dropWiresOutside(List<WireElement> wires, List<PortModel> ports, Vector2f worldPosition) {
        var wiresToConnect = new ArrayList<Pair<WireModel, WireSide>>();
        for (int i = 0; i < wires.size(); i++) {
            var wire = wires.get(i);
            var port = ports.get(i);
            WireSide side = port.getDirection() == PortDirection.INPUT
                    ? WireSide.FROM
                    : WireSide.TO;
            wiresToConnect.add(Pair.of(wire.getModel(), side));
        }
        var wiresToDelete = wires.stream().map(GraphElement::getModel).filter(m -> m instanceof IGhostWireModel).toList();
        createNodesFromWires(graphView, wiresToConnect, worldPosition, wiresToDelete);
    }

    protected void createNodesFromWires(GraphView graphView,
                                        List<Pair<WireModel, WireSide>> wires,
                                        Vector2f worldPosition,
                                        List<WireModel> wiresToDelete) {
        var localPosition = graphView.getContentViewContainer().worldToLocalLayoutOffset(worldPosition);
        var portModels = wires.stream().map(w -> w.left().getOtherPort(w.right())).toList();
        if (!portModels.isEmpty()) {
            if (portModels.getFirst().getDirection() == PortDirection.NONE) return;
            graphView.itemLibrary.showWithNodesFitPort(worldPosition.x, worldPosition.y, portModels, item -> {
                if (item instanceof NodeModelLibraryItem nodeItem) {
                    graphView.dispatchCommand(new NodeCommands.CreateNodeCommand().withNodeOnWires(nodeItem, wires, localPosition, null));
                }

//                if (item is VariableLibraryItem variableItem){
//                    var blackboardSection = view.GraphModel.GetSectionModel(GraphModel.DefaultSectionName);
//                    var index = blackboardSection.Items.Count;
//
//                    view.Dispatch(CreateNodeCommand.OnWireSide(variableItem, wires, localPosition, blackboardSection, index));
//                }

                var allWiresToDelete = wires.stream().map(Pair::left).filter(IGhostWireModel.class::isInstance).collect(Collectors.toList());
                if (wiresToDelete != null) {
                    allWiresToDelete.addAll(wiresToDelete);
                }

                if (!allWiresToDelete.isEmpty()) {
                    for (WireModel wireModel : allWiresToDelete) {
                        if (graphView.getModelElement(wireModel) instanceof WireElement wireElement) {
                            graphView.removeElement(wireElement);
                        }
                    }
                }
            });
        }
    }
}
