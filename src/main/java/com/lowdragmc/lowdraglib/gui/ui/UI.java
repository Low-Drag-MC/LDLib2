package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleSheet;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;
import java.util.function.Function;

@Data(staticConstructor = "of")
public final class UI {
    public final UIElement rootElement;
    public final StyleSheet styleSheet;
    @Nullable
    public final Function<Size, Size> dynamicSize;

    public static UI of(UIElement rootElement, @Nullable Function<Size, Size> dynamicSize) {
        return of(rootElement, new StyleSheet(), dynamicSize);
    }

    public static UI of(UIElement rootElement) {
        return of(rootElement, null);
    }

    public static UI of() {
        return of(new UIElement());
    }

}
