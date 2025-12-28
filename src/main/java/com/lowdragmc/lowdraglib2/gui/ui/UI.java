package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.style.HierarchicalStyleMatcher;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Data(staticConstructor = "of")
@KJSBindings
public final class UI {
    private static final UI EMPTY = UI.of(new UIElement());

    public static UI empty() {
        return EMPTY;
    }

    @FunctionalInterface
    public interface DynamicSizeProvider extends Function<Size, Size> {
        /**
         * Applies a transformation to the given screen size and returns a new {@code Size} object.
         *
         * @param screenSize the input size representing the dimensions of the screen
         * @return a new {@code Size} object representing the result of the transformation
         */
        @Override
        Size apply(Size screenSize);
    }
    public final UIElement rootElement;
    public final List<Stylesheet> stylesheets;
    @Nullable
    public final DynamicSizeProvider dynamicSize;

    public static UI of(UIElement rootElement) {
        return of(rootElement, Collections.emptyList(), null);
    }

    public static UI of(UIElement rootElement, List<Stylesheet> stylesheets) {
        return of(rootElement, stylesheets, null);
    }

    public static UI of(UIElement rootElement, Stylesheet... stylesheets) {
        return of(rootElement, Arrays.stream(stylesheets).toList(), null);
    }

    public static UI of(UIElement rootElement, @Nullable DynamicSizeProvider dynamicSize) {
        return of(rootElement, Collections.emptyList(), dynamicSize);
    }

    public static UI of() {
        return of(new UIElement());
    }

    public UITemplate toTemplate() {
        return UITemplate.of(rootElement);
    }

    /**
     * Selects and retrieves a stream of {@link UIElement} objects that match the specified selector.
     * The method analyzes the hierarchy of {@code rootElement}'s children and filters elements based on the provided selector.
     *
     * @param selector the CSS-like selector used to filter the {@link UIElement} objects
     * @return a {@link Stream} of {@link UIElement} objects that match the given selector
     */
    public Stream<UIElement> select(String selector) {
        var match = HierarchicalStyleMatcher.parse(selector);
        return rootElement.selfAndAllChildren().filter(match::matches);
    }

    public <T> Stream<T> select(String selector, Class<T> type) {
        return select(selector).filter(type::isInstance).map(type::cast);
    }

    /**
     * Selects and retrieves a stream of {@link UIElement} objects whose IDs match the specified regular expression.
     * The method evaluates all the children, including the {@code rootElement} itself,
     * and filters elements based on their IDs using the provided regex pattern.
     *
     * @param regex the regular expression used to match the {@code id} of {@link UIElement} objects
     * @return a {@link Stream} of {@link UIElement} objects whose IDs satisfy the given regex
     */
    public Stream<UIElement> selectRegex(String regex) {
        var pattern = Pattern.compile(regex);
        return rootElement.selfAndAllChildren().filter(element -> pattern.matcher(element.getId()).find());
    }

    public <T> Stream<T> selectRegex(String regex, Class<T> type) {
        return selectRegex(regex).filter(type::isInstance).map(type::cast);
    }
}
