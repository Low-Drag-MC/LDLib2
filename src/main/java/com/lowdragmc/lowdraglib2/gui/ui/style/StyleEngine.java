package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StyleEngine {
    public final ModularUI modularUI;
    public final List<Stylesheet> globalSheets = new ArrayList<>();

    // runtime
    private final Set<UIElement> dirtyElements = ConcurrentHashMap.newKeySet();
    private final HashSet<StyleBag> queue = new HashSet<>();
    private int styleEpoch = 0;
    @Getter
    private final Map<UIElement, Map<Stylesheet, List<StyleRule>>> elementStyleRules = new ConcurrentHashMap<>();

    public StyleEngine(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    public void addStylesheets(Stylesheet... stylesheets) {
        for (Stylesheet sheet : stylesheets) {
            addStylesheet(sheet);
        }
    }

    public void addStylesheets(List<Stylesheet> stylesheets) {
        stylesheets.forEach(this::addStylesheet);
    }

    public void addStylesheet(Stylesheet stylesheet) {
        globalSheets.add(stylesheet);
        for (UIElement element : modularUI.getAllElements()) {
            var rules = stylesheet.calculateValues(element);
            if (!rules.isEmpty()) {
                elementStyleRules.computeIfAbsent(element, e -> new ConcurrentHashMap<>()).put(stylesheet, rules);
                element.addStyleRules(rules);
            }
        }
    }

    public void removeStylesheet(Stylesheet sheet) {
        globalSheets.remove(sheet);
        for (var entry : elementStyleRules.entrySet()) {
            var rules = entry.getValue().remove(sheet);
            if (rules != null) {
                entry.getKey().removeStyleRules(rules);
            }
        }
    }

    public void clearAllStylesheets() {
        globalSheets.clear();
        for (var element : elementStyleRules.keySet()) {
            element.removeAllRules();
        }
        elementStyleRules.clear();
    }

    public void enqueue(StyleBag bag) {
        queue.add(bag);
    }

    public boolean inQueue(StyleBag bag) {
        return queue.contains(bag);
    }

    public boolean requireCalculate() {
        return !queue.isEmpty() || !dirtyElements.isEmpty();
    }

    public void remove(StyleBag bag) {
        queue.remove(bag);
    }

    public void calculateStyle() {
        dirtyElements.forEach(this::updateElementStyle);
        dirtyElements.clear();

        if (queue.isEmpty()) return;
        styleEpoch++;
        var bags = new ArrayList<>(queue);
        queue.clear();
        for (StyleBag bag : bags) {
            bag.compute(styleEpoch);
        }
    }

    public void onElementRegister(UIElement element) {
        enqueue(element.getStyleBag());
        for (var stylesheet : globalSheets) {
            var rules = stylesheet.calculateValues(element);
            if (!rules.isEmpty()) {
                elementStyleRules.computeIfAbsent(element, e -> new ConcurrentHashMap<>()).put(stylesheet, rules);
                element.addStyleRules(rules);
            }
        }
    }

    public void onElementUnregister(UIElement element) {
        if (elementStyleRules.containsKey(element)) {
            remove(element.getStyleBag());
            element.removeAllRules();
            elementStyleRules.remove(element);
        }
    }

    public void scheduleReloadElementStyles(UIElement element) {
        if (dirtyElements.contains(element)) return;
        dirtyElements.add(element);

        // 4. update children
        for (var child : element.getSafeChildren()) {
            scheduleReloadElementStyles(child);
        }
    }

    private void updateElementStyle(UIElement element) {
        if (element.getModularUI() != modularUI) return;
        // 1. cache old for comparison
        List<StyleRule> oldRules = new ArrayList<>();
        var currentRulesMap = elementStyleRules.get(element);
        if (currentRulesMap != null) {
            for (List<StyleRule> rules : currentRulesMap.values()) {
                oldRules.addAll(rules);
            }
        }

        // 2. calculate new style rules
        Map<Stylesheet, List<StyleRule>> newRulesMap = new HashMap<>();
        List<StyleRule> newRules = new ArrayList<>();

        for (var stylesheet : globalSheets) {
            var rules = stylesheet.calculateValues(element);
            if (!rules.isEmpty()) {
                newRulesMap.put(stylesheet, rules);
                newRules.addAll(rules);
            }
        }

        // 3. compare old and new rules
        boolean rulesChanged = !oldRules.equals(newRules);

        if (rulesChanged) {
            // A. clear old rules
            if (!oldRules.isEmpty()) {
                element.removeStyleRules(oldRules);
            }

            // B. update rules map
            if (newRulesMap.isEmpty()) {
                elementStyleRules.remove(element);
            } else {
                elementStyleRules.put(element, new ConcurrentHashMap<>(newRulesMap));
            }

            // C. apply new rules
            if (!newRules.isEmpty()) {
                element.addStyleRules(newRules);
                enqueue(element.getStyleBag());
            }
        } else {
            // ok, nothing changes
        }
    }
}