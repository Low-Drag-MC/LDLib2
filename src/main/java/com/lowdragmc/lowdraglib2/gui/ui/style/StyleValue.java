package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class StyleValue<T> {
    public final String rawValue;
    private volatile T computedValue;
    private volatile boolean computed = false;

    public StyleValue(String rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Computes the value based on the raw input value if it has not been computed already.
     * The computation process is defined by the implementation of the {@code doCompute(String)}
     * method in subclasses. If an exception occurs during the computation, the computed value
     * is set to {@code null}.
     *
     * @return the computed value of type {@code T}, or {@code null} if an exception occurs during computation
     */
    public T compute() {
        if (!computed) {
            try {
                computedValue = doCompute(rawValue);
            } catch (Exception e) {
                LDLib2.LOGGER.warn("Failed to parse style value '{}': {}", rawValue, e.getMessage());
                computedValue = null;
            }
            computed = true;
        }
        return computedValue;
    }

    /**
     * Performs computation on the provided raw value to produce a result of type {@code T}.
     * The implementation of this method should define the specific logic for converting
     * the raw input string into a computed value.
     *
     * @param rawValue the raw input value as a string
     * @return the computed value of type {@code T}
     */
    protected abstract @Nullable T doCompute(String rawValue);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StyleValue<?> that)) return false;
        return Objects.equals(rawValue, that.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rawValue);
    }
}
