package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides where a dragged rectangle should actually land: lined up with a nearby element if one is
 * in reach, otherwise on the grid.
 *
 * <p>Each axis is solved on its own, because they are independent questions — a node can be left-
 * aligned with one neighbour and top-aligned with a different one, and that is exactly the case a
 * per-axis solution gets right for free.</p>
 *
 * <p>Element alignment wins over the grid whenever it is in reach, rather than the two competing on
 * distance. Lining up with a neighbour is something the user is <em>trying</em> to do; landing on a
 * grid line is a tidiness default. Letting the grid win by being half a pixel closer would make the
 * feature fail exactly when it is being used deliberately.</p>
 *
 * <p>Pure geometry — rectangles in, offsets out, no models and no UI — so it can be unit tested.
 * All rectangles are {@code (x, y, width, height)} in canvas content coordinates.</p>
 */
public final class SnapEngine {

    private static final float EPSILON = 1e-3f;
    /** Anchors in the order a tie is broken: an edge reads as more deliberate than a centre. */
    private static final SnapAnchor[] ANCHORS = {SnapAnchor.START, SnapAnchor.END, SnapAnchor.CENTER};

    private SnapEngine() {
    }

    /**
     * @param moving  the dragged rectangle at the position the cursor put it
     * @param targets every other element's rectangle; the dragged ones must already be excluded, or
     *                the drag would happily snap to where it currently is and never move
     */
    public static SnapResult snap(Vector4f moving, List<Vector4f> targets, SnapSettings settings) {
        var x = resolveAxis(moving, targets, settings, true);
        var y = resolveAxis(moving, targets, settings, false);
        if (x == null && y == null) return SnapResult.NONE;

        var offsetX = x == null ? 0f : x.delta;
        var offsetY = y == null ? 0f : y.delta;
        var snapped = new Vector4f(moving.x + offsetX, moving.y + offsetY, moving.z, moving.w);

        var guides = new ArrayList<SnapGuide>(2);
        if (x != null && x.fromElement) guides.add(buildGuide(true, x, snapped, targets, settings));
        if (y != null && y.fromElement) guides.add(buildGuide(false, y, snapped, targets, settings));
        return new SnapResult(offsetX, offsetY, List.copyOf(guides));
    }

    /** The winning correction on one axis. */
    private record AxisSnap(float delta, float line, SnapAnchor anchor, boolean fromElement) {
    }

    private static @Nullable AxisSnap resolveAxis(Vector4f moving, List<Vector4f> targets,
                                                  SnapSettings settings, boolean horizontal) {
        if (settings.align() && settings.alignThreshold() > 0) {
            var aligned = bestElementSnap(moving, targets, settings, horizontal);
            if (aligned != null) return aligned;
        }
        if (settings.grid() && settings.gridSize() > 0) {
            return bestGridSnap(moving, settings.gridSize(), horizontal);
        }
        return null;
    }

    private static @Nullable AxisSnap bestElementSnap(Vector4f moving, List<Vector4f> targets,
                                                      SnapSettings settings, boolean horizontal) {
        AxisSnap best = null;
        for (var target : targets) {
            if (!isNearby(moving, target, horizontal, settings.searchRange())) continue;
            for (var targetAnchor : ANCHORS) {
                var line = targetAnchor.of(target, horizontal);
                for (var ownAnchor : ANCHORS) {
                    var delta = line - ownAnchor.of(moving, horizontal);
                    if (Math.abs(delta) > settings.alignThreshold()) continue;
                    var candidate = new AxisSnap(delta, line, ownAnchor, true);
                    if (best == null || beats(candidate, best)) best = candidate;
                }
            }
        }
        return best;
    }

    /** Closer wins; at the same distance the anchor listed first in {@link #ANCHORS} wins. */
    private static boolean beats(AxisSnap candidate, AxisSnap incumbent) {
        var difference = Math.abs(candidate.delta) - Math.abs(incumbent.delta);
        if (difference < -EPSILON) return true;
        if (difference > EPSILON) return false;
        return priority(candidate.anchor) < priority(incumbent.anchor);
    }

    private static int priority(SnapAnchor anchor) {
        for (var i = 0; i < ANCHORS.length; i++) {
            if (ANCHORS[i] == anchor) return i;
        }
        return ANCHORS.length;
    }

    /**
     * Rounds whichever of the three anchors is closest to a grid line, rather than always the
     * top-left corner — so a node whose right edge is a hair off a grid line gets pulled by that
     * edge instead of being shoved half a cell sideways by its left one.
     */
    private static AxisSnap bestGridSnap(Vector4f moving, float gridSize, boolean horizontal) {
        AxisSnap best = null;
        for (var anchor : ANCHORS) {
            var own = anchor.of(moving, horizontal);
            var line = Math.round(own / gridSize) * gridSize;
            var candidate = new AxisSnap(line - own, line, anchor, false);
            if (best == null || beats(candidate, best)) best = candidate;
        }
        return best;
    }

    /**
     * Whether a target is close enough <em>across</em> the guide to count. Measured as the gap
     * between the two rectangles' extents on the other axis, so a node directly above counts no
     * matter how tall the column is, and one off in the corner of the canvas does not.
     */
    private static boolean isNearby(Vector4f moving, Vector4f target, boolean horizontal, float range) {
        var movingMin = horizontal ? moving.y : moving.x;
        var movingMax = movingMin + (horizontal ? moving.w : moving.z);
        var targetMin = horizontal ? target.y : target.x;
        var targetMax = targetMin + (horizontal ? target.w : target.z);
        var gap = Math.max(0f, Math.max(movingMin - targetMax, targetMin - movingMax));
        return gap <= range;
    }

    /**
     * The line to draw, spanning the dragged rectangle and every nearby element sitting on the same
     * line — which is what turns "something snapped" into "these three are left-aligned".
     */
    private static SnapGuide buildGuide(boolean vertical, AxisSnap snap, Vector4f snapped,
                                        List<Vector4f> targets, SnapSettings settings) {
        var horizontal = vertical; // a vertical guide is the result of solving the x axis
        var start = otherMin(snapped, horizontal);
        var end = otherMax(snapped, horizontal);
        for (var target : targets) {
            if (!isNearby(snapped, target, horizontal, settings.searchRange())) continue;
            var onLine = false;
            for (var anchor : ANCHORS) {
                if (Math.abs(anchor.of(target, horizontal) - snap.line) <= EPSILON) {
                    onLine = true;
                    break;
                }
            }
            if (!onLine) continue;
            start = Math.min(start, otherMin(target, horizontal));
            end = Math.max(end, otherMax(target, horizontal));
        }
        return new SnapGuide(vertical, snap.line, start, end, snap.anchor);
    }

    private static float otherMin(Vector4f rect, boolean horizontal) {
        return horizontal ? rect.y : rect.x;
    }

    private static float otherMax(Vector4f rect, boolean horizontal) {
        return horizontal ? rect.y + rect.w : rect.x + rect.z;
    }
}
