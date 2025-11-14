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
        return !queue.isEmpty();
    }

    public void remove(StyleBag bag) {
        queue.remove(bag);
    }

    public void calculateStyle() {
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

    public void reloadElementStyles(UIElement element) {
        onElementUnregister(element);
        onElementRegister(element);
    }
}