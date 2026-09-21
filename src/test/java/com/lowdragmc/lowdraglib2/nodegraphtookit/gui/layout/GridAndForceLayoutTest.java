package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridAndForceLayoutTest {

    private static final float EPSILON = 1e-3f;

    private static List<LayoutBox> boxes(int count, float width, float height) {
        var out = new ArrayList<LayoutBox>(count);
        for (var i = 0; i < count; i++) out.add(new LayoutBox(i, width, height, 0, 0));
        return out;
    }

    private static void assertNoOverlap(List<LayoutBox> boxes) {
        for (var i = 0; i < boxes.size(); i++) {
            for (var j = i + 1; j < boxes.size(); j++) {
                var a = boxes.get(i);
                var b = boxes.get(j);
                var separated = a.x + a.width <= b.x + EPSILON
                        || b.x + b.width <= a.x + EPSILON
                        || a.y + a.height <= b.y + EPSILON
                        || b.y + b.height <= a.y + EPSILON;
                assertTrue(separated, a + " overlaps " + b);
            }
        }
    }

    // region grid

    @Test
    void gridPacksIntoASquareIshBlockWithoutOverlap() {
        var boxes = boxes(9, 100, 40);
        new GridLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
        var columns = boxes.stream().map(b -> b.x).distinct().count();
        var rows = boxes.stream().map(b -> b.y).distinct().count();
        assertEquals(3, columns);
        assertEquals(3, rows);
    }

    @Test
    void gridKeepsReadingOrder() {
        var boxes = new ArrayList<>(List.of(
                new LayoutBox(0, 100, 40, 500, 0),
                new LayoutBox(1, 100, 40, 0, 0),
                new LayoutBox(2, 100, 40, 250, 0)));
        new GridLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        // Two columns for three boxes: the leftmost two share the first row, left to right.
        assertTrue(boxes.get(1).x < boxes.get(2).x, "the box that was leftmost should stay leftmost");
    }

    @Test
    void gridTreatsANearlyAlignedRowAsOneRow() {
        // A few pixels of drift must not read as a staircase of separate rows.
        var boxes = new ArrayList<>(List.of(
                new LayoutBox(0, 100, 40, 0, 0),
                new LayoutBox(1, 100, 40, 200, 3),
                new LayoutBox(2, 100, 40, 400, -2),
                new LayoutBox(3, 100, 40, 600, 1)));
        new GridLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
        assertEquals(boxes.get(0).y, boxes.get(1).y, EPSILON);
        assertEquals(boxes.get(2).y, boxes.get(3).y, EPSILON);
    }

    @Test
    void gridHandlesOneBoxAndNone() {
        var single = boxes(1, 100, 40);
        new GridLayout().layout(single, List.of(), LayoutOptions.defaults());
        assertEquals(0f, single.getFirst().x, EPSILON);
        assertEquals(0f, single.getFirst().y, EPSILON);

        var none = new ArrayList<LayoutBox>();
        new GridLayout().layout(none, List.of(), LayoutOptions.defaults());
        assertTrue(none.isEmpty());
    }

    @Test
    void gridVariesTrackSizesWithItsContents() {
        var boxes = new ArrayList<>(List.of(
                new LayoutBox(0, 50, 40, 0, 0),
                new LayoutBox(1, 300, 40, 100, 0),
                new LayoutBox(2, 50, 40, 0, 200),
                new LayoutBox(3, 50, 40, 100, 200)));
        new GridLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
        // The wide box sets its column's width, so the second column starts past it.
        assertTrue(boxes.get(1).x >= boxes.get(0).x + 50);
        assertEquals(boxes.get(1).x, boxes.get(3).x, EPSILON);
    }

    // endregion

    // region force directed

    @Test
    void forceDirectedSeparatesEverythingItPlaces() {
        var boxes = boxes(14, 120, 60);
        var edges = new ArrayList<LayoutEdge>();
        for (var i = 0; i + 1 < boxes.size(); i++) edges.add(new LayoutEdge(i, i + 1));
        edges.add(new LayoutEdge(13, 0));
        new ForceDirectedLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    @Test
    void forceDirectedSeparatesEvenWithNoEdgesAtAll() {
        var boxes = boxes(10, 150, 80);
        new ForceDirectedLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    @Test
    void forceDirectedIsReproducibleForTheSameSeed() {
        var edges = List.of(new LayoutEdge(0, 1), new LayoutEdge(1, 2), new LayoutEdge(2, 0),
                new LayoutEdge(2, 3), new LayoutEdge(3, 4));
        var first = boxes(5, 100, 50);
        var second = boxes(5, 100, 50);
        new ForceDirectedLayout().layout(first, edges, LayoutOptions.defaults());
        new ForceDirectedLayout().layout(second, edges, LayoutOptions.defaults());
        for (var i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).x, second.get(i).x, EPSILON);
            assertEquals(first.get(i).y, second.get(i).y, EPSILON);
        }
    }

    @Test
    void forceDirectedPullsConnectedNodesCloserThanUnconnectedOnes() {
        // 0-1 wired together, 2 and 3 loose: the wired pair should end up the closest pair.
        var boxes = boxes(6, 80, 40);
        var edges = List.of(new LayoutEdge(0, 1));
        new ForceDirectedLayout().layout(boxes, edges, LayoutOptions.defaults());
        var wired = distance(boxes.get(0), boxes.get(1));
        for (var i = 0; i < boxes.size(); i++) {
            for (var j = i + 1; j < boxes.size(); j++) {
                if (i == 0 && j == 1) continue;
                assertTrue(distance(boxes.get(i), boxes.get(j)) >= wired - EPSILON,
                        "wired nodes " + wired + " should not be further apart than " + i + "/" + j);
            }
        }
    }

    @Test
    void forceDirectedHandlesOneBoxAndNone() {
        var single = boxes(1, 100, 40);
        new ForceDirectedLayout().layout(single, List.of(), LayoutOptions.defaults());
        assertEquals(0f, single.getFirst().x, EPSILON);

        var none = new ArrayList<LayoutBox>();
        new ForceDirectedLayout().layout(none, List.of(), LayoutOptions.defaults());
        assertTrue(none.isEmpty());
    }

    @Test
    void forceDirectedSeparatesBoxesStackedExactlyOnTopOfEachOther() {
        var boxes = new ArrayList<>(List.of(
                new LayoutBox(0, 100, 40, 0, 0),
                new LayoutBox(1, 100, 40, 0, 0),
                new LayoutBox(2, 100, 40, 0, 0)));
        new ForceDirectedLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    // endregion

    // region disconnected components

    /** Area of the box that would have to be framed to see everything. */
    private static float boundingArea(List<LayoutBox> boxes) {
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
        return (maxX - minX) * (maxY - minY);
    }

    private static List<LayoutEdge> twoChains() {
        return List.of(new LayoutEdge(0, 1), new LayoutEdge(1, 2), new LayoutEdge(2, 3),
                new LayoutEdge(4, 5), new LayoutEdge(5, 6), new LayoutEdge(6, 7));
    }

    @Test
    void componentsAreFoundOverEdgesReadAsUndirected() {
        var components = ComponentwiseLayout.components(8, twoChains());
        for (var v = 1; v < 4; v++) assertEquals(components[0], components[v]);
        for (var v = 5; v < 8; v++) assertEquals(components[4], components[v]);
        assertTrue(components[0] != components[4], "the two chains are not connected");
    }

    @Test
    void anIsolatedBoxIsItsOwnComponent() {
        var components = ComponentwiseLayout.components(3, List.of(new LayoutEdge(0, 1)));
        assertEquals(components[0], components[1]);
        assertTrue(components[2] != components[0]);
    }

    @Test
    void outOfRangeEdgesDoNotMergeComponents() {
        var components = ComponentwiseLayout.components(2, List.of(new LayoutEdge(0, 99), new LayoutEdge(-1, 1)));
        assertTrue(components[0] != components[1]);
    }

    /**
     * The bug this wrapper exists for: two unconnected clusters have nothing pulling them together,
     * so a bare spring embedder pushes them apart until its cooling schedule runs out. Measured
     * before the fix, two four-node chains came out in a bounding box nine times the area of the
     * same eight nodes joined into one chain — a graph mostly made of empty space.
     */
    @Test
    void disconnectedComponentsAreNotFlungApart() {
        var separate = boxes(8, 100, 50);
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(separate, twoChains(), LayoutOptions.defaults());
        assertNoOverlap(separate);

        var joined = boxes(8, 100, 50);
        var joinedEdges = new ArrayList<>(twoChains());
        joinedEdges.add(new LayoutEdge(3, 4));
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(joined, joinedEdges, LayoutOptions.defaults());

        // Splitting one chain into two cannot reasonably need more room than the chain did.
        assertTrue(boundingArea(separate) < boundingArea(joined) * 2f,
                "two components took %.0f vs %.0f for one".formatted(boundingArea(separate), boundingArea(joined)));
    }

    @Test
    void manyIsolatedBoxesPackInsteadOfScattering() {
        var boxes = boxes(12, 100, 50);
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
        // Twelve 100x50 boxes are 60000 units of content. Packed with gutters this should land
        // within a small multiple of that; scattered, it was two orders of magnitude out.
        assertTrue(boundingArea(boxes) < 60000f * 6f,
                "bounding area was %.0f".formatted(boundingArea(boxes)));
    }

    @Test
    void componentwiseDelegatesUntouchedWhenEverythingIsConnected() {
        var wrapped = boxes(5, 100, 50);
        var bare = boxes(5, 100, 50);
        var edges = List.of(new LayoutEdge(0, 1), new LayoutEdge(1, 2), new LayoutEdge(2, 3), new LayoutEdge(3, 4));
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(wrapped, edges, LayoutOptions.defaults());
        new ForceDirectedLayout().layout(bare, edges, LayoutOptions.defaults());
        for (var i = 0; i < wrapped.size(); i++) {
            assertEquals(bare.get(i).x, wrapped.get(i).x, EPSILON);
            assertEquals(bare.get(i).y, wrapped.get(i).y, EPSILON);
        }
    }

    @Test
    void componentwiseKeepsEachComponentsOwnShape() {
        // Lay a single chain out on its own, then the same chain beside an unrelated one: the
        // chain's internal geometry must be identical, only translated.
        var alone = boxes(4, 100, 50);
        var chain = List.of(new LayoutEdge(0, 1), new LayoutEdge(1, 2), new LayoutEdge(2, 3));
        new ForceDirectedLayout().layout(alone, chain, LayoutOptions.defaults());

        var together = boxes(8, 100, 50);
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(together, twoChains(), LayoutOptions.defaults());

        var dx = together.get(0).x - alone.get(0).x;
        var dy = together.get(0).y - alone.get(0).y;
        for (var i = 0; i < 4; i++) {
            assertEquals(alone.get(i).x + dx, together.get(i).x, 0.01f);
            assertEquals(alone.get(i).y + dy, together.get(i).y, 0.01f);
        }
    }

    @Test
    void componentwiseHandlesDegenerateInput() {
        var none = new ArrayList<LayoutBox>();
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(none, List.of(), LayoutOptions.defaults());
        assertTrue(none.isEmpty());

        var single = boxes(1, 100, 50);
        new ComponentwiseLayout(new ForceDirectedLayout()).layout(single, List.of(), LayoutOptions.defaults());
        assertEquals(1, single.size());
    }

    private static float distance(LayoutBox a, LayoutBox b) {
        var dx = (a.x + a.width / 2f) - (b.x + b.width / 2f);
        var dy = (a.y + a.height / 2f) - (b.y + b.height / 2f);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // endregion
}
