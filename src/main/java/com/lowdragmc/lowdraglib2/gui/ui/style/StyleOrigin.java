package com.lowdragmc.lowdraglib2.gui.ui.style;

/**
 * Represents the origin (cascade layer) of a style value applied to a UI element.
 * Higher priority values win when multiple origins compete for the same property.
 *
 * <p>Priority ordering (ascending):
 * <ol>
 *   <li>{@link #DEFAULT}    (0) – component's internal default; the lowest precedence.</li>
 *   <li>{@link #STYLESHEET} (2) – rules loaded from {@code .lss} files via the style engine.</li>
 *   <li>{@link #INLINE}     (3) – programmatic {@code setStyle()} / {@code lss()} calls on the element.</li>
 *   <li>{@link #ANIMATION}  (4) – values driven by a {@link com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimation}.</li>
 *   <li>{@link #IMPORTANT}  (5) – highest precedence; use to force a value above all other origins.</li>
 * </ol>
 *
 * <p>Note: priority 1 is intentionally unused to leave room for future origins
 * between DEFAULT and STYLESHEET (e.g. inherited values).
 */
public enum StyleOrigin {
    /**
     * Default style set by the UI component itself (internal defaults).
     * Priority 0 – lowest precedence.
     */
    DEFAULT(0),
    /**
     * Style originating from an external {@code .lss} stylesheet loaded by the style engine.
     * Priority 2.
     */
    STYLESHEET(2),
    /**
     * Style set programmatically via {@code element.lss()} or {@code element.setStyle()}.
     * Priority 3.
     */
    INLINE(3),
    /**
     * Style driven by a running {@link com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimation}.
     * Priority 4 – overrides inline styles during animation.
     */
    ANIMATION(4),
    /**
     * Highest-priority origin, equivalent to CSS {@code !important}.
     * Use {@code element.getStyleBag().putCandidate(p, StyleSlot.of(p, IMPORTANT, ...))} to apply.
     * Priority 5.
     */
    IMPORTANT(5),
    ;
    public final int priority;

    StyleOrigin(int priority) {
        this.priority = priority;
    }
}
