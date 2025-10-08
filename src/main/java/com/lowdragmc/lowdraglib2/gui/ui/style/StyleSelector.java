package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.NotNull;

public record StyleSelector(SelectorType type, String identifier) {
    public static StyleSelector parse(String raw) {
        if (raw.startsWith(".")) {
            return new StyleSelector(SelectorType.CLASS, raw.substring(1));
        } else if (raw.startsWith("#")) {
            return new StyleSelector(SelectorType.ID, raw.substring(1));
        } else if (raw.equals("*"))  {
            return new StyleSelector(SelectorType.UNIVERSAL, "*");
        } else {
            return new StyleSelector(SelectorType.ELEMENT, raw);
        }
    }

    public boolean matches(UIElement element) {
        return switch (type) {
            case CLASS -> element.hasClass(identifier);
            case ID -> identifier.equals(element.getId());
            case ELEMENT -> identifier.equals(element.getElementName());
            case UNIVERSAL -> true;
        };
    }

    public int weight() {
        return switch (type) {
            case CLASS -> 10;
            case ID -> 100;
            case ELEMENT -> 1;
            case UNIVERSAL -> 0;
        };
    }

    @Override
    public @NotNull String toString() {
        return switch (type) {
            case CLASS -> "." + identifier;
            case ID -> "#" + identifier;
            case ELEMENT -> identifier;
            case UNIVERSAL -> "*";
        };
    }
}
