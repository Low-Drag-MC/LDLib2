package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry of {@link IMarkStrategy}s: cheap ways to tell whether values of particular classes changed. The sync asks
 * it wherever it would otherwise serialize a value to compare it, which is for
 * {@link net.neoforged.neoforge.common.util.ValueIOSerializable} values and fields marked with their codec. Accessors of
 * your own can do the same through {@link #orElse}.
 * <br>
 * Built in: values implementing {@link com.lowdragmc.lowdraglib2.syncdata.IContentVersion} (priority -1), and
 * {@link net.neoforged.neoforge.transfer.StacksResourceHandler} subclasses - NeoForge's item, fluid and resource stack
 * handlers - that still save and hand out their stacks the way that class does (priority 1000).
 */
public final class MarkStrategies {
    private MarkStrategies() {}

    private record Holder(IMarkStrategy<?, ?> strategy, int priority) {}

    // what a class was found to use, and among which strategies, so a lookup made before a registration is redone
    private record Lookup(List<Holder> among, @Nullable IMarkStrategy<Object, Object> strategy) {}

    private static volatile List<Holder> strategies = List.of();
    private static final Map<Class<?>, Lookup> LOOKUP = new ConcurrentHashMap<>();
    private static boolean initialized;

    /**
     * Registers a strategy. Lower priorities are asked first; strategies of equal priority in the order they were
     * registered. Usually done while the game loads; a value already being compared another way is synced once more
     * when its class gets a strategy later.
     */
    public static synchronized void register(IMarkStrategy<?, ?> strategy, int priority) {
        var next = new ArrayList<>(strategies);
        int index = 0;
        while (index < next.size() && next.get(index).priority <= priority) {
            index++;
        }
        next.add(index, new Holder(strategy, priority));
        strategies = List.copyOf(next);
    }

    /**
     * @return the first registered strategy accepting values of exactly this class, or {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static IMarkStrategy<Object, Object> find(Class<?> type) {
        var among = strategies;
        var lookup = LOOKUP.get(type);
        if (lookup == null || lookup.among != among) {
            IMarkStrategy<Object, Object> found = null;
            for (var holder : among) {
                if (holder.strategy.test(type)) {
                    found = (IMarkStrategy<Object, Object>) holder.strategy;
                    break;
                }
            }
            lookup = new Lookup(among, found);
            LOOKUP.put(type, lookup);
        }
        return lookup.strategy;
    }

    /**
     * Marks that compare a value with the strategy registered for its class, or with {@code fallback} when there is
     * none. A value that changes to a class compared another way counts as changed.
     */
    @SuppressWarnings("unchecked")
    public static <T> IMarkFunction<T, Object> orElse(IMarkFunction<? super T, ?> fallback) {
        return new WithFallback<>((IMarkFunction<Object, Object>) fallback);
    }

    /**
     * Whether {@code type} uses {@code owner}'s declaration of the public method, rather than overriding it. For
     * strategies that read a class's contents the way a base class saves them.
     */
    public static boolean isDeclaredBy(Class<?> type, Class<?> owner, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters).getDeclaringClass() == owner;
        } catch (NoSuchMethodException | LinkageError e) {
            // looking a method up resolves every public signature of the class, which fails on a dedicated server
            // when one mentions a client class
            return false;
        }
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        register(new ContentVersionStrategy(), -1);
        register(new StacksHandlerStrategy(), 1000);
    }

    private record Marked(IMarkFunction<Object, Object> by, Object mark) {}

    private record WithFallback<T>(IMarkFunction<Object, Object> fallback) implements IMarkFunction<T, Object> {
        private IMarkFunction<Object, Object> markerOf(T value) {
            var strategy = find(value.getClass());
            return strategy == null ? fallback : strategy;
        }

        @Override
        public @NotNull Object obtainManagedMark(@NotNull T value) {
            var by = markerOf(value);
            return new Marked(by, by.obtainManagedMark(value));
        }

        @Override
        public boolean areDifferent(@NotNull Object managedMark, @NotNull T value) {
            var by = markerOf(value);
            return !(managedMark instanceof Marked marked && marked.by == by) || by.areDifferent(marked.mark, value);
        }

        @Override
        public Object updateMark(@NotNull Object managedMark, @NotNull T value) {
            var by = markerOf(value);
            if (!(managedMark instanceof Marked marked && marked.by == by)) {
                return new Marked(by, by.obtainManagedMark(value));
            }
            var mark = by.updateMark(marked.mark, value);
            return mark == null ? null : new Marked(by, mark);
        }
    }
}
