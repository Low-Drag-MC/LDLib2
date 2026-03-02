package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.TransitionAnimation;
import com.lowdragmc.lowdraglib2.gui.ui.style.properties.TransitionProperty;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class StyleBag {
    public final UIElement element;
    public final Map<Property<?>, List<StyleSlot<?>>> candidates = new HashMap<>();

    // runtime
    private int inlineSourceOrder = 0;
    private final BitSet dirtyProps = new BitSet();
    private boolean dirty = true;
    private int lastStyleEpoch = -1;
    private final Map<Property<?>, StyleSlot<?>> computedSlots = new HashMap<>();
    private final Map<Property<?>, TransitionAnimation<?>> transitionAnimations = new HashMap<>();

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

        var old = new HashMap<Property<?>, StyleSlot<?>>();

        for (int pid = dirtyProps.nextSetBit(0); pid >= 0; pid = dirtyProps.nextSetBit(pid + 1)) {
            var p = PropertyRegistry.byId(pid);
            if (p == null) continue;
            old.put(p, computedSlots.get(p));
            computedSlots.put(p, computeCandidateSlot(p));
        }

        dirtyProps.clear();
        dirty = false;

        var transition = getComputed(PropertyRegistry.TRANSITION);
        if (transition == null) transition = PropertyRegistry.TRANSITION.initialValue;

        for (var entry : old.entrySet()) {
            var property = entry.getKey();
            var oldSlot = entry.getValue();
            var newSlot = computedSlots.get(property);
            var oldValue = oldSlot == null ? null : oldSlot.value();
            var newValue = newSlot == null ? null : newSlot.value();
            if (!Objects.equals(oldValue, newValue)) {
                // apply transition while changes
                var animation = transition.animations().get(property);
                if (lastStyleEpoch > -1 && animation != null && TransitionProperty.shouldTriggerTransition(cast(property), cast(oldSlot), cast(newSlot))) {
                    var transitionAnimation = new TransitionAnimation<>(this, property, animation);
                    var from = oldValue == null ? property.initialValue : oldValue;
                    var to = newValue == null ? property.initialValue : newValue;
                    if (transitionAnimations.containsKey(property)) {
                        var anim = transitionAnimations.get(property);
                        var current = anim.getCurrentValue();
                        if (current != null) {
                            from = current;
                        }
                        anim.stop();
                    }

                    transitionAnimation.play(
                            cast(from),
                            cast(to)
                    );
                    transitionAnimations.put(property, transitionAnimation);
                } else {
                    property.notifyListeners(element, cast(oldValue), cast(newValue));
                }
            }
        }
        lastStyleEpoch = currentStyleEpoch;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) { return (T) o; }

    public <T> StyleSlot<T> computeCandidateSlot(Property<T> p) {
        List<StyleSlot<?>> list = candidates.get(p);
        if (list != null && !list.isEmpty()) {
            var best = list.getFirst();
            for (int i = 1; i < list.size(); i++) {
                StyleSlot<?> cur = list.get(i);
                if (StyleSlot.compare(best, cur) < 0) {
                    best = cur;
                }
            }
            return cast(best);
        }
        return null;
    }

    public <T> T computeCandidate(Property<T> p) {
        var slot = computeCandidateSlot(p);
        if (slot != null) return slot.value();
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getComputed(Property<T> p) {
        var transited = transitionAnimations.get(p);
        if (transited != null) {
            return (T) transited.getCurrentValue();
        }
        var computedSlot = computedSlots.get(p);
        return computedSlot == null ? null : (T) computedSlot.value();
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

    public <T> void onTransitionUpdate(TransitionAnimation<T> transitionAnimation, T oldValue, T newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            transitionAnimation.property.notifyListeners(element, oldValue, newValue);
        }
    }

    public <T> void onTransitionFinished(TransitionAnimation<T> transitionAnimation) {
        transitionAnimations.remove(transitionAnimation.property);
    }

    /**
     * Atomically removes animation candidates matching the predicate and puts the final slot,
     * triggering markDirty and onStyleChanged only once. Used by StyleAnimation.onFinished.
     */
    public <T> void replaceAnimationFinal(Property<T> p, Predicate<StyleSlot<?>> removePredicate, StyleSlot<T> finalSlot) {
        var slots = candidates.computeIfAbsent(p, k -> new ArrayList<>());
        slots.removeIf(removePredicate);
        // replaceOrPut the final slot
        var iterator = slots.iterator();
        while (iterator.hasNext()) {
            var existSlot = iterator.next();
            if (existSlot.typeEquals(finalSlot)) {
                if (existSlot.equals(finalSlot)) {
                    // already correct, just mark once
                    dirtyProps.set(p.id);
                    markDirty();
                    candidates.values().removeIf(List::isEmpty);
                    element.onStyleChanged();
                    return;
                }
                iterator.remove();
                break;
            }
        }
        slots.add(finalSlot);
        dirtyProps.set(p.id);
        markDirty();
        candidates.values().removeIf(List::isEmpty);
        element.onStyleChanged();
    }

    /**
     * Update an animation-driven property value directly without triggering a full dirty cycle.
     * This bypasses StyleSlot allocation and markDirty, notifying listeners directly.
     * Used by {@link com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleAnimation} per-frame.
     */
    @SuppressWarnings("unchecked")
    public <T> void onAnimationUpdate(StyleOrigin animationOrigin, Property<T> p, T value) {
        var slots = candidates.computeIfAbsent(p, k -> new ArrayList<>());
        StyleSlot<T> oldComputed = cast(computedSlots.get(p));
        for (int i = 0; i < slots.size(); i++) {
            var existSlot = slots.get(i);
            if (existSlot.origin() == animationOrigin
                    && existSlot.specificity() == 999
                    && existSlot.sourceOrder() == 0) {
                T oldValue = (T) existSlot.value();
                if (!Objects.equals(oldValue, value)) {
                    var animationSlot = new StyleSlot<>(p, animationOrigin, 999, 0, value);
                    slots.set(i, animationSlot);
                    animationUpdateCompute(p, oldComputed);
                }
                return;
            }
        }
        // First frame: no slot yet, insert and notify
        var animationSlot = new StyleSlot<>(p, animationOrigin, 999, 0, value);
        slots.add(animationSlot);
        animationUpdateCompute(p, oldComputed);
    }

    private <T> void animationUpdateCompute(Property<T> p, StyleSlot<T> oldComputed) {
        var computed = computeCandidateSlot(p);
        if (computed == null) {
            computedSlots.remove(p);
        } else {
            computedSlots.put(p, computed);
        }
        T oldComputedValue = oldComputed == null ? null : oldComputed.value();
        T newComputedValue = computed == null ? null : cast(computed.value());
        if (!Objects.equals(oldComputedValue, newComputedValue)) {
            p.notifyListeners(element, oldComputedValue, newComputedValue);
        }
    }
}
