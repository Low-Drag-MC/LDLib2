package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import java.util.*;

/**
 * Static lists used to avoid allocations when using simple change hint lists.
 */
public final class ChangeHintList {
    public static final ChangeHintList UNSPECIFIED = new ChangeHintList(ChangeHint.UNSPECIFIED);
    public static final ChangeHintList LAYOUT      = new ChangeHintList(ChangeHint.LAYOUT);
    public static final ChangeHintList STYLE       = new ChangeHintList(ChangeHint.STYLE);
    public static final ChangeHintList DATA        = new ChangeHintList(ChangeHint.DATA);
    public static final ChangeHintList GRAPH_TOPOLOGY = new ChangeHintList(ChangeHint.GRAPH_TOPOLOGY);
    public static final ChangeHintList GROUPING    = new ChangeHintList(ChangeHint.GROUPING);

    private final ArrayList<ChangeHint> changeHints;
    private final List<ChangeHint> changeHintsView;

    private ChangeHintList(ChangeHint changeHint) {
        this.changeHints = new ArrayList<>(1);
        this.changeHints.add(changeHint);
        this.changeHintsView = Collections.unmodifiableList(changeHints);
    }

    private ChangeHintList(ChangeHintList other) {
        this.changeHints = new ArrayList<>(other.changeHints);
        this.changeHintsView = Collections.unmodifiableList(changeHints);
    }

    public List<ChangeHint> getHints() {
        return changeHintsView;
    }

    private int count() {
        return changeHints.size();
    }

    private ChangeHint get(int index) {
        return changeHints.get(index);
    }

    private static boolean isSharedList(ChangeHintList list) {
        return list == UNSPECIFIED
            || list == LAYOUT
            || list == STYLE
            || list == DATA
            || list == GRAPH_TOPOLOGY
            || list == GROUPING;
    }

    /**
     * Merges two lists of ChangeHints.
     *
     * @param dest   The first list. May be modified and returned (copy-on-write if shared).
     * @param source The list to merge into dest. Not modified; may be returned if it is a shared list.
     * @return The merged list.
     */
    public static ChangeHintList addRange(ChangeHintList dest, ChangeHintList source) {
        if (source == null || source.count() == 0) {
            return dest;
        }

        if (dest == null) {
            // If source is a shared singleton, just return it.
            if (isSharedList(source)) {
                return source;
            }
            return new ChangeHintList(source);
        }

        // Unspecified dominates
        if (dest.contains(ChangeHint.UNSPECIFIED) || source.contains(ChangeHint.UNSPECIFIED)) {
            return UNSPECIFIED;
        }

        // If they are the exact same instance, nothing to do
        if (dest == source) {
            return dest;
        }

        // Copy-on-write if dest is shared
        ChangeHintList returnValue = isSharedList(dest) ? new ChangeHintList(dest) : dest;
        ArrayList<ChangeHint> writable = returnValue.changeHints;

        // Ensure capacity (optional micro-opt)
        writable.ensureCapacity(writable.size() + source.count());

        for (int i = 0; i < source.count(); i++) {
            ChangeHint hint = source.get(i);
            if (!writable.contains(hint)) {
                writable.add(hint);
            }
        }

        return returnValue;
    }

    /**
     * Adds a ChangeHint to a ChangeHintList.
     *
     * @param list       The list of hints.
     * @param changeHint The hint to add.
     * @return The new list of hints (may be same instance).
     */
    public static ChangeHintList add(ChangeHintList list, ChangeHint changeHint) {
        if (list == null) {
            return toSharedList(changeHint);
        }

        if (changeHint == ChangeHint.UNSPECIFIED) {
            return UNSPECIFIED;
        }

        if (list.changeHints.contains(changeHint)) {
            return list;
        }

        ChangeHintList returnValue = isSharedList(list) ? new ChangeHintList(list) : list;
        returnValue.changeHints.add(changeHint);
        return returnValue;
    }

    /**
     * Converts a single ChangeHint to a (possibly shared) ChangeHintList.
     */
    public static ChangeHintList toSharedList(ChangeHint changeHint) {
        if (changeHint == null) return null;

        return switch (changeHint) {
            case UNSPECIFIED    -> UNSPECIFIED;
            case LAYOUT         -> LAYOUT;
            case STYLE          -> STYLE;
            case DATA           -> DATA;
            case GRAPH_TOPOLOGY -> GRAPH_TOPOLOGY;
            case GROUPING       -> GROUPING;
            default             -> new ChangeHintList(changeHint);
        };
    }

    boolean contains(ChangeHint needle) {
        for (int i = 0; i < changeHints.size(); i++) {
            if (changeHints.get(i) == needle) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if this list contains needle OR Unspecified.
     */
    public boolean hasChange(ChangeHint needle) {
        for (ChangeHint hint : changeHints) {
            if (hint == needle || hint == ChangeHint.UNSPECIFIED) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyChange(ChangeHint needle1, ChangeHint needle2) {
        for (ChangeHint hint : changeHints) {
            if (hint == needle1 || hint == needle2 || hint == ChangeHint.UNSPECIFIED) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyChange(List<ChangeHint> needles) {
        for (ChangeHint hint : changeHints) {
            if (hint == ChangeHint.UNSPECIFIED || needles.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if this contains all hints in needles, OR this contains Unspecified.
     *
     * Note: returns false if needles contains Unspecified unless this contains Unspecified itself.
     */
    public boolean isSupersetOf(ChangeHintList needles) {
        if (this.contains(ChangeHint.UNSPECIFIED)) {
            return true;
        }
        for (int i = 0; i < needles.changeHints.size(); i++) {
            ChangeHint hint = needles.changeHints.get(i);
            if (hint == ChangeHint.UNSPECIFIED) {
                return false;
            }
            if (!this.contains(hint)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ChangeHintList other && changeHints.equals(other.changeHints);
    }

    @Override
    public int hashCode() {
        return changeHints.hashCode();
    }

    @Override
    public String toString() {
        return changeHints.toString();
    }
}
