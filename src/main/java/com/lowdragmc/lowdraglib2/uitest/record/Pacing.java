package com.lowdragmc.lowdraglib2.uitest.record;

import java.util.Random;

/**
 * The rhythm of recorded gestures, in milliseconds of video time: how long the cursor rests before a
 * press, how long a button or key stays down, how fast text is typed.
 *
 * <p>Every value varies a little, as a person's would, from a generator seeded by the scenario, so
 * recording the same scenario again produces the same video.
 */
public final class Pacing {

    private final Random random;

    public Pacing(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Plans one cursor move. {@code elbow} goes across and then along, for walking a menu without
     * crossing a sibling entry; otherwise the path bows gently, the way a hand does.
     *
     * @param targetSize   the target's smaller dimension; small targets are approached more slowly
     * @param screenHeight GUI-scaled viewport height
     * @param slowdown     multiplies the duration, above {@code 1} for a move that carries a drag
     */
    public CursorPath travel(float fromX, float fromY, float toX, float toY, float targetSize, float screenHeight,
                             boolean elbow, float slowdown) {
        return elbow
                ? CursorPath.elbow(fromX, fromY, toX, toY, targetSize, screenHeight, random)
                : CursorPath.curve(fromX, fromY, toX, toY, targetSize, screenHeight, slowdown, random);
    }

    private long between(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }

    public long beforePress() {
        return between(90, 150);
    }

    public long pressHold() {
        return between(70, 105);
    }

    /** A press held briefly enough that a second one still lands inside the double-click window. */
    public long quickHold() {
        return between(55, 75);
    }

    public long doubleClickGap() {
        return between(60, 85);
    }

    public long afterRelease() {
        return between(110, 170);
    }

    public long beforeKey() {
        return between(80, 140);
    }

    /** Between a modifier going down and the key it modifies. */
    public long modifierLead() {
        return between(50, 90);
    }

    public long keyHold() {
        return between(60, 95);
    }

    public long afterKey() {
        return between(100, 160);
    }

    /** Before typing a character, given the one typed just before it ({@code 0} for the first). */
    public long beforeChar(char previous) {
        var gap = between(55, 110);
        if (previous == ' ') gap += between(30, 80);
        if (".,;:!?".indexOf(previous) >= 0) gap += between(90, 160);
        return gap;
    }

    public long afterTyping() {
        return between(160, 260);
    }

    public long scrollGap() {
        return between(80, 120);
    }

    public long beforeDrop() {
        return between(110, 170);
    }
}
