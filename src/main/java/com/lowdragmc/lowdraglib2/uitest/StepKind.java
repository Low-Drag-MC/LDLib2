package com.lowdragmc.lowdraglib2.uitest;

/**
 * Coarse classification of a step, recorded in the report so a reader can tell at a glance whether a
 * failure was "the UI did the wrong thing" ({@link #ASSERT}) or "we never got the UI into the state
 * the assertion assumed" ({@link #WORLD}, {@link #OPEN}, {@link #WAIT}).
 */
public enum StepKind {
    /** Mutates the world or the server. Runs on the server thread. */
    WORLD,
    /** Opens or closes a screen. */
    OPEN,
    /** Delivers synthetic input. */
    INPUT,
    /** Blocks until a condition holds, or times out. */
    WAIT,
    /** Records an expectation. */
    ASSERT,
    /** Produces a screenshot or a snapshot. */
    CAPTURE,
    /** Arbitrary user code — the escape hatch. */
    CUSTOM,
    /** Cleanup registered with {@code teardown(..)}; always runs, even after an abort. */
    TEARDOWN
}
