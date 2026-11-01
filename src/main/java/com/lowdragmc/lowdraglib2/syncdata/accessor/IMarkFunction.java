package com.lowdragmc.lowdraglib2.syncdata.accessor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

public interface IMarkFunction<TYPE, MARK> {
    record Simple<T, M>(@Nonnull Function<T, M> managedMarkFunction,
                        @Nonnull BiPredicate<M, T> areDifferentFunction) implements IMarkFunction<T, M> {
        @Override
        public @Nonnull M obtainManagedMark(@Nonnull T value) {
            return managedMarkFunction.apply(value);
        }

        @Override
        public boolean areDifferent(@NotNull M managedMark, @NotNull T value) {
            return areDifferentFunction.test(managedMark, value);
        }
    }

    /**
     * This is a simple implementation of {@link IMarkFunction} which does not store any mark.
     */
    interface LAZY<TYPE> extends IMarkFunction<TYPE, TYPE> {
        @Override
        @NotNull
        default TYPE obtainManagedMark(@NotNull TYPE value) {
            return value;
        }

        @Override
        default boolean areDifferent(@NotNull TYPE managedMark, @NotNull TYPE value) {
            return !Objects.equals(managedMark, value);
        }
    }

    IMarkFunction LAZY = new LAZY<>() {};

    static <T, M> IMarkFunction<T, M> lazy() {
        return LAZY;
    }

    /**
     * This method will be called to store a copy of the value for managed mark.
     * which will be used to compare with the latest value in the {@link #areDifferent(Object manaagedMark, TYPE value)}
     * @param value raw value
     * @return the managed mark value. this value may not be the same type as the value.
     */
    @Nonnull MARK obtainManagedMark(@Nonnull TYPE value);

    /**
     * Check if the value is different from the given managed mark value.
     * @param managedMark the managed mark value. which is obtained from {@link #obtainManagedMark(TYPE value)}.
     *                    this value may not be the same type as the value.
     * @param value the value to compare.
     * @return true if the two values are different.
     */
    boolean areDifferent(@Nonnull MARK managedMark, @Nonnull TYPE value);

    /**
     * Compares the value with its mark and returns the mark to keep from now on. Refs call this each time they check
     * their value for changes, instead of {@link #areDifferent} followed by {@link #obtainManagedMark}.
     * <br>
     * The default does exactly that. Override it when the comparison already produces the new mark (e.g. both
     * serialize the value), so a change costs one computation instead of two. Collection and map accessors still call
     * the two methods directly for their elements, so an override must agree with them: return {@code null} exactly
     * when {@link #areDifferent} would return false, and otherwise a mark equal to what {@link #obtainManagedMark}
     * would return.
     *
     * @param managedMark the current mark, obtained from {@link #obtainManagedMark} or a previous call
     * @param value       the value to compare
     * @return {@code null} if the value has not changed, otherwise the new mark of the value
     */
    @Nullable
    default MARK updateMark(@Nonnull MARK managedMark, @Nonnull TYPE value) {
        return areDifferent(managedMark, value) ? obtainManagedMark(value) : null;
    }
}
