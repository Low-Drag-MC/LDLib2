package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Sugiyama-style layered layout — what every serious dataflow editor converges on (Graphviz
 * {@code dot}, ELK Layered, dagre, Blueprint auto-arrange). Four passes, in the classic order:
 *
 * <ol>
 *     <li><b>Cycle removal.</b> DFS back edges are reversed so everything downstream can assume a
 *     DAG. They are reversed back for nothing — the reversal only ever affects layering, and the
 *     wires themselves are drawn from the real ports either way.</li>
 *     <li><b>Layer assignment.</b> Longest path from the sources, then a reverse pass that slides
 *     each node as late as its earliest consumer allows. That second pass is the difference between
 *     a constant sitting far left with a wire stretching across the canvas and the same constant
 *     sitting directly in front of the node that reads it.</li>
 *     <li><b>Crossing reduction.</b> Alternating down/up median sweeps plus adjacent transposition,
 *     keeping the best ordering seen — the standard heuristic, since the exact problem is NP-hard
 *     even for two layers.</li>
 *     <li><b>Coordinate assignment.</b> Each layer is placed by isotonic regression against the
 *     position its neighbours pull it towards. That is an exact least-squares solution to "put every
 *     node where its wires want it, in this order, without overlapping" — the same objective
 *     Brandes–Köpf approximates, at a fraction of the code.</li>
 * </ol>
 *
 * <p>Both layering and coordinate assignment are port-aware via {@link LayoutEdge}'s offsets, so a
 * wire that leaves near the bottom of a tall node lands straight rather than kinked.</p>
 */
public class LayeredLayout implements IGraphLayout {

    @Override
    public void layout(List<LayoutBox> boxes, List<LayoutEdge> edges, LayoutOptions options) {
        var n = boxes.size();
        if (n == 0) return;
        var direction = options.direction();

        var acyclic = removeCycles(n, edges);
        var layerOf = assignLayers(n, acyclic);

        var layerCount = 0;
        for (var v = 0; v < n; v++) layerCount = Math.max(layerCount, layerOf[v] + 1);
        var layers = new ArrayList<List<Integer>>(layerCount);
        for (var i = 0; i < layerCount; i++) layers.add(new ArrayList<>());

        // Seed each layer's order from where the boxes already are, so tidying an almost-tidy graph
        // keeps nodes near the neighbours the user put them next to.
        var seeded = new ArrayList<Integer>(n);
        for (var v = 0; v < n; v++) seeded.add(v);
        final var dir = direction;
        seeded.sort(Comparator.comparingDouble(v -> boxes.get(v).cross(dir)));
        for (var v : seeded) layers.get(layerOf[v]).add(v);

        reduceCrossings(layers, acyclic, n, layerOf, Math.max(1, options.iterations()));
        assignFlow(boxes, layers, direction, options.layerSpacing());
        assignCross(boxes, layers, acyclic, n, direction, options.nodeSpacing(), Math.max(2, options.iterations()));
    }

    // region cycle removal

    /**
     * Returns the edge list with DFS back edges flipped, self-loops and out-of-range endpoints
     * dropped. Greedy and order-dependent — it does not minimise the number of reversals, which is
     * NP-hard, but on a dataflow graph there is usually nothing to reverse at all.
     */
    static List<LayoutEdge> removeCycles(int n, List<LayoutEdge> edges) {
        var valid = new ArrayList<LayoutEdge>(edges.size());
        for (var edge : edges) {
            if (edge.from() == edge.to()) continue;
            if (edge.from() < 0 || edge.from() >= n || edge.to() < 0 || edge.to() >= n) continue;
            valid.add(edge);
        }
        var outgoing = new ArrayList<List<Integer>>(n);
        for (var i = 0; i < n; i++) outgoing.add(new ArrayList<>());
        for (var i = 0; i < valid.size(); i++) outgoing.get(valid.get(i).from()).add(i);

        // 0 = unvisited, 1 = on the current DFS path, 2 = finished. An edge into a node still on
        // the path closes a cycle.
        var state = new byte[n];
        var reversed = new boolean[valid.size()];
        var stack = new ArrayDeque<int[]>();
        for (var root = 0; root < n; root++) {
            if (state[root] != 0) continue;
            state[root] = 1;
            stack.push(new int[]{root, 0});
            while (!stack.isEmpty()) {
                var frame = stack.peek();
                var list = outgoing.get(frame[0]);
                if (frame[1] >= list.size()) {
                    state[frame[0]] = 2;
                    stack.pop();
                    continue;
                }
                var edgeIndex = list.get(frame[1]++);
                var target = valid.get(edgeIndex).to();
                if (state[target] == 1) {
                    reversed[edgeIndex] = true;
                } else if (state[target] == 0) {
                    state[target] = 1;
                    stack.push(new int[]{target, 0});
                }
            }
        }

        var out = new ArrayList<LayoutEdge>(valid.size());
        for (var i = 0; i < valid.size(); i++) {
            out.add(reversed[i] ? valid.get(i).reversed() : valid.get(i));
        }
        return out;
    }

    // endregion

    // region layering

    /** Longest path from the sources, then "as late as possible" without passing any consumer. */
    static int[] assignLayers(int n, List<LayoutEdge> acyclic) {
        var successors = neighbours(n, acyclic, true);
        var predecessors = neighbours(n, acyclic, false);

        var remaining = new int[n];
        for (var edge : acyclic) remaining[edge.to()]++;
        var queue = new ArrayDeque<Integer>();
        for (var v = 0; v < n; v++) if (remaining[v] == 0) queue.add(v);
        var order = new ArrayList<Integer>(n);
        var visited = new boolean[n];
        while (!queue.isEmpty()) {
            var v = queue.poll();
            order.add(v);
            visited[v] = true;
            for (var w : successors.get(v)) {
                if (--remaining[w] == 0) queue.add(w);
            }
        }
        // removeCycles leaves a DAG, but a pair of anti-parallel edges survives it as a two-cycle.
        // Append whatever the topological sort could not reach so no node silently disappears.
        for (var v = 0; v < n; v++) if (!visited[v]) order.add(v);

        var layer = new int[n];
        for (var v : order) {
            for (var u : predecessors.get(v)) layer[v] = Math.max(layer[v], layer[u] + 1);
        }
        for (var i = order.size() - 1; i >= 0; i--) {
            var v = order.get(i);
            var out = successors.get(v);
            if (out.isEmpty()) continue;
            var latest = Integer.MAX_VALUE;
            for (var w : out) latest = Math.min(latest, layer[w] - 1);
            if (latest > layer[v]) layer[v] = latest;
        }
        return layer;
    }

    // endregion

    // region crossing reduction

    private static void reduceCrossings(List<List<Integer>> layers, List<LayoutEdge> edges, int n,
                                        int[] layerOf, int iterations) {
        if (layers.size() < 2) return;
        var successors = neighbours(n, edges, true);
        var predecessors = neighbours(n, edges, false);
        var position = new int[n];
        updatePositions(layers, position);

        var best = snapshot(layers);
        var bestCrossings = countCrossings(edges, layerOf, position);
        for (var iteration = 0; iteration < iterations; iteration++) {
            for (var i = 1; i < layers.size(); i++) sortByMedian(layers.get(i), predecessors, position);
            updatePositions(layers, position);
            for (var i = layers.size() - 2; i >= 0; i--) sortByMedian(layers.get(i), successors, position);
            updatePositions(layers, position);
            transpose(layers, successors, predecessors, position);

            var crossings = countCrossings(edges, layerOf, position);
            if (crossings < bestCrossings) {
                bestCrossings = crossings;
                best = snapshot(layers);
            }
            if (bestCrossings == 0) break;
        }
        for (var i = 0; i < layers.size(); i++) {
            layers.get(i).clear();
            layers.get(i).addAll(best.get(i));
        }
    }

    /**
     * Reorders one layer by the median position of each node's neighbours in the adjacent layer.
     * Nodes with no neighbours there keep their current index as their key, which is the standard
     * trick for stopping them from drifting to one end.
     */
    private static void sortByMedian(List<Integer> layer, List<List<Integer>> neighbours, int[] position) {
        if (layer.size() < 2) return;
        var keys = new HashMap<Integer, Double>(layer.size() * 2);
        for (var i = 0; i < layer.size(); i++) {
            var v = layer.get(i);
            var adjacent = neighbours.get(v);
            if (adjacent.isEmpty()) {
                keys.put(v, (double) i);
                continue;
            }
            var positions = adjacent.stream().mapToInt(w -> position[w]).sorted().toArray();
            var middle = positions.length / 2;
            keys.put(v, positions.length % 2 == 1
                    ? positions[middle]
                    : (positions[middle - 1] + positions[middle]) / 2.0);
        }
        layer.sort(Comparator.comparingDouble(keys::get));
    }

    /** Swaps adjacent pairs while doing so strictly reduces the crossings they are involved in. */
    private static void transpose(List<List<Integer>> layers, List<List<Integer>> successors,
                                  List<List<Integer>> predecessors, int[] position) {
        var improved = true;
        var guard = 0;
        while (improved && guard++ < 8) {
            improved = false;
            for (var layer : layers) {
                for (var i = 0; i + 1 < layer.size(); i++) {
                    var v = layer.get(i);
                    var w = layer.get(i + 1);
                    var before = pairCrossings(v, w, successors, position) + pairCrossings(v, w, predecessors, position);
                    var after = pairCrossings(w, v, successors, position) + pairCrossings(w, v, predecessors, position);
                    if (after < before) {
                        layer.set(i, w);
                        layer.set(i + 1, v);
                        improved = true;
                    }
                }
            }
            updatePositions(layers, position);
        }
    }

    /** Crossings between the edges of {@code left} and {@code right} when placed in that order. */
    private static int pairCrossings(int left, int right, List<List<Integer>> neighbours, int[] position) {
        var crossings = 0;
        for (var a : neighbours.get(left)) {
            for (var b : neighbours.get(right)) {
                if (position[a] > position[b]) crossings++;
            }
        }
        return crossings;
    }

    /** Total crossings over all adjacent layer pairs, counted naively over edge pairs. */
    private static int countCrossings(List<LayoutEdge> edges, int[] layerOf, int[] position) {
        var byLayer = new HashMap<Integer, List<LayoutEdge>>();
        for (var edge : edges) {
            if (layerOf[edge.to()] - layerOf[edge.from()] != 1) continue;
            byLayer.computeIfAbsent(layerOf[edge.from()], k -> new ArrayList<>()).add(edge);
        }
        var crossings = 0;
        for (var bucket : byLayer.values()) {
            for (var i = 0; i < bucket.size(); i++) {
                for (var j = i + 1; j < bucket.size(); j++) {
                    var a = bucket.get(i);
                    var b = bucket.get(j);
                    var top = position[a.from()] - position[b.from()];
                    var bottom = position[a.to()] - position[b.to()];
                    if ((long) top * bottom < 0) crossings++;
                }
            }
        }
        return crossings;
    }

    private static void updatePositions(List<List<Integer>> layers, int[] position) {
        for (var layer : layers) {
            for (var i = 0; i < layer.size(); i++) position[layer.get(i)] = i;
        }
    }

    private static List<List<Integer>> snapshot(List<List<Integer>> layers) {
        var copy = new ArrayList<List<Integer>>(layers.size());
        for (var layer : layers) copy.add(new ArrayList<>(layer));
        return copy;
    }

    // endregion

    // region coordinates

    /** One column (or row) per layer, sized by its widest member, left/top aligned within it. */
    private static void assignFlow(List<LayoutBox> boxes, List<List<Integer>> layers,
                                   LayoutDirection direction, float spacing) {
        var cursor = 0f;
        for (var layer : layers) {
            if (layer.isEmpty()) continue;
            var extent = 0f;
            for (var v : layer) extent = Math.max(extent, boxes.get(v).flowSize(direction));
            for (var v : layer) boxes.get(v).setFlow(direction, cursor);
            cursor += extent + spacing;
        }
    }

    private static void assignCross(List<LayoutBox> boxes, List<List<Integer>> layers, List<LayoutEdge> edges,
                                    int n, LayoutDirection direction, float spacing, int iterations) {
        for (var layer : layers) {
            var cursor = 0f;
            for (var v : layer) {
                boxes.get(v).setCross(direction, cursor);
                cursor += boxes.get(v).crossSize(direction) + spacing;
            }
        }
        var incoming = incidentEdges(n, edges, false);
        var outgoing = incidentEdges(n, edges, true);
        for (var iteration = 0; iteration < iterations; iteration++) {
            if (iteration % 2 == 0) {
                for (var i = 1; i < layers.size(); i++) {
                    relaxLayer(boxes, layers.get(i), incoming, true, direction, spacing);
                }
            } else {
                for (var i = layers.size() - 2; i >= 0; i--) {
                    relaxLayer(boxes, layers.get(i), outgoing, false, direction, spacing);
                }
            }
        }
    }

    /**
     * Places one layer at the cross positions its neighbours ask for, subject to the layer's fixed
     * order and minimum separation.
     *
     * @param fromSource {@code true} when the incident edges arrive from the previous layer (so the
     *                   neighbour is the edge's source), {@code false} when they leave for the next
     */
    private static void relaxLayer(List<LayoutBox> boxes, List<Integer> layer,
                                   List<List<LayoutEdge>> incident, boolean fromSource,
                                   LayoutDirection direction, float spacing) {
        var count = layer.size();
        if (count == 0) return;
        var desired = new float[count];
        for (var i = 0; i < count; i++) {
            var box = boxes.get(layer.get(i));
            var list = incident.get(layer.get(i));
            if (list.isEmpty()) {
                desired[i] = box.cross(direction);
                continue;
            }
            var sum = 0.0;
            for (var edge : list) {
                var otherBox = boxes.get(fromSource ? edge.from() : edge.to());
                var otherOffset = fromSource
                        ? edge.resolveFromOffset(otherBox, direction)
                        : edge.resolveToOffset(otherBox, direction);
                var ownOffset = fromSource
                        ? edge.resolveToOffset(box, direction)
                        : edge.resolveFromOffset(box, direction);
                sum += otherBox.cross(direction) + otherOffset - ownOffset;
            }
            desired[i] = (float) (sum / list.size());
        }

        var gaps = new float[Math.max(0, count - 1)];
        for (var i = 0; i + 1 < count; i++) {
            gaps[i] = boxes.get(layer.get(i)).crossSize(direction) + spacing;
        }
        var placed = isotonic(desired, gaps);
        for (var i = 0; i < count; i++) boxes.get(layer.get(i)).setCross(direction, placed[i]);
    }

    /**
     * Least-squares fit of {@code desired} subject to {@code result[i] + gaps[i] <= result[i + 1]},
     * by pool-adjacent-violators on the gap-compensated sequence.
     *
     * <p>Exact and linear time. Nodes that fit where their wires want them end up exactly there;
     * nodes that would collide are merged into a block and share the block's average, which is the
     * minimum total movement that resolves the collision.</p>
     */
    static float[] isotonic(float[] desired, float[] gaps) {
        var count = desired.length;
        if (count == 0) return new float[0];
        var offset = new float[count];
        for (var i = 1; i < count; i++) offset[i] = offset[i - 1] + gaps[i - 1];

        var value = new double[count];
        var weight = new int[count];
        var blocks = 0;
        for (var i = 0; i < count; i++) {
            value[blocks] = desired[i] - offset[i];
            weight[blocks] = 1;
            blocks++;
            while (blocks > 1 && value[blocks - 2] > value[blocks - 1]) {
                var total = weight[blocks - 2] + weight[blocks - 1];
                value[blocks - 2] = (value[blocks - 2] * weight[blocks - 2] + value[blocks - 1] * weight[blocks - 1]) / total;
                weight[blocks - 2] = total;
                blocks--;
            }
        }

        var out = new float[count];
        var index = 0;
        for (var block = 0; block < blocks; block++) {
            for (var j = 0; j < weight[block]; j++) {
                out[index] = (float) (value[block] + offset[index]);
                index++;
            }
        }
        return out;
    }

    // endregion

    // region helpers

    private static List<List<Integer>> neighbours(int n, List<LayoutEdge> edges, boolean forward) {
        var lists = new ArrayList<List<Integer>>(n);
        for (var i = 0; i < n; i++) lists.add(new ArrayList<>());
        for (var edge : edges) {
            if (forward) lists.get(edge.from()).add(edge.to());
            else lists.get(edge.to()).add(edge.from());
        }
        return lists;
    }

    private static List<List<LayoutEdge>> incidentEdges(int n, List<LayoutEdge> edges, boolean outgoing) {
        var lists = new ArrayList<List<LayoutEdge>>(n);
        for (var i = 0; i < n; i++) lists.add(new ArrayList<>());
        for (var edge : edges) lists.get(outgoing ? edge.from() : edge.to()).add(edge);
        return lists;
    }

    // endregion
}
