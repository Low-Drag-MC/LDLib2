package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Collects graph diagnostics produced by {@link Graph#onGraphChanged(GraphLogger)}.
 */
public class GraphLogger {
    private final List<Entry> entries = new ArrayList<>();

    public GraphLogger error(Component message) {
        return error(message, null);
    }

    public GraphLogger error(Component message, @Nullable Object context) {
        return log(Level.ERROR, message, context);
    }

    public GraphLogger warning(Component message) {
        return warning(message, null);
    }

    public GraphLogger warning(Component message, @Nullable Object context) {
        return log(Level.WARNING, message, context);
    }

    public GraphLogger info(Component message) {
        return info(message, null);
    }

    public GraphLogger info(Component message, @Nullable Object context) {
        return log(Level.INFO, message, context);
    }

    public GraphLogger log(Level level, Component message) {
        return log(level, message, null);
    }

    public GraphLogger log(Level level, Component message, @Nullable Object context) {
        entries.add(new Entry(
                Objects.requireNonNull(level, "level"),
                Objects.requireNonNull(message, "message"),
                context));
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public List<Entry> getSortedEntries() {
        var sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(entry -> entry.level.priority));
        return List.copyOf(sorted);
    }

    public void clear() {
        entries.clear();
    }

    public enum Level {
        ERROR(0),
        WARNING(1),
        INFO(2);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }
    }

    public record Entry(Level level, Component message, @Nullable Object context) {
    }
}
