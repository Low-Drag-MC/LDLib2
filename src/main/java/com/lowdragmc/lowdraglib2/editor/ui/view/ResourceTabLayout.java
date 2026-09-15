package com.lowdragmc.lowdraglib2.editor.ui.view;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * How a {@link ResourceView} arranges its tab strip: which side of the content it sits on, how thick
 * it is, what order the resource tabs come in and which of them the user has hidden.
 *
 * <p>Kept apart from the view so that it can be read from disk before the view has any tabs — the
 * order and the hidden set are named by {@code Resource#getName()} rather than by the tab elements,
 * which do not exist until a project is loaded and are thrown away again when it is closed.
 *
 * <p>The thickness is remembered per axis rather than per side: flipping a column of tabs from the
 * left of the content to the right keeps the width the user gave it, while moving it to the top is a
 * different question — two columns of tabs is a common thing to want, two rows much less so.
 */
public class ResourceTabLayout {
    /** Which side of the resource content the tab strip lives on. */
    public enum Placement {
        /** A column of tabs down the left; the content takes the rest of the width. */
        LEFT(true, true),
        /** A column of tabs down the right; the content takes the rest of the width. */
        RIGHT(true, false),
        /** A row of tabs across the top; the content takes the rest of the height. */
        TOP(false, true),
        /** A row of tabs across the bottom; the content takes the rest of the height. */
        BOTTOM(false, false);

        /** True when the strip is a column, so its thickness is a width and its tabs wrap into rows. */
        public final boolean isColumn;
        /** True when the strip comes before the content — above it, or to the left of it. */
        public final boolean isLeading;

        Placement(boolean isColumn, boolean isLeading) {
            this.isColumn = isColumn;
            this.isLeading = isLeading;
        }

        public static Placement byName(String name) {
            for (var placement : values()) {
                if (placement.name().equalsIgnoreCase(name)) return placement;
            }
            return LEFT;
        }

        /** The lower-case name a stylesheet knows this placement by. */
        public String styleName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Thick enough for exactly one tab in the built-in themes. Only the fallback: with no size stored
     * the strip takes whatever thickness the theme gives it, and never less than the tabs need — see
     * {@code ResourceView#updateStripMinimum}.
     */
    public static final float DEFAULT_STRIP_SIZE = 18;
    public static final float MAX_STRIP_SIZE = 160;
    /** Stored thickness meaning "no size of my own; take the theme's". */
    private static final float UNSET_STRIP_SIZE = 0;

    private static final String PLACEMENT_KEY = "placement";
    private static final String COLUMN_SIZE_KEY = "stripWidth";
    private static final String ROW_SIZE_KEY = "stripHeight";
    private static final String ORDER_KEY = "order";
    private static final String HIDDEN_KEY = "hidden";

    @Getter
    private Placement placement = Placement.LEFT;
    private float columnStripWidth = UNSET_STRIP_SIZE;
    private float rowStripHeight = UNSET_STRIP_SIZE;
    private final List<String> order = new ArrayList<>();
    private final Set<String> hidden = new LinkedHashSet<>();

    public void setPlacement(Placement placement) {
        this.placement = placement;
    }

    /**
     * The thickness the user gave the strip on the axis this placement measures it on — a width for a
     * column of tabs, a height for a row — or zero while they have never resized it.
     */
    public float getStripSize() {
        return placement.isColumn ? columnStripWidth : rowStripHeight;
    }

    /** Whether the user has given the strip a thickness, as opposed to leaving it to the theme. */
    public boolean hasStripSize() {
        return getStripSize() > UNSET_STRIP_SIZE;
    }

    public void setStripSize(float size) {
        var clamped = Math.min(size, MAX_STRIP_SIZE);
        if (placement.isColumn) {
            columnStripWidth = clamped;
        } else {
            rowStripHeight = clamped;
        }
    }

    /** Hands the thickness back to the theme. */
    public void clearStripSize() {
        if (placement.isColumn) {
            columnStripWidth = UNSET_STRIP_SIZE;
        } else {
            rowStripHeight = UNSET_STRIP_SIZE;
        }
    }

    /**
     * The remembered order of the resource tabs, by {@code Resource#getName()}. Resources not in it
     * are new since the order was recorded and keep their natural position after the ones that are.
     */
    public List<String> getOrder() {
        return List.copyOf(order);
    }

    public void setOrder(Collection<String> names) {
        order.clear();
        order.addAll(names);
    }

    public boolean isHidden(String resourceName) {
        return hidden.contains(resourceName);
    }

    public void setHidden(String resourceName, boolean isHidden) {
        if (isHidden) {
            hidden.add(resourceName);
        } else {
            hidden.remove(resourceName);
        }
    }

    public Set<String> getHidden() {
        return Set.copyOf(hidden);
    }

    /** Back to how a strip looks for a user who has never touched it. */
    public void reset() {
        placement = Placement.LEFT;
        columnStripWidth = UNSET_STRIP_SIZE;
        rowStripHeight = UNSET_STRIP_SIZE;
        order.clear();
        hidden.clear();
    }

    public CompoundTag serialize() {
        var tag = new CompoundTag();
        tag.putString(PLACEMENT_KEY, placement.name());
        tag.putFloat(COLUMN_SIZE_KEY, columnStripWidth);
        tag.putFloat(ROW_SIZE_KEY, rowStripHeight);
        tag.put(ORDER_KEY, toList(order));
        tag.put(HIDDEN_KEY, toList(hidden));
        return tag;
    }

    /**
     * Reads a strip arrangement back. Every field is optional: a tag written by an older version, or
     * one that has been hand-edited into nonsense, gives the defaults for whatever it is missing
     * rather than costing the user the rest of the arrangement.
     */
    public static ResourceTabLayout deserialize(CompoundTag tag) {
        var layout = new ResourceTabLayout();
        if (tag.contains(PLACEMENT_KEY, Tag.TAG_STRING)) {
            layout.placement = Placement.byName(tag.getString(PLACEMENT_KEY));
        }
        layout.columnStripWidth = readSize(tag, COLUMN_SIZE_KEY);
        layout.rowStripHeight = readSize(tag, ROW_SIZE_KEY);
        readList(tag, ORDER_KEY, layout.order::add);
        readList(tag, HIDDEN_KEY, layout.hidden::add);
        return layout;
    }

    /**
     * A stored thickness, or {@link #UNSET_STRIP_SIZE} when there is none — which is also the answer
     * for a nonsensical one, since "take the theme's" beats honouring a hand-edited zero.
     */
    private static float readSize(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return UNSET_STRIP_SIZE;
        var size = tag.getFloat(key);
        return size <= UNSET_STRIP_SIZE ? UNSET_STRIP_SIZE : Math.min(size, MAX_STRIP_SIZE);
    }

    private static ListTag toList(Collection<String> names) {
        var list = new ListTag();
        for (var name : names) {
            list.add(StringTag.valueOf(name));
        }
        return list;
    }

    private static void readList(CompoundTag tag, String key, Consumer<String> sink) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        var list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            var name = list.getString(i);
            if (!name.isEmpty()) {
                sink.accept(name);
            }
        }
    }
}
