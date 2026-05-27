package com.lowdragmc.lowdraglib2.syncdata.utils;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Resolves a {@link Supplier} that constructs a fresh empty instance of a given {@link Class}.
 * Used by {@code MapAccessor} / {@code CollectionAccessor} to fabricate read-only K/V/child
 * instances when payload structure differs from the existing container, removing the need for
 * {@code @ReadOnlyManaged} on fields whose value types either have a no-arg constructor or are
 * one of the well-known collection interfaces.
 *
 * <p>Returns {@code null} when the type cannot be fabricated; callers fall back to their
 * existing strict behavior in that case.
 */
public final class TypeFabricator {

    private static final Map<Class<?>, Supplier<?>> INTERFACE_DEFAULTS = Map.of(
            List.class, ArrayList::new,
            Set.class, HashSet::new,
            Queue.class, ArrayDeque::new,
            Deque.class, ArrayDeque::new,
            Map.class, HashMap::new,
            Collection.class, ArrayList::new
    );

    private static final ClassValue<Supplier<?>> CACHE = new ClassValue<>() {
        @Override
        protected Supplier<?> computeValue(Class<?> type) {
            var iface = INTERFACE_DEFAULTS.get(type);
            if (iface != null) return iface;
            try {
                Constructor<?> ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                return () -> {
                    try {
                        return ctor.newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Failed to fabricate instance of " + type.getName(), e);
                    }
                };
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    };

    private TypeFabricator() {}

    public static @Nullable Supplier<?> fabricator(Class<?> type) {
        return CACHE.get(type);
    }

    public static boolean canFabricate(Class<?> type) {
        return fabricator(type) != null;
    }
}
