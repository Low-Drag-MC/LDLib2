package com.lowdragmc.lowdraglib2.gui.sync.bindings;

import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IDataSource<T> {
    IDataSource<?> EMPTY = new IDataSource<Object>() {
        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public IDataSource<Object> setValue(@Nullable Object value) {
            return this;
        }
    };

    @SuppressWarnings("unchecked")
    static <T> IDataSource<T> empty() {
        return (IDataSource<T>) EMPTY;
    }

    record Simple<T>(Consumer<T> setter, Supplier<T> getter) implements IDataSource<T> {
        @Override
        public T getValue() {
            return getter.get();
        }

        @Override
        public IDataSource<T> setValue(@Nullable T value) {
            setter.accept(value);
            return this;
        }
    }

    static <T> IDataSource<T> of(Consumer<T> setter, Supplier<T> getter) {
        return new Simple<>(setter, getter);
    }

    /**
     * Gets the current value.
     */
    T getValue();

    /**
     * Sets the value.
     *
     * @param value The new value to set.
     */
    IDataSource<T> setValue(@Nullable T value);
}
