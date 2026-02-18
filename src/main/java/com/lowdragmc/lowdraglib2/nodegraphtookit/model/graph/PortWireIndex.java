package com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.utils.ReorderType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.IPortWireIndexModel;

import java.util.*;

/**
 * Implements an index to quickly retrieve the list of wires that are connected to a port.
 * the index needs to be kept up-to-date. In addition to adding and removing wires to it,
 * it needs to be notified when any of the ports of a wire changes, so that the index can be updated.
 */
public class PortWireIndex<TWire extends IPortWireIndexModel> {
    private final List<TWire> wireModels; // equivalent to IReadOnlyList<TWire>
    private boolean isDirty;

    private final Map<PortKey, List<TWire>> wiresByPort;

    // Used to avoid allocating a 1-element list in wireReordered.
    // In Java, static generic fields are awkward; make it per-instance.
    private final List<TWire> oneWireList = new ArrayList<>(1);

    public PortWireIndex(List<TWire> wireModels) {
        this.wireModels = wireModels;
        this.wiresByPort = new HashMap<>();
        this.isDirty = true;
    }

    /**
     * Gets the list of wires connected to a port.
     */
    public List<TWire> getWiresForPort(PortModel portModel) {
        if (portModel == null || portModel.getNodeModel() == null) {
            return Collections.emptyList();
        }

        List<TWire> list = tryGetWiresForPort(portModel);
        return list != null ? list : Collections.emptyList();
    }

    /**
     * Returns the wire list if found; otherwise null.
     */
    private List<TWire> tryGetWiresForPort(PortModel portModel) {
        if (isDirty) {
            reindex();
        }

        PortKey key = new PortKey(
                portModel.getNodeModel().getUid(),
                portModel.getUniqueName(),
                portModel.getDirection()
        );

        return wiresByPort.get(key);
    }

    /**
     * Marks the index as needing a full rebuild.
     */
    public void markDirty() {
        isDirty = true;
    }

    /**
     * Updates the index when a wire is added.
     */
    public void wireAdded(TWire wireModel) {
        if (isDirty || wireModel == null) {
            // Do not bother if index is already dirty: it will be rebuilt soon.
            return;
        }

        PortModel from = wireModel.getFromPort();
        if (from != null) {
            PortKey key = new PortKey(from.getNodeModel().getUid(), from.getUniqueName(), from.getDirection());
            addKeyWire(key, wireModel);
        }

        PortModel to = wireModel.getToPort();
        if (to != null) {
            PortKey key = new PortKey(to.getNodeModel().getUid(), to.getUniqueName(), to.getDirection());
            addKeyWire(key, wireModel);
        }
    }

    private void addKeyWire(PortKey key, TWire wire) {
        List<TWire> list = wiresByPort.computeIfAbsent(key, k -> new ArrayList<>());
        if (!list.contains(wire)) {
            list.add(wire);
        }
    }

    /**
     * Updates a wire in the index when one of its ports changes.
     */
    public void wirePortsChanged(TWire wireModel, PortModel oldPort, PortModel newPort) {
        if (isDirty || oldPort == newPort) {
            // Do not bother if index is already dirty: it will be rebuilt soon.
            return;
        }

        if (oldPort != null) {
            PortKey key = new PortKey(oldPort.getNodeModel().getUid(), oldPort.getUniqueName(), oldPort.getDirection());
            List<TWire> list = wiresByPort.get(key);
            if (list != null) {
                list.remove(wireModel);
                if (list.isEmpty()) {
                    wiresByPort.remove(key);
                }
            }
        }

        if (newPort != null) {
            PortKey key = new PortKey(newPort.getNodeModel().getUid(), newPort.getUniqueName(), newPort.getDirection());
            List<TWire> list = wiresByPort.computeIfAbsent(key, k -> new ArrayList<>());
            if (!list.contains(wireModel)) {
                list.add(wireModel);
            }
        }
    }

    /**
     * Updates the index when the port unique name changes.
     */
    public void portUniqueNameChanged(PortModel portModel, String oldName, String newName) {
        if (isDirty || Objects.equals(oldName, newName) || oldName == null || newName == null) {
            return;
        }

        PortKey key = new PortKey(portModel.getNodeModel().getUid(), oldName, portModel.getDirection());
        List<TWire> list = wiresByPort.remove(key);
        if (list == null) {
            return;
        }

        PortKey newKey = new PortKey(portModel.getNodeModel().getUid(), newName, portModel.getDirection());
        wiresByPort.put(newKey, list);
    }

    /**
     * Updates the index when the port direction changes.
     */
    public void portDirectionChanged(PortModel portModel, PortDirection oldDirection, PortDirection newDirection) {
        if (isDirty || oldDirection == newDirection) {
            return;
        }

        PortKey key = new PortKey(portModel.getNodeModel().getUid(), portModel.getUniqueName(), oldDirection);
        List<TWire> list = wiresByPort.remove(key);
        if (list == null) {
            return;
        }

        PortKey newKey = new PortKey(portModel.getNodeModel().getUid(), portModel.getUniqueName(), newDirection);
        wiresByPort.put(newKey, list);
    }

    /**
     * Updates the index when a wire is removed.
     */
    public void wireRemoved(TWire wireModel) {
        if (isDirty || wireModel == null) {
            return;
        }

        PortModel from = wireModel.getFromPort();
        if (from != null) {
            PortKey key = new PortKey(from.getNodeModel().getUid(), from.getUniqueName(), from.getDirection());
            removeKeyWire(key, wireModel);
        }

        PortModel to = wireModel.getToPort();
        if (to != null) {
            PortKey key = new PortKey(to.getNodeModel().getUid(), to.getUniqueName(), to.getDirection());
            removeKeyWire(key, wireModel);
        }
    }

    private void removeKeyWire(PortKey key, TWire wire) {
        List<TWire> list = wiresByPort.get(key);
        if (list != null) {
            list.remove(wire);
            if (list.isEmpty()) {
                wiresByPort.remove(key);
            }
        }
    }

    private void reindex() {
        isDirty = false;

        // Clear all existing lists (keep keys, like the C# version does)
        for (List<TWire> list : wiresByPort.values()) {
            list.clear();
        }

        for (TWire wire : wireModels) {
            wireAdded(wire);
        }

        // Remove keys with empty lists
        wiresByPort.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
    }

    /**
     * Updates the index when a wire is reordered.
     */
    public void wireReordered(TWire wireModel, ReorderType reorderType) {
        PortModel fromPort = wireModel.getFromPort();
        List<TWire> list = (fromPort != null) ? tryGetWiresForPort(fromPort) : null;

        if (list != null) {
            oneWireList.clear();
            oneWireList.add(wireModel);
            reorderElements(list, new HashSet<>(oneWireList), reorderType);
        } else {
            throw new IndexOutOfBoundsException(
                    wireModel + " not part of the " + PortWireIndex.class.getSimpleName() + "."
            );
        }
    }

    private static <T> void reorderElements(List<T> list, HashSet<T> elements, ReorderType reorderType) {
        if (elements == null || elements.isEmpty() || list == null || list.size() <= 1) {
            return;
        }

        boolean increaseIndices = (reorderType == ReorderType.MOVE_DOWN || reorderType == ReorderType.MOVE_LAST);
        boolean moveAllTheWay = (reorderType == ReorderType.MOVE_LAST || reorderType == ReorderType.MOVE_FIRST);

        int nextEndIdx = increaseIndices ? (list.size() - 1) : 0;

        for (int j = 1; j < list.size(); j++) {
            int i = increaseIndices ? (list.size() - 1 - j) : j;

            if (!elements.contains(list.get(i))) {
                continue;
            }

            int moveToIdx = increaseIndices ? (i + 1) : (i - 1);

            if (moveAllTheWay) {
                while (elements.contains(list.get(nextEndIdx)) && nextEndIdx != i) {
                    nextEndIdx += increaseIndices ? -1 : 1;
                }

                if (nextEndIdx == i) {
                    continue;
                }

                moveToIdx = nextEndIdx;
            } else {
                if (elements.contains(list.get(moveToIdx))) {
                    continue;
                }
            }

            T element = list.get(i);
            list.remove(i);
            list.add(moveToIdx, element);
        }
    }

    /**
     * Key: (nodeGUID, portUniqueName, direction)
     *
     * @param nodeGuid Hash128 in C#, use your Java type here
     */
    private record PortKey(UUID nodeGuid, String portUniqueName, PortDirection direction) { }

    /**
     * TestAccess equivalent.
     */
    public static final class TestAccess<TTestWire extends IPortWireIndexModel> {
        private final PortWireIndex<TTestWire> portWireIndex;

        public TestAccess(PortWireIndex<TTestWire> portWireIndex) {
            this.portWireIndex = portWireIndex;
        }

        public boolean tryGetWiresForPort(UUID uid, String uniqueName,
                                          PortDirection direction,
                                          List<TTestWire> outWireList) {
            if (portWireIndex.isDirty) {
                portWireIndex.reindex();
            }

            List<TTestWire> list = portWireIndex.wiresByPort.get(new PortKey(uid, uniqueName, direction));
            if (list == null) {
                return false;
            }

            outWireList.clear();
            outWireList.addAll(list);
            return true;
        }
    }
}
