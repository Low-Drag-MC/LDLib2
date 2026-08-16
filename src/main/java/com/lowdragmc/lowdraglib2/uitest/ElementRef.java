package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.uitest.target.ElementPath;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A resolved element plus everything a step or a report needs to talk about it: where it is on
 * screen, what it says, and a stable name for it.
 *
 * <p>Resolution is a snapshot. Bounds are read live from the element each time {@link #bounds()} is
 * called, so a ref held across steps stays accurate as long as the element is still in the tree.
 *
 * @param element  the live element
 * @param selector the selector or query description it was resolved from, for error messages
 */
public record ElementRef(UIElement element, String selector) {

    /** @throws IllegalStateException if the element is not of the expected type */
    public <T extends UIElement> T as(Class<T> type) {
        if (!type.isInstance(element)) {
            throw new IllegalStateException("Element '" + selector + "' resolved to "
                    + element.getElementName() + ", not a " + type.getSimpleName());
        }
        return type.cast(element);
    }

    /** The stable structural id — see {@link ElementPath}. */
    public String path() {
        return ElementPath.of(element);
    }

    /** Screen-space rectangle. See {@link ElementBounds#of} for why this is not the layout rect. */
    public ElementBounds bounds() {
        return ElementBounds.of(element);
    }

    public String text() {
        return Texts.of(element);
    }

    @Nullable
    public Object value() {
        return Texts.valueOf(element);
    }

    public boolean isVisible() {
        return element.isVisible() && element.isDisplayed();
    }

    public boolean isActive() {
        return element.isActive();
    }

    public boolean isFocused() {
        return element.hasClass("__focused__");
    }

    public boolean isHovered() {
        return element.hasClass("__hovered__");
    }

    public boolean hasClass(String styleClass) {
        return element.hasClass(styleClass);
    }

    /** Declared classes, excluding runtime {@code __state__} ones. */
    public List<String> classes() {
        return ElementPath.stableClasses(element);
    }

    /** Only the runtime {@code __state__} classes, e.g. {@code __hovered__}. */
    public Set<String> stateClasses() {
        return element.getClasses().stream()
                .filter(ElementPath::isStateClass)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return selector + " -> " + element.getElementName()
                + (element.getId().isEmpty() ? "" : "#" + element.getId());
    }
}
