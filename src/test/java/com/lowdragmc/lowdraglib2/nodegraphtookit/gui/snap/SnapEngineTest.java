package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapEngineTest {

    private static final float EPSILON = 1e-3f;

    private static Vector4f rect(float x, float y, float width, float height) {
        return new Vector4f(x, y, width, height);
    }

    private static SnapSettings align(float threshold) {
        return new SnapSettings(false, 0, true, threshold, 1000f);
    }

    private static SnapSettings both(float gridSize, float threshold) {
        return new SnapSettings(true, gridSize, true, threshold, 1000f);
    }

    // region grid

    @Test
    void gridPullsWhicheverEdgeIsClosestToALine() {
        // Left edge is 4 off its line, the right edge only 1 off. The old "round the top-left
        // corner" rule would shove this 4 units sideways; the near edge should win instead.
        var result = SnapEngine.snap(rect(20, 0, 27, 10), List.of(), SnapSettings.gridOnly(16));
        assertEquals(1f, result.offsetX(), EPSILON);
        assertTrue(result.guides().isEmpty(), "the grid does not get a guide drawn through it");
    }

    @Test
    void gridCanAlsoPullByTheCentre() {
        // Left 10 (6 off), right 42 (6 off the other way), centre 26 (6 off) — all tied, so the
        // documented tie-break applies and the leading edge wins.
        var result = SnapEngine.snap(rect(10, 0, 32, 10), List.of(), SnapSettings.gridOnly(16));
        assertEquals(6f, result.offsetX(), EPSILON);
    }

    @Test
    void gridSnapsBothAxesIndependently() {
        var result = SnapEngine.snap(rect(20, 35, 27, 27), List.of(), SnapSettings.gridOnly(16));
        assertEquals(1f, result.offsetX(), EPSILON, "x: right edge 47 -> 48 beats left 20 -> 16");
        // y anchors are 35, 48.5 and 62; the centre is the closest to a line, by a long way.
        assertEquals(-0.5f, result.offsetY(), EPSILON);
    }

    @Test
    void gridOffDoesNothing() {
        var result = SnapEngine.snap(rect(13, 7, 20, 20), List.of(), SnapSettings.NONE);
        assertSame(SnapResult.NONE, result);
        assertTrue(result.isIdentity());
    }

    // endregion

    // region element alignment

    @Test
    void alignsLeftEdgeToLeftEdge() {
        var moving = rect(100, 0, 50, 30);
        var target = rect(103, 200, 50, 30);
        var result = SnapEngine.snap(moving, List.of(target), align(8));
        assertEquals(3f, result.offsetX(), EPSILON);
        assertEquals(0f, result.offsetY(), EPSILON);
        assertEquals(1, result.guides().size());
        var guide = result.guides().getFirst();
        assertTrue(guide.vertical());
        assertEquals(103f, guide.position(), EPSILON);
        assertEquals(SnapAnchor.START, guide.anchor());
    }

    @Test
    void alignsARightEdgeToANeighboursLeftEdge() {
        // Butting one node up against the next is alignment too, and it is the case a rule that
        // only ever looked at top-left corners could never express.
        var moving = rect(100, 0, 50, 30);
        var target = rect(152, 100, 40, 30);
        var result = SnapEngine.snap(moving, List.of(target), align(8));
        assertEquals(2f, result.offsetX(), EPSILON);
        assertEquals(SnapAnchor.END, result.guides().getFirst().anchor());
    }

    @Test
    void alignsCentresWhenThatIsTheNearestFit() {
        var moving = rect(100, 0, 50, 30);
        var target = rect(110, 100, 34, 30); // centre 127 vs the moving centre 125
        var result = SnapEngine.snap(moving, List.of(target), align(8));
        assertEquals(2f, result.offsetX(), EPSILON);
        assertEquals(SnapAnchor.CENTER, result.guides().getFirst().anchor());
        assertEquals(127f, result.guides().getFirst().position(), EPSILON);
    }

    @Test
    void alignsTopEdgesTheSameWay() {
        var moving = rect(0, 100, 50, 30);
        var target = rect(400, 96, 50, 30);
        var result = SnapEngine.snap(moving, List.of(target), align(8));
        assertEquals(0f, result.offsetX(), EPSILON);
        assertEquals(-4f, result.offsetY(), EPSILON);
        assertFalse(result.guides().getFirst().vertical());
    }

    @Test
    void theTwoAxesCanLineUpWithDifferentNeighbours() {
        var moving = rect(100, 100, 50, 30);
        var column = rect(97, 600, 50, 30);
        var row = rect(700, 103, 50, 30);
        var result = SnapEngine.snap(moving, List.of(column, row), align(8));
        assertEquals(-3f, result.offsetX(), EPSILON);
        assertEquals(3f, result.offsetY(), EPSILON);
        assertEquals(2, result.guides().size(), "one guide per axis");
    }

    @Test
    void nothingWithinReachLeavesTheAxisAlone() {
        var result = SnapEngine.snap(rect(100, 0, 50, 30), List.of(rect(140, 200, 50, 30)), align(8));
        assertTrue(result.isIdentity(), "40 away is not a snap");
    }

    @Test
    void elementsFarAcrossTheGuideAreNotNearby() {
        // Perfectly aligned in x, but a screenful away down the canvas: not something the user is
        // lining up with, and drawing a guide to it would be noise.
        var settings = new SnapSettings(false, 0, true, 8f, 400f);
        var result = SnapEngine.snap(rect(100, 0, 50, 30), List.of(rect(103, 5000, 50, 30)), settings);
        assertTrue(result.isIdentity());
    }

    @Test
    void theNearestEdgeWinsWhenSeveralAreInReach() {
        var moving = rect(100, 0, 50, 30);
        var far = rect(106, 100, 50, 30);
        var near = rect(101, 200, 50, 30);
        var result = SnapEngine.snap(moving, List.of(far, near), align(8));
        assertEquals(1f, result.offsetX(), EPSILON);
    }

    // endregion

    // region priority

    @Test
    void elementAlignmentBeatsTheGridEvenWhenTheGridIsCloser() {
        // Lining up with a neighbour is deliberate; landing on a grid line is a tidiness default.
        // Letting the grid win on distance would break the feature exactly when it is being used.
        var moving = rect(15, 0, 50, 30);       // grid 16 is one unit away
        var target = rect(18, 200, 50, 30);     // the neighbour is three
        var result = SnapEngine.snap(moving, List.of(target), both(16, 8));
        assertEquals(3f, result.offsetX(), EPSILON);
        assertEquals(1, result.guides().size());
    }

    @Test
    void theGridStillCatchesTheAxisNothingLinedUpOn() {
        var moving = rect(15, 20, 50, 30);
        var target = rect(18, 600, 50, 30);     // only helps on x
        var result = SnapEngine.snap(moving, List.of(target), both(16, 8));
        assertEquals(3f, result.offsetX(), EPSILON);
        // y anchors are 20, 35 and 50; the bottom edge is nearest its line.
        assertEquals(-2f, result.offsetY(), EPSILON, "y falls back to the grid");
        assertEquals(1, result.guides().size(), "the grid snap gets no guide");
    }

    @Test
    void alignmentTurnedOffFallsStraightThroughToTheGrid() {
        var settings = new SnapSettings(true, 16, false, 8, 1000);
        var result = SnapEngine.snap(rect(15, 0, 50, 30), List.of(rect(18, 200, 50, 30)), settings);
        assertEquals(1f, result.offsetX(), EPSILON);
        assertTrue(result.guides().isEmpty());
    }

    // endregion

    // region guides

    @Test
    void aGuideSpansEveryNeighbourSittingOnTheSameLine() {
        // Three nodes already left-aligned, and a fourth dropped into the column: the guide should
        // run through all of them, which is what says "these four line up" rather than "it moved".
        var moving = rect(100, 300, 50, 30);
        var targets = List.of(rect(103, 0, 50, 30), rect(103, 500, 50, 30), rect(103, 900, 50, 30));
        var result = SnapEngine.snap(moving, targets, align(8));
        var guide = result.guides().getFirst();
        assertEquals(0f, guide.start(), EPSILON);
        assertEquals(930f, guide.end(), EPSILON);
    }

    @Test
    void aGuideAlwaysCoversTheDraggedRectangleItself() {
        var moving = rect(100, 2000, 50, 30);
        var result = SnapEngine.snap(moving, List.of(rect(103, 1900, 50, 30)), align(8));
        var guide = result.guides().getFirst();
        assertEquals(1900f, guide.start(), EPSILON);
        assertEquals(2030f, guide.end(), EPSILON);
    }

    @Test
    void aGuideIgnoresNeighboursOnTheLineButOutOfRange() {
        var settings = new SnapSettings(false, 0, true, 8f, 400f);
        var moving = rect(100, 0, 50, 30);
        var targets = List.of(rect(103, 100, 50, 30), rect(103, 9000, 50, 30));
        var result = SnapEngine.snap(moving, targets, settings);
        assertEquals(130f, result.guides().getFirst().end(), EPSILON, "the far one is not drawn through");
    }

    @Test
    void guidesAreMeasuredAtTheSnappedPositionNotTheCursorPosition() {
        // The y snap moves the rectangle before the x guide's span is measured; taking the span from
        // where the cursor was would leave the guide a few units short at one end.
        var moving = rect(100, 104, 50, 30);
        var targets = List.of(rect(103, 500, 50, 30), rect(600, 100, 50, 30));
        var result = SnapEngine.snap(moving, targets, align(8));
        assertEquals(-4f, result.offsetY(), EPSILON);
        var vertical = result.guides().stream().filter(SnapGuide::vertical).findFirst().orElseThrow();
        assertEquals(100f, vertical.start(), EPSILON);
    }

    // endregion

    @Test
    void anchorsReadTheRectangleTheWayTheyClaimTo() {
        var box = rect(10, 20, 100, 50);
        assertEquals(10f, SnapAnchor.START.of(box, true), EPSILON);
        assertEquals(60f, SnapAnchor.CENTER.of(box, true), EPSILON);
        assertEquals(110f, SnapAnchor.END.of(box, true), EPSILON);
        assertEquals(20f, SnapAnchor.START.of(box, false), EPSILON);
        assertEquals(45f, SnapAnchor.CENTER.of(box, false), EPSILON);
        assertEquals(70f, SnapAnchor.END.of(box, false), EPSILON);
    }

    @Test
    void aZeroSizedDragStillSnaps() {
        var result = SnapEngine.snap(rect(15, 15, 0, 0), List.of(), SnapSettings.gridOnly(16));
        assertEquals(1f, result.offsetX(), EPSILON);
        assertEquals(1f, result.offsetY(), EPSILON);
    }
}
