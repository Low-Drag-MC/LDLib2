package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.function.Function;

@Data(staticConstructor = "of")
public final class UI {
    public final UIElement rootElement;
    @Nullable
    public final Function<Size, Size> dynamicSize;

    public static UI of(UIElement rootElement) {
        return of(rootElement, null);
    }

    public static UI of() {
        return of(new UIElement());
    }

    public UITemplate toTemplate() {
        return UITemplate.of(rootElement);
    }
}
