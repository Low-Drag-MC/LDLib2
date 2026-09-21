package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wire;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the polyline a wire is drawn along, for the styles that route rather than connect.
 *
 * <p>Everything here is one idea applied twice. First an <b>orthogonal</b> path is laid out: leave
 * the port along its own axis, cross over on a trunk placed midway, arrive along the other port's
 * axis. Then every corner is <b>chamfered</b> — the corner is cut back by the same distance along
 * both of its segments, which on an axis-aligned path is exactly a 45° diagonal.</p>
 *
 * <p>Letting the chamfer grow as large as the corner allows is what produces the
 * {@link WireRouteStyle#OCTILINEAR} look, and it falls out of the arithmetic rather than needing to
 * be special-cased:</p>
 * <ul>
 *     <li>When the two ports are less than 45° apart, the cut consumes the whole trunk, the two
 *     chamfers meet in the middle, and the result is <em>run, diagonal, run</em> — the wire goes
 *     straight until it can reach the target on a 45° and then does.</li>
 *     <li>When they are more than 45° apart, the cut is limited by the runs instead, and the result
 *     is <em>diagonal, trunk, diagonal</em> — as close to the target's lane as a 45° can get, then
 *     straight across.</li>
 * </ul>
 *
 * <p>No dependency on the graph, the UI or Minecraft: takes points, returns points.</p>
 */
public final class WireRouter {

    /** Which way a wire leaves or enters a port, from the port's orientation. */
    public enum Axis {
        HORIZONTAL,
        VERTICAL
    }

    /**
     * How far a wire is pushed straight out of an endpoint before a backward route is allowed to
     * turn around, so the turn never happens on top of the node.
     */
    public static final float DEFAULT_MIN_JOG = 24f;
    /**
     * How far a backward wire between two exactly level endpoints bows out of the straight line.
     * Without it the detour would be a flat line running back over both nodes.
     */
    public static final float BACKWARD_DETOUR = 48f;

    private static final float EPSILON = 1e-3f;

    private WireRouter() {
    }

    /**
     * Routes a single run between two points, both included in the result.
     *
     * @param minJog minimum straight run out of each endpoint on a backward route; pass
     *               {@link #DEFAULT_MIN_JOG} when there is nothing better to base it on
     */
    public static List<Vector2f> route(Vector2f from, Axis fromAxis,
                                       Vector2f to, Axis toAxis,
                                       WireRouteStyle style, float minJog) {
        if (style == WireRouteStyle.CURVED) {
            return curve(from, fromAxis, to, toAxis, minJog);
        }
        var path = orthogonalPath(from, fromAxis, to, toAxis, minJog);
        if (style == WireRouteStyle.OCTILINEAR) {
            // Unbounded: the chamfer is only ever limited by the corner it is cutting.
            return chamferCorners(path, 0f);
        }
        return path;
    }

    // region curve

    /**
     * How much of the distance between two ports the tangent reaches across on a forward wire. Half
     * puts the two control points on top of each other at the midpoint, which is the classic
     * node-editor S — flat at both ports, steepest in the middle.
     */
    private static final float FORWARD_TANGENT = 0.5f;
    /**
     * Tangent for a wire that has to double back, as a multiple of the square root of how far back.
     * Sublinear on purpose: a linear tangent turns a wire across the canvas into a loop the size of
     * the canvas, and past a certain bulge a bigger one says nothing more.
     */
    private static final float BACKWARD_TANGENT = 6.25f;
    /** How far the polyline may stray from the true curve, in content units. */
    private static final float FLATNESS = 0.4f;
    /**
     * Recursion cap, so a pathological curve cannot produce an unbounded polyline: at most
     * {@code 2^MAX_DEPTH} segments. Only a wire long enough to cross the canvas several times gets
     * anywhere near it.
     */
    private static final int MAX_DEPTH = 6;

    /**
     * A cubic Bézier leaving each port along the port's own axis — the curve Blender, Unreal and
     * every web node editor draw.
     *
     * <p>Sampled rather than handed to a curve renderer because the polyline is also what
     * hit-testing and the bounding box are built from: a wire the cursor misses because it is
     * really a straight line under a curved picture is worse than a slightly faceted curve.</p>
     *
     * <p>Sampling is <b>adaptive</b> — recursive de Casteljau subdivision until each piece is within
     * {@link #FLATNESS} of its own chord — rather than a fixed number of steps in {@code t}. A wire
     * that has to double back hooks through nearly 180° inside the first tenth of its parameter
     * range and then barely turns at all for the rest, so an even split spends most of its points
     * where nothing is happening and still visibly facets the hooks. This puts them where the curve
     * bends, and gives a flat stretch two points.</p>
     */
    static List<Vector2f> curve(Vector2f from, Axis fromAxis, Vector2f to, Axis toAxis, float minTangent) {
        var fromControl = offsetAlong(from, fromAxis, tangent(from, to, fromAxis, minTangent));
        var toControl = offsetAlong(to, toAxis, -tangent(from, to, toAxis, minTangent));
        var points = new ArrayList<Vector2f>();
        points.add(new Vector2f(from));
        subdivide(from, fromControl, toControl, to, 0, points);
        return simplify(points);
    }

    /** Appends everything after {@code p0} of the cubic, splitting until it is flat enough. */
    private static void subdivide(Vector2f p0, Vector2f c1, Vector2f c2, Vector2f p3,
                                  int depth, List<Vector2f> out) {
        if (depth >= MAX_DEPTH || isFlat(p0, c1, c2, p3)) {
            out.add(new Vector2f(p3));
            return;
        }
        var p01 = midpoint(p0, c1);
        var p12 = midpoint(c1, c2);
        var p23 = midpoint(c2, p3);
        var p012 = midpoint(p01, p12);
        var p123 = midpoint(p12, p23);
        var centre = midpoint(p012, p123);
        subdivide(p0, p01, p012, centre, depth + 1, out);
        subdivide(centre, p123, p23, p3, depth + 1, out);
    }

    /**
     * Whether the chord is already a good enough stand-in for the curve: both control points lie
     * within {@link #FLATNESS} of it. A cubic never strays further from its chord than its control
     * points do, so this bounds the error of the whole piece.
     */
    private static boolean isFlat(Vector2f p0, Vector2f c1, Vector2f c2, Vector2f p3) {
        return distanceToChord(p0, p3, c1) <= FLATNESS && distanceToChord(p0, p3, c2) <= FLATNESS;
    }

    /**
     * Distance from {@code point} to the line through {@code a} and {@code b} — or to {@code a}
     * itself when the two coincide, which is a loop returning to where it started and is the one
     * case where "on the line" would wrongly accept any control point at all.
     */
    private static float distanceToChord(Vector2f a, Vector2f b, Vector2f point) {
        var dx = b.x - a.x;
        var dy = b.y - a.y;
        var length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < EPSILON) return point.distance(a);
        return Math.abs(dx * (a.y - point.y) - (a.x - point.x) * dy) / length;
    }

    private static Vector2f midpoint(Vector2f a, Vector2f b) {
        return new Vector2f((a.x + b.x) / 2f, (a.y + b.y) / 2f);
    }

    /**
     * Tangent length, from how far the wire travels along {@code axis} in the source-to-target
     * direction. Always measured that way round, for both ends: asking the arriving end how far
     * away the source is would flip the sign and make every forward wire look like a backward one.
     * The caller negates the result for the arriving end.
     */
    private static float tangent(Vector2f from, Vector2f to, Axis axis, float minTangent) {
        var delta = axis == Axis.HORIZONTAL ? to.x - from.x : to.y - from.y;
        return delta >= 0
                ? Math.max(minTangent, FORWARD_TANGENT * delta)
                : Math.max(minTangent, (float) (BACKWARD_TANGENT * Math.sqrt(-delta)));
    }

    private static Vector2f offsetAlong(Vector2f point, Axis axis, float distance) {
        return axis == Axis.HORIZONTAL
                ? new Vector2f(point.x + distance, point.y)
                : new Vector2f(point.x, point.y + distance);
    }

    // endregion

    /**
     * The axis-aligned route between two points: out along {@code fromAxis}, across on a trunk
     * halfway between them, in along {@code toAxis}.
     */
    static List<Vector2f> orthogonalPath(Vector2f from, Axis fromAxis, Vector2f to, Axis toAxis, float minJog) {
        var points = new ArrayList<Vector2f>(6);
        points.add(new Vector2f(from));
        if (fromAxis == Axis.HORIZONTAL && toAxis == Axis.HORIZONTAL) {
            if (to.x > from.x) {
                var trunk = (from.x + to.x) / 2f;
                points.add(new Vector2f(trunk, from.y));
                points.add(new Vector2f(trunk, to.y));
            } else {
                // Target is behind the source: go forward far enough to clear the port, cross over
                // on a lane between the two, and come back in from in front of the target.
                var lane = detourLane(from.y, to.y);
                points.add(new Vector2f(from.x + minJog, from.y));
                points.add(new Vector2f(from.x + minJog, lane));
                points.add(new Vector2f(to.x - minJog, lane));
                points.add(new Vector2f(to.x - minJog, to.y));
            }
        } else if (fromAxis == Axis.VERTICAL && toAxis == Axis.VERTICAL) {
            if (to.y > from.y) {
                var trunk = (from.y + to.y) / 2f;
                points.add(new Vector2f(from.x, trunk));
                points.add(new Vector2f(to.x, trunk));
            } else {
                var lane = detourLane(from.x, to.x);
                points.add(new Vector2f(from.x, from.y + minJog));
                points.add(new Vector2f(lane, from.y + minJog));
                points.add(new Vector2f(lane, to.y - minJog));
                points.add(new Vector2f(to.x, to.y - minJog));
            }
        } else if (fromAxis == Axis.HORIZONTAL) {
            // Leaves sideways, arrives from above or below: a single elbow does it.
            points.add(new Vector2f(to.x, from.y));
        } else {
            points.add(new Vector2f(from.x, to.y));
        }
        points.add(new Vector2f(to));
        return simplify(points);
    }

    /** The cross-axis coordinate a detour runs along: midway, bowed aside when the ends are level. */
    private static float detourLane(float from, float to) {
        return Math.abs(to - from) < EPSILON ? from + BACKWARD_DETOUR : (from + to) / 2f;
    }

    /**
     * Replaces each interior corner with a straight cut of equal length along both of its segments.
     *
     * <p>A segment between two corners is shared, so each of them may only claim half of it —
     * otherwise two neighbouring cuts would overrun each other and the wire would double back. The
     * segments at the two ends belong to one corner each and can be claimed in full.</p>
     *
     * @param maxChamfer cap on the cut length; {@code <= 0} means uncapped, which is what produces
     *                   continuous 45° diagonals rather than small corner bevels
     */
    static List<Vector2f> chamferCorners(List<Vector2f> points, float maxChamfer) {
        var count = points.size();
        if (count < 3) return points;
        var out = new ArrayList<Vector2f>(count * 2);
        out.add(new Vector2f(points.get(0)));
        for (var i = 1; i < count - 1; i++) {
            var previous = points.get(i - 1);
            var corner = points.get(i);
            var next = points.get(i + 1);
            var backward = corner.distance(previous) * (i - 1 > 0 ? 0.5f : 1f);
            var forward = next.distance(corner) * (i + 1 < count - 1 ? 0.5f : 1f);
            var cut = Math.min(backward, forward);
            if (maxChamfer > 0) cut = Math.min(cut, maxChamfer);
            if (cut < EPSILON) {
                out.add(new Vector2f(corner));
                continue;
            }
            out.add(towards(corner, previous, cut));
            out.add(towards(corner, next, cut));
        }
        out.add(new Vector2f(points.get(count - 1)));
        return simplify(out);
    }

    /** The point {@code distance} away from {@code origin} in the direction of {@code target}. */
    private static Vector2f towards(Vector2f origin, Vector2f target, float distance) {
        var direction = new Vector2f(target).sub(origin);
        var length = direction.length();
        if (length < EPSILON) return new Vector2f(origin);
        return direction.div(length).mul(distance).add(origin);
    }

    /**
     * Drops points that carry no shape: ones coinciding with the point before them, and ones in
     * the middle of a straight run.
     *
     * <p>Both fall out of the passes above rather than out of bad input. A trunk the two chamfers
     * consume entirely leaves their cut points on top of each other, and the corner they were
     * cutting becomes a point sitting in the middle of the resulting diagonal — harmless to the
     * geometry, but the wire is drawn as a wide textured strip and every surplus vertex is a seam
     * in it.</p>
     */
    static List<Vector2f> simplify(List<Vector2f> points) {
        var out = new ArrayList<Vector2f>(points.size());
        for (var point : points) {
            if (!out.isEmpty() && out.getLast().distance(point) < EPSILON) continue;
            while (out.size() >= 2 && isStraightThrough(out.get(out.size() - 2), out.getLast(), point)) {
                out.removeLast();
            }
            out.add(point);
        }
        return out;
    }

    /** Whether {@code b} is a point on the straight run from {@code a} to {@code c}, not a corner. */
    private static boolean isStraightThrough(Vector2f a, Vector2f b, Vector2f c) {
        var inX = b.x - a.x;
        var inY = b.y - a.y;
        var outX = c.x - b.x;
        var outY = c.y - b.y;
        var inLength = (float) Math.sqrt(inX * inX + inY * inY);
        var outLength = (float) Math.sqrt(outX * outX + outY * outY);
        if (inLength < EPSILON || outLength < EPSILON) return true;
        var cross = (inX * outY - inY * outX) / (inLength * outLength);
        var dot = (inX * outX + inY * outY) / (inLength * outLength);
        // A 180° turn has a zero cross product too, and dropping its vertex would erase the fold.
        return Math.abs(cross) < 1e-4f && dot > 0;
    }
}
