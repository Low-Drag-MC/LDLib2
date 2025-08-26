package com.lowdragmc.lowdraglib2.utils.search;

import java.util.function.Consumer;

@FunctionalInterface
public
interface IResultHandler<T> extends Consumer<T> {
    /**
     * Handles the result of a search or processing operation.
     * <br/>
     * NOTE! This may be called in an async-thread.
     *
     * @param result The result object of type {@code T} that was processed or found.
     */
    void acceptResult(T result);

    @Override
    default void accept(T t) {
        acceptResult(t);
    }
}
