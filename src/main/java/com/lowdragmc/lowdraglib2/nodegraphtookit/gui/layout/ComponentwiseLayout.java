package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Runs another layout on each connected component separately, then packs the results side by side.
 *
 * <p>Wrapped around {@link ForceDirectedLayout} because a spring embedder has nothing holding two
 * unconnected clusters together: repulsion is between every pair of nodes, attraction only along
 * edges, so two components push each other apart until the cooling schedule runs out and stop
 * wherever that happened to be. The picture is not wrong so much as mostly empty — measured on two
 * four-node chains, the bounding box came out nine times the area of the same nodes joined into
 * one chain.</p>
 *
 * <p>The usual alternative is a gravity term pulling everything towards the centre, which is one
 * line but buys compactness by squashing large components. Laying each component out on its own and
 * then packing the boxes leaves every component exactly as the delegate drew it, which is what
 * Graphviz {@code neato -Gpack} and ELK's component packing do too.</p>
 */
public class ComponentwiseLayout implements IGraphLayout {

    /** Width-to-height ratio the packing aims for. Wider than square: canvases and screens are. */
    private static final float TARGET_ASPECT = 1.6f;

    private final IGraphLayout delegate;

    public ComponentwiseLayout(IGraphLayout delegate) {
        this.delegate = delegate;
    }

    @Override
    public void layout(List<LayoutBox> boxes, List<LayoutEdge> edges, LayoutOptions options) {
        var n = boxes.size();
        if (n == 0) return;
        var componentOf = components(n, edges);
        var count = 0;
        for (var component : componentOf) count = Math.max(count, component + 1);
        if (count <= 1) {
            delegate.layout(boxes, edges, options);
            return;
        }

        var members = new ArrayList<List<Integer>>(count);
        for (var i = 0; i < count; i++) members.add(new ArrayList<>());
        for (var v = 0; v < n; v++) members.get(componentOf[v]).add(v);

        var groups = new ArrayList<List<LayoutBox>>(count);
        for (var member : members) {
            // Edges address boxes by position in the list, so a component needs its own indices.
            var local = new HashMap<Integer, Integer>(member.size() * 2);
            var subBoxes = new ArrayList<LayoutBox>(member.size());
            for (var v : member) {
                local.put(v, subBoxes.size());
                subBoxes.add(boxes.get(v));
            }
            var subEdges = new ArrayList<LayoutEdge>();
            for (var edge : edges) {
                var from = local.get(edge.from());
                var to = local.get(edge.to());
                // Null covers both "belongs to another component" and "out of range"; an edge can
                // never span two components, because spanning one is what makes them one component.
                if (from == null || to == null) continue;
                subEdges.add(new LayoutEdge(from, to, edge.fromOffset(), edge.toOffset()));
            }
            // The boxes are shared, not copied, so the delegate writes straight through.
            delegate.layout(subBoxes, subEdges, options);
            groups.add(subBoxes);
        }

        pack(groups, options.nodeSpacing() + options.layerSpacing() / 2f);
    }

    /** Component index per box, over the edges read as undirected. Out-of-range edges are ignored. */
    static int[] components(int n, List<LayoutEdge> edges) {
        var parent = new int[n];
        for (var v = 0; v < n; v++) parent[v] = v;
        for (var edge : edges) {
            if (edge.from() < 0 || edge.from() >= n || edge.to() < 0 || edge.to() >= n) continue;
            union(parent, edge.from(), edge.to());
        }
        var label = new int[n];
        var labels = new HashMap<Integer, Integer>();
        for (var v = 0; v < n; v++) {
            label[v] = labels.computeIfAbsent(find(parent, v), key -> labels.size());
        }
        return label;
    }

    private static int find(int[] parent, int v) {
        while (parent[v] != v) {
            parent[v] = parent[parent[v]];
            v = parent[v];
        }
        return v;
    }

    private static void union(int[] parent, int a, int b) {
        var rootA = find(parent, a);
        var rootB = find(parent, b);
        if (rootA != rootB) parent[rootB] = rootA;
    }

    /**
     * Shelf packing: components are laid into rows, tallest first, wrapping once a row reaches the
     * width that would make the whole block roughly {@link #TARGET_ASPECT}.
     *
     * <p>Not optimal — optimal rectangle packing is NP-hard and the gain over shelves is a few
     * percent of whitespace, which nobody looks at a node graph and notices.</p>
     */
    private static void pack(List<List<LayoutBox>> groups, float gutter) {
        var frames = new ArrayList<Frame>(groups.size());
        var totalArea = 0f;
        var widest = 0f;
        for (var group : groups) {
            var frame = Frame.of(group);
            if (frame == null) continue;
            frames.add(frame);
            totalArea += frame.width * frame.height;
            widest = Math.max(widest, frame.width);
        }
        if (frames.isEmpty()) return;
        frames.sort(Comparator.comparingDouble((Frame frame) -> frame.height).reversed());

        // A row never has to be narrower than the widest single component, or it could not hold it.
        var target = Math.max(widest, (float) Math.sqrt(totalArea * TARGET_ASPECT));
        var x = 0f;
        var y = 0f;
        var rowHeight = 0f;
        for (var frame : frames) {
            if (x > 0 && x + frame.width > target) {
                x = 0;
                y += rowHeight + gutter;
                rowHeight = 0;
            }
            frame.moveTo(x, y);
            x += frame.width + gutter;
            rowHeight = Math.max(rowHeight, frame.height);
        }
    }

    /** One component's boxes plus the bounding box they currently occupy. */
    private record Frame(List<LayoutBox> boxes, float minX, float minY, float width, float height) {

        static Frame of(List<LayoutBox> boxes) {
            if (boxes.isEmpty()) return null;
            var minX = Float.MAX_VALUE;
            var minY = Float.MAX_VALUE;
            var maxX = -Float.MAX_VALUE;
            var maxY = -Float.MAX_VALUE;
            for (var box : boxes) {
                minX = Math.min(minX, box.x);
                minY = Math.min(minY, box.y);
                maxX = Math.max(maxX, box.x + box.width);
                maxY = Math.max(maxY, box.y + box.height);
            }
            return new Frame(boxes, minX, minY, maxX - minX, maxY - minY);
        }

        void moveTo(float x, float y) {
            var dx = x - minX;
            var dy = y - minY;
            for (var box : boxes) {
                box.x += dx;
                box.y += dy;
            }
        }
    }
}
