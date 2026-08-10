package com.lowdragmc.lowdraglib2.utils;

/**
 * {@link AutoCloseable} without the checked exception, so it can be used in try-with-resources
 * without a catch.
 *
 * <p>The library has several "install this for the duration of a block" overrides — the active UI,
 * the surface being drawn into, where key state is read from, which GL context is current. They all
 * want the same shape, and a checked {@code close()} would force every one of their call sites into
 * a try/catch that can never fire.
 */
@FunctionalInterface
public interface Scope extends AutoCloseable {
    @Override
    void close();
}
