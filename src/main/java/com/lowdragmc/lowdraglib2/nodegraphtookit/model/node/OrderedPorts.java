package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Java equivalent of C# OrderedPorts.
 *
 * Supports:
 *  - lookup by uniqueName (map)
 *  - ordered (and re-orderable) list view via 'order' indirection
 */
public final class OrderedPorts implements Iterable<PortModel> {
    private final HashMap<String, PortModel> dictionary;
    /**
     * orderViewIndex -> portModelsIndex
     * size == portModels.size()
     */
    private final ArrayList<Integer> order;
    private final ArrayList<PortModel> portModels;

    public OrderedPorts() {
        this(0);
    }

    public OrderedPorts(int capacity) {
        // HashMap initial capacity: Java uses different load factor behavior;
        // we do a best-effort sizing.
        this.dictionary = new HashMap<>(Math.max(0, capacity * 2));
        this.order = new ArrayList<>(Math.max(0, capacity));
        this.portModels = new ArrayList<>(Math.max(0, capacity));
    }

    public Map<String, PortModel> getDictionary() {
        return dictionary;
    }

    /** Number of ports. */
    public int size() {
        return dictionary.size();
    }

    public boolean isEmpty() {
        return dictionary.isEmpty();
    }

    /** Adds a port at the end. */
    public void add(PortModel portModel) {
        dictionary.put(portModel.getUniqueName(), portModel);
        portModels.add(portModel);
        order.add(order.size()); // new element maps to its own index

        markDirtyIfPossible(portModel);
    }

    /**
     * Insert a range at a given index in the LIST VIEW.
     */
    public void insertRange(int index, List<PortModel> portsToInsert) {
        Objects.requireNonNull(portsToInsert, "portsToInsert");
        if (index < 0 || index > order.size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + order.size());
        }
        if (portsToInsert.isEmpty()) return;

        // In C#: they increase any order entries >= index by count
        // (because underlying portModels list will shift to the right)
        int count = portsToInsert.size();
        for (int i = 0; i < order.size(); i++) {
            int v = order.get(i);
            if (v >= index) {
                order.set(i, v + count);
            }
        }

        // Ensure capacity
        portModels.ensureCapacity(portModels.size() + count);
        order.ensureCapacity(order.size() + count);

        // Insert into dictionary + underlying storage + order mapping
        for (int i = 0; i < count; i++) {
            PortModel pm = Objects.requireNonNull(portsToInsert.get(i), "portsToInsert[" + i + "]");
            String key = pm.getUniqueName();
            dictionary.put(key, pm);

            portModels.add(index + i, pm);
            order.add(index + i, index + i);

            markDirtyIfPossible(pm);
        }
    }

    /** Remove by model. */
    public boolean remove(PortModel portModel) {
        if (portModel == null) return false;
        return remove(portModel.getUniqueName());
    }

    /** Change a port name while keeping the same order. */
    public boolean changePortName(PortModel model, String oldPortName) {
        Objects.requireNonNull(model, "model");

        if (oldPortName == null) {
            // C# version scans dictionary to find the key with same value reference
            // Java reference equality: == is the equivalent.
            for (Map.Entry<String, PortModel> e : dictionary.entrySet()) {
                if (e.getValue() == model) {
                    oldPortName = e.getKey();
                    break;
                }
            }
        }
        if (oldPortName == null) return false;

        PortModel removed = dictionary.remove(oldPortName);
        if (removed != null) {
            String newKey = model.getUniqueName();
            if (dictionary.containsKey(newKey)) {
                // restore to avoid losing mapping
                dictionary.put(oldPortName, removed);
                throw new IllegalArgumentException("Duplicate port uniqueName: " + newKey);
            }
            dictionary.put(newKey, model);
            return true;
        }
        return false;
    }

    /** Remove by uniqueName. */
    public boolean remove(String uniqueName) {
        if (uniqueName == null) return false;

        PortModel portModel = dictionary.remove(uniqueName);
        if (portModel == null) return false;

        int index = portModels.indexOf(portModel);
        if (index < 0) {
            // Invariant broken: map contains item not in list.
            // Still report removed from map, but try to keep consistent by rebuilding.
            rebuildOrderFromPortModels();
            return true;
        }

        portModels.remove(index);

        // In Java: order.remove(Integer.valueOf(index))
        order.remove(Integer.valueOf(index));

        // Then decrement any order values > index
        for (int i = 0; i < order.size(); i++) {
            int v = order.get(i);
            if (v > index) {
                order.set(i, v - 1);
            }
        }

        markDirtyIfPossible(portModel);
        return true;
    }

    /** Swap order positions in the ORDER VIEW (does not move underlying storage). */
    public void swapPortsOrder(PortModel a, PortModel b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        int indexA = portModels.indexOf(a);
        int indexB = portModels.indexOf(b);
        if (indexA < 0 || indexB < 0) {
            throw new IllegalArgumentException("Both ports must exist in container.");
        }

        // find where those underlying indexes appear in the order list?
        // NOTE: In your C# code, they do:
        //   int indexA = m_PortModels.IndexOf(a);
        //   int indexB = m_PortModels.IndexOf(b);
        //   int oldAOrder = m_Order[indexA];
        //   m_Order[indexA] = m_Order[indexB];
        //   m_Order[indexB] = oldAOrder;
        //
        // That means: they treat "indexA/indexB" as positions in order list too,
        // implying m_Order is aligned with m_PortModels indices (same length) and
        // they swap at those positions.
        //
        // We'll keep exact behavior:
        int oldA = order.get(indexA);
        order.set(indexA, order.get(indexB));
        order.set(indexB, oldA);
    }

    /** Lookup by key (throws if absent, similar to C# indexer). */
    public PortModel get(String key) {
        return dictionary.get(key);
    }

    /** Try-get by key. */
    public Optional<PortModel> tryGet(String key) {
        return Optional.ofNullable(dictionary.get(key));
    }

    public boolean containsKey(String key) {
        return dictionary.containsKey(key);
    }

    /** Keys view. */
    public Set<String> keys() {
        return dictionary.keySet();
    }

    /**
     * Values view (UNDERLYING storage order, like your C# Values => m_PortModels).
     * Note this is NOT the reordered view.
     */
    public List<PortModel> values() {
        return portModels;
    }

    /** Get by index in ORDER VIEW (equivalent to C# this[int index] => m_PortModels[m_Order[index]]). */
    public PortModel get(int index) {
        checkInvariant();
        int storageIndex = order.get(index);
        return portModels.get(storageIndex);
    }

    /** Iterate in ORDER VIEW (equivalent to IEnumerable<PortModel>.GetEnumerator()) */
    @Override
    public @NotNull Iterator<PortModel> iterator() {
        checkInvariant();
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < order.size();
            }

            @Override
            public PortModel next() {
                if (!hasNext()) throw new NoSuchElementException();
                int storageIndex = order.get(i++);
                return portModels.get(storageIndex);
            }
        };
    }

    /** If you want map-style enumeration too. */
    public Set<Map.Entry<String, PortModel>> entrySet() {
        return dictionary.entrySet();
    }

    // --------- helpers ----------
    private void checkInvariant() {
        if (order.size() != portModels.size()) {
            throw new IllegalStateException("order and portModels should always be the same size");
        }
    }

    private void rebuildOrderFromPortModels() {
        order.clear();
        order.ensureCapacity(portModels.size());
        for (int i = 0; i < portModels.size(); i++) {
            order.add(i);
        }
    }

    /**
     * Placeholder for:
     * var graphModel = portModel.NodeModel?.GraphModel;
     * graphModel?.PortWireIndex.MarkDirty();
     *
     * You need to adapt it to your Java model graph API.
     */
    private void markDirtyIfPossible(PortModel portModel) {
        var node = portModel.getNodeModel();
        if (node == null) return;
        var graph = node.getGraphModel();
        if (graph == null) return;
        graph.getPortWireIndex().markDirty();
    }
}
