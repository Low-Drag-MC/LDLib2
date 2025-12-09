package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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

    public static UI of(UIElement rootElement, @Nullable DynamicSizeProvider dynamicSize) {
        return of(rootElement, Collections.emptyList(), dynamicSize);
    }

    public static UI of() {
        return of(new UIElement());
    }

    public UITemplate toTemplate() {
        return UITemplate.of(rootElement);
    }
}
