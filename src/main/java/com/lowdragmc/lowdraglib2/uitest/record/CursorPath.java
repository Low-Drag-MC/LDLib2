package com.lowdragmc.lowdraglib2.uitest.record;

import java.util.Random;

/**
 * Where a recorded cursor is at each moment of one move, in GUI-scaled screen space.
 *
 * <p>The duration follows Fitts's law: longer for far and small targets. Position along the path
 * follows a minimum-jerk profile, accelerating smoothly and settling onto the target without a bump.
 * A {@link #curve} bows slightly to one side, the way a hand pivoting at the wrist does; an
 * {@link #elbow} goes across first and then down, which is how a menu has to be walked so the pointer
 * never crosses a sibling entry and closes the submenu it is heading for.
 */
public final class CursorPath {

    /** Distances are judged on a 1080-line screen whatever the GUI scale, so pacing looks the same at any resolution. */
    private static final float REFERENCE_HEIGHT = 1080f;

    private final float[] points;
    private final boolean elbow;
    private final float duration;

    private CursorPath(float[] points, boolean elbow, float duration) {
        this.points = points;
        this.elbow = elbow;
        this.duration = duration;
    }

    /**
     * @param targetSize   the target's smaller dimension, so small targets take longer to land on
     * @param screenHeight GUI-scaled viewport height
     * @param slowdown     multiplies the duration; above {@code 1} for moves that carry something
     */
    public static CursorPath curve(float fromX, float fromY, float toX, float toY, float targetSize,
                                   float screenHeight, float slowdown, Random random) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float side = random.nextFloat() < 0.7f ? 1f : -1f;
        float arc = side * (0.04f + random.nextFloat() * 0.06f);
        // perpendicular to the move, as long as the move itself, so the bow scales with distance
        float nx = -dy * arc;
        float ny = dx * arc;
        var points = new float[]{
                fromX, fromY,
                fromX + dx * 0.3f + nx, fromY + dy * 0.3f + ny,
                fromX + dx * 0.72f + nx * 0.55f, fromY + dy * 0.72f + ny * 0.55f,
                toX, toY};
        float distance = (float) Math.hypot(dx, dy);
        return new CursorPath(points, false, duration(distance, targetSize, screenHeight, random) * slowdown);
    }

    /** Across to the target's column first, then along it. */
    public static CursorPath elbow(float fromX, float fromY, float toX, float toY, float targetSize,
                                   float screenHeight, Random random) {
        var points = new float[]{fromX, fromY, toX, fromY, toX, toY};
        float distance = Math.abs(toX - fromX) + Math.abs(toY - fromY);
        return new CursorPath(points, true, duration(distance, targetSize, screenHeight, random));
    }

    private static float duration(float distance, float targetSize, float screenHeight, Random random) {
        if (distance < 0.5f) return 0;
        float scale = REFERENCE_HEIGHT / Math.max(1f, screenHeight);
        float d = distance * scale;
        float w = Math.max(12f, targetSize * scale);
        double seconds = 0.2 + 0.1 * (Math.log(d / w + 1) / Math.log(2));
        seconds = Math.min(0.9, Math.max(0.18, seconds));
        return (float) (seconds * (0.92 + random.nextFloat() * 0.16));
    }

    public float durationSeconds() {
        return duration;
    }

    /**
     * Writes the position {@code seconds} into the move into {@code out}.
     *
     * @return whether the move has finished, in which case {@code out} is the target
     */
    public boolean at(float seconds, float[] out) {
        float u = duration <= 0 ? 1 : Math.min(1f, Math.max(0f, seconds / duration));
        // a little more of the move happens early, as in a real reach: peak speed comes before the midpoint
        float e = (float) Math.pow(u, 0.92);
        float s = e * e * e * (10 - 15 * e + 6 * e * e);
        if (elbow) {
            elbowAt(s, out);
        } else {
            float a = 1 - s;
            out[0] = a * a * a * points[0] + 3 * a * a * s * points[2] + 3 * a * s * s * points[4] + s * s * s * points[6];
            out[1] = a * a * a * points[1] + 3 * a * a * s * points[3] + 3 * a * s * s * points[5] + s * s * s * points[7];
        }
        return u >= 1;
    }

    private void elbowAt(float s, float[] out) {
        float first = Math.abs(points[2] - points[0]);
        float second = Math.abs(points[5] - points[3]);
        float travelled = s * (first + second);
        if (travelled <= first && first > 0) {
            float k = travelled / first;
            out[0] = points[0] + (points[2] - points[0]) * k;
            out[1] = points[1];
        } else {
            float k = second <= 0 ? 1 : (travelled - first) / second;
            out[0] = points[4];
            out[1] = points[3] + (points[5] - points[3]) * k;
        }
    }
}
