package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.jetbrains.annotations.NotNull;

public record StyleSelector(SelectorType type, String identifier, SelectorScope scope) {
    public static StyleSelector parse(String raw) {
        var scope = SelectorScope.ALL;
        raw = raw.trim();
        var idx = raw.lastIndexOf(':');
        if (idx > 0) {
            scope = switch (raw.substring(idx + 1)) {
                case "host" -> SelectorScope.HOST;
                case "internal" -> SelectorScope.INTERNAL;
                default -> SelectorScope.ALL;
            };
            raw = raw.substring(0, idx);
        }
        if (raw.startsWith(".")) {
            return new StyleSelector(SelectorType.CLASS, raw.substring(1), scope);
        } else if (raw.startsWith("#")) {
            return new StyleSelector(SelectorType.ID, raw.substring(1), scope);
        } else if (raw.equals("*"))  {
            return new StyleSelector(SelectorType.UNIVERSAL, "*", scope);
        } else {
            return new StyleSelector(SelectorType.ELEMENT, raw, scope);
        }
    }

    public boolean matches(UIElement element) {
        if (!switch (scope) {
            case ALL -> true;
            case HOST -> !element.isInternalUI();
            case INTERNAL -> element.isInternalUI();
        }) return false;
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
