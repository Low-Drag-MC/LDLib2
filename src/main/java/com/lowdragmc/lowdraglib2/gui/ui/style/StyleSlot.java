package com.lowdragmc.lowdraglib2.gui.ui.style;

public record StyleSlot<T> (
        Property<T> property,
        StyleOrigin origin,
        int specificity,
        int sourceOrder,
        T value
) {

    public static <T> StyleSlot<T> of(Property<T> property, StyleOrigin origin, int specificity, int sourceOrder, T value) {
        return new StyleSlot<>(property, origin, specificity, sourceOrder, value);
    }

    public boolean typeEquals(StyleSlot<?> slot) {
        return slot.property == this.property &&
                slot.origin == this.origin &&
                slot.specificity == this.specificity &&
                slot.sourceOrder == this.sourceOrder;
    }

    public static int compare(StyleSlot<?> a, StyleSlot<?> b) {
        return a.compareTo(b);
    }

    public int compareTo(StyleSlot<?> o) {
        // compare priority, specificity, sourceOrder
        var c = Integer.compare(this.origin.priority, o.origin.priority);
        if (c != 0) return c;
        c = Integer.compare(this.specificity, o.specificity);
        if (c != 0) return c;
        return Integer.compare(this.sourceOrder, o.sourceOrder);
    }
}