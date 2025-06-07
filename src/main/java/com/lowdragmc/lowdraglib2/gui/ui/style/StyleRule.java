package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class StyleRule {
    public enum SelectorType { CLASS, ID, ELEMENT, UNIVERSAL }

    private final SelectorType type;
    private final String identifier;
    private final Map<String, StyleValue<?>> properties = new ConcurrentHashMap<>();

    public StyleRule(SelectorType type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    public void addProperty(String name, StyleValue<?> value) {
        properties.put(name, value);
    }

    public boolean matches(UIElement element) {
        return switch (type) {
            case CLASS -> element.hasClass(identifier);
            case ID -> identifier.equals(element.getId());
            case ELEMENT -> identifier.equals(element.getElementName());
            case UNIVERSAL -> true;
        };
    }
}