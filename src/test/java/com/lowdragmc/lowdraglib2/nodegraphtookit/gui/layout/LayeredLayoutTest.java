package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayeredLayoutTest {

    private static final float EPSILON = 1e-3f;

    private static List<LayoutBox> boxes(int count, float width, float height) {
        var out = new ArrayList<LayoutBox>(count);
        for (var i = 0; i < count; i++) out.add(new LayoutBox(i, width, height, 0, 0));
        return out;
    }

    private static LayoutEdge edge(int from, int to) {
        return new LayoutEdge(from, to);
    }

    /** No two boxes may share a pixel — the whole point of the coordinate pass. */
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

    // region layering

    @Test
    void chainGetsOneLayerPerLink() {
        var layers = LayeredLayout.assignLayers(3, LayeredLayout.removeCycles(3,
                List.of(edge(0, 1), edge(1, 2))));
        assertEquals(0, layers[0]);
        assertEquals(1, layers[1]);
        assertEquals(2, layers[2]);
    }

    @Test
    void diamondPutsBothMiddleNodesInTheSameLayer() {
        var edges = List.of(edge(0, 1), edge(0, 2), edge(1, 3), edge(2, 3));
        var layers = LayeredLayout.assignLayers(4, LayeredLayout.removeCycles(4, edges));
        assertEquals(0, layers[0]);
        assertEquals(1, layers[1]);
        assertEquals(1, layers[2]);
        assertEquals(2, layers[3]);
    }

    @Test
    void aShortBranchIsPulledForwardToMeetItsConsumer() {
        // 0 -> 1 -> 2 -> 3, plus 4 -> 3. Longest-path-from-the-sources alone would leave 4 sitting
        // in layer 0 with a wire stretching across three layers; the "as late as possible" pass is
        // what puts it directly in front of the node that reads it.
        var edges = List.of(edge(0, 1), edge(1, 2), edge(2, 3), edge(4, 3));
        var layers = LayeredLayout.assignLayers(5, LayeredLayout.removeCycles(5, edges));
        assertEquals(3, layers[3]);
        assertEquals(2, layers[4], "the short branch should sit one layer before its consumer");
    }

    @Test
    void sinksKeepTheirOwnDepthRatherThanBeingFlushedRight() {
        // Two independent chains of different lengths: the short one must not be stretched to match.
        var edges = List.of(edge(0, 1), edge(1, 2), edge(3, 4));
        var layers = LayeredLayout.assignLayers(5, LayeredLayout.removeCycles(5, edges));
        assertEquals(2, layers[2]);
        assertEquals(1, layers[4]);
    }

    @Test
    void cyclesAreBrokenAndStillProduceALayering() {
        var edges = List.of(edge(0, 1), edge(1, 2), edge(2, 0));
        var acyclic = LayeredLayout.removeCycles(3, edges);
        assertEquals(3, acyclic.size(), "breaking a cycle reverses an edge, it does not drop one");
        var layers = LayeredLayout.assignLayers(3, acyclic);
        for (var layer : layers) assertTrue(layer >= 0);
        // The two edges that were not reversed still point forwards.
        var forwards = 0;
        for (var e : acyclic) if (layers[e.to()] > layers[e.from()]) forwards++;
        assertTrue(forwards >= 2, "only the reversed edge may end up flat or backwards");
    }

    @Test
    void selfWiresAndDanglingEndpointsAreIgnoredRatherThanThrowing() {
        var edges = List.of(edge(0, 0), edge(1, 99), edge(-1, 0), edge(0, 1));
        var acyclic = LayeredLayout.removeCycles(2, edges);
        assertEquals(1, acyclic.size());
        assertEquals(0, acyclic.getFirst().from());
        assertEquals(1, acyclic.getFirst().to());
    }

    // endregion

    // region coordinates

    @Test
    void isotonicPlacesEveryoneExactlyWhereAskedWhenNothingCollides() {
        var placed = LayeredLayout.isotonic(new float[]{0, 100, 250}, new float[]{50, 50});
        assertEquals(0f, placed[0], EPSILON);
        assertEquals(100f, placed[1], EPSILON);
        assertEquals(250f, placed[2], EPSILON);
    }

    @Test
    void isotonicResolvesACollisionBySharingTheAverage() {
        // Both want 0, and they need 50 between them: the least total movement is -25 / +25.
        var placed = LayeredLayout.isotonic(new float[]{0, 0}, new float[]{50});
        assertEquals(-25f, placed[0], EPSILON);
        assertEquals(25f, placed[1], EPSILON);
    }

    @Test
    void isotonicNeverViolatesTheMinimumGap() {
        var desired = new float[]{300, 10, 200, 0, 5};
        var gaps = new float[]{40, 40, 40, 40};
        var placed = LayeredLayout.isotonic(desired, gaps);
        for (var i = 0; i + 1 < placed.length; i++) {
            assertTrue(placed[i + 1] >= placed[i] + gaps[i] - EPSILON,
                    "gap " + i + " violated in " + java.util.Arrays.toString(placed));
        }
    }

    @Test
    void isotonicHandlesDegenerateInput() {
        assertEquals(0, LayeredLayout.isotonic(new float[0], new float[0]).length);
        assertEquals(7f, LayeredLayout.isotonic(new float[]{7}, new float[0])[0], EPSILON);
    }

    // endregion

    // region end to end

    @Test
    void chainIsLaidOutLeftToRightWithoutOverlap() {
        var boxes = boxes(4, 120, 50);
        var edges = List.of(edge(0, 1), edge(1, 2), edge(2, 3));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
        for (var i = 0; i + 1 < boxes.size(); i++) {
            assertTrue(boxes.get(i + 1).x >= boxes.get(i).x + boxes.get(i).width,
                    "layer " + (i + 1) + " should start after layer " + i + " ends");
        }
    }

    @Test
    void topBottomDirectionFlowsDownInstead() {
        var boxes = boxes(3, 120, 50);
        var edges = List.of(edge(0, 1), edge(1, 2));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults().withDirection(LayoutDirection.TOP_BOTTOM));
        assertNoOverlap(boxes);
        assertTrue(boxes.get(1).y >= boxes.get(0).y + boxes.get(0).height);
        assertTrue(boxes.get(2).y >= boxes.get(1).y + boxes.get(1).height);
    }

    @Test
    void aStraightChainComesOutActuallyStraight() {
        var boxes = boxes(5, 120, 50);
        var edges = List.of(edge(0, 1), edge(1, 2), edge(2, 3), edge(3, 4));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        for (var box : boxes) {
            assertEquals(boxes.getFirst().y, box.y, 0.5f, "nothing pulls a plain chain off its line");
        }
    }

    @Test
    void portOffsetsLineUpThePortsRatherThanTheBoxes() {
        // A short source whose output sits 20 down, feeding a tall target whose input sits 90 down.
        // Lining the ports up means the target has to ride 70 higher than the source.
        var boxes = List.of(new LayoutBox(0, 100, 40, 0, 0), new LayoutBox(1, 100, 200, 0, 0));
        var mutable = new ArrayList<>(boxes);
        var edges = List.of(new LayoutEdge(0, 1, 20f, 90f));
        new LayeredLayout().layout(mutable, edges, LayoutOptions.defaults());
        assertEquals(mutable.get(0).y + 20f, mutable.get(1).y + 90f, 0.5f);
    }

    @Test
    void aFanInIsCentredOnItsConsumer() {
        // Three sources into one sink: the sink should end up level with the middle source.
        var boxes = boxes(4, 100, 40);
        var edges = List.of(edge(0, 3), edge(1, 3), edge(2, 3));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
        var sources = List.of(boxes.get(0), boxes.get(1), boxes.get(2));
        var average = (float) sources.stream().mapToDouble(b -> b.y + b.height / 2f).average().orElseThrow();
        assertEquals(average, boxes.get(3).y + boxes.get(3).height / 2f, 1f);
    }

    @Test
    void disconnectedNodesAreStillPlacedAndStillDoNotOverlap() {
        var boxes = boxes(6, 90, 40);
        new LayeredLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    @Test
    void mixedSizesNeverCollide() {
        var boxes = new ArrayList<LayoutBox>();
        for (var i = 0; i < 12; i++) {
            boxes.add(new LayoutBox(i, 60 + (i % 4) * 70, 30 + (i % 5) * 60, i * 13, i * 7));
        }
        var edges = List.of(edge(0, 4), edge(1, 4), edge(2, 5), edge(3, 5), edge(4, 6), edge(5, 6),
                edge(6, 7), edge(7, 8), edge(8, 9), edge(2, 9), edge(10, 11), edge(11, 6));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    @Test
    void aCyclicGraphStillTerminatesAndStillDoesNotOverlap() {
        var boxes = boxes(6, 100, 40);
        var edges = List.of(edge(0, 1), edge(1, 2), edge(2, 3), edge(3, 1), edge(3, 4), edge(4, 5), edge(5, 0));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
    }

    @Test
    void layoutIsDeterministic() {
        var edges = List.of(edge(0, 2), edge(1, 2), edge(2, 3), edge(3, 4), edge(0, 4));
        var first = boxes(5, 110, 45);
        var second = boxes(5, 110, 45);
        new LayeredLayout().layout(first, edges, LayoutOptions.defaults());
        new LayeredLayout().layout(second, edges, LayoutOptions.defaults());
        for (var i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).x, second.get(i).x, EPSILON);
            assertEquals(first.get(i).y, second.get(i).y, EPSILON);
        }
    }

    @Test
    void crossingReductionUntanglesASwap() {
        // 0 -> 3 and 1 -> 2, seeded in the order that crosses. Ordering should swap one side back.
        var boxes = new ArrayList<>(List.of(
                new LayoutBox(0, 100, 40, 0, 0),
                new LayoutBox(1, 100, 40, 0, 100),
                new LayoutBox(2, 100, 40, 200, 0),
                new LayoutBox(3, 100, 40, 200, 100)));
        var edges = List.of(edge(0, 3), edge(1, 2));
        new LayeredLayout().layout(boxes, edges, LayoutOptions.defaults());
        assertNoOverlap(boxes);
        // Whichever way it resolves, the two wires must end up parallel rather than crossed.
        var crossed = (boxes.get(0).y < boxes.get(1).y) != (boxes.get(3).y < boxes.get(2).y);
        assertTrue(!crossed, "expected the crossing to be removed: " + boxes);
    }

    @Test
    void emptyInputIsANoOp() {
        var boxes = new ArrayList<LayoutBox>();
        new LayeredLayout().layout(boxes, List.of(), LayoutOptions.defaults());
        assertTrue(boxes.isEmpty());
    }

    // endregion
}
