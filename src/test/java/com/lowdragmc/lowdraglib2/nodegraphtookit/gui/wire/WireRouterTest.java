package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire;

import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireRouterTest {

    private static final float EPSILON = 1e-3f;

    // region invariants

    /**
     * The one property the style is named after: every segment runs along an axis or at exactly 45°.
     * Eight legal directions, hence octilinear.
     */
    private static void assertOctilinear(List<Vector2f> points) {
        for (var i = 0; i + 1 < points.size(); i++) {
            var dx = Math.abs(points.get(i + 1).x - points.get(i).x);
            var dy = Math.abs(points.get(i + 1).y - points.get(i).y);
            var axisAligned = dx < EPSILON || dy < EPSILON;
            var diagonal = Math.abs(dx - dy) < EPSILON;
            assertTrue(axisAligned || diagonal,
                    "segment " + i + " of " + points + " runs at neither an axis nor 45°: d=(" + dx + ", " + dy + ")");
        }
    }

    private static void assertAxisAligned(List<Vector2f> points) {
        for (var i = 0; i + 1 < points.size(); i++) {
            var dx = Math.abs(points.get(i + 1).x - points.get(i).x);
            var dy = Math.abs(points.get(i + 1).y - points.get(i).y);
            assertTrue(dx < EPSILON || dy < EPSILON,
                    "segment " + i + " of " + points + " is not axis aligned");
        }
    }

    private static void assertEndpoints(List<Vector2f> points, Vector2f from, Vector2f to) {
        assertTrue(points.getFirst().distance(from) < EPSILON, "route starts at " + points.getFirst() + ", not " + from);
        assertTrue(points.getLast().distance(to) < EPSILON, "route ends at " + points.getLast() + ", not " + to);
    }

    private static float length(List<Vector2f> points) {
        var total = 0f;
        for (var i = 0; i + 1 < points.size(); i++) total += points.get(i).distance(points.get(i + 1));
        return total;
    }

    /** Counts segments that are neither horizontal nor vertical. */
    private static int diagonalCount(List<Vector2f> points) {
        var count = 0;
        for (var i = 0; i + 1 < points.size(); i++) {
            var dx = Math.abs(points.get(i + 1).x - points.get(i).x);
            var dy = Math.abs(points.get(i + 1).y - points.get(i).y);
            if (dx > EPSILON && dy > EPSILON) count++;
        }
        return count;
    }

    private static List<Vector2f> octilinear(float x0, float y0, float x1, float y1) {
        return WireRouter.route(new Vector2f(x0, y0), WireRouter.Axis.HORIZONTAL,
                new Vector2f(x1, y1), WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.OCTILINEAR, WireRouter.DEFAULT_MIN_JOG);
    }

    // endregion

    @Test
    void shallowForwardRunIsStraightThenOneDiagonalThenStraight() {
        // 300 across, 80 down: well under 45°, so the wire should hold its lane, cut across once,
        // and hold the new lane.
        var route = octilinear(0, 0, 300, 80);
        assertOctilinear(route);
        assertEndpoints(route, new Vector2f(0, 0), new Vector2f(300, 80));
        assertEquals(1, diagonalCount(route), "expected exactly one 45° crossover in " + route);

        // The diagonal covers the whole vertical offset, so the two straight runs split what is left.
        var run = (300 - 80) / 2f;
        assertEquals(4, route.size(), "expected run/diagonal/run in " + route);
        assertEquals(run, route.get(1).x, EPSILON);
        assertEquals(0f, route.get(1).y, EPSILON);
        assertEquals(run + 80, route.get(2).x, EPSILON);
        assertEquals(80f, route.get(2).y, EPSILON);
    }

    @Test
    void exactly45DegreesIsOneUnbrokenDiagonal() {
        var route = octilinear(0, 0, 200, 200);
        assertOctilinear(route);
        assertEndpoints(route, new Vector2f(0, 0), new Vector2f(200, 200));
        // Nothing to run straight along: the whole thing is the crossover.
        assertEquals(1, diagonalCount(route));
        assertEquals(length(route), new Vector2f(0, 0).distance(new Vector2f(200, 200)), 0.01f,
                "a 45° run should be the straight line, not a detour");
    }

    @Test
    void steepForwardRunIsDiagonalThenTrunkThenDiagonal() {
        // 100 across, 400 down: past 45°, so the wire gets as close as a diagonal allows and then
        // drops straight down.
        var route = octilinear(0, 0, 100, 400);
        assertOctilinear(route);
        assertEndpoints(route, new Vector2f(0, 0), new Vector2f(100, 400));
        assertEquals(2, diagonalCount(route), "expected diagonal/trunk/diagonal in " + route);

        var trunkX = 50f;
        assertEquals(4, route.size(), route.toString());
        assertEquals(trunkX, route.get(1).x, EPSILON);
        assertEquals(50f, route.get(1).y, EPSILON);   // dropped by the same 50 it moved across
        assertEquals(trunkX, route.get(2).x, EPSILON);
        assertEquals(350f, route.get(2).y, EPSILON);
    }

    @Test
    void levelRunIsASingleStraightLine() {
        var route = octilinear(0, 40, 260, 40);
        assertOctilinear(route);
        assertEquals(2, route.size(), "a level wire needs no corners: " + route);
        assertEquals(0, diagonalCount(route));
    }

    @Test
    void backwardRunDetoursForwardOutOfBothPortsBeforeTurningBack() {
        var from = new Vector2f(400, 100);
        var to = new Vector2f(0, 300);
        var route = WireRouter.route(from, WireRouter.Axis.HORIZONTAL, to, WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.OCTILINEAR, WireRouter.DEFAULT_MIN_JOG);
        assertOctilinear(route);
        assertEndpoints(route, from, to);

        var maxX = route.stream().map(p -> p.x).max(Float::compare).orElseThrow();
        var minX = route.stream().map(p -> p.x).min(Float::compare).orElseThrow();
        assertTrue(maxX > from.x, "route should clear the source port before turning back: " + route);
        assertTrue(minX < to.x, "route should come back into the target from in front of it: " + route);
    }

    @Test
    void backwardRunBetweenLevelPortsBowsAsideInsteadOfOverlappingItself() {
        var from = new Vector2f(300, 50);
        var to = new Vector2f(0, 50);
        var route = WireRouter.route(from, WireRouter.Axis.HORIZONTAL, to, WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.OCTILINEAR, WireRouter.DEFAULT_MIN_JOG);
        assertOctilinear(route);
        assertEndpoints(route, from, to);
        var maxY = route.stream().map(p -> p.y).max(Float::compare).orElseThrow();
        assertEquals(50f + WireRouter.BACKWARD_DETOUR, maxY, EPSILON,
                "a level backward wire has to leave the shared lane: " + route);
    }

    @Test
    void verticalPortsRouteOnTheOtherAxis() {
        var from = new Vector2f(0, 0);
        var to = new Vector2f(80, 300);
        var route = WireRouter.route(from, WireRouter.Axis.VERTICAL, to, WireRouter.Axis.VERTICAL,
                WireRouteStyle.OCTILINEAR, WireRouter.DEFAULT_MIN_JOG);
        assertOctilinear(route);
        assertEndpoints(route, from, to);
        assertEquals(from.x, route.get(1).x, EPSILON);
        assertTrue(route.get(1).y > from.y);
    }

    @Test
    void mixedPortOrientationsUseASingleElbow() {
        var from = new Vector2f(0, 0);
        var to = new Vector2f(200, 150);
        var route = WireRouter.route(from, WireRouter.Axis.HORIZONTAL, to, WireRouter.Axis.VERTICAL,
                WireRouteStyle.ORTHOGONAL, WireRouter.DEFAULT_MIN_JOG);
        assertAxisAligned(route);
        assertEndpoints(route, from, to);
        assertEquals(3, route.size(), route.toString());
        assertEquals(to.x, route.get(1).x, EPSILON);
        assertEquals(from.y, route.get(1).y, EPSILON);
    }

    @Test
    void orthogonalStyleNeverProducesADiagonal() {
        float[][] cases = {
                {0, 0, 300, 80}, {0, 0, 80, 300}, {400, 100, 0, 300}, {300, 50, 0, 50}, {0, 0, 200, 200},
        };
        for (var c : cases) {
            var route = WireRouter.route(new Vector2f(c[0], c[1]), WireRouter.Axis.HORIZONTAL,
                    new Vector2f(c[2], c[3]), WireRouter.Axis.HORIZONTAL,
                    WireRouteStyle.ORTHOGONAL, WireRouter.DEFAULT_MIN_JOG);
            assertAxisAligned(route);
            assertEndpoints(route, new Vector2f(c[0], c[1]), new Vector2f(c[2], c[3]));
        }
    }

    @Test
    void chamferingOnlyEverShortensThePath() {
        float[][] cases = {
                {0, 0, 300, 80}, {0, 0, 80, 300}, {400, 100, 0, 300}, {300, 50, 0, 50},
                {0, 0, 5, 400}, {0, 0, 400, 3}, {10, 10, 11, 11},
        };
        for (var c : cases) {
            var from = new Vector2f(c[0], c[1]);
            var to = new Vector2f(c[2], c[3]);
            var orthogonal = WireRouter.orthogonalPath(from, WireRouter.Axis.HORIZONTAL, to,
                    WireRouter.Axis.HORIZONTAL, WireRouter.DEFAULT_MIN_JOG);
            var chamfered = WireRouter.chamferCorners(orthogonal, 0f);
            assertOctilinear(chamfered);
            assertEndpoints(chamfered, from, to);
            assertTrue(length(chamfered) <= length(orthogonal) + EPSILON,
                    "cutting a corner cannot make the path longer: " + chamfered + " vs " + orthogonal);
        }
    }

    @Test
    void chamfersOfNeighbouringCornersNeverOverrunEachOther() {
        // A path whose middle segment is far shorter than its ends — the case where two greedy cuts
        // would each want more of it than there is, and the wire would visibly double back.
        var path = List.of(new Vector2f(0, 0), new Vector2f(500, 0), new Vector2f(500, 10), new Vector2f(1000, 10));
        var chamfered = WireRouter.chamferCorners(path, 0f);
        assertOctilinear(chamfered);
        for (var i = 0; i + 1 < chamfered.size(); i++) {
            assertTrue(chamfered.get(i + 1).x >= chamfered.get(i).x - EPSILON,
                    "x should never go backwards on a forward path: " + chamfered);
        }
    }

    @Test
    void cappedChamferLeavesTheCornersInPlace() {
        var path = List.of(new Vector2f(0, 0), new Vector2f(500, 0), new Vector2f(500, 500));
        var chamfered = WireRouter.chamferCorners(path, 10f);
        assertOctilinear(chamfered);
        // Only the 10-unit bevel is taken out; the rest of the right angle survives.
        assertEquals(4, chamfered.size(), chamfered.toString());
        assertEquals(490f, chamfered.get(1).x, EPSILON);
        assertEquals(500f, chamfered.get(2).x, EPSILON);
        assertEquals(10f, chamfered.get(2).y, EPSILON);
    }

    @Test
    void simplifyDropsCoincidentPointsButKeepsCorners() {
        var points = List.of(new Vector2f(0, 0), new Vector2f(0, 0), new Vector2f(10, 0),
                new Vector2f(10, 0), new Vector2f(10, 10));
        var simplified = WireRouter.simplify(points);
        assertEquals(3, simplified.size(), simplified.toString());
        assertEndpoints(simplified, new Vector2f(0, 0), new Vector2f(10, 10));
    }

    @Test
    void simplifyDropsPointsSittingInTheMiddleOfAStraightRun() {
        var points = List.of(new Vector2f(0, 0), new Vector2f(5, 5), new Vector2f(10, 10),
                new Vector2f(20, 20), new Vector2f(20, 40));
        var simplified = WireRouter.simplify(points);
        assertEquals(3, simplified.size(), simplified.toString());
        assertEquals(new Vector2f(20, 20), simplified.get(1));
    }

    @Test
    void simplifyKeepsAFoldBackOnItself() {
        // Collinear, but a 180° turn: dropping the middle point would erase the fold entirely.
        var points = List.of(new Vector2f(0, 0), new Vector2f(50, 0), new Vector2f(20, 0));
        assertEquals(3, WireRouter.simplify(points).size());
    }

    // region curved

    private static List<Vector2f> curved(float x0, float y0, float x1, float y1) {
        return WireRouter.route(new Vector2f(x0, y0), WireRouter.Axis.HORIZONTAL,
                new Vector2f(x1, y1), WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.CURVED, WireRouter.DEFAULT_MIN_JOG);
    }

    /** The sharpest direction change between consecutive segments, in degrees. */
    private static float sharpestTurn(List<Vector2f> points) {
        var worst = 0f;
        for (var i = 0; i + 2 < points.size(); i++) {
            var inX = points.get(i + 1).x - points.get(i).x;
            var inY = points.get(i + 1).y - points.get(i).y;
            var outX = points.get(i + 2).x - points.get(i + 1).x;
            var outY = points.get(i + 2).y - points.get(i + 1).y;
            var inLength = (float) Math.sqrt(inX * inX + inY * inY);
            var outLength = (float) Math.sqrt(outX * outX + outY * outY);
            if (inLength < EPSILON || outLength < EPSILON) continue;
            var cos = (inX * outX + inY * outY) / (inLength * outLength);
            worst = Math.max(worst, (float) Math.toDegrees(Math.acos(Math.clamp(cos, -1f, 1f))));
        }
        return worst;
    }

    @Test
    void curvedRunIsSmoothRatherThanACornerPolyline() {
        var route = curved(0, 0, 300, 200);
        assertEndpoints(route, new Vector2f(0, 0), new Vector2f(300, 200));
        assertTrue(route.size() > 4, "a curve is sampled, not a four-point elbow: " + route.size());
        assertTrue(sharpestTurn(route) < 20f,
                "a sampled cubic should never kink: sharpest turn was " + sharpestTurn(route) + "°");
    }

    @Test
    void curvedLeavesAndArrivesAlongThePortAxis() {
        var route = curved(0, 0, 300, 200);
        var startDx = route.get(1).x - route.getFirst().x;
        var startDy = route.get(1).y - route.getFirst().y;
        assertTrue(Math.abs(startDy) < Math.abs(startDx) * 0.2f,
                "the wire should leave the port sideways, not diagonally: d=(%f, %f)".formatted(startDx, startDy));
        var endDx = route.getLast().x - route.get(route.size() - 2).x;
        var endDy = route.getLast().y - route.get(route.size() - 2).y;
        assertTrue(Math.abs(endDy) < Math.abs(endDx) * 0.2f,
                "the wire should arrive sideways too: d=(%f, %f)".formatted(endDx, endDy));
    }

    @Test
    void curvedLevelRunIsJustAStraightLine() {
        // Both control points land on the same line as the ports, so the cubic is degenerate and
        // every sample is collinear — which simplify then folds back down to one segment.
        var route = curved(0, 40, 300, 40);
        assertEquals(2, route.size(), route.toString());
    }

    @Test
    void curvedIsSymmetricAboutItsMidpoint() {
        var route = curved(0, 0, 300, 200);
        // Equal tangents at both ends put the midpoint of the two ports exactly on the curve.
        // Measured against the polyline, not against the nearest vertex: simplify folds away the
        // samples around the inflection, where the curve is at its straightest.
        var distance = distanceToPolyline(route, new Vector2f(150, 100));
        assertTrue(distance < 1f, "the curve should pass through the midpoint; it passed " + distance + " away");
    }

    private static float distanceToPolyline(List<Vector2f> points, Vector2f target) {
        var best = Float.MAX_VALUE;
        for (var i = 0; i + 1 < points.size(); i++) {
            var a = points.get(i);
            var b = points.get(i + 1);
            var dx = b.x - a.x;
            var dy = b.y - a.y;
            var lengthSquared = dx * dx + dy * dy;
            var t = lengthSquared < EPSILON ? 0f : ((target.x - a.x) * dx + (target.y - a.y) * dy) / lengthSquared;
            t = Math.clamp(t, 0f, 1f);
            best = Math.min(best, target.distance(new Vector2f(a.x + t * dx, a.y + t * dy)));
        }
        return best;
    }

    @Test
    void curvedBackwardRunLoopsOutOfBothPorts() {
        var from = new Vector2f(300, 0);
        var to = new Vector2f(0, 150);
        var route = WireRouter.route(from, WireRouter.Axis.HORIZONTAL, to, WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.CURVED, WireRouter.DEFAULT_MIN_JOG);
        assertEndpoints(route, from, to);
        var maxX = route.stream().map(p -> p.x).max(Float::compare).orElseThrow();
        var minX = route.stream().map(p -> p.x).min(Float::compare).orElseThrow();
        assertTrue(maxX > from.x, "the curve should bulge past the source port: " + maxX);
        assertTrue(minX < to.x, "and behind the target port: " + minX);
    }

    @Test
    void curvedBackwardBulgeGrowsSublinearly() {
        // A wire across the whole canvas must not loop the size of the whole canvas.
        var near = WireRouter.route(new Vector2f(200, 0), WireRouter.Axis.HORIZONTAL,
                new Vector2f(0, 50), WireRouter.Axis.HORIZONTAL, WireRouteStyle.CURVED, 0f);
        var far = WireRouter.route(new Vector2f(4000, 0), WireRouter.Axis.HORIZONTAL,
                new Vector2f(0, 50), WireRouter.Axis.HORIZONTAL, WireRouteStyle.CURVED, 0f);
        var nearBulge = near.stream().map(p -> p.x).max(Float::compare).orElseThrow() - 200f;
        var farBulge = far.stream().map(p -> p.x).max(Float::compare).orElseThrow() - 4000f;
        assertTrue(farBulge > nearBulge, "a longer reach should still bulge more");
        assertTrue(farBulge < nearBulge * 6f,
                "twenty times the distance must not mean twenty times the loop: %f vs %f"
                        .formatted(nearBulge, farBulge));
    }

    @Test
    void curvedVerticalPortsLeaveVertically() {
        var from = new Vector2f(0, 0);
        var to = new Vector2f(200, 300);
        var route = WireRouter.route(from, WireRouter.Axis.VERTICAL, to, WireRouter.Axis.VERTICAL,
                WireRouteStyle.CURVED, WireRouter.DEFAULT_MIN_JOG);
        assertEndpoints(route, from, to);
        var dx = route.get(1).x - route.getFirst().x;
        var dy = route.get(1).y - route.getFirst().y;
        assertTrue(Math.abs(dx) < Math.abs(dy) * 0.2f, "should leave downwards: d=(%f, %f)".formatted(dx, dy));
        assertTrue(dy > 0);
    }

    @Test
    void curvedSampleCountStaysBounded() {
        // A very long wire must not turn into a thousand-point polyline that hit-testing then walks
        // segment by segment on every frame. Adaptive subdivision is capped at six levels.
        var route = curved(0, 0, 20000, 9000);
        assertTrue(route.size() <= 65, "expected a bounded sample count, got " + route.size());
    }

    @Test
    void curvedSpendsItsPointsWhereTheCurveActuallyBends() {
        // A gentle S needs far fewer segments than a wire that has to hook back on itself. An even
        // split in t cannot tell the two apart, which is what used to facet the hooks.
        var gentle = curved(0, 0, 400, 60);
        var hooked = WireRouter.route(new Vector2f(300, 0), WireRouter.Axis.HORIZONTAL,
                new Vector2f(-300, 200), WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.CURVED, WireRouter.DEFAULT_MIN_JOG);
        assertTrue(hooked.size() > gentle.size(),
                "the hooked wire should be sampled more finely: %d vs %d".formatted(hooked.size(), gentle.size()));
    }

    @Test
    void curvedNeverStraysFarFromTheTrueCubic() {
        // The control-point rule is restated here on purpose: it is the contract, and the whole
        // point of adaptive subdivision is that the polyline stands in for this exact curve.
        var from = new Vector2f(300, 0);
        var to = new Vector2f(0, 150);
        var reach = (float) (6.25 * Math.sqrt(300));
        var c1 = new Vector2f(from.x + reach, from.y);
        var c2 = new Vector2f(to.x - reach, to.y);
        var route = WireRouter.route(from, WireRouter.Axis.HORIZONTAL, to, WireRouter.Axis.HORIZONTAL,
                WireRouteStyle.CURVED, 0f);
        var worst = 0f;
        for (var i = 0; i <= 400; i++) {
            worst = Math.max(worst, distanceToPolyline(route, cubic(from, c1, c2, to, i / 400f)));
        }
        assertTrue(worst < 1f, "the polyline strayed " + worst + " from the curve it stands for");
    }

    private static Vector2f cubic(Vector2f p0, Vector2f c1, Vector2f c2, Vector2f p3, float t) {
        var u = 1 - t;
        var w0 = u * u * u;
        var w1 = 3 * u * u * t;
        var w2 = 3 * u * t * t;
        var w3 = t * t * t;
        return new Vector2f(
                w0 * p0.x + w1 * c1.x + w2 * c2.x + w3 * p3.x,
                w0 * p0.y + w1 * c1.y + w2 * c2.y + w3 * p3.y);
    }

    @Test
    void curvedIsStableForTheSameInput() {
        assertEquals(curved(0, 0, 317, 91), curved(0, 0, 317, 91));
    }

    // endregion

    @Test
    void routeIsStableForTheSameInput() {
        var a = octilinear(0, 0, 317, 91);
        var b = octilinear(0, 0, 317, 91);
        assertEquals(a, b);
    }
}
