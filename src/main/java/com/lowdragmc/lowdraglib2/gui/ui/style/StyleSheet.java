package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;

import java.util.*;

public final class StyleSheet {
    public final List<StyleRule> rules = new ArrayList<>();

    // runtime: specificity -> list of (matcher, rule)
    public final TreeMap<Integer, List<Map.Entry<HierarchicalStyleMatcher, StyleRule>>> buckets = new TreeMap<>();

    private void addToBucket(HierarchicalStyleMatcher m, StyleRule r) {
        buckets.computeIfAbsent(m.getSpecificity(), k -> new ArrayList<>()).add(Map.entry(m, r));
    }

    public void addRule(StyleRule rule) {
        rules.add(rule);
        for (HierarchicalStyleMatcher m : rule.getMatchers()) {
            addToBucket(m, rule);
        }
    }

    public void removeRule(StyleRule rule) {
        rules.remove(rule);
        var it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            e.getValue().removeIf(pair -> pair.getValue() == rule);
            if (e.getValue().isEmpty()) it.remove();
        }
    }


    public void clear() {
        rules.clear();
        buckets.clear();
    }

    public void merge(StyleSheet other) {
        for (StyleRule r : other.rules) {
            addRule(r);
        }
    }

    /**
     * Calculates a map of style property values for a given UI element by resolving
     * applicable style rules from the stylesheet.
     *
     * This method evaluates all applicable style rules for the specified UI element,
     * matches them using hierarchical style matchers, and combines the resulting style
     * property values into an unmodifiable map.
     *
     * @param element the UIElement for which style values need to be calculated
     * @return an unmodifiable map containing style property names as keys and their corresponding values as StyleValue<?> instances
     */
    public Map<String, StyleValue<?>> calculateValues(UIElement element) {
        var styleValues = new HashMap<String, StyleValue<?>>();
        for (var entry : buckets.entrySet()) {
            List<Map.Entry<HierarchicalStyleMatcher, StyleRule>> list = entry.getValue();
            for (var pair : list) {
                var m = pair.getKey();
                var r = pair.getValue();
                if (m.matches(element)) {
                    styleValues.putAll(r.getProperties());
                }
            }
        }
        return Collections.unmodifiableMap(styleValues);
    }
}