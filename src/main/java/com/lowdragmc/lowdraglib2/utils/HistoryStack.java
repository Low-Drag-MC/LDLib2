package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A generic undo/redo history stack.
 * Layout:
 *   [undo ... oldest -> newest]  <current>  [redo newest <- oldest ...]
 *
 * maxSize counts current + undo entries (redo is ephemeral).
 */
public final class HistoryStack<T> {

    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    @Configurable(name = "ldlib.gui.editor.name.maxCount")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    private int maxSize;
    @Setter @Getter
    private boolean dedupeConsecutive;
    private T current;

    public HistoryStack(int maxSize) {
        this(maxSize, true);
    }

    public HistoryStack(int maxSize, boolean dedupeConsecutive) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be >= 1");
        this.maxSize = maxSize;
        this.dedupeConsecutive = dedupeConsecutive;
    }

    /** Clear all history and current value. */
    public void clear() {
        undo.clear();
        redo.clear();
        current = null;
    }

    /** Returns current value (may be null if never recorded). */
    public T getCurrent() {
        return current;
    }

    /** Replace current value without affecting undo/redo stacks. */
    public void replaceCurrent(T value) {
        current = value;
        // does not touch redo/undo; useful when initializing or loading.
    }

    /**
     * Record a new state:
     *  - If there is a current, push it to undo.
     *  - Set current = value.
     *  - Clear redo (standard undo/redo behavior).
     *  - Enforce capacity.
     *  - Optionally dedupe consecutive duplicates.
     */
    public void record(T value) {
        if (dedupeConsecutive && Objects.equals(current, value)) {
            return; // no-op on identical consecutive states
        }
        if (current != null) {
            // push current to undo (newest at tail)
            undo.addLast(current);
        }
        current = value;
        // new branch invalidates redo
        redo.clear();
        enforceCapacity();
    }

    /** Undo: move one step from undo -> current, and current -> redo. */
    public boolean undo() {
        if (!canUndo()) return false;
        // move current to redo (if it exists)
        if (current != null) {
            redo.addLast(current);
        }
        // pop newest undo
        current = undo.pollLast();
        return true;
    }

    /** Redo: move one step from redo -> current, and current -> undo. */
    public boolean redo() {
        if (!canRedo()) return false;
        if (current != null) {
            undo.addLast(current);
            enforceCapacity(); // adding to undo may exceed capacity
        }
        current = redo.pollLast();
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** Set the maximum size (>=1). Trims the oldest undo entries if needed. */
    @ConfigSetter(field = "maxSize")
    public void setMaxSize(int newMax) {
        if (newMax < 1) throw new IllegalArgumentException("maxSize must be >= 1");
        this.maxSize = newMax;
        enforceCapacity();
    }

    /** Size helpers (not counting redo unless explicitly asked). */
    public int undoSize() { return undo.size(); }
    public int redoSize() { return redo.size(); }

    /** Total tracked = undo + (current!=null?1:0). */
    public int trackedSize() { return undo.size() + (current == null ? 0 : 1); }

    private void enforceCapacity() {
        // Ensure (undo.size + current) <= maxSize; drop oldest undo if needed.
        while (trackedSize() > maxSize && !undo.isEmpty()) {
            undo.pollFirst(); // drop the oldest
        }
    }
}