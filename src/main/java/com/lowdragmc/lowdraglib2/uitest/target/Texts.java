package com.lowdragmc.lowdraglib2.uitest.target;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Best-effort "what does this element say" extraction, used by text assertions, tree snapshots and
 * the {@code withText} query filter.
 *
 * <p>Composite elements are handled by looking one level in: a {@link Button}'s caption lives on a
 * child {@link TextElement}, so asking a button for its text has to find that child or every
 * button-by-caption lookup fails.
 *
 * <p>Downstream elements register their own extractor rather than having this class grow a chain of
 * {@code instanceof} branches for types LDLib2 does not know about.
 */
public final class Texts {

    private static final Map<Class<?>, Function<UIElement, String>> EXTRACTORS = new LinkedHashMap<>();

    private Texts() {
    }

    /**
     * Teaches text extraction about a custom element type. Later registrations win for the same
     * class; lookup walks up the class hierarchy, so registering a base type covers its subclasses.
     */
    public static <T extends UIElement> void register(Class<T> type, Function<T, String> extractor) {
        @SuppressWarnings("unchecked")
        var erased = (Function<UIElement, String>) extractor;
        EXTRACTORS.put(type, erased);
    }

    /**
     * @return the element's visible text, or an empty string when it has none. Never {@code null},
     *         so callers can use it directly in {@code equals}/{@code contains} without guarding.
     */
    public static String of(UIElement element) {
        for (Class<?> type = element.getClass(); type != null; type = type.getSuperclass()) {
            var extractor = EXTRACTORS.get(type);
            if (extractor != null) {
                var text = extractor.apply(element);
                return text == null ? "" : text;
            }
        }
        if (element instanceof TextElement textElement) {
            return textElement.getText().getString();
        }
        if (element instanceof Button button) {
            return button.text.getText().getString();
        }
        if (element instanceof BindableUIElement<?> bindable) {
            var value = bindable.getValue();
            return value == null ? "" : String.valueOf(value);
        }
        // A wrapper around a single text child — the common "labelled control" shape.
        for (var child : element.getChildren()) {
            if (child instanceof TextElement textChild) {
                return textChild.getText().getString();
            }
        }
        return "";
    }

    /** The element's value, for elements that model one. {@code null} when it does not. */
    @Nullable
    public static Object valueOf(UIElement element) {
        return element instanceof BindableUIElement<?> bindable ? bindable.getValue() : null;
    }
}
