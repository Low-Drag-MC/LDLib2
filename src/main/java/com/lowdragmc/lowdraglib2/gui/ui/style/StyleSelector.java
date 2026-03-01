package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.NotNull;

public record StyleSelector(SelectorType type, Either<String, HierarchicalStyleMatcher> identity, SelectorScope scope) {

    public static StyleSelector parseNotSelector(String raw) {
        return new StyleSelector(SelectorType.NOT, Either.right(HierarchicalStyleMatcher.parse(raw)), SelectorScope.ALL);
    }

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
            return new StyleSelector(SelectorType.CLASS, Either.left(raw.substring(1)), scope);
        } else if (raw.startsWith("#")) {
            return new StyleSelector(SelectorType.ID, Either.left(raw.substring(1)), scope);
        } else if (raw.equals("*"))  {
            return new StyleSelector(SelectorType.UNIVERSAL, Either.left("*"), scope);
        } else {
            return new StyleSelector(SelectorType.ELEMENT, Either.left(raw), scope);
        }
    }

    public boolean matches(UIElement element) {
        if (!switch (scope) {
            case ALL -> true;
            case HOST -> !element.isInternalUI();
            case INTERNAL -> element.isInternalUI();
        }) return false;
        return identity.map(left -> switch (type) {
            case CLASS -> element.hasClass(left);
            case ID -> left.equals(element.getId());
            case ELEMENT -> left.equals(element.getElementName());
            default -> true;
        }, right -> !right.matches(element));
    }

    public int weight() {
        return switch (type) {
            case NOT -> 1000;
            case CLASS -> 10;
            case ID -> 100;
            case ELEMENT -> 1;
            case UNIVERSAL -> 0;
        };
    }

    @Override
    public @NotNull String toString() {
        return identity.map(left -> switch (type) {
            case CLASS -> {
                if (left.length() > 4 && left.startsWith("__") && left.endsWith("__")) {
                    var clazz = left.substring(2, left.length() - 2);
                    if (clazz.equals("hovered")) {
                        yield ":hover";
                    }
                    yield ":" + clazz;
                }
                yield "." + left;
            }
            case ID -> "#" + left;
            case ELEMENT -> left;
            default -> "*";
        }, right -> ":not(" + right + ")") + switch (scope) {
            case ALL -> "";
            case HOST -> ":host";
            case INTERNAL -> ":internal";
        };
    }
}
