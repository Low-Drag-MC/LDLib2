package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class StyleBag {
    public final UIElement element;
    public final Map<Property<?>, List<StyleSlot<?>>> candidates = new HashMap<>();
    public final Map<Property<?>, Object> computed = new HashMap<>();

    // runtime
    private int inlineSourceOrder = 0;
    private final BitSet dirtyProps = new BitSet(); // 按属性粒度
    private boolean dirty = true;
    private int lastStyleEpoch = -1;

    public StyleBag(UIElement element) {
        this.element = element;
    }

    public void moveInlineAsDefault() {
        inlineSourceOrder++;
        for (var entry : candidates.entrySet()) {
            var p = entry.getKey();
            var list = entry.getValue();
            for (int i = list.size() - 1; i >= 0; i--) {
                var slot = list.get(i);
                if (slot.origin() == StyleOrigin.INLINE) {
                    list.remove(i);
                    list.add(StyleSlot.of(
                            cast(p),
                            StyleOrigin.DEFAULT,
                            0, inlineSourceOrder,
                            slot.value()
                    ));
                    dirtyProps.set(p.id);
                }
            }
        }
        if (!dirtyProps.isEmpty()) {
            markDirty();
            element.onStyleChanged();
        }
    }

    public <T> void putCandidate(Property<T> p, StyleSlot<T> slot) {
        candidates.computeIfAbsent(p, k -> new ArrayList<>()).add(slot);
        dirtyProps.set(p.id);
        markDirty();
        element.onStyleChanged();
    }

    public <T> void replaceOrPutCandidate(Property<T> p, StyleSlot<T> slot) {
        var slots = candidates.get(p);
        if (slots != null) {
            var iterator = slots.iterator();
            while (iterator.hasNext()) {
                var existSlot = iterator.next();
                if (existSlot.typeEquals(slot)) {
                    if (existSlot.equals(slot)) return;
                    iterator.remove();
                    break;
                }
            }
        }
        putCandidate(p, slot);
    }

    public void putCandidates(Map<Property<?>, StyleValue<?>> values,
                              StyleOrigin origin,
                              int specificity, int sourceOrder) {
        if (values.isEmpty()) return;
        for (var entry : values.entrySet()) {
            var p = entry.getKey();
            var v = entry.getValue();
            candidates.computeIfAbsent(p, k -> new ArrayList<>()).add(StyleSlot.of(
                    cast(p),
                    origin,
                    specificity,
                    sourceOrder,
                    cast(v.compute())
            ));
            dirtyProps.set(p.id);
        }
        markDirty();
        element.onStyleChanged();
    }

    public boolean containsCandidate(Property<?> property, Predicate<StyleSlot<?>> predicate) {
        var slots = candidates.get(property);
        if (slots == null || slots.isEmpty()) return false;
        return slots.stream().anyMatch(predicate);
    }

    public void removeCandidates(Predicate<StyleSlot<?>> predicate) {
        var changed = false;
        for (var entry : candidates.entrySet()) {
            var p = entry.getKey();
            List<StyleSlot<?>> list = entry.getValue();
            if (list.removeIf(predicate)) {
                dirtyProps.set(p.id);
                markDirty();
                changed = true;
            }
        }
        if (changed) {
            candidates.values().removeIf(List::isEmpty);
            element.onStyleChanged();
        }
    }

    public void removeCandidates(Property<?> property, Predicate<StyleSlot<?>> predicate) {
        var slots = candidates.get(property);
        if (slots == null || slots.isEmpty()) return;
        if (slots.removeIf(predicate)) {
            dirtyProps.set(property.id);
            markDirty();
            candidates.values().removeIf(List::isEmpty);
            element.onStyleChanged();
        }
    }

    public void clearCandidates() {
        for (var p : candidates.keySet()) {
            dirtyProps.set(p.id);
        }
        candidates.clear();
        markDirty();
        element.onStyleChanged();
    }

    public void compute(int currentStyleEpoch) {
        if (!isDirty() && lastStyleEpoch == currentStyleEpoch) return;

        var old = new HashMap<Property<?>, Object>();

        for (int pid = dirtyProps.nextSetBit(0); pid >= 0; pid = dirtyProps.nextSetBit(pid + 1)) {
            var p = PropertyRegistry.byId(pid);
            if (p == null) continue;
            old.put(p, computed.get(p));
            computed.put(p, computeCandidate(p));
        }

        dirtyProps.clear();
        dirty = false;
        lastStyleEpoch = currentStyleEpoch;

        for (var entry : old.entrySet()) {
            var property = entry.getKey();
            Object oldValue = entry.getValue();
            Object newValue = computed.get(property);
            if (!Objects.equals(oldValue, newValue)) {
                property.notifyListeners(element, cast(oldValue), cast(newValue));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) { return (T) o; }

    public <T> T computeCandidate(Property<T> p) {
        List<StyleSlot<?>> list = candidates.get(p);
        if (list != null && !list.isEmpty()) {
            StyleSlot<?> best = list.getFirst();
            for (int i = 1; i < list.size(); i++) {
                StyleSlot<?> cur = list.get(i);
                if (StyleSlot.compare(best, cur) < 0) {
                    best = cur;
                }
            }
            return p.type.cast(best.value());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getComputed(Property<T> p) {
        return (T) computed.get(p);
    }

    public void markDirty() {
        if (!this.dirty) {
            var modularUI = element.getModularUI();
            if (modularUI != null) {
                modularUI.getStyleEngine().enqueue(this);
            }
            this.dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }
}