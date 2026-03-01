package com.lowdragmc.lowdraglib2.gui.ui.style;

@FunctionalInterface
@SuppressWarnings("rawtypes")
public interface IValueInterpolator<T> {
    /** Instantly snaps to the end value (equivalent to CSS transition-timing-function: step-end). */
    IValueInterpolator SNAP_TO_END = (from, to, interpolation) -> to;
    /** @deprecated Use {@link #SNAP_TO_END} */
    @Deprecated IValueInterpolator ALWAYS_INTERPOLATE = SNAP_TO_END;
    IValueInterpolator NEVER_INTERPOLATE = (from, to, interpolation) -> from;
    IValueInterpolator BINARY = (from, to, interpolation) -> interpolation < 0.5f ? from : to;

    static <V> IValueInterpolator<V> snapToEnd() {
        return SNAP_TO_END;
    }

    /** @deprecated Use {@link #snapToEnd()} */
    @Deprecated
    static <V> IValueInterpolator<V> alwaysInterpolate() {
        return SNAP_TO_END;
    }

    static <V> IValueInterpolator<V> neverInterpolate() {
        return NEVER_INTERPOLATE;
    }

    static <V> IValueInterpolator<V> binary() {
        return BINARY;
    }

    /**
     * Interpolates between two values of type {@code T} based on the given interpolation factor.
     *
     * @param from the initial value of type {@code T}
     * @param to the target value of type {@code T}
     * @param interpolation a float value between 0.0 and 1.0 that determines the interpolation position
     */
    T interpolate(T from, T to, float interpolation);
}
