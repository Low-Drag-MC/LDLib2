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
    private final Map<String, StyleValue<?>> computedValues = new ConcurrentHashMap<>();
    private boolean isComputed = false;
    
    public StyleContext(UIElement element, Map<String, StyleValue<?>> inlineValues) {
        this.element = element;
        this.inlineValues = inlineValues;
    }

    public void loadStyleRules() {
        matchedRules.clear();
        collectMatchedRules();
        sortRulesBySpecificity();
        isComputed = false;
        calculateComputedValues();
        element.applyStyle(computedValues);
    }

    protected void collectMatchedRules() {
        if (element.getModularUI() != null) {
            for (var rule : element.getModularUI().ui.styleSheet.rules) {
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

    protected void calculateComputedValues() {
        computedValues.clear();
        applyMatchedRules();
        applyInlineStyles();
        isComputed = true;
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

}