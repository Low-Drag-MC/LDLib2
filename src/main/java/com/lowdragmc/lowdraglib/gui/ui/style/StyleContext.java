package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.style.value.StyleValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StyleContext {
    private final UIElement element;
    private final List<StyleRule> matchedRules = new ArrayList<>();
    private final Map<String, StyleValue<?>> inlineValues;
    private final Map<String, StyleValue<?>> customValues = new ConcurrentHashMap<>();
    private final Map<String, StyleValue<?>> computedValues = new ConcurrentHashMap<>();
    private boolean isComputed = false;
    
    public StyleContext(UIElement element, Map<String, StyleValue<?>> inlineValues) {
        this.element = element;
        this.inlineValues = inlineValues;
    }

    protected void loadStyleRules() {
        matchedRules.clear();
        collectMatchedRules();
        sortRulesBySpecificity();
    }

    protected void collectMatchedRules() {
        if (element.getModularUI() != null) {
            for (var rule : element.getModularUI().getStyleSheet().rules) {
                if (rule.matches(element)) {
                    matchedRules.add(rule);
                }
            }
        }
    }

    protected void sortRulesBySpecificity() {
        matchedRules.sort((a, b) ->
            Integer.compare(calculateSpecificity(b), calculateSpecificity(a)));
    }
    
    protected int calculateSpecificity(StyleRule rule) {
        return switch (rule.getType()) {
            case ID -> 100;
            case CLASS -> 10;
            case ELEMENT -> 1;
            case UNIVERSAL -> 0;
        };
    }

    protected Map<String, StyleValue<?>> getComputedValues() {
        if (!isComputed) {
            calculateComputedValues();
        }
        return computedValues;
    }

    protected void calculateComputedValues() {
        var removedValues = new HashMap<>(computedValues);
        computedValues.clear();
        applyInheritedProperties();
        applyMatchedRules();
        applyInlineStyles();
        applyCustomStyles();
        isComputed = true;
        removedValues.keySet().removeAll(computedValues.keySet());
        removedValues.forEach((key, value) -> element.applyStyle(key, null));
        computedValues.forEach(element::applyStyle);
    }

    protected void applyInheritedProperties() {
        if (element.getParent() == null) return;
        element.getParent().getStyleContext().getComputedValues().forEach((key, value) -> {
            if (InheritableProperties.isInheritable(key) && element.supportStyle(key)) {
                computedValues.put(key, value);
            }
        });
    }

    protected void applyMatchedRules() {
        for (StyleRule rule : matchedRules) {
            for (var entry : rule.getProperties().entrySet()) {
                if (element.supportStyle(entry.getKey())) {
                    computedValues.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected void applyInlineStyles() {
        for (var entry : inlineValues.entrySet()) {
            if (element.supportStyle(entry.getKey())) {
                computedValues.put(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void applyCustomStyles() {
        for (var entry : customValues.entrySet()) {
            if (element.supportStyle(entry.getKey())) {
                computedValues.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addCustomStyle(String key, StyleValue<?> value) {
        customValues.put(key, value);
    }

    public StyleValue<?> removeCustomStyle(String key) {
        return customValues.get(key);
    }
}